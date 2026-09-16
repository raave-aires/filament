package com.raave.filament.ui.login

import com.raave.filament.domain.model.AppError

enum class LoginStep { EMAIL, CODE }

data class LoginUiState(
    val step: LoginStep = LoginStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val isSendingCode: Boolean = false,
    val isVerifyingCode: Boolean = false,
    val isPasskeyLoading: Boolean = false,
    val isMicrosoftLoading: Boolean = false,
    val error: AppError? = null,
    /**
     * Ação que só a UI consegue executar (precisa da Activity). O ViewModel não guarda `Context`:
     * antes ele recebia a Activity e a usava depois de uma chamada de rede — se a tela girasse no
     * meio, o Credential Manager ou a Custom Tab recebiam uma Activity já destruída.
     */
    val pendingAction: LoginAction? = null,
) {
    val isBusy: Boolean
        get() = isSendingCode || isVerifyingCode || isPasskeyLoading || isMicrosoftLoading
}

sealed interface LoginAction {
    /** Abrir o seletor de passkey do sistema com estas opções WebAuthn. */
    data class RequestPasskey(val optionsJson: String) : LoginAction

    /** Abrir a URL de autorização no navegador (Custom Tab). */
    data class OpenBrowser(val url: String) : LoginAction
}
