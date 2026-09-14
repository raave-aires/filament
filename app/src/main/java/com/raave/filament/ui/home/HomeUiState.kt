package com.raave.filament.ui.home

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String? = null,
    val userEmail: String? = null,
    val signedOut: Boolean = false,
)
