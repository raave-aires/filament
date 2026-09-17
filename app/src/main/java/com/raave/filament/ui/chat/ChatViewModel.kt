package com.raave.filament.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.Attachment
import com.raave.filament.domain.repository.AttachmentRepository
import com.raave.filament.domain.repository.TicketRepository
import com.raave.filament.domain.usecase.LoadConversationUseCase
import com.raave.filament.ui.navigation.ChatRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Conversa de um chamado. Um ViewModel por entrada de navegação: abrir outro chamado cria outra
 * instância, então não há mais a checagem de "o chat aberto ainda é o mesmo chamado" que existia
 * quando o chat era um estado dentro da Home.
 */
@HiltViewModel(assistedFactory = ChatViewModel.Factory::class)
class ChatViewModel @AssistedInject constructor(
    @Assisted private val route: ChatRoute,
    private val loadConversation: LoadConversationUseCase,
    private val ticketRepository: TicketRepository,
    private val attachmentRepository: AttachmentRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(route: ChatRoute): ChatViewModel
    }

    private val _uiState = MutableStateFlow(ChatUiState(ticketTitle = route.ticketTitle))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** Anexos com os bytes, na mesma ordem de [ChatUiState.attachments]. */
    private var pendingAttachments: List<Attachment> = emptyList()

    init {
        load()
    }

    fun onRetryClick() = load()

    fun onRefresh() {
        val state = _uiState.value
        // Durante o envio a lista vai ganhar a mensagem nova; atualizar ao mesmo tempo só cria corrida.
        if (state.isRefreshing || state.isLoading || state.isSending) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, refreshError = null) }
            when (val result = loadConversation(route.ticketId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isRefreshing = false, messages = result.value, loadError = null)
                }
                is AppResult.Failure -> _uiState.update { it.copy(isRefreshing = false, refreshError = result.error) }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            when (val result = loadConversation(route.ticketId)) {
                is AppResult.Success -> _uiState.update { it.copy(isLoading = false, messages = result.value) }
                is AppResult.Failure -> _uiState.update { it.copy(isLoading = false, loadError = result.error) }
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
            when (val result = ticketRepository.sendMessage(route.ticketId, text, attachments)) {
                is AppResult.Success -> {
                    setAttachments(emptyList(), attachmentError = null)
                    _uiState.update {
                        // Sem duplicar se uma atualização concluída no meio do envio já trouxe a mensagem:
                        // id repetido na LazyColumn derruba o app.
                        val sent = result.value
                        it.copy(
                            isSending = false,
                            draftMessage = "",
                            messages = it.messages.filterNot { message -> message.id == sent.id } + sent,
                        )
                    }
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
