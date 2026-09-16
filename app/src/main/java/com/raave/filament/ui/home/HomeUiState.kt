package com.raave.filament.ui.home

import com.raave.filament.data.glpi.GlpiFollowup
import com.raave.filament.data.glpi.GlpiTicketSummary

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
    // Só confirma o que foi aberto nesta sessão — fica como retaguarda caso a listagem abaixo
    // ainda não esteja no ar no backend (ver GlpiRepository.getTickets).
    val lastCreatedTicketId: Long? = null,
    val chamados: List<GlpiTicketSummary> = emptyList(),
    val isChamadosLoading: Boolean = false,
    val chamadosError: String? = null,
    // Badge da aba Chamados. Preenchido a partir do total de [chamados] assim que a listagem
    // carrega com sucesso; continua nulo enquanto isso não acontece.
    val chamadosBadgeCount: Int? = null,
    // Não nulo abre a tela de chat em tela cheia por cima das abas (ver HomeScreen).
    val chatState: ChatUiState? = null,
)

data class ChatUiState(
    val ticketId: Long,
    val ticketName: String,
    val followups: List<GlpiFollowup> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val draftMessage: String = "",
    val isSending: Boolean = false,
    val sendError: String? = null,
)
