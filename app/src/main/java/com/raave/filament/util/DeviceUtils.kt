package com.raave.filament.util

import android.content.Context
import android.os.Build
import android.os.PowerManager

object DeviceUtils {

    /**
     * Alguns aparelhos Samsung até o Android 15 (One UI 7) têm a implementação nativa de blur
     * quebrada, resultando numa camada cinza sobre a tela. Nesses casos o blur é desligado.
     */
    fun isBlurProblematicDevice(): Boolean =
        Build.MANUFACTURER.equals("samsung", ignoreCase = true) && Build.VERSION.SDK_INT <= 35

    /** Em economia de bateria, evita o custo de GPU do blur. */
    fun isPowerSaveMode(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isPowerSaveMode == true
    }

    /** Blur só deve ser aplicado quando nenhuma dessas condições o desaconselha. */
    fun isBlurSupported(context: Context): Boolean =
        !isBlurProblematicDevice() && !isPowerSaveMode(context)
}
