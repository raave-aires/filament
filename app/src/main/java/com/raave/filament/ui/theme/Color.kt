package com.raave.filament.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Azul de marca da Elinsa. As paletas abaixo foram geradas a partir dessa seed com o
// mesmo algoritmo tonal (HCT/CAM16, variante TonalSpot) que o Material3 usa em
// dynamicColorScheme — só que "sementeado" pela marca, não pelo wallpaper do usuário.
// Gerado com @material/material-color-utilities (Google, MIT/Apache-2.0) e sobreposto
// à mão no esquema escuro (ver comentário abaixo); não recalcular à mão.
val ElinsaSeed = Color(0xFF24A3DD)

val ElinsaLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF206487),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC6E7FF),
    onPrimaryContainer = Color(0xFF004C6B),
    inversePrimary = Color(0xFF92CEF5),
    secondary = Color(0xFF4F616E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E5F4),
    onSecondaryContainer = Color(0xFF374955),
    tertiary = Color(0xFF62597C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8DDFF),
    onTertiaryContainer = Color(0xFF4A4263),
    background = Color(0xFFF6FAFE),
    onBackground = Color(0xFF181C1F),
    surface = Color(0xFFF6FAFE),
    onSurface = Color(0xFF181C1F),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41484D),
    surfaceTint = Color(0xFF206487),
    inverseSurface = Color(0xFF2C3135),
    inverseOnSurface = Color(0xFFEEF1F6),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF71787E),
    outlineVariant = Color(0xFFC1C7CE),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF6FAFE),
    surfaceDim = Color(0xFFD7DADF),
    surfaceContainer = Color(0xFFEBEEF3),
    surfaceContainerHigh = Color(0xFFE5E8ED),
    surfaceContainerHighest = Color(0xFFDFE3E7),
    surfaceContainerLow = Color(0xFFF0F4F8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    primaryFixed = Color(0xFFC6E7FF),
    primaryFixedDim = Color(0xFF92CEF5),
    onPrimaryFixed = Color(0xFF001E2D),
    onPrimaryFixedVariant = Color(0xFF004C6B),
    secondaryFixed = Color(0xFFD2E5F4),
    secondaryFixedDim = Color(0xFFB6C9D8),
    onSecondaryFixed = Color(0xFF0A1D28),
    onSecondaryFixedVariant = Color(0xFF374955),
    tertiaryFixed = Color(0xFFE8DDFF),
    tertiaryFixedDim = Color(0xFFCCC1E9),
    onTertiaryFixed = Color(0xFF1E1635),
    onTertiaryFixedVariant = Color(0xFF4A4263),
)

// Igual ao esquema escuro gerado pelo algoritmo, exceto background/surface/surfaceDim/
// surfaceContainerLowest: forçados a preto puro (em vez do cinza-azulado #0F1417 que o
// Material gera) pro visual "AMOLED" à la Samsung. Os demais surfaceContainer* seguem
// a rampa original, criando o degradê de elevação dos cards sobre o fundo preto.
val ElinsaDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF92CEF5),
    onPrimary = Color(0xFF00344B),
    primaryContainer = Color(0xFF004C6B),
    onPrimaryContainer = Color(0xFFC6E7FF),
    inversePrimary = Color(0xFF206487),
    secondary = Color(0xFFB6C9D8),
    onSecondary = Color(0xFF21323E),
    secondaryContainer = Color(0xFF374955),
    onSecondaryContainer = Color(0xFFD2E5F4),
    tertiary = Color(0xFFCCC1E9),
    onTertiary = Color(0xFF332C4B),
    tertiaryContainer = Color(0xFF4A4263),
    onTertiaryContainer = Color(0xFFE8DDFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFDFE3E7),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFDFE3E7),
    surfaceVariant = Color(0xFF41484D),
    onSurfaceVariant = Color(0xFFC1C7CE),
    surfaceTint = Color(0xFF92CEF5),
    inverseSurface = Color(0xFFDFE3E7),
    inverseOnSurface = Color(0xFF2C3135),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8B9198),
    outlineVariant = Color(0xFF41484D),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF353A3D),
    surfaceDim = Color(0xFF000000),
    surfaceContainer = Color(0xFF1C2024),
    surfaceContainerHigh = Color(0xFF262B2E),
    surfaceContainerHighest = Color(0xFF313539),
    surfaceContainerLow = Color(0xFF181C1F),
    surfaceContainerLowest = Color(0xFF000000),
    primaryFixed = Color(0xFFC6E7FF),
    primaryFixedDim = Color(0xFF92CEF5),
    onPrimaryFixed = Color(0xFF001E2D),
    onPrimaryFixedVariant = Color(0xFF004C6B),
    secondaryFixed = Color(0xFFD2E5F4),
    secondaryFixedDim = Color(0xFFB6C9D8),
    onSecondaryFixed = Color(0xFF0A1D28),
    onSecondaryFixedVariant = Color(0xFF374955),
    tertiaryFixed = Color(0xFFE8DDFF),
    tertiaryFixedDim = Color(0xFFCCC1E9),
    onTertiaryFixed = Color(0xFF1E1635),
    onTertiaryFixedVariant = Color(0xFF4A4263),
)
