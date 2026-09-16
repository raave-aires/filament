package com.raave.filament.ui.chat

import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.usecase.LoadConversationUseCase
import com.raave.filament.testing.FakeAttachmentRepository
import com.raave.filament.testing.FakeTicketRepository
import com.raave.filament.testing.attachment
import com.raave.filament.testing.message
import com.raave.filament.ui.navigation.ChatRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        loadConversation = LoadConversationUseCase(tickets),
        ticketRepository = tickets,
        attachmentRepository = attachments,
    )

    @Test
    fun `carrega a conversa ao abrir`() {
        tickets.messagesResult = AppResult.Success(listOf(message(id = 1)))

        val state = viewModel().uiState.value

        assertEquals("Impressora", state.ticketTitle)
        assertFalse(state.isLoading)
        assertEquals(listOf(1L), state.messages.map { it.id })
    }

    @Test
    fun `erro ao carregar pode ser tentado de novo`() {
        tickets.messagesResult = AppResult.Failure(AppError.Network)
        val viewModel = viewModel()
        assertEquals(AppError.Network, viewModel.uiState.value.loadError)

        tickets.messagesResult = AppResult.Success(listOf(message(id = 1)))
        viewModel.onRetryClick()

        assertEquals(null, viewModel.uiState.value.loadError)
        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `enviar anexa a mensagem e limpa rascunho e anexos`() {
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
