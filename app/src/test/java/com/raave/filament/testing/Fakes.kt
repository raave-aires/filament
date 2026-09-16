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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeTicketRepository : TicketRepository {
    var ticketsResult: AppResult<List<Ticket>> = AppResult.Success(emptyList())
    var ticketResult: AppResult<Ticket> = AppResult.Failure(AppError.Network)
    var createResult: AppResult<Long> = AppResult.Success(1L)
    var messagesResult: AppResult<List<Message>> = AppResult.Success(emptyList())
    var sendResult: (text: String, attachments: List<Attachment>) -> AppResult<Message> =
        { text, _ -> AppResult.Success(message(id = 99, text = text, isMine = true)) }

    val createdTickets = mutableListOf<Pair<String, String>>()
    val sentMessages = mutableListOf<Triple<Long, String, List<Attachment>>>()

    override suspend fun getTickets() = ticketsResult
    override suspend fun getTicket(id: Long) = ticketResult
    override suspend fun createTicket(title: String, description: String): AppResult<Long> {
        createdTickets += title to description
        return createResult
    }
    override suspend fun getMessages(ticketId: Long) = messagesResult
    override suspend fun sendMessage(ticketId: Long, text: String, attachments: List<Attachment>): AppResult<Message> {
        sentMessages += Triple(ticketId, text, attachments)
        return sendResult(text, attachments)
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
    override suspend fun getCurrentUser(): AppResult<User> = AppResult.Success(User("Teste", "teste@elinsa.com.br"))
    override suspend fun signOut() = Unit
}

class FakeAttachmentRepository : AttachmentRepository {
    val results = mutableMapOf<String, AppResult<Attachment>>()
    override suspend fun read(uri: String): AppResult<Attachment> =
        results[uri] ?: AppResult.Failure(AppError.AttachmentUnreadable)
}

fun message(id: Long, text: String = "mensagem $id", isMine: Boolean = false, authorName: String? = "Suporte") =
    Message(id = id, text = text, sentAt = null, authorName = authorName, isMine = isMine)

fun attachment(id: String, fileName: String = "$id.pdf") =
    Attachment(id = id, fileName = fileName, mimeType = "application/pdf", bytes = byteArrayOf(1, 2, 3))
