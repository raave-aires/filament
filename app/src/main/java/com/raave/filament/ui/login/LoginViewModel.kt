package com.raave.filament.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.PasskeyChallenge
import com.raave.filament.domain.repository.AuthRepository
import com.raave.filament.domain.usecase.RequestEmailCodeUseCase
import com.raave.filament.domain.usecase.VerifyEmailCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Login concluído não é sinalizado aqui: o token salvo muda [AuthRepository.isSignedIn] e a navegação
 * troca de tela sozinha (ver ui/navigation/FilamentNavigation.kt).
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val requestEmailCode: RequestEmailCodeUseCase,
    private val verifyEmailCode: VerifyEmailCodeUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Desafio em andamento: precisa voltar junto com a resposta do Credential Manager. */
    private var passkeyChallenge: PasskeyChallenge? = null

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onCodeChange(code: String) {
        _uiState.update { it.copy(code = code, error = null) }
    }

    fun onSendCodeClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingCode = true, error = null) }
            when (val result = requestEmailCode(_uiState.value.email)) {
                is AppResult.Success -> _uiState.update { it.copy(isSendingCode = false, step = LoginStep.CODE) }
                is AppResult.Failure -> _uiState.update { it.copy(isSendingCode = false, error = result.error) }
            }
        }
    }

    fun onBackToEmailClick() {
        _uiState.update { it.copy(step = LoginStep.EMAIL, code = "", error = null) }
    }

    fun onVerifyCodeClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingCode = true, error = null) }
            val state = _uiState.value
            val result = verifyEmailCode(state.email, state.code)
            // No sucesso a tela sai de cena; o loading só é desligado se ficarmos aqui.
            if (result is AppResult.Failure) {
                _uiState.update { it.copy(isVerifyingCode = false, error = result.error) }
            }
        }
    }

    fun onPasskeyClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPasskeyLoading = true, error = null) }
            when (val result = authRepository.getPasskeyChallenge()) {
                is AppResult.Success -> {
                    passkeyChallenge = result.value
                    _uiState.update { it.copy(pendingAction = LoginAction.RequestPasskey(result.value.optionsJson)) }
                }
                is AppResult.Failure -> _uiState.update { it.copy(isPasskeyLoading = false, error = result.error) }
            }
        }
    }

    fun onPasskeyCredential(responseJson: String) {
        val challenge = passkeyChallenge ?: return onPasskeyFailed(AppError.ExternalSignInFailed)
        _uiState.update { it.copy(pendingAction = null) }
        viewModelScope.launch {
            val result = authRepository.verifyPasskey(responseJson, challenge)
            passkeyChallenge = null
            if (result is AppResult.Failure) {
                _uiState.update { it.copy(isPasskeyLoading = false, error = result.error) }
            }
        }
    }

    /** Usuário fechou o seletor de passkey: não é erro, só volta ao estado inicial. */
    fun onPasskeyCancelled() {
        passkeyChallenge = null
        _uiState.update { it.copy(isPasskeyLoading = false, pendingAction = null) }
    }

    fun onPasskeyFailed(error: AppError) {
        passkeyChallenge = null
        _uiState.update { it.copy(isPasskeyLoading = false, pendingAction = null, error = error) }
    }

    fun onMicrosoftClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMicrosoftLoading = true, error = null) }
            when (val result = authRepository.startMicrosoftSignIn()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isMicrosoftLoading = false, pendingAction = LoginAction.OpenBrowser(result.value))
                }
                is AppResult.Failure -> _uiState.update { it.copy(isMicrosoftLoading = false, error = result.error) }
            }
        }
    }

    fun onBrowserOpened() {
        _uiState.update { it.copy(pendingAction = null) }
    }

    /**
     * Volta do navegador pelo deep link. [oneTimeToken] nulo significa que o link chegou sem o token —
     * a ponte do backend não concluiu o login.
     */
    fun onExternalSignInCallback(oneTimeToken: String?) {
        if (oneTimeToken == null) {
            _uiState.update { it.copy(error = AppError.ExternalSignInFailed) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isMicrosoftLoading = true, error = null) }
            val result = authRepository.completeExternalSignIn(oneTimeToken)
            if (result is AppResult.Failure) {
                _uiState.update { it.copy(isMicrosoftLoading = false, error = result.error) }
            }
        }
    }
}
