package com.raave.filament

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.raave.filament.data.auth.AuthTokenStore
import com.raave.filament.ui.LoginScreen
import com.raave.filament.ui.login.LoginViewModel
import com.raave.filament.ui.theme.FilamentTheme

class LoginActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (AuthTokenStore(this).getToken() != null) {
            // Já existe uma sessão guardada (ver AuthTokenStore) — pula a tela de login.
            navigateToHome()
            return
        }

        setContent {
            FilamentTheme {
                LoginScreen(viewModel = loginViewModel, onLoginSuccess = ::navigateToHome)
            }
        }
        handleMicrosoftCallback(intent)
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleMicrosoftCallback(intent)
    }

    // Recebe de volta o deep link `filament://auth-callback` aberto pela Custom Tab depois do
    // login social (ver o intent-filter no manifest e LoginViewModel.onMicrosoftClick).
    private fun handleMicrosoftCallback(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme == "filament" && uri.host == "auth-callback") {
            loginViewModel.onMicrosoftCallback(uri)
        }
    }
}
