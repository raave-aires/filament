package com.raave.filament

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.raave.filament.ui.HomeScreen
import com.raave.filament.ui.theme.FilamentTheme

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A barra flutuante desenha sobre a navigation bar; sem isso o sistema aplica um fundo
        // de contraste atrás dela e quebra o efeito de blur/transparência.
        window.isNavigationBarContrastEnforced = false
        setContent {
            FilamentTheme {
                HomeScreen(onSignedOut = ::navigateToLogin)
            }
        }
    }

    private fun navigateToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        finish()
    }
}
