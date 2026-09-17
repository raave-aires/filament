package com.raave.filament.domain.usecase

import com.raave.filament.domain.model.Ticket
import com.raave.filament.testing.FakeTicketRepository
import com.raave.filament.testing.message
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveConversationUseCaseTest {

    private val repository = FakeTicketRepository()
    private val useCase = ObserveConversationUseCase(repository)

    private val openedAt = Instant.parse("2026-09-15T10:00:00Z")

    @Test
    fun `prefixa a descricao do chamado como mensagem propria de abertura`() = runTest {
        repository.storedTickets.value = listOf(ticket(description = "Impressora sem tinta"))
        repository.storedMessages.value = mapOf(42L to listOf(message(id = 1), message(id = 2)))

        val conversation = useCase(42).first().messages

        assertEquals(listOf(ObserveConversationUseCase.OPENING_MESSAGE_ID, 1L, 2L), conversation.map { it.id })
        val opening = conversation.first()
        assertEquals("Impressora sem tinta", opening.text)
        assertEquals(openedAt, opening.sentAt)
        assertTrue(opening.isMine)
    }

    @Test
    fun `sem descricao a conversa fica so com as mensagens`() = runTest {
        repository.storedTickets.value = listOf(ticket(description = "  "))
        repository.storedMessages.value = mapOf(42L to listOf(message(id = 1)))

        assertEquals(listOf(1L), useCase(42).first().messages.map { it.id })
    }

    @Test
    fun `chamado ainda nao guardado nao impede mostrar as mensagens`() = runTest {
        repository.storedMessages.value = mapOf(42L to listOf(message(id = 1)))

        val conversation = useCase(42).first()
        assertEquals(listOf(1L), conversation.messages.map { it.id })
        assertEquals(null, conversation.lastReadMessageId)
    }

    @Test
    fun `leva junto o marcador da ultima mensagem vista`() = runTest {
        repository.storedTickets.value = listOf(ticket(description = null).copy(lastReadMessageId = 7))

        assertEquals(7L, useCase(42).first().lastReadMessageId)
    }

    @Test
    fun `atualiza quando chega mensagem nova`() = runTest {
        repository.storedTickets.value = listOf(ticket(description = "Abertura"))
        repository.storedMessages.value = mapOf(42L to listOf(message(id = 1)))

        repository.storedMessages.value = mapOf(42L to listOf(message(id = 1), message(id = 2)))

        assertEquals(listOf(0L, 1L, 2L), useCase(42).first().messages.map { it.id })
    }

    private fun ticket(description: String?) =
        Ticket(id = 42, title = "Chamado", status = "Novo", openedAt = openedAt, description = description)
}
