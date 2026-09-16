package com.raave.filament.ui.home

import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.Ticket
import com.raave.filament.domain.model.User

enum class HomeTab { INICIO, CHAMADOS, CONTA }

data class HomeUiState(
    val isUserLoading: Boolean = true,
    val user: User? = null,
    /** Falha ao carregar a conta que NÃO encerra a sessão (rede, servidor): a tela oferece tentar de novo. */
    val userError: AppError? = null,
    val selectedTab: HomeTab = HomeTab.INICIO,
    val isBlurEnabled: Boolean = false,
    val newTicket: NewTicketFormState? = null,
    // Só confirma o que foi aberto nesta sessão — fica como retaguarda caso a listagem ainda não
    // reflita o chamado recém-criado.
    val lastCreatedTicketId: Long? = null,
    val tickets: List<Ticket> = emptyList(),
    val isTicketsLoading: Boolean = false,
    val ticketsError: AppError? = null,
) {
    /** Badge da aba Chamados: só aparece depois que a listagem carrega e se houver chamado. */
    val ticketsBadgeCount: Int?
        get() = tickets.size.takeIf { it > 0 }
}

/** Formulário de novo chamado; `null` em [HomeUiState.newTicket] significa diálogo fechado. */
data class NewTicketFormState(
    val title: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
)
