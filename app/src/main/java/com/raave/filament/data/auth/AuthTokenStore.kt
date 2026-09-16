package com.raave.filament.data.auth

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Guarda o bearer token da sessão (header `set-auth-token` retornado pelo backend Better Auth).
 * Fica fora do backup na nuvem e da transferência entre aparelhos (backup_rules.xml e data_extraction_rules.xml).
 * TODO: criptografar antes de ir pra produção (a androidx.security-crypto foi descontinuada;
 *  avaliar Keystore direto ou Tink).
 */
@Singleton
class AuthTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _token = MutableStateFlow(prefs.getString(KEY_TOKEN, null))
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _hasToken = MutableStateFlow(_token.value != null)
    val hasToken: StateFlow<Boolean> = _hasToken.asStateFlow()

    @Synchronized
    fun save(token: String) {
        prefs.edit { putString(KEY_TOKEN, token) }
        _token.value = token
        _hasToken.value = true
    }

    @Synchronized
    fun clear() {
        prefs.edit { remove(KEY_TOKEN) }
        _token.value = null
        _hasToken.value = false
    }

    /**
     * Só limpa se o token ainda for o que foi usado na chamada recusada: uma resposta 401 atrasada
     * de uma sessão antiga não pode derrubar um login que aconteceu no meio tempo.
     */
    @Synchronized
    fun clearIfCurrent(token: String) {
        if (_token.value == token) clear()
    }

    private companion object {
        // Nome referenciado em res/xml/backup_rules.xml e data_extraction_rules.xml.
        const val PREFS_NAME = "filament_auth_prefs"
        const val KEY_TOKEN = "bearer_token"
    }
}
