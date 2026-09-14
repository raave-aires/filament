package com.raave.filament.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.mutableStateOf

/**
 * Feedback tátil centralizado, para manter a resposta consistente em todo o app.
 * O toggle global permite desligar haptics por preferência do usuário.
 */
object HapticUtil {

    val isEnabled = mutableStateOf(true)

    /** Toque padrão de UI (sensação de tecla). */
    fun performUIHaptic(view: View) {
        if (!isEnabled.value) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** Tick leve, para seleção/rolagem. */
    fun performLightHaptic(view: View) {
        if (!isEnabled.value) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Toque mais firme, para ações significativas (botões, navegação). */
    fun performHeavyHaptic(view: View) {
        if (!isEnabled.value) return
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}
