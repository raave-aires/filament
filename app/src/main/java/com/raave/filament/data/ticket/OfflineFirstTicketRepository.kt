package com.raave.filament.data.ticket

import androidx.room3.withWriteTransaction
import com.raave.filament.data.glpi.TicketRemoteDataSource
import com.raave.filament.data.local.FilamentDatabase
import com.raave.filament.data.local.MessageEntity
import com.raave.filament.data.local.TicketEntity
import com.raave.filament.data.local.toDomain
import com.raave.filament.data.local.toEntity
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.Attachment
import com.raave.filament.domain.model.Message
import com.raave.filament.domain.model.Ticket
import com.raave.filament.domain.model.getOrNull
import com.raave.filament.domain.model.map
import com.raave.filament.domain.repository.TicketRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineFirstTicketRepository @Inject constructor(
    private val remote: TicketRemoteDataSource,
    private val database: FilamentDatabase,
) : TicketRepository {

    private val ticketDao = database.ticketDao()
    private val messageDao = database.messageDao()

    override fun observeTickets(): Flow<List<Ticket>> =
        ticketDao.observeAll().map { rows -> rows.map(TicketEntity::toDomain) }

    override fun observeTicket(id: Long): Flow<Ticket?> = ticketDao.observe(id).map { it?.toDomain() }

    override fun observeMessages(ticketId: Long): Flow<List<Message>> =
        messageDao.observe(ticketId).map { rows -> rows.map(MessageEntity::toDomain) }

    override suspend fun refreshTickets(): AppResult<Unit> = remote.getTickets().map { tickets ->
        database.withWriteTransaction {
            ticketDao.insertIgnore(
                tickets.mapIndexed { position, ticket ->
                    TicketEntity(
                        id = ticket.id,
                        title = ticket.title,
                        status = ticket.status,
                        openedAtEpochMillis = ticket.openedAt?.toEpochMilli(),
                        description = null,
                        descriptionSynced = false,
                        listPosition = position,
                    )
                },
            )
            tickets.forEachIndexed { position, ticket ->
                ticketDao.updateSummary(
                    id = ticket.id,
                    title = ticket.title,
                    status = ticket.status,
                    openedAtEpochMillis = ticket.openedAt?.toEpochMilli(),
                    listPosition = position,
                )
            }
            ticketDao.deleteNotIn(tickets.map { it.id })
            messageDao.deleteOrphans()
        }
    }

    override suspend fun syncConversation(ticketId: Long, full: Boolean): AppResult<Unit> = coroutineScope {
        val needsDetail = full || !isDetailSynced(ticketId)
        // Tirado antes da chamada: delimita o que a releitura completa pode apagar (ver deleteStale).
        val lastStoredId = messageDao.maxId(ticketId)
        val afterId = if (full) null else lastStoredId
        val detail = async { if (needsDetail) remote.getTicket(ticketId) else null }
        val messages = remote.getMessages(ticketId, afterId)
        val ticket = detail.await()?.getOrNull()
        // Uma gravação só: a conversa observada nunca recebe a abertura do chamado sem as mensagens (ou o
        // contrário). Na primeira abertura, meia conversa montava a lista e depois ela rolava até o fim.
        database.withWriteTransaction {
            ticket?.let { saveDetail(it) }
            messages.getOrNull()?.let { saveMessages(ticketId, it, afterId, lastStoredId) }
        }
        // Falha só no detalhe não impede a conversa: a descrição vem na próxima sincronização.
        messages.map { }
    }

    private suspend fun isDetailSynced(ticketId: Long): Boolean = ticketDao.isDescriptionSynced(ticketId) ?: false

    private suspend fun saveDetail(ticket: Ticket) {
        // Chamado ainda fora da lista local (ex.: aberto antes de a listagem sincronizar): entra no fim.
        ticketDao.insertIgnore(
            listOf(
                TicketEntity(
                    id = ticket.id,
                    title = ticket.title,
                    status = ticket.status,
                    openedAtEpochMillis = ticket.openedAt?.toEpochMilli(),
                    description = null,
                    descriptionSynced = false,
                    listPosition = Int.MAX_VALUE,
                ),
            ),
        )
        ticketDao.updateDetail(
            id = ticket.id,
            title = ticket.title,
            status = ticket.status,
            openedAtEpochMillis = ticket.openedAt?.toEpochMilli(),
            description = ticket.description,
        )
    }

    private suspend fun saveMessages(ticketId: Long, messages: List<Message>, afterId: Long?, lastStoredId: Long?) {
        if (afterId == null && lastStoredId != null) {
            messageDao.deleteStale(ticketId, upToId = lastStoredId, keepIds = messages.map { it.id })
        }
        messageDao.upsert(messages.map { it.toEntity(ticketId) })
    }

    override suspend fun markRead(ticketId: Long, messageId: Long) {
        ticketDao.advanceReadMarker(ticketId, messageId)
    }

    override suspend fun createTicket(title: String, description: String): AppResult<Long> =
        remote.createTicket(title, description)

    override suspend fun sendMessage(ticketId: Long, text: String, attachments: List<Attachment>): AppResult<Message> =
        remote.sendMessage(ticketId, text, attachments).map { message ->
            messageDao.upsert(listOf(message.toEntity(ticketId)))
            message
        }
}
