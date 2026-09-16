package com.raave.filament.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raave.filament.data.auth.AuthRepository
import com.raave.filament.data.auth.AuthTokenStore
import com.raave.filament.data.glpi.GlpiRepository
import com.raave.filament.data.glpi.GlpiTicketSummary
import com.raave.filament.util.DeviceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
                        _uiState.update { it.copy(isLoading = false, signedOut = true) }
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
                    _uiState.update { it.copy(isLoading = false, signedOut = true) }
                },
            )
        }
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
        if (content.isEmpty()) return
        viewModelScope.launch {
            updateChat(chat.ticketId) { it.copy(isSending = true, sendError = null) }
            glpiRepository.sendFollowup(chat.ticketId, content).fold(
                onSuccess = { followup ->
                    updateChat(chat.ticketId) {
                        it.copy(isSending = false, draftMessage = "", followups = it.followups + followup)
                    }
                },
                onFailure = { error ->
                    updateChat(chat.ticketId) { it.copy(isSending = false, sendError = error.message) }
                },
            )
        }
    }

    private fun loadFollowups(ticketId: Long) {
        viewModelScope.launch {
            updateChat(ticketId) { it.copy(isLoading = true, loadError = null) }
            glpiRepository.getFollowups(ticketId).fold(
                onSuccess = { followups ->
                    updateChat(ticketId) { it.copy(isLoading = false, followups = followups) }
                },
                onFailure = { error ->
                    updateChat(ticketId) { it.copy(isLoading = false, loadError = error.message) }
                },
            )
        }
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
}
