package com.raave.filament.ui.chat

import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.Message

data class ChatUiState(
    val ticketTitle: String,
    val messages: List<Message> = emptyList(),
    /**
     * Se a conversa guardada no aparelho já foi lida pelo menos uma vez. Até lá a tela não mostra nada
     * (nem carregando, nem vazio): é questão de milissegundos e qualquer coisa ali pisca.
     */
    val isConversationReady: Boolean = false,
    /**
     * Primeira mensagem recebida que ainda não tinha sido vista quando a conversa abriu. Define onde
     * a lista começa e onde fica o divisor "Novas mensagens"; fixa enquanto a tela estiver aberta.
     */
    val firstUnreadMessageId: Long? = null,
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    /** Atualização puxada pelo usuário (pull-to-refresh) pra buscar respostas novas. */
    val isRefreshing: Boolean = false,
    /** Falha ao atualizar com a conversa já na tela: ela continua visível e o erro aparece junto. */
    val refreshError: AppError? = null,
    val draftMessage: String = "",
    /** Só o que a tela exibe; os bytes ficam no ViewModel, fora do estado observado pela UI. */
    val attachments: List<AttachmentChip> = emptyList(),
    val attachmentError: AppError? = null,
    val isSending: Boolean = false,
    val sendError: AppError? = null,
) {
    val canSend: Boolean
        get() = !isSending && (draftMessage.isNotBlank() || attachments.isNotEmpty())
}

data class AttachmentChip(val id: String, val fileName: String)
