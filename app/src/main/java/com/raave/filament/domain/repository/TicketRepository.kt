package com.raave.filament.domain.repository

import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.Attachment
import com.raave.filament.domain.model.Message
import com.raave.filament.domain.model.Ticket

interface TicketRepository {

    /** Chamados abertos pelo usuário logado. */
    suspend fun getTickets(): AppResult<List<Ticket>>

    /** Detalhe do chamado, incluindo a mensagem de abertura ([Ticket.description]). */
    suspend fun getTicket(id: Long): AppResult<Ticket>

    /** Devolve o número do chamado criado. */
    suspend fun createTicket(title: String, description: String): AppResult<Long>

    /** Mensagens públicas do chamado, em ordem cronológica. Não inclui a mensagem de abertura. */
    suspend fun getMessages(ticketId: Long): AppResult<List<Message>>

    suspend fun sendMessage(ticketId: Long, text: String, attachments: List<Attachment>): AppResult<Message>
}
