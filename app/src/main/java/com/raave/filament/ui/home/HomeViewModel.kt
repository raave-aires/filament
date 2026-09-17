package com.raave.filament.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.repository.AuthRepository
import com.raave.filament.domain.repository.TicketRepository
import com.raave.filament.domain.usecase.CreateTicketUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Sessão expirada não é tratada aqui: um 401 em qualquer chamada descarta o token e a navegação
 * volta pro login sozinha (ver [AuthRepository.isSignedIn]).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val ticketRepository: TicketRepository,
    private val createTicket: CreateTicketUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * A primeira sincronização gravou chamados, mas o flow observado ainda não os entregou: o
     * carregamento só termina quando eles chegam. Sem isso a tela piscava "Nenhum chamado ainda".
     */
    private var awaitingTickets = false

    init {
        // Lista vem do aparelho: aparece na hora, mesmo sem rede; a sincronização só a atualiza.
        ticketRepository.observeTickets()
            .onEach { tickets ->
                val arrived = awaitingTickets && tickets.isNotEmpty()
                if (arrived) awaitingTickets = false
                _uiState.update {
                    it.copy(tickets = tickets, isTicketsLoading = if (arrived) false else it.isTicketsLoading)
                }
            }
            .launchIn(viewModelScope)
        loadUser()
        loadTickets()
    }

    private fun loadUser() {
        // Conta guardada da sessão: a Home abre na hora, inclusive sem rede, e a carga só a atualiza.
        val cachedUser = authRepository.getCachedUser()
        viewModelScope.launch {
            _uiState.update {
                it.copy(isUserLoading = cachedUser == null, user = cachedUser ?: it.user, userError = null)
            }
            when (val result = authRepository.getCurrentUser()) {
                is AppResult.Success -> _uiState.update { it.copy(isUserLoading = false, user = result.value) }
                // Sessão inválida: a tela está prestes a ser trocada pelo login, não há erro a mostrar.
                // Com a conta guardada, falha de rede não bloqueia a Home.
                is AppResult.Failure -> if (result.error != AppError.Unauthorized && cachedUser == null) {
                    _uiState.update { it.copy(isUserLoading = false, userError = result.error) }
                }
            }
        }
    }

    /** Tentar de novo depois de abrir o app sem conexão: a listagem provavelmente falhou junto. */
    fun onUserRetryClick() {
        loadUser()
        if (_uiState.value.ticketsError != null) loadTickets()
    }

    fun onTabSelected(tab: HomeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onSignOutClick() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun loadTickets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTicketsLoading = true, ticketsError = null) }
            when (val result = ticketRepository.refreshTickets()) {
                is AppResult.Success -> finishTicketsLoading()
                // A lista guardada continua na tela: o erro aparece acima dela em vez de esvaziá-la.
                is AppResult.Failure -> _uiState.update { it.copy(isTicketsLoading = false, ticketsError = result.error) }
            }
        }
    }

    private suspend fun finishTicketsLoading() {
        val hasTickets = ticketRepository.observeTickets().first().isNotEmpty()
        if (hasTickets && _uiState.value.tickets.isEmpty()) {
            awaitingTickets = true
        } else {
            _uiState.update { it.copy(isTicketsLoading = false) }
        }
    }

    fun onTicketsRefresh() {
        if (_uiState.value.isTicketsRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTicketsRefreshing = true) }
            when (val result = ticketRepository.refreshTickets()) {
                is AppResult.Success -> _uiState.update { it.copy(isTicketsRefreshing = false, ticketsError = null) }
                is AppResult.Failure -> _uiState.update { it.copy(isTicketsRefreshing = false, ticketsError = result.error) }
            }
        }
    }

    fun onNewTicketClick() {
        _uiState.update { it.copy(newTicket = NewTicketFormState()) }
    }

    fun onNewTicketDismiss() {
        _uiState.update { state ->
            if (state.newTicket?.isSubmitting == true) state else state.copy(newTicket = null)
        }
    }

    fun onNewTicketTitleChange(title: String) {
        updateNewTicket { it.copy(title = title, error = null) }
    }

    fun onNewTicketDescriptionChange(description: String) {
        updateNewTicket { it.copy(description = description, error = null) }
    }

    fun onNewTicketSubmit() {
        val form = _uiState.value.newTicket ?: return
        if (form.isSubmitting) return
        viewModelScope.launch {
            updateNewTicket { it.copy(isSubmitting = true, error = null) }
            when (val result = createTicket(form.title, form.description)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(newTicket = null, lastCreatedTicketId = result.value, selectedTab = HomeTab.CHAMADOS)
                    }
                    loadTickets()
                }
                is AppResult.Failure -> updateNewTicket { it.copy(isSubmitting = false, error = result.error) }
            }
        }
    }

    private fun updateNewTicket(transform: (NewTicketFormState) -> NewTicketFormState) {
        _uiState.update { state -> state.newTicket?.let { state.copy(newTicket = transform(it)) } ?: state }
    }
}
