package com.raave.filament.domain.repository

import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.PasskeyChallenge
import com.raave.filament.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    /**
     * Fonte de verdade da navegação entre login e área logada: muda ao entrar, ao sair e quando
     * qualquer chamada autenticada recebe 401.
     */
    val isSignedIn: StateFlow<Boolean>

    suspend fun requestEmailCode(email: String): AppResult<Unit>

    suspend fun verifyEmailCode(email: String, code: String): AppResult<Unit>

    suspend fun getPasskeyChallenge(): AppResult<PasskeyChallenge>

    suspend fun verifyPasskey(responseJson: String, challenge: PasskeyChallenge): AppResult<Unit>

    /** Devolve a URL de autorização da Microsoft, a abrir no navegador (Custom Tab). */
    suspend fun startMicrosoftSignIn(): AppResult<String>

    /** Troca o token de uso único recebido no deep link de volta do navegador pela sessão. */
    suspend fun completeExternalSignIn(oneTimeToken: String): AppResult<Unit>

    suspend fun getCurrentUser(): AppResult<User>

    /** Última conta carregada nesta sessão, guardada no aparelho; `null` se ainda não houve carga. */
    fun getCachedUser(): User?

    /** Sempre encerra a sessão local, mesmo que o backend não responda. */
    suspend fun signOut()
}
