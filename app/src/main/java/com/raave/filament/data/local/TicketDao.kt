package com.raave.filament.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {

    @Query("SELECT * FROM tickets ORDER BY listPosition, id DESC")
    fun observeAll(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE id = :id")
    fun observe(id: Long): Flow<TicketEntity?>

    /** `null` quando o chamado ainda não está guardado. */
    @Query("SELECT descriptionSynced FROM tickets WHERE id = :id")
    suspend fun isDescriptionSynced(id: Long): Boolean?

    /** Só cria o que não existe: a atualização vem depois, campo a campo, sem apagar o detalhe. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(tickets: List<TicketEntity>)

    /** Campos que a listagem traz. Não mexe em descrição: ela só vem no detalhe. */
    @Query(
        """
        UPDATE tickets SET title = :title, status = :status, openedAtEpochMillis = :openedAtEpochMillis,
            listPosition = :listPosition
        WHERE id = :id
        """,
    )
    suspend fun updateSummary(id: Long, title: String, status: String, openedAtEpochMillis: Long?, listPosition: Int)

    @Query(
        """
        UPDATE tickets SET title = :title, status = :status, openedAtEpochMillis = :openedAtEpochMillis,
            description = :description, descriptionSynced = 1
        WHERE id = :id
        """,
    )
    suspend fun updateDetail(id: Long, title: String, status: String, openedAtEpochMillis: Long?, description: String?)

    /** Marcador de leitura só avança: marcar uma mensagem mais antiga como vista não o faz voltar. */
    @Query(
        """
        UPDATE tickets SET lastReadMessageId = :messageId
        WHERE id = :ticketId AND (lastReadMessageId IS NULL OR lastReadMessageId < :messageId)
        """,
    )
    suspend fun advanceReadMarker(ticketId: Long, messageId: Long)

    /** Chamados que saíram da listagem do backend (lista vazia apaga todos). */
    @Query("DELETE FROM tickets WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<Long>)
}
