package com.raave.filament.domain.usecase

import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.Ticket
import com.raave.filament.testing.FakeTicketRepository
import com.raave.filament.testing.message
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadConversationUseCaseTest {

    private val repository = FakeTicketRepository()
    private val useCase = LoadConversationUseCase(repository)

    private val openedAt = Instant.parse("2026-09-15T10:00:00Z")

    @Test
    fun `prefixa a descricao do chamado como mensagem propria de abertura`() = runTest {
        repository.ticketResult = AppResult.Success(ticket(description = "Impressora sem tinta"))
        repository.messagesResult = AppResult.Success(listOf(message(id = 1), message(id = 2)))

        val result = useCase(ticketId = 42) as AppResult.Success

        assertEquals(listOf(LoadConversationUseCase.OPENING_MESSAGE_ID, 1L, 2L), result.value.map { it.id })
        val opening = result.value.first()
        assertEquals("Impressora sem tinta", opening.text)
        assertEquals(openedAt, opening.sentAt)
        assertTrue(opening.isMine)
    }

    @Test
    fun `sem descricao a conversa fica so com as mensagens`() = runTest {
        repository.ticketResult = AppResult.Success(ticket(description = "  "))
        repository.messagesResult = AppResult.Success(listOf(message(id = 1)))

        val result = useCase(ticketId = 42) as AppResult.Success

        assertEquals(listOf(1L), result.value.map { it.id })
    }

    @Test
    fun `falha so no detalhe do chamado nao impede a conversa`() = runTest {
        repository.ticketResult = AppResult.Failure(AppError.Network)
        repository.messagesResult = AppResult.Success(listOf(message(id = 1)))

        val result = useCase(ticketId = 42)

        assertEquals(AppResult.Success(listOf(message(id = 1))), result)
    }

    @Test
    fun `falha nas mensagens propaga o erro`() = runTest {
        repository.ticketResult = AppResult.Success(ticket(description = "Abertura"))
        repository.messagesResult = AppResult.Failure(AppError.Unauthorized)

        assertEquals(AppResult.Failure(AppError.Unauthorized), useCase(ticketId = 42))
    }

    private fun ticket(description: String?) =
        Ticket(id = 42, title = "Chamado", status = "Novo", openedAt = openedAt, description = description)
}
