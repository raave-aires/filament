package com.raave.filament.ui.home

enum class HomeTab { INICIO, CHAMADOS, CONTA }

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String? = null,
    val userEmail: String? = null,
    val selectedTab: HomeTab = HomeTab.INICIO,
    val isBlurEnabled: Boolean = false,
    val signedOut: Boolean = false,
)
