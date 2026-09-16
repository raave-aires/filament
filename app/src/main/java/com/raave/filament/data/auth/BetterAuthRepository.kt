package com.raave.filament.data.auth

import com.raave.filament.data.network.ApiResponse
import com.raave.filament.data.network.HttpClient
import com.raave.filament.data.network.apiCall
import com.raave.filament.di.BaseUrl
import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.PasskeyChallenge
import com.raave.filament.domain.model.User
import com.raave.filament.domain.repository.AuthRepository
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

// Endpoints do backend Better Auth (montado sob "/api/auth") e da ponte de OAuth nativo.
// Confirmados com o time de backend (sessão "backbone-bc") em 2026-09-14.
private object AuthPaths {
    const val EMAIL_OTP_SEND = "/api/auth/email-otp/send-verification-otp"
    const val EMAIL_OTP_SIGN_IN = "/api/auth/sign-in/email-otp"
    const val PASSKEY_AUTH_OPTIONS = "/api/auth/passkey/generate-authenticate-options"
    const val PASSKEY_VERIFY_AUTH = "/api/auth/passkey/verify-authentication"
    const val SIGN_IN_SOCIAL = "/api/auth/sign-in/social"
    const val NATIVE_OAUTH_BRIDGE = "/native-oauth-bridge"
    const val ONE_TIME_TOKEN_VERIFY = "/api/auth/one-time-token/verify"
    const val GET_SESSION = "/api/auth/get-session"
    const val SIGN_OUT = "/api/auth/sign-out"
}

@Singleton
class BetterAuthRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val tokenStore: AuthTokenStore,
    private val authorizedApiCall: AuthorizedApiCall,
    @param:BaseUrl private val baseUrl: String,
) : AuthRepository {

    override val isSignedIn: StateFlow<Boolean> = tokenStore.hasToken

    override suspend fun requestEmailCode(email: String): AppResult<Unit> = apiCall {
        httpClient.postJson(
            url = baseUrl + AuthPaths.EMAIL_OTP_SEND,
            body = JSONObject().put("email", email).put("type", "sign-in"),
        )
        Unit
    }

    override suspend fun verifyEmailCode(email: String, code: String): AppResult<Unit> = signInCall {
        httpClient.postJson(
            url = baseUrl + AuthPaths.EMAIL_OTP_SIGN_IN,
            body = JSONObject().put("email", email).put("otp", code),
        )
    }

    override suspend fun getPasskeyChallenge(): AppResult<PasskeyChallenge> = apiCall {
        val response = httpClient.getJson(url = baseUrl + AuthPaths.PASSKEY_AUTH_OPTIONS)
        PasskeyChallenge(optionsJson = response.body.toString(), handle = response.cookieHeaderValue())
    }

    override suspend fun verifyPasskey(responseJson: String, challenge: PasskeyChallenge): AppResult<Unit> =
        signInCall {
            httpClient.postJson(
                url = baseUrl + AuthPaths.PASSKEY_VERIFY_AUTH,
                body = JSONObject().put("response", JSONObject(responseJson)),
                headers = challenge.handle?.let { mapOf("Cookie" to it) } ?: emptyMap(),
            )
        }

    /**
     * O `callbackURL` mandado pro backend não é o deep link do app diretamente — é a rota
     * `/native-oauth-bridge` do próprio backend, que roda no mesmo domínio da API e por isso recebe
     * o cookie de sessão que o callback da Microsoft acabou de setar (o app nativo nunca teria acesso
     * a esse cookie, só o navegador do Custom Tabs). Essa ponte troca o cookie por um token de uso
     * único e só então redireciona pro deep link real ([AuthCallback.URL]).
     */
    override suspend fun startMicrosoftSignIn(): AppResult<String> {
        val bridgeUrl = baseUrl + AuthPaths.NATIVE_OAUTH_BRIDGE +
            "?returnTo=" + URLEncoder.encode(AuthCallback.URL, "UTF-8")
        val result = apiCall {
            httpClient.postJson(
                url = baseUrl + AuthPaths.SIGN_IN_SOCIAL,
                body = JSONObject().put("provider", "microsoft").put("callbackURL", bridgeUrl),
            ).body.optString("url").takeIf(String::isNotBlank)
        }
        return when (result) {
            is AppResult.Success -> result.value?.let { AppResult.Success(it) }
                ?: AppResult.Failure(AppError.UnexpectedResponse)
            is AppResult.Failure -> result
        }
    }

    override suspend fun completeExternalSignIn(oneTimeToken: String): AppResult<Unit> = signInCall {
        httpClient.postJson(
            url = baseUrl + AuthPaths.ONE_TIME_TOKEN_VERIFY,
            body = JSONObject().put("token", oneTimeToken),
        )
    }

    override suspend fun getCurrentUser(): AppResult<User> {
        val result = authorizedApiCall { headers ->
            httpClient.getJson(url = baseUrl + AuthPaths.GET_SESSION, headers = headers)
                .body.optJSONObject("user")
                ?.let { user ->
                    User(
                        name = user.optString("name").takeIf(String::isNotBlank),
                        email = user.optString("email").takeIf(String::isNotBlank),
                    )
                }
        }
        return when (result) {
            is AppResult.Success -> result.value?.let { AppResult.Success(it) } ?: run {
                // Better Auth responde 200 com corpo `null` quando o token não corresponde mais a
                // uma sessão: é sessão inválida, não erro de rede.
                tokenStore.clear()
                AppResult.Failure(AppError.Unauthorized)
            }
            is AppResult.Failure -> result
        }
    }

    override suspend fun signOut() {
        // Melhor esforço no backend; logout local não pode travar por causa da rede.
        authorizedApiCall { headers ->
            httpClient.postJson(url = baseUrl + AuthPaths.SIGN_OUT, body = JSONObject(), headers = headers)
        }
        tokenStore.clear()
    }

    /** Chamada de login: o token de sessão chega no header `set-auth-token` da resposta. */
    private suspend fun signInCall(block: suspend () -> ApiResponse): AppResult<Unit> {
        val result = apiCall { block().header("set-auth-token") }
        return when (result) {
            is AppResult.Success -> result.value?.let { token ->
                tokenStore.save(token)
                AppResult.Success(Unit)
            } ?: AppResult.Failure(AppError.UnexpectedResponse)
            is AppResult.Failure -> result
        }
    }
}
