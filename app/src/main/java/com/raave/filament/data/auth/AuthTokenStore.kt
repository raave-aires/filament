package com.raave.filament.data.auth

import android.content.Context
import androidx.core.content.edit

/**
 * Guarda o bearer token da sessão (header `set-auth-token` retornado pelo backend Better Auth).
 * TODO: trocar por EncryptedSharedPreferences (androidx.security-crypto) antes de ir pra produção.
 */
class AuthTokenStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit { putString(KEY_TOKEN, token) }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun clear() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    private companion object {
        const val PREFS_NAME = "filament_auth_prefs"
        const val KEY_TOKEN = "bearer_token"
    }
}
