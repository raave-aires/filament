package com.raave.filament.ui.home

enum class HomeTab { INICIO, CHAMADOS, CONTA }

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String? = null,
    val userEmail: String? = null,
    val selectedTab: HomeTab = HomeTab.INICIO,
    val isBlurEnabled: Boolean = false,
    val signedOut: Boolean = false,
    val isNewTicketDialogOpen: Boolean = false,
    val newTicketName: String = "",
    val newTicketContent: String = "",
    val isCreatingTicket: Boolean = false,
    val newTicketError: String? = null,
    // Só confirma o que foi aberto nesta sessão — a listagem de chamados depende de um filtro
    // por requerente que o GLPI ainda não atende (ver NOTES.md do backbone).
    val lastCreatedTicketId: Long? = null,
)
