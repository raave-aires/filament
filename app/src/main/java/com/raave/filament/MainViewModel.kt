package com.raave.filament

import androidx.lifecycle.ViewModel
import com.raave.filament.domain.repository.AuthRepository
import com.raave.filament.ui.navigation.ExternalAuthCallback
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    val isSignedIn: StateFlow<Boolean> = authRepository.isSignedIn

    /**
     * Deep link de login guardado aqui (e não na Activity) pra sobreviver a uma rotação entre a
     * chegada do link e a tela de login tratá-lo — o token é de uso único.
     */
    private val _authCallback = MutableStateFlow<ExternalAuthCallback?>(null)
    val authCallback: StateFlow<ExternalAuthCallback?> = _authCallback.asStateFlow()

    fun onAuthCallbackReceived(callback: ExternalAuthCallback) {
        _authCallback.value = callback
    }

    fun onAuthCallbackConsumed() {
        _authCallback.value = null
    }
}
