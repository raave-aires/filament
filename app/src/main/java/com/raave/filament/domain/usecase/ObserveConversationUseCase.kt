package com.raave.filament.domain.usecase

import com.raave.filament.domain.model.Conversation
import com.raave.filament.domain.model.Message
import com.raave.filament.domain.model.Ticket
import com.raave.filament.domain.repository.TicketRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Conversa completa de um chamado, como a tela mostra. O GLPI não trata a mensagem de abertura
 * (descrição do chamado) como followup — sem juntar as duas fontes ela nunca aparecia no chat.
 */
class ObserveConversationUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
) {
    operator fun invoke(ticketId: Long): Flow<Conversation> =
        combine(
            ticketRepository.observeTicket(ticketId),
            ticketRepository.observeMessages(ticketId),
        ) { ticket, messages ->
            Conversation(
                messages = listOfNotNull(ticket?.toOpeningMessage()) + messages,
                lastReadMessageId = ticket?.lastReadMessageId,
            )
        }

    companion object {
        /** Os followups reais do GLPI começam em 1, então o id 0 nunca colide. */
        const val OPENING_MESSAGE_ID = 0L
    }
}

/**
 * `isMine = true` sempre: a listagem já é escopada ao requerente, então quem abriu o chamado é o
 * próprio usuário. Sem nome de autor porque ele só é exibido nas mensagens recebidas.
 */
private fun Ticket.toOpeningMessage(): Message? {
    val text = description?.takeIf(String::isNotBlank) ?: return null
    return Message(
        id = ObserveConversationUseCase.OPENING_MESSAGE_ID,
        text = text,
        sentAt = openedAt,
        authorName = null,
        isMine = true,
    )
}
