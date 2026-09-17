package com.raave.filament

import androidx.lifecycle.ViewModel
import com.raave.filament.domain.repository.AuthRepository
import com.raave.filament.ui.navigation.ExternalAuthCallback
import com.raave.filament.util.DeviceCapabilities
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val deviceCapabilities: DeviceCapabilities,
) : ViewModel() {

    val isSignedIn: StateFlow<Boolean> = authRepository.isSignedIn

    private val _isBlurEnabled = MutableStateFlow(deviceCapabilities.isBlurSupported())

    /** Se as bordas do conteúdo sob os controles flutuantes podem ter blur (ver LocalBlurEnabled). */
    val isBlurEnabled: StateFlow<Boolean> = _isBlurEnabled.asStateFlow()

    /** Reavaliado a cada retomada: a economia de bateria pode ser ligada com o app aberto. */
    fun refreshBlurSupport() {
        _isBlurEnabled.value = deviceCapabilities.isBlurSupported()
    }

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
