package com.raave.filament.ui.home

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raave.filament.data.auth.AuthRepository
import com.raave.filament.data.auth.AuthTokenStore
import com.raave.filament.data.glpi.GlpiFollowup
import com.raave.filament.data.glpi.GlpiRepository
import com.raave.filament.data.glpi.GlpiTicketDetail
import com.raave.filament.data.glpi.GlpiTicketSummary
import com.raave.filament.data.glpi.PendingAttachment
import com.raave.filament.util.DeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStore = AuthTokenStore(application)
    private val repository = AuthRepository(tokenStore = tokenStore)
    private val glpiRepository = GlpiRepository(tokenStore = tokenStore)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadSession()
        refreshBlurState()
        loadChamados()
    }

    private fun loadSession() {
        viewModelScope.launch {
            repository.getCurrentSession().fold(
                onSuccess = { body ->
                    val user = body.optJSONObject("user")
                    if (user == null) {
                        // Token guardado não corresponde mais a uma sessão válida no backend.
                        signOutLocally()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                userName = user.optString("name").takeIf(String::isNotBlank),
                                userEmail = user.optString("email").takeIf(String::isNotBlank),
                            )
                        }
                    }
                },
                onFailure = {
                    signOutLocally()
                },
            )
        }
    }

    /**
     * Sem isso, LoginActivity via o token antigo ainda salvo, achava que a sessão continuava
     * válida e mandava de volta pra HomeActivity — que checava de novo, falhava de novo, e
     * devolvia pra Login: loop infinito entre as duas telas sem gerar exceção nenhuma pra
     * aparecer no logcat, sem jeito de o usuário logar de novo a não ser desinstalando o app.
     */
    private fun signOutLocally() {
        tokenStore.clear()
        _uiState.update { it.copy(isLoading = false, signedOut = true) }
    }

    /**
     * Reavaliado a cada retomada da tela: o modo de economia de bateria pode ser ligado enquanto
     * o app está aberto.
     */
    fun refreshBlurState() {
        val supported = DeviceUtils.isBlurSupported(getApplication())
        _uiState.update { it.copy(isBlurEnabled = supported) }
    }

    fun onTabSelected(tab: HomeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onSignOutClick() {
        viewModelScope.launch {
            repository.signOut()
            _uiState.update { it.copy(signedOut = true) }
        }
    }

    fun onNewTicketButtonClick() {
        _uiState.update {
            it.copy(isNewTicketDialogOpen = true, newTicketName = "", newTicketContent = "", newTicketError = null)
        }
    }

    fun onNewTicketDismiss() {
        _uiState.update { it.copy(isNewTicketDialogOpen = false, newTicketError = null) }
    }

    fun onNewTicketNameChange(name: String) {
        _uiState.update { it.copy(newTicketName = name, newTicketError = null) }
    }

    fun onNewTicketContentChange(content: String) {
        _uiState.update { it.copy(newTicketContent = content, newTicketError = null) }
    }

    fun onNewTicketSubmit() {
        val state = _uiState.value
        val name = state.newTicketName.trim()
        val content = state.newTicketContent.trim()
        if (name.isEmpty() || content.isEmpty()) {
            _uiState.update { it.copy(newTicketError = "Preencha o título e a descrição") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingTicket = true, newTicketError = null) }
            glpiRepository.createTicket(name, content).fold(
                onSuccess = { ticketId ->
                    _uiState.update {
                        it.copy(
                            isCreatingTicket = false,
                            isNewTicketDialogOpen = false,
                            newTicketName = "",
                            newTicketContent = "",
                            lastCreatedTicketId = ticketId,
                            selectedTab = HomeTab.CHAMADOS,
                        )
                    }
                    loadChamados()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isCreatingTicket = false, newTicketError = error.message) }
                },
            )
        }
    }

    fun loadChamados() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChamadosLoading = true, chamadosError = null) }
            glpiRepository.getTickets().fold(
                onSuccess = { tickets ->
                    _uiState.update {
                        it.copy(
                            isChamadosLoading = false,
                            chamados = tickets,
                            chamadosBadgeCount = tickets.size.takeIf { count -> count > 0 },
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isChamadosLoading = false, chamadosError = error.message) }
                },
            )
        }
    }

    fun onTicketClick(ticket: GlpiTicketSummary) {
        _uiState.update { it.copy(chatState = ChatUiState(ticketId = ticket.id, ticketName = ticket.name)) }
        loadFollowups(ticket.id)
    }

    fun onChatBackClick() {
        _uiState.update { it.copy(chatState = null) }
    }

    fun onChatRetryClick() {
        _uiState.value.chatState?.let { loadFollowups(it.ticketId) }
    }

    fun onChatMessageChange(text: String) {
        updateChat { it.copy(draftMessage = text, sendError = null) }
    }

    fun onChatSendClick() {
        val chat = _uiState.value.chatState ?: return
        val content = chat.draftMessage.trim()
        if (content.isEmpty() && chat.pendingAttachments.isEmpty()) return
        viewModelScope.launch {
            updateChat(chat.ticketId) { it.copy(isSending = true, sendError = null) }
            glpiRepository.sendFollowup(chat.ticketId, content, chat.pendingAttachments).fold(
                onSuccess = { followup ->
                    updateChat(chat.ticketId) {
                        it.copy(
                            isSending = false,
                            draftMessage = "",
                            pendingAttachments = emptyList(),
                            followups = it.followups + followup,
                        )
                    }
                },
                onFailure = { error ->
                    updateChat(chat.ticketId) { it.copy(isSending = false, sendError = error.message) }
                },
            )
        }
    }

    /** Lê cada arquivo escolhido pro fim (nome, tipo, bytes) — o resto do app não lida com `Uri`/`ContentResolver`. */
    fun onAttachmentsPicked(uris: List<Uri>) {
        val chat = _uiState.value.chatState ?: return
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            val results = uris.map { uri -> readAttachment(resolver, uri) }
            val read = results.mapNotNull { it.attachment }
            val oversized = results.mapNotNull { it.oversizedFileName }
            updateChat(chat.ticketId) {
                it.copy(
                    pendingAttachments = it.pendingAttachments + read,
                    attachmentError = oversized.firstOrNull()?.let { name ->
                        "Arquivo muito grande (máx. ${MAX_ATTACHMENT_SIZE_BYTES / (1024 * 1024)}MB): $name"
                    },
                )
            }
        }
    }

    fun onChatRemoveAttachment(attachmentId: String) {
        updateChat { it.copy(pendingAttachments = it.pendingAttachments.filterNot { a -> a.id == attachmentId }) }
    }

    private class AttachmentReadResult(val attachment: PendingAttachment?, val oversizedFileName: String?)

    private suspend fun readAttachment(resolver: ContentResolver, uri: Uri): AttachmentReadResult =
        withContext(Dispatchers.IO) {
            val fileName = queryDisplayName(resolver, uri) ?: uri.lastPathSegment ?: "arquivo"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext AttachmentReadResult(null, null)
            if (bytes.size > MAX_ATTACHMENT_SIZE_BYTES) {
                return@withContext AttachmentReadResult(null, fileName)
            }
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
            AttachmentReadResult(PendingAttachment(fileName = fileName, mimeType = mimeType, bytes = bytes), null)
        }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
        }
        return null
    }

    /**
     * O GLPI não trata a mensagem de abertura do chamado (campo `content` do ticket) como um
     * followup — sem isso ela nunca aparece no chat. Busca os dois em paralelo e prefixa a
     * abertura na lista, já que o requerente do chamado é sempre o usuário logado.
     */
    private fun loadFollowups(ticketId: Long) {
        viewModelScope.launch {
            updateChat(ticketId) { it.copy(isLoading = true, loadError = null) }
            val ticketDeferred = async { glpiRepository.getTicket(ticketId) }
            glpiRepository.getFollowups(ticketId).fold(
                onSuccess = { followups ->
                    val opening = ticketDeferred.await().getOrNull()?.toOpeningMessageOrNull(
                        authorName = _uiState.value.userName,
                        authorEmail = _uiState.value.userEmail,
                    )
                    updateChat(ticketId) {
                        it.copy(isLoading = false, followups = listOfNotNull(opening) + followups)
                    }
                },
                onFailure = { error ->
                    ticketDeferred.cancel()
                    updateChat(ticketId) { it.copy(isLoading = false, loadError = error.message) }
                },
            )
        }
    }

    /**
     * `id = 0L`: sentinela — os followups reais do GLPI começam em 1, então nunca colide.
     * `isMine = true` sempre: a listagem de chamados já é escopada ao usuário logado, então quem
     * abriu o chamado (dono do `content`) é sempre ele.
     */
    private fun GlpiTicketDetail.toOpeningMessageOrNull(authorName: String?, authorEmail: String?): GlpiFollowup? {
        val text = content?.takeIf(String::isNotBlank) ?: return null
        return GlpiFollowup(
            id = 0L,
            content = text,
            date = date,
            authorName = authorName,
            authorEmail = authorEmail,
            isMine = true,
        )
    }

    private fun updateChat(transform: (ChatUiState) -> ChatUiState) {
        _uiState.value.chatState?.let { updateChat(it.ticketId, transform) }
    }

    /** Só aplica se o chat aberto ainda for o mesmo chamado — evita corrida se o usuário já voltou pra lista. */
    private fun updateChat(ticketId: Long, transform: (ChatUiState) -> ChatUiState) {
        _uiState.update { state ->
            val chat = state.chatState
            if (chat != null && chat.ticketId == ticketId) state.copy(chatState = transform(chat)) else state
        }
    }

    private companion object {
        const val MAX_ATTACHMENT_SIZE_BYTES = 15 * 1024 * 1024
    }
}
