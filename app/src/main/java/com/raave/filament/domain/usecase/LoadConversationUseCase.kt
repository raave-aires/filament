package com.raave.filament.domain.usecase

import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.Message
import com.raave.filament.domain.model.Ticket
import com.raave.filament.domain.model.getOrNull
import com.raave.filament.domain.repository.TicketRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Conversa completa de um chamado. O GLPI não trata a mensagem de abertura (descrição do chamado)
 * como followup — sem juntar as duas fontes ela nunca aparecia no chat.
 */
class LoadConversationUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
) {
    suspend operator fun invoke(ticketId: Long): AppResult<List<Message>> = coroutineScope {
        val ticket = async { ticketRepository.getTicket(ticketId) }
        when (val messages = ticketRepository.getMessages(ticketId)) {
            is AppResult.Failure -> {
                ticket.cancel()
                messages
            }
            is AppResult.Success -> {
                // O detalhe é complementar: se só ele falhar, a conversa aparece sem a abertura.
                val opening = ticket.await().getOrNull()?.toOpeningMessage()
                AppResult.Success(listOfNotNull(opening) + messages.value)
            }
        }
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
        id = LoadConversationUseCase.OPENING_MESSAGE_ID,
        text = text,
        sentAt = openedAt,
        authorName = null,
        isMine = true,
    )
}
