package com.raave.filament

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raave.filament.data.auth.AuthCallback
import com.raave.filament.ui.components.LocalBlurEnabled
import com.raave.filament.ui.navigation.ExternalAuthCallback
import com.raave.filament.ui.navigation.FilamentNavigation
import com.raave.filament.ui.theme.FilamentTheme
import dagger.hilt.android.AndroidEntryPoint

/** Activity única: login, área logada e chat são destinos de navegação (ver FilamentNavigation). */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A barra flutuante desenha sobre a navigation bar; sem isso o sistema aplica um fundo
        // de contraste atrás dela e quebra o efeito de blur/transparência.
        window.isNavigationBarContrastEnforced = false

        // Numa recriação (rotação) o intent original ainda é o deep link, já tratado antes.
        if (savedInstanceState == null) handleAuthCallback(intent)

        setContent {
            FilamentTheme {
                val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
                val authCallback by viewModel.authCallback.collectAsStateWithLifecycle()
                val isBlurEnabled by viewModel.isBlurEnabled.collectAsStateWithLifecycle()
                LifecycleResumeEffect(Unit) {
                    viewModel.refreshBlurSupport()
                    onPauseOrDispose {}
                }
                CompositionLocalProvider(LocalBlurEnabled provides isBlurEnabled) {
                    FilamentNavigation(
                        isSignedIn = isSignedIn,
                        authCallback = authCallback,
                        onAuthCallbackConsumed = viewModel::onAuthCallbackConsumed,
                    )
                }
            }
        }
    }

    // launchMode="singleTask": a Custom Tab do login fica por cima desta Activity e o deep link de
    // volta chega aqui, fechando a aba em vez de abrir uma segunda instância do app.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthCallback(intent)
    }

    private fun handleAuthCallback(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme == AuthCallback.SCHEME && uri.host == AuthCallback.HOST) {
            viewModel.onAuthCallbackReceived(
                ExternalAuthCallback(oneTimeToken = uri.getQueryParameter(AuthCallback.TOKEN_PARAMETER)),
            )
        }
    }
}
