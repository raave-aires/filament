package com.raave.filament.ui.chat

import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.Message

data class ChatUiState(
    val ticketTitle: String,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
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
