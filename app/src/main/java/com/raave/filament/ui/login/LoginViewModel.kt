package com.raave.filament.ui.login

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Patterns
import androidx.browser.customtabs.CustomTabsIntent
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raave.filament.data.auth.AuthRepository
import com.raave.filament.data.auth.AuthTokenStore
import com.raave.filament.data.auth.PasskeyChallenge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MICROSOFT_AUTH_CALLBACK_URL = "filament://auth-callback"

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(tokenStore = AuthTokenStore(application))

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onCodeChange(code: String) {
        _uiState.update { it.copy(code = code, errorMessage = null) }
    }

    fun onSendCodeClick() {
        val email = _uiState.value.email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = "Digite um e-mail válido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingCode = true, errorMessage = null) }
            repository.requestEmailCode(email).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSendingCode = false, step = LoginStep.CODE) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSendingCode = false, errorMessage = error.message) }
                },
            )
        }
    }

    fun onBackToEmailClick() {
        _uiState.update { it.copy(step = LoginStep.EMAIL, code = "", errorMessage = null) }
    }

    fun onVerifyCodeClick() {
        val state = _uiState.value
        val code = state.code.trim()
        if (code.length < 6) {
            _uiState.update { it.copy(errorMessage = "Digite o código de 6 dígitos") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingCode = true, errorMessage = null) }
            repository.verifyEmailCode(state.email.trim(), code).fold(
                onSuccess = {
                    _uiState.update { it.copy(isVerifyingCode = false, isSignedIn = true) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isVerifyingCode = false, errorMessage = error.message) }
                },
            )
        }
    }

    fun onPasskeyClick(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPasskeyLoading = true, errorMessage = null) }
            repository.passkeyAuthenticationOptions()
                .fold(
                    onSuccess = { challenge -> authenticateWithCredentialManager(context, challenge) },
                    onFailure = { error -> Result.failure(error) },
                )
                .fold(
                    onSuccess = {
                        _uiState.update { it.copy(isPasskeyLoading = false, isSignedIn = true) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isPasskeyLoading = false, errorMessage = error.message) }
                    },
                )
        }
    }

    private suspend fun authenticateWithCredentialManager(
        context: Context,
        challenge: PasskeyChallenge,
    ): Result<Unit> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val request = GetCredentialRequest(
                credentialOptions = listOf(GetPublicKeyCredentialOption(requestJson = challenge.optionsJson)),
            )
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential as? PublicKeyCredential
                ?: return Result.failure(IllegalStateException("Credencial inesperada recebida do sistema"))
            repository.verifyPasskeyAuthentication(credential.authenticationResponseJson, challenge.challengeCookie)
        } catch (e: GetCredentialException) {
            Result.failure(IllegalStateException(e.message ?: "Não foi possível autenticar com a passkey"))
        }
    }

    fun onMicrosoftClick(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMicrosoftLoading = true, errorMessage = null) }
            repository.startMicrosoftSignIn(MICROSOFT_AUTH_CALLBACK_URL).fold(
                onSuccess = { authorizationUrl ->
                    _uiState.update { it.copy(isMicrosoftLoading = false) }
                    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(authorizationUrl))
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isMicrosoftLoading = false, errorMessage = error.message) }
                },
            )
        }
    }

    /**
     * Chamado pela Activity ao receber de volta o deep link `auth-callback` do fluxo Microsoft,
     * já com um one-time-token (o backend troca o cookie de sessão do navegador por esse token
     * na rota `/native-oauth-bridge` antes de redirecionar pro app — ver AuthRepository).
     */
    fun onMicrosoftCallback(uri: Uri) {
        val token = uri.getQueryParameter("token")
        if (token == null) {
            _uiState.update { it.copy(errorMessage = "Não foi possível concluir o login com a Microsoft") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isMicrosoftLoading = true, errorMessage = null) }
            repository.verifyOneTimeToken(token).fold(
                onSuccess = {
                    _uiState.update { it.copy(isMicrosoftLoading = false, isSignedIn = true) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isMicrosoftLoading = false, errorMessage = error.message) }
                },
            )
        }
    }
}
