package com.raave.filament.util

import android.content.Context
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCapabilities @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Blur só é aplicado quando nada o desaconselha. Reavaliar a cada retomada da tela: a economia de
     * bateria pode ser ligada com o app aberto.
     */
    fun isBlurSupported(): Boolean = !isBlurProblematicDevice() && !isPowerSaveMode()

    /**
     * Alguns aparelhos Samsung até o Android 15 (One UI 7) têm a implementação nativa de blur
     * quebrada, resultando numa camada cinza sobre a tela.
     */
    private fun isBlurProblematicDevice(): Boolean =
        Build.MANUFACTURER.equals("samsung", ignoreCase = true) && Build.VERSION.SDK_INT <= 35

    /** Em economia de bateria, evita o custo de GPU do blur. */
    private fun isPowerSaveMode(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isPowerSaveMode == true
    }
}
