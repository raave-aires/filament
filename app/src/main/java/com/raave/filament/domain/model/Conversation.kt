package com.raave.filament.domain.model

/**
 * Conversa de um chamado como a tela mostra: mensagem de abertura + followups, em ordem cronológica.
 *
 * @property lastReadMessageId última mensagem que o usuário já viu neste aparelho; `null` se nunca abriu.
 */
data class Conversation(
    val messages: List<Message>,
    val lastReadMessageId: Long?,
)
