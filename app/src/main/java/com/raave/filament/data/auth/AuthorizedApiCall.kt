package com.raave.filament.data.auth

import com.raave.filament.data.network.apiCall
import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executa uma chamada com o bearer token da sessão. Ponto único que decide o fim da sessão: um 401
 * em qualquer endpoint descarta o token, e a navegação volta pro login sozinha (ver
 * [com.raave.filament.domain.repository.AuthRepository.isSignedIn]). Antes cada repositório repetia
 * a montagem do header e só a checagem inicial de sessão tratava a expiração.
 */
@Singleton
class AuthorizedApiCall @Inject constructor(
    private val tokenStore: AuthTokenStore,
) {
    suspend operator fun <T> invoke(block: suspend (authHeaders: Map<String, String>) -> T): AppResult<T> {
        val token = tokenStore.token.value ?: return AppResult.Failure(AppError.Unauthorized)
        val result = apiCall { block(mapOf("Authorization" to "Bearer $token")) }
        if (result is AppResult.Failure && result.error == AppError.Unauthorized) {
            tokenStore.clearIfCurrent(token)
        }
        return result
    }
}
