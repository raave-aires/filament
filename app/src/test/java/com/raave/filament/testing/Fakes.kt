package com.raave.filament.testing

import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.Attachment
import com.raave.filament.domain.model.Message
import com.raave.filament.domain.model.PasskeyChallenge
import com.raave.filament.domain.model.Ticket
import com.raave.filament.domain.model.User
import com.raave.filament.domain.repository.AttachmentRepository
import com.raave.filament.domain.repository.AuthRepository
import com.raave.filament.domain.repository.TicketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Repositório em memória com o mesmo contrato do offline-first: os flows são o "banco", e
 * [refreshTickets]/[syncConversation] copiam do "backend" ([remoteTickets]/[remoteMessages]) pra ele.
 */
class FakeTicketRepository : TicketRepository {
    val storedTickets = MutableStateFlow<List<Ticket>>(emptyList())
    val storedMessages = MutableStateFlow<Map<Long, List<Message>>>(emptyMap())

    var remoteTickets: List<Ticket> = emptyList()
    var refreshError: AppError? = null
    val remoteMessages = mutableMapOf<Long, List<Message>>()
    var syncError: AppError? = null
    var createResult: AppResult<Long> = AppResult.Success(1L)
    var sendResult: (text: String, attachments: List<Attachment>) -> AppResult<Message> =
        { text, _ -> AppResult.Success(message(id = 99, text = text, isMine = true)) }

    val syncCalls = mutableListOf<Pair<Long, Boolean>>()
    var refreshCalls = 0
    val createdTickets = mutableListOf<Pair<String, String>>()
    val sentMessages = mutableListOf<Triple<Long, String, List<Attachment>>>()

    override fun observeTickets(): Flow<List<Ticket>> = storedTickets

    override suspend fun refreshTickets(): AppResult<Unit> {
        refreshCalls++
        refreshError?.let { return AppResult.Failure(it) }
        storedTickets.value = remoteTickets
        return AppResult.Success(Unit)
    }

    override fun observeTicket(id: Long): Flow<Ticket?> = storedTickets.map { list -> list.find { it.id == id } }

    override fun observeMessages(ticketId: Long): Flow<List<Message>> =
        storedMessages.map { it[ticketId].orEmpty() }

    override suspend fun syncConversation(ticketId: Long, full: Boolean): AppResult<Unit> {
        syncCalls += ticketId to full
        syncError?.let { return AppResult.Failure(it) }
        storedMessages.update { it + (ticketId to remoteMessages[ticketId].orEmpty()) }
        return AppResult.Success(Unit)
    }

    override suspend fun createTicket(title: String, description: String): AppResult<Long> {
        createdTickets += title to description
        return createResult
    }

    val markedRead = mutableListOf<Pair<Long, Long>>()

    override suspend fun markRead(ticketId: Long, messageId: Long) {
        markedRead += ticketId to messageId
        storedTickets.update { list ->
            list.map { ticket ->
                if (ticket.id == ticketId && (ticket.lastReadMessageId ?: 0L) < messageId) {
                    ticket.copy(lastReadMessageId = messageId)
                } else {
                    ticket
                }
            }
        }
    }

    override suspend fun sendMessage(ticketId: Long, text: String, attachments: List<Attachment>): AppResult<Message> {
        sentMessages += Triple(ticketId, text, attachments)
        val result = sendResult(text, attachments)
        if (result is AppResult.Success) {
            storedMessages.update { stored ->
                val current = stored[ticketId].orEmpty().filterNot { it.id == result.value.id }
                stored + (ticketId to current + result.value)
            }
        }
        return result
    }
}

class FakeAuthRepository : AuthRepository {
    override val isSignedIn: StateFlow<Boolean> = MutableStateFlow(true)
    var requestCodeResult: AppResult<Unit> = AppResult.Success(Unit)
    var verifyCodeResult: AppResult<Unit> = AppResult.Success(Unit)
    val requestedEmails = mutableListOf<String>()
    val verifiedCodes = mutableListOf<Pair<String, String>>()

    override suspend fun requestEmailCode(email: String): AppResult<Unit> {
        requestedEmails += email
        return requestCodeResult
    }
    override suspend fun verifyEmailCode(email: String, code: String): AppResult<Unit> {
        verifiedCodes += email to code
        return verifyCodeResult
    }
    override suspend fun getPasskeyChallenge(): AppResult<PasskeyChallenge> = AppResult.Failure(AppError.Network)
    override suspend fun verifyPasskey(responseJson: String, challenge: PasskeyChallenge) = AppResult.Success(Unit)
    override suspend fun startMicrosoftSignIn(): AppResult<String> = AppResult.Failure(AppError.Network)
    override suspend fun completeExternalSignIn(oneTimeToken: String) = AppResult.Success(Unit)
    var currentUserResult: AppResult<User> = AppResult.Success(User("Teste", "teste@elinsa.com.br"))
    override suspend fun getCurrentUser(): AppResult<User> = currentUserResult
    var storedUser: User? = null
    override fun getCachedUser(): User? = storedUser
    override suspend fun signOut() = Unit
}

class FakeAttachmentRepository : AttachmentRepository {
    val results = mutableMapOf<String, AppResult<Attachment>>()
    override suspend fun read(uri: String): AppResult<Attachment> =
        results[uri] ?: AppResult.Failure(AppError.AttachmentUnreadable)
}

fun ticket(id: Long, title: String = "Chamado $id", description: String? = null, lastReadMessageId: Long? = null) =
    Ticket(id = id, title = title, status = "Novo", openedAt = null, description = description, lastReadMessageId = lastReadMessageId)

fun message(id: Long, text: String = "mensagem $id", isMine: Boolean = false, authorName: String? = "Suporte") =
    Message(id = id, text = text, sentAt = null, authorName = authorName, isMine = isMine)

fun attachment(id: String, fileName: String = "$id.pdf") =
    Attachment(id = id, fileName = fileName, mimeType = "application/pdf", bytes = byteArrayOf(1, 2, 3))
