package com.raave.filament.domain.repository

import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.Attachment
import com.raave.filament.domain.model.Message
import com.raave.filament.domain.model.Ticket
import kotlinx.coroutines.flow.Flow

/**
 * Chamados e conversas, com o aparelho como fonte de verdade: a UI observa os flows e as funções de
 * sync só atualizam o que está guardado. Sem rede, os flows continuam entregando o último estado.
 */
interface TicketRepository {

    /** Chamados do usuário logado guardados no aparelho, na ordem da última listagem do backend. */
    fun observeTickets(): Flow<List<Ticket>>

    /** Substitui a lista guardada pela do backend (chamados que sumiram saem junto com as mensagens). */
    suspend fun refreshTickets(): AppResult<Unit>

    /** Chamado guardado. [Ticket.description] só é preenchida depois de sincronizar a conversa. */
    fun observeTicket(id: Long): Flow<Ticket?>

    /** Mensagens guardadas do chamado, em ordem cronológica. Não inclui a mensagem de abertura. */
    fun observeMessages(ticketId: Long): Flow<List<Message>>

    /**
     * Atualiza a conversa guardada.
     *
     * - [full] = false: só mensagens posteriores à última guardada (ou tudo, se nada foi guardado ainda).
     * - [full] = true: relê a conversa inteira e reconcilia, pegando edição, exclusão ou privatização
     *   feitas no GLPI.
     *
     * O detalhe do chamado (mensagem de abertura) é buscado junto quando ainda não foi, ou sempre no
     * modo completo. Falha só no detalhe não é falha da sincronização.
     */
    suspend fun syncConversation(ticketId: Long, full: Boolean): AppResult<Unit>

    /** Devolve o número do chamado criado. */
    suspend fun createTicket(title: String, description: String): AppResult<Long>

    /** Registra que a mensagem [messageId] (e todas as anteriores) já apareceu na tela. */
    suspend fun markRead(ticketId: Long, messageId: Long)

    /** Envia e guarda a mensagem, que passa a aparecer em [observeMessages]. */
    suspend fun sendMessage(ticketId: Long, text: String, attachments: List<Attachment>): AppResult<Message>
}
