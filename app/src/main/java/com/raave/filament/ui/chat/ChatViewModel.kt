package com.raave.filament.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.Attachment
import com.raave.filament.domain.model.Conversation
import com.raave.filament.domain.repository.AttachmentRepository
import com.raave.filament.domain.repository.TicketRepository
import com.raave.filament.domain.usecase.ObserveConversationUseCase
import com.raave.filament.ui.navigation.ChatRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Conversa de um chamado. As mensagens vêm do que está guardado no aparelho (aparecem na hora, mesmo
 * sem rede); as sincronizações só atualizam esse cache. Um ViewModel por entrada de navegação.
 */
@HiltViewModel(assistedFactory = ChatViewModel.Factory::class)
class ChatViewModel @AssistedInject constructor(
    @Assisted private val route: ChatRoute,
    observeConversation: ObserveConversationUseCase,
    private val ticketRepository: TicketRepository,
    private val attachmentRepository: AttachmentRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(route: ChatRoute): ChatViewModel
    }

    private val _uiState = MutableStateFlow(ChatUiState(ticketTitle = route.ticketTitle))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val conversation = observeConversation(route.ticketId)

    /** Anexos com os bytes, na mesma ordem de [ChatUiState.attachments]. */
    private var pendingAttachments: List<Attachment> = emptyList()

    private var syncJob: Job? = null

    /**
     * A primeira sincronização gravou mensagens, mas o flow observado ainda não as entregou: o
     * carregamento só termina quando elas chegam. Sem isso a tela piscava "sem mensagens" nesse meio.
     */
    private var awaitingMessages = false

    /** O divisor de não lidas é decidido uma vez, quando a conversa aparece pela primeira vez. */
    private var unreadAnchorResolved = false

    /** Maior mensagem já informada como vista nesta tela: evita regravar o marcador a cada rolagem. */
    private var lastSeenReported = 0L

    init {
        conversation
            .onEach { conversation ->
                val messages = conversation.messages
                val arrived = awaitingMessages && messages.isNotEmpty()
                if (arrived) awaitingMessages = false
                val anchor = if (!unreadAnchorResolved && messages.isNotEmpty()) {
                    unreadAnchorResolved = true
                    lastSeenReported = conversation.lastReadMessageId ?: 0L
                    firstUnreadMessageId(conversation)
                } else {
                    null
                }
                _uiState.update { state ->
                    // Com a conversa guardada na tela, uma falha anterior deixa de ser tela de erro e
                    // vira o aviso discreto da barra de mensagem.
                    val hasMessages = messages.isNotEmpty()
                    state.copy(
                        messages = messages,
                        isConversationReady = true,
                        firstUnreadMessageId = anchor ?: state.firstUnreadMessageId,
                        isLoading = if (arrived) false else state.isLoading,
                        loadError = if (hasMessages) null else state.loadError,
                        refreshError = if (hasMessages && state.loadError != null) state.loadError else state.refreshError,
                    )
                }
            }
            .launchIn(viewModelScope)
        load()
    }

    /**
     * Primeira mensagem de outra pessoa depois da última vista. Sem marcador (conversa nunca aberta
     * neste aparelho) não há "novas": a conversa abre no fim.
     */
    private fun firstUnreadMessageId(conversation: Conversation): Long? {
        val lastRead = conversation.lastReadMessageId ?: return null
        return conversation.messages.firstOrNull { it.id > lastRead && !it.isMine }?.id
    }

    /** A tela informa a mensagem mais recente que já apareceu; o marcador só avança. */
    fun onMessagesSeen(messageId: Long) {
        if (messageId <= lastSeenReported) return
        lastSeenReported = messageId
        viewModelScope.launch { ticketRepository.markRead(route.ticketId, messageId) }
    }

    fun onRetryClick() = load()

    /** Abertura e "tentar de novo": traz só o que é novo desde a última mensagem guardada. */
    private fun load() {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            when (val result = ticketRepository.syncConversation(route.ticketId, full = false)) {
                is AppResult.Success -> finishLoading()
                is AppResult.Failure -> _uiState.update { state ->
                    if (state.messages.isEmpty()) {
                        state.copy(isLoading = false, loadError = result.error)
                    } else {
                        state.copy(isLoading = false, refreshError = result.error)
                    }
                }
            }
        }
    }

    private suspend fun finishLoading() {
        val hasMessages = conversation.first().messages.isNotEmpty()
        if (hasMessages && _uiState.value.messages.isEmpty()) {
            awaitingMessages = true
        } else {
            _uiState.update { it.copy(isLoading = false, refreshError = null) }
        }
    }

    /**
     * Volta ao app: respostas do atendimento podem ter chegado enquanto estava fora. Busca só as
     * novas, sem indicador — o pull-to-refresh continua como gesto manual.
     */
    fun onResume() {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            when (val result = ticketRepository.syncConversation(route.ticketId, full = false)) {
                is AppResult.Success -> _uiState.update { it.copy(refreshError = null) }
                is AppResult.Failure -> _uiState.update { it.copy(refreshError = result.error) }
            }
        }
    }

    /** Pull-to-refresh: relê a conversa inteira pra pegar também edição e exclusão feitas no GLPI. */
    fun onRefresh() {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, refreshError = null) }
            when (val result = ticketRepository.syncConversation(route.ticketId, full = true)) {
                is AppResult.Success -> _uiState.update { it.copy(isRefreshing = false, loadError = null) }
                is AppResult.Failure -> _uiState.update { it.copy(isRefreshing = false, refreshError = result.error) }
            }
        }
    }

    fun onMessageChange(text: String) {
        _uiState.update { it.copy(draftMessage = text, sendError = null) }
    }

    /** [uris] na forma textual dos `content://` devolvidos pelo seletor de arquivos. */
    fun onAttachmentsPicked(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val results = uris.map { attachmentRepository.read(it) }
            val read = results.filterIsInstance<AppResult.Success<Attachment>>().map { it.value }
            val firstError = results.filterIsInstance<AppResult.Failure>().firstOrNull()?.error
            setAttachments(pendingAttachments + read, attachmentError = firstError)
        }
    }

    fun onRemoveAttachment(attachmentId: String) {
        setAttachments(pendingAttachments.filterNot { it.id == attachmentId }, attachmentError = null)
    }

    fun onSendClick() {
        val state = _uiState.value
        if (!state.canSend) return
        val text = state.draftMessage.trim()
        val attachments = pendingAttachments
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, sendError = null) }
            // A mensagem enviada é guardada pelo repositório e chega pela conversa observada.
            when (val result = ticketRepository.sendMessage(route.ticketId, text, attachments)) {
                is AppResult.Success -> {
                    setAttachments(emptyList(), attachmentError = null)
                    _uiState.update { it.copy(isSending = false, draftMessage = "") }
                }
                is AppResult.Failure -> _uiState.update { it.copy(isSending = false, sendError = result.error) }
            }
        }
    }

    private fun setAttachments(attachments: List<Attachment>, attachmentError: AppError?) {
        pendingAttachments = attachments
        _uiState.update { state ->
            state.copy(
                attachments = attachments.map { AttachmentChip(id = it.id, fileName = it.fileName) },
                attachmentError = attachmentError,
            )
        }
    }
}
