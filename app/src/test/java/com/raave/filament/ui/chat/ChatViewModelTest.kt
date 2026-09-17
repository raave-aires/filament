package com.raave.filament.ui.chat

import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.usecase.ObserveConversationUseCase
import com.raave.filament.testing.FakeAttachmentRepository
import com.raave.filament.testing.FakeTicketRepository
import com.raave.filament.testing.attachment
import com.raave.filament.testing.message
import com.raave.filament.testing.ticket
import com.raave.filament.ui.navigation.ChatRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val tickets = FakeTicketRepository()
    private val attachments = FakeAttachmentRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ChatViewModel(
        route = ChatRoute(ticketId = 42, ticketTitle = "Impressora"),
        observeConversation = ObserveConversationUseCase(tickets),
        ticketRepository = tickets,
        attachmentRepository = attachments,
    )

    @Test
    fun `abrir busca so o que e novo e mostra a conversa`() {
        tickets.remoteMessages[42] = listOf(message(id = 1))

        val state = viewModel().uiState.value

        assertEquals(listOf(42L to false), tickets.syncCalls)
        assertEquals("Impressora", state.ticketTitle)
        assertFalse(state.isLoading)
        assertEquals(listOf(1L), state.messages.map { it.id })
    }

    @Test
    fun `abre com o divisor na primeira mensagem nao vista de outra pessoa`() {
        tickets.storedTickets.value = listOf(ticket(42, lastReadMessageId = 2))
        tickets.storedMessages.value = mapOf(
            42L to listOf(message(id = 1), message(id = 2), message(id = 3, isMine = true), message(id = 4)),
        )
        tickets.remoteMessages[42] = tickets.storedMessages.value.getValue(42)

        val state = viewModel().uiState.value

        assertTrue(state.isConversationReady)
        assertEquals(4L, state.firstUnreadMessageId)
    }

    @Test
    fun `sem mensagens novas ou nunca aberta abre no fim sem divisor`() {
        tickets.storedTickets.value = listOf(ticket(42, lastReadMessageId = 2))
        tickets.remoteMessages[42] = listOf(message(id = 1), message(id = 2))
        assertNull(viewModel().uiState.value.firstUnreadMessageId)

        tickets.storedTickets.value = listOf(ticket(42))
        assertNull(viewModel().uiState.value.firstUnreadMessageId)
    }

    @Test
    fun `divisor fica fixo enquanto a conversa esta aberta`() {
        tickets.storedTickets.value = listOf(ticket(42, lastReadMessageId = 1))
        tickets.remoteMessages[42] = listOf(message(id = 1), message(id = 2))
        val viewModel = viewModel()
        assertEquals(2L, viewModel.uiState.value.firstUnreadMessageId)

        viewModel.onMessagesSeen(2)
        tickets.storedMessages.value = mapOf(42L to listOf(message(id = 1), message(id = 2), message(id = 3)))

        assertEquals(2L, viewModel.uiState.value.firstUnreadMessageId)
    }

    @Test
    fun `marca como vista so quando avanca`() {
        tickets.storedTickets.value = listOf(ticket(42, lastReadMessageId = 5))
        tickets.remoteMessages[42] = listOf(message(id = 5), message(id = 6), message(id = 7))
        val viewModel = viewModel()

        viewModel.onMessagesSeen(4)
        viewModel.onMessagesSeen(7)
        viewModel.onMessagesSeen(6)
        viewModel.onMessagesSeen(7)

        assertEquals(listOf(42L to 7L), tickets.markedRead)
    }

    @Test
    fun `antes de ler a conversa guardada a tela nao esta pronta`() {
        val state = ChatUiState(ticketTitle = "x")

        assertFalse(state.isConversationReady)
    }

    @Test
    fun `sem rede a conversa guardada continua na tela com aviso discreto`() {
        tickets.storedMessages.value = mapOf(42L to listOf(message(id = 1), message(id = 2)))
        tickets.syncError = AppError.Network

        val state = viewModel().uiState.value

        assertEquals(listOf(1L, 2L), state.messages.map { it.id })
        assertNull(state.loadError)
        assertEquals(AppError.Network, state.refreshError)
    }

    @Test
    fun `sem rede e sem nada guardado mostra erro com tentar de novo`() {
        tickets.syncError = AppError.Network
        val viewModel = viewModel()
        assertEquals(AppError.Network, viewModel.uiState.value.loadError)

        tickets.syncError = null
        tickets.remoteMessages[42] = listOf(message(id = 1))
        viewModel.onRetryClick()

        assertNull(viewModel.uiState.value.loadError)
        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `pull-to-refresh rele a conversa inteira`() {
        val viewModel = viewModel()

        tickets.remoteMessages[42] = listOf(message(id = 1), message(id = 2))
        viewModel.onRefresh()

        assertEquals(listOf(42L to false, 42L to true), tickets.syncCalls)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.messages.map { it.id })
    }

    @Test
    fun `falha ao atualizar mantem a conversa na tela`() {
        tickets.remoteMessages[42] = listOf(message(id = 1))
        val viewModel = viewModel()

        tickets.syncError = AppError.Network
        viewModel.onRefresh()

        val state = viewModel.uiState.value
        assertEquals(AppError.Network, state.refreshError)
        assertNull(state.loadError)
        assertEquals(listOf(1L), state.messages.map { it.id })
    }

    @Test
    fun `retomar o app busca so mensagens novas`() {
        val viewModel = viewModel()

        viewModel.onResume()

        assertEquals(listOf(42L to false, 42L to false), tickets.syncCalls)
    }

    @Test
    fun `mensagem enviada aparece pela conversa e limpa rascunho e anexos`() {
        attachments.results["content://a"] = AppResult.Success(attachment("a"))
        val viewModel = viewModel()
        viewModel.onMessageChange("  Segue a foto  ")
        viewModel.onAttachmentsPicked(listOf("content://a"))

        viewModel.onSendClick()

        val (ticketId, text, sent) = tickets.sentMessages.single()
        assertEquals(42L, ticketId)
        assertEquals("Segue a foto", text)
        assertEquals(listOf("a"), sent.map { it.id })
        val state = viewModel.uiState.value
        assertEquals("", state.draftMessage)
        assertTrue(state.attachments.isEmpty())
        assertEquals(listOf(99L), state.messages.map { it.id })
    }

    @Test
    fun `mensagem enviada nao duplica se a conversa ja a trouxe`() {
        tickets.remoteMessages[42] = listOf(message(id = 1), message(id = 99, isMine = true))
        val viewModel = viewModel()
        viewModel.onMessageChange("Olá")

        viewModel.onSendClick()

        assertEquals(listOf(1L, 99L), viewModel.uiState.value.messages.map { it.id })
    }

    @Test
    fun `falha no envio mantem rascunho e anexos pra tentar de novo`() {
        tickets.sendResult = { _, _ -> AppResult.Failure(AppError.Network) }
        attachments.results["content://a"] = AppResult.Success(attachment("a"))
        val viewModel = viewModel()
        viewModel.onMessageChange("Olá")
        viewModel.onAttachmentsPicked(listOf("content://a"))

        viewModel.onSendClick()

        val state = viewModel.uiState.value
        assertEquals(AppError.Network, state.sendError)
        assertEquals("Olá", state.draftMessage)
        assertEquals(listOf("a"), state.attachments.map { it.id })
    }

    @Test
    fun `anexo grande demais e recusado sem descartar os validos`() {
        attachments.results["content://ok"] = AppResult.Success(attachment("ok"))
        attachments.results["content://video"] = AppResult.Failure(AppError.AttachmentTooLarge("video.mp4"))
        val viewModel = viewModel()

        viewModel.onAttachmentsPicked(listOf("content://ok", "content://video"))

        val state = viewModel.uiState.value
        assertEquals(listOf("ok"), state.attachments.map { it.id })
        assertEquals(AppError.AttachmentTooLarge("video.mp4"), state.attachmentError)
    }

    @Test
    fun `nao envia mensagem vazia sem anexo`() {
        val viewModel = viewModel()
        viewModel.onMessageChange("   ")

        viewModel.onSendClick()

        assertTrue(tickets.sentMessages.isEmpty())
    }
}
