package com.raave.filament.data.local

import com.raave.filament.domain.model.Message
import com.raave.filament.domain.model.Ticket
import java.time.Instant

fun TicketEntity.toDomain() = Ticket(
    id = id,
    title = title,
    status = status,
    openedAt = openedAtEpochMillis?.let(Instant::ofEpochMilli),
    description = description,
    lastReadMessageId = lastReadMessageId,
)

fun MessageEntity.toDomain() = Message(
    id = id,
    text = text,
    sentAt = sentAtEpochMillis?.let(Instant::ofEpochMilli),
    authorName = authorName,
    isMine = isMine,
)

fun Message.toEntity(ticketId: Long) = MessageEntity(
    id = id,
    ticketId = ticketId,
    text = text,
    sentAtEpochMillis = sentAt?.toEpochMilli(),
    authorName = authorName,
    isMine = isMine,
)
