package com.raave.filament.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Serializáveis pra pilha de navegação sobreviver à morte do processo (rememberNavBackStack).

@Serializable
data object LoginRoute : NavKey

@Serializable
data object HomeRoute : NavKey

/** [ticketTitle] vai junto pra o cabeçalho aparecer na hora, antes de a conversa carregar. */
@Serializable
data class ChatRoute(val ticketId: Long, val ticketTitle: String) : NavKey
