package com.raave.filament.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Chamado guardado no aparelho. Cache do backend: pode ser apagado e reconstruído a qualquer momento.
 *
 * @property listPosition posição na última listagem do backend, pra manter a ordem que ele define.
 *   Chamado que entrou só pelo detalhe (ainda fora da listagem) vai pro fim.
 * @property descriptionSynced se o detalhe (com a mensagem de abertura) já foi buscado. Separado de
 *   [description] porque um chamado pode legitimamente não ter descrição.
 * @property lastReadMessageId maior id de mensagem que já apareceu na tela da conversa. Só avança.
 */
@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val status: String,
    val openedAtEpochMillis: Long?,
    val description: String?,
    val descriptionSynced: Boolean,
    val listPosition: Int,
    val lastReadMessageId: Long? = null,
)
