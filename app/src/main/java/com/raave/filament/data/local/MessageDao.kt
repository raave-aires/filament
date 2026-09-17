package com.raave.filament.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    /** Ordem cronológica pelo id do GLPI, que cresce a cada followup criado. */
    @Query("SELECT * FROM messages WHERE ticketId = :ticketId ORDER BY id")
    fun observe(ticketId: Long): Flow<List<MessageEntity>>

    @Query("SELECT MAX(id) FROM messages WHERE ticketId = :ticketId")
    suspend fun maxId(ticketId: Long): Long?

    /** Upsert por id: push, sync e eco de envio podem trazer a mesma mensagem sem duplicar. */
    @Upsert
    suspend fun upsert(messages: List<MessageEntity>)

    /**
     * Reconciliação depois de reler a conversa inteira: apaga as que já estavam guardadas quando a
     * releitura começou ([upToId]) e não vieram nela — excluídas ou tornadas privadas no GLPI. Mensagens
     * mais novas que [upToId] ficam: chegaram durante a releitura (ex.: envio) e ela não as conhecia.
     */
    @Query("DELETE FROM messages WHERE ticketId = :ticketId AND id <= :upToId AND id NOT IN (:keepIds)")
    suspend fun deleteStale(ticketId: Long, upToId: Long, keepIds: List<Long>)

    @Query("DELETE FROM messages WHERE ticketId NOT IN (SELECT id FROM tickets)")
    suspend fun deleteOrphans()
}
