package com.raave.filament.data.auth

import com.raave.filament.BuildConfig
import com.raave.filament.data.network.ApiException
import com.raave.filament.data.network.ApiResponse
import com.raave.filament.data.network.HttpClient
import java.io.IOException
import java.net.URLEncoder
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

/** Opções do WebAuthn (`PublicKeyCredentialRequestOptionsJSON`) + o cookie de challenge que precisa voltar em [AuthRepository.verifyPasskeyAuthentication]. */
data class PasskeyChallenge(val optionsJson: String, val challengeCookie: String?)

class AuthRepository(
    private val baseUrl: String = BuildConfig.API_BASE_URL,
    private val tokenStore: AuthTokenStore,
) {

    suspend fun requestEmailCode(email: String): Result<Unit> = safeCall {
        HttpClient.postJson(
            url = baseUrl + AuthPaths.EMAIL_OTP_SEND,
            body = JSONObject().put("email", email).put("type", "sign-in"),
        )
    }

    suspend fun verifyEmailCode(email: String, code: String): Result<Unit> = safeCall {
        val response = HttpClient.postJson(
            url = baseUrl + AuthPaths.EMAIL_OTP_SIGN_IN,
            body = JSONObject().put("email", email).put("otp", code),
        )
        storeToken(response)
    }

    suspend fun passkeyAuthenticationOptions(): Result<PasskeyChallenge> {
        return try {
            val response = HttpClient.getJson(url = baseUrl + AuthPaths.PASSKEY_AUTH_OPTIONS)
            Result.success(PasskeyChallenge(response.body.toString(), response.cookieHeaderValue()))
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(IOException(NETWORK_ERROR_MESSAGE, e))
        }
    }

    suspend fun verifyPasskeyAuthentication(
        authenticationResponseJson: String,
        challengeCookie: String?,
    ): Result<Unit> = safeCall {
        val response = HttpClient.postJson(
            url = baseUrl + AuthPaths.PASSKEY_VERIFY_AUTH,
            body = JSONObject().put("response", JSONObject(authenticationResponseJson)),
            headers = challengeCookie?.let { mapOf("Cookie" to it) } ?: emptyMap(),
        )
        storeToken(response)
    }

    /**
     * Inicia o login com a Microsoft. O `callbackURL` mandado pro backend não é o deep link do
     * app diretamente — é a rota `/native-oauth-bridge` do próprio backend, que roda no mesmo
     * domínio da API e por isso recebe o cookie de sessão que o callback da Microsoft acabou de
     * setar (o app nativo nunca teria acesso a esse cookie, só o navegador do Custom Tabs).
     * Essa ponte troca o cookie por um one-time-token e só então redireciona pro deep link real.
     */
    suspend fun startMicrosoftSignIn(deepLinkCallbackUrl: String): Result<String> {
        val bridgeUrl = baseUrl + AuthPaths.NATIVE_OAUTH_BRIDGE +
            "?returnTo=" + URLEncoder.encode(deepLinkCallbackUrl, "UTF-8")
        return try {
            val response = HttpClient.postJson(
                url = baseUrl + AuthPaths.SIGN_IN_SOCIAL,
                body = JSONObject().put("provider", "microsoft").put("callbackURL", bridgeUrl),
            )
            val authorizationUrl = response.body.optString("url").takeIf { it.isNotBlank() }
                ?: return Result.failure(IOException("Resposta inesperada do servidor"))
            Result.success(authorizationUrl)
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(IOException(NETWORK_ERROR_MESSAGE, e))
        }
    }

    /** Troca o one-time-token recebido no deep link de volta (`filament://auth-callback?token=...`) pela sessão. */
    suspend fun verifyOneTimeToken(token: String): Result<Unit> = safeCall {
        val response = HttpClient.postJson(
            url = baseUrl + AuthPaths.ONE_TIME_TOKEN_VERIFY,
            body = JSONObject().put("token", token),
        )
        storeToken(response)
    }

    fun isSignedIn(): Boolean = tokenStore.getToken() != null

    /** Sessão atual (`{ session, user }`) do backend, autenticada pelo bearer token guardado. */
    suspend fun getCurrentSession(): Result<JSONObject> {
        val token = tokenStore.getToken() ?: return Result.failure(IllegalStateException("Sem sessão ativa"))
        return try {
            val response = HttpClient.getJson(
                url = baseUrl + AuthPaths.GET_SESSION,
                headers = mapOf("Authorization" to "Bearer $token"),
            )
            Result.success(response.body)
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(IOException(NETWORK_ERROR_MESSAGE, e))
        }
    }

    /** Sempre limpa o token local, mesmo se a chamada ao backend falhar — logout não pode travar por causa da rede. */
    suspend fun signOut(): Result<Unit> {
        tokenStore.getToken()?.let { token ->
            runCatching {
                HttpClient.postJson(
                    url = baseUrl + AuthPaths.SIGN_OUT,
                    body = JSONObject(),
                    headers = mapOf("Authorization" to "Bearer $token"),
                )
            }
        }
        tokenStore.clear()
        return Result.success(Unit)
    }

    private fun storeToken(response: ApiResponse) {
        response.header("set-auth-token")?.let { tokenStore.saveToken(it) }
    }

    private suspend fun safeCall(block: suspend () -> Unit): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(IOException(NETWORK_ERROR_MESSAGE, e))
        }
    }

    private companion object {
        const val NETWORK_ERROR_MESSAGE = "Falha de conexão. Verifique sua internet e tente novamente."
    }
}
