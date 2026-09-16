package com.raave.filament.domain.model

import java.time.Instant

/**
 * Chamado do GLPI. [status] já vem traduzido pelo GLPI (ex.: "Novo") e é exibido como está.
 * [description] é a mensagem de abertura; só vem preenchida no detalhe do chamado, não na listagem.
 */
data class Ticket(
    val id: Long,
    val title: String,
    val status: String,
    val openedAt: Instant?,
    val description: String? = null,
)
