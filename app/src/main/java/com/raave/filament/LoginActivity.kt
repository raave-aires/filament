package com.raave.filament

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.raave.filament.ui.LoginScreen
import com.raave.filament.ui.theme.FilamentTheme

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FilamentTheme {
                LoginScreen()
            }
        }
    }
}
