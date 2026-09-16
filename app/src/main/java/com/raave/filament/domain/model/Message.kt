package com.raave.filament.domain.model

import java.time.Instant

/**
 * Mensagem na conversa de um chamado (followup do GLPI). [isMine] é calculado no backend a partir
 * do usuário do GLPI — nome e e-mail podem faltar por dado legítimo, então nunca decidem o lado.
 */
data class Message(
    val id: Long,
    val text: String,
    val sentAt: Instant?,
    val authorName: String?,
    val isMine: Boolean,
)
