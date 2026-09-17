package com.raave.filament.data.local

import com.raave.filament.data.auth.AuthTokenStore
import com.raave.filament.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Garante que dados de uma conta nunca aparecem pra outra.
 *
 * - [clear] antes de gravar o token de um novo login (chamado pelo repositório de auth): é o ponto que
 *   torna a troca de conta segura — a Home só abre depois do token, então nunca vê o cache anterior.
 * - [start] observa a sessão: sem token (sair, 401, app aberto deslogado), o cache é apagado pra não
 *   ficar histórico de chamados parado no aparelho.
 */
@Singleton
class SessionCacheCleaner @Inject constructor(
    private val database: FilamentDatabase,
    private val tokenStore: AuthTokenStore,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    suspend fun clear() {
        database.clearAllTables()
    }

    fun start() {
        tokenStore.hasToken
            .filter { hasToken -> !hasToken }
            .onEach { clear() }
            .launchIn(scope)
    }
}
