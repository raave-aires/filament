package com.raave.filament.ui.login

enum class LoginStep { EMAIL, CODE }

data class LoginUiState(
    val step: LoginStep = LoginStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val isSendingCode: Boolean = false,
    val isVerifyingCode: Boolean = false,
    val isPasskeyLoading: Boolean = false,
    val isMicrosoftLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSignedIn: Boolean = false,
) {
    val isBusy: Boolean
        get() = isSendingCode || isVerifyingCode || isPasskeyLoading || isMicrosoftLoading
}
