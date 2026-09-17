package com.raave.filament.ui.home

import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.User
import com.raave.filament.domain.usecase.CreateTicketUseCase
import com.raave.filament.testing.FakeAuthRepository
import com.raave.filament.testing.FakeTicketRepository
import com.raave.filament.testing.ticket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val tickets = FakeTicketRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = HomeViewModel(
        authRepository = FakeAuthRepository(),
        ticketRepository = tickets,
        createTicket = CreateTicketUseCase(tickets),
    )

    @Test
    fun `lista vem do backend e fica guardada`() {
        tickets.remoteTickets = listOf(ticket(1), ticket(2))

        val state = viewModel().uiState.value

        assertFalse(state.isTicketsLoading)
        assertNull(state.ticketsError)
        assertEquals(listOf(1L, 2L), state.tickets.map { it.id })
    }

    @Test
    fun `sem rede os chamados guardados continuam na tela com o erro`() {
        tickets.storedTickets.value = listOf(ticket(1))
        tickets.refreshError = AppError.Network

        val state = viewModel().uiState.value

        assertFalse(state.isTicketsLoading)
        assertEquals(AppError.Network, state.ticketsError)
        assertEquals(listOf(1L), state.tickets.map { it.id })
    }

    @Test
    fun `sem rede a conta guardada abre a Home sem tela de erro`() {
        val auth = FakeAuthRepository().apply {
            storedUser = User(name = "Raave", email = "raave@elinsa.com.br")
            currentUserResult = AppResult.Failure(AppError.Network)
        }
        tickets.storedTickets.value = listOf(ticket(1))
        tickets.refreshError = AppError.Network

        val state = HomeViewModel(auth, tickets, CreateTicketUseCase(tickets)).uiState.value

        assertFalse(state.isUserLoading)
        assertNull(state.userError)
        assertEquals("Raave", state.user?.name)
        assertEquals(listOf(1L), state.tickets.map { it.id })
    }

    @Test
    fun `sem rede e sem conta guardada mostra erro`() {
        val auth = FakeAuthRepository().apply { currentUserResult = AppResult.Failure(AppError.Network) }

        val state = HomeViewModel(auth, tickets, CreateTicketUseCase(tickets)).uiState.value

        assertEquals(AppError.Network, state.userError)
    }

    @Test
    fun `criar chamado atualiza a lista e vai pra aba de chamados`() {
        val viewModel = viewModel()
        tickets.remoteTickets = listOf(ticket(7))

        viewModel.onNewTicketClick()
        viewModel.onNewTicketTitleChange("Impressora")
        viewModel.onNewTicketDescriptionChange("Sem tinta")
        viewModel.onNewTicketSubmit()

        val state = viewModel.uiState.value
        assertEquals(HomeTab.CHAMADOS, state.selectedTab)
        assertNull(state.newTicket)
        assertEquals(2, tickets.refreshCalls)
        assertEquals(listOf(7L), state.tickets.map { it.id })
    }
}
