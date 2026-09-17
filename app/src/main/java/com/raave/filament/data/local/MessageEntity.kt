package com.raave.filament.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Mensagem (followup público) guardada no aparelho. O id é o do GLPI, global e crescente entre todos
 * os chamados — é o que permite pedir ao backend só o que veio depois da última guardada.
 *
 * Sem chave estrangeira pra [TicketEntity] de propósito: mensagens podem chegar antes do chamado
 * existir localmente (sync em paralelo com o detalhe, push). Órfãs são limpas quando a listagem é
 * atualizada.
 */
@Entity(tableName = "messages", indices = [Index("ticketId")])
data class MessageEntity(
    @PrimaryKey val id: Long,
    val ticketId: Long,
    val text: String,
    val sentAtEpochMillis: Long?,
    val authorName: String?,
    val isMine: Boolean,
)
