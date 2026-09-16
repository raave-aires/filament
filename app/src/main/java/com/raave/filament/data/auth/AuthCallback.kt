package com.raave.filament.data.auth

/**
 * Deep link de retorno do login social aberto no navegador. Precisa bater com o intent-filter da
 * MainActivity no AndroidManifest.xml.
 */
object AuthCallback {
    const val SCHEME = "filament"
    const val HOST = "auth-callback"
    const val URL = "$SCHEME://$HOST"

    /** Parâmetro com o token de uso único que a ponte do backend anexa ao deep link. */
    const val TOKEN_PARAMETER = "token"
}
