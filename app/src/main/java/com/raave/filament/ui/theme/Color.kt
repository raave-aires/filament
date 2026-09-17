package com.raave.filament.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp

/**
 * Tokens do tema, no vocabulário do shadcn/ui (paleta "Mist"). Os componentes continuam sendo do
 * Material 3: [toColorScheme] traduz estes tokens para os papéis do ColorScheme, e só o que o
 * Material não tem papel equivalente (borda, card, vidro) é lido direto daqui via
 * `FilamentTheme.colors`.
 *
 * Valores convertidos de OKLCH para sRGB com mapeamento de gamut por redução de croma (como os
 * navegadores fazem), não por recorte de canal — `destructive` e `sidebarPrimary` do escuro caem
 * fora do sRGB. Não recalcular à mão.
 */
@Immutable
data class FilamentColors(
    val background: Color,
    val foreground: Color,
    val card: Color,
    val cardForeground: Color,
    val popover: Color,
    val popoverForeground: Color,
    val primary: Color,
    val primaryForeground: Color,
    val secondary: Color,
    val secondaryForeground: Color,
    val muted: Color,
    val mutedForeground: Color,
    val accent: Color,
    val accentForeground: Color,
    val destructive: Color,
    val border: Color,
    val input: Color,
    val ring: Color,
    val sidebar: Color,
    val sidebarForeground: Color,
    val sidebarPrimary: Color,
    val sidebarPrimaryForeground: Color,
    val sidebarAccent: Color,
    val sidebarAccentForeground: Color,
    val sidebarBorder: Color,
    val sidebarRing: Color,
    /**
     * Cor de destaque para TEXTO e indicadores sobre o fundo (links, foco, "último chamado"). Não é
     * token do shadcn: no escuro o `primary` (#193CB8) só serve de preenchimento — como texto sobre o
     * fundo dá 2,2:1. Claro: `primary` (6,8:1). Escuro: `sidebarPrimary` (5,3:1).
     */
    val primaryText: Color,
    /** Tinta translúcida das superfícies de vidro fosco (barras), aplicada sobre o conteúdo desfocado. */
    val glassTint: Color,
    /** Contorno de 1px das superfícies de vidro, pra separá-las do conteúdo que passa por trás. */
    val glassBorder: Color,
)

val MistLight = FilamentColors(
    background = Color(0xFFFFFFFF),
    foreground = Color(0xFF090B0C),
    card = Color(0xFFFFFFFF),
    cardForeground = Color(0xFF090B0C),
    popover = Color(0xFFFFFFFF),
    popoverForeground = Color(0xFF090B0C),
    primary = Color(0xFF1447E6),
    primaryForeground = Color(0xFFEFF6FF),
    secondary = Color(0xFFF4F4F5),
    secondaryForeground = Color(0xFF18181B),
    muted = Color(0xFFF1F3F3),
    mutedForeground = Color(0xFF67787C),
    accent = Color(0xFFF1F3F3),
    accentForeground = Color(0xFF161B1D),
    destructive = Color(0xFFE40016),
    border = Color(0xFFE3E7E8),
    input = Color(0xFFE3E7E8),
    ring = Color(0xFF9CA8AB),
    sidebar = Color(0xFFF9FBFB),
    sidebarForeground = Color(0xFF090B0C),
    sidebarPrimary = Color(0xFF155DFC),
    sidebarPrimaryForeground = Color(0xFFEFF6FF),
    sidebarAccent = Color(0xFFF1F3F3),
    sidebarAccentForeground = Color(0xFF161B1D),
    sidebarBorder = Color(0xFFE3E7E8),
    sidebarRing = Color(0xFF9CA8AB),
    primaryText = Color(0xFF1447E6),
    glassTint = Color(0xFFFFFFFF).copy(alpha = 0.72f),
    glassBorder = Color(0xFFE3E7E8),
)

val MistDark = FilamentColors(
    background = Color(0xFF090B0C),
    foreground = Color(0xFFF9FBFB),
    card = Color(0xFF161B1D),
    cardForeground = Color(0xFFF9FBFB),
    popover = Color(0xFF161B1D),
    popoverForeground = Color(0xFFF9FBFB),
    primary = Color(0xFF193CB8),
    primaryForeground = Color(0xFFEFF6FF),
    secondary = Color(0xFF27272A),
    secondaryForeground = Color(0xFFFAFAFA),
    muted = Color(0xFF22292B),
    mutedForeground = Color(0xFF9CA8AB),
    accent = Color(0xFF22292B),
    accentForeground = Color(0xFFF9FBFB),
    destructive = Color(0xFFFF6568),
    border = Color(0x1AFFFFFF),
    input = Color(0x26FFFFFF),
    ring = Color(0xFF67787C),
    sidebar = Color(0xFF161B1D),
    sidebarForeground = Color(0xFFF9FBFB),
    sidebarPrimary = Color(0xFF3280FF),
    sidebarPrimaryForeground = Color(0xFFEFF6FF),
    sidebarAccent = Color(0xFF22292B),
    sidebarAccentForeground = Color(0xFFF9FBFB),
    sidebarBorder = Color(0x1AFFFFFF),
    sidebarRing = Color(0xFF67787C),
    primaryText = Color(0xFF3280FF),
    glassTint = Color(0xFF090B0C).copy(alpha = 0.64f),
    glassBorder = Color(0x1AFFFFFF),
)

/**
 * Mapeia os tokens shadcn para os papéis do Material 3, que é o que os componentes leem.
 *
 * - O shadcn não tem "containers" tonais: `primaryContainer` é o próprio `primary`, e
 *   `secondaryContainer` o `secondary` (botões tonais viram o botão secundário cinza).
 * - Bordas translúcidas do escuro são compostas sobre o fundo: alguns componentes do M3 assumem
 *   `outline` opaco.
 * - `surfaceTint` transparente desliga a sobreposição de cor por elevação, que não existe no shadcn.
 */
fun FilamentColors.toColorScheme(dark: Boolean): ColorScheme {
    val outline = input.compositeOver(background)
    val outlineVariant = border.compositeOver(background)
    val errorContainer = destructive.copy(alpha = 0.12f).compositeOver(background)
    // Rampa de superfícies monotônica entre o fundo e o `muted`, passando pelo card (escuro) ou pela
    // sidebar (claro) — é o degradê que o M3 usa pra diferenciar camadas.
    val lowest = background
    val low = if (dark) lerp(background, card, 0.5f) else sidebar
    val mid = if (dark) card else lerp(sidebar, muted, 0.5f)
    val high = if (dark) lerp(card, muted, 0.5f) else muted
    val highest = if (dark) muted else lerp(muted, outlineVariant, 0.35f)

    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = primaryForeground,
        primaryContainer = primary,
        onPrimaryContainer = primaryForeground,
        inversePrimary = sidebarPrimary,
        secondary = secondary,
        onSecondary = secondaryForeground,
        secondaryContainer = secondary,
        onSecondaryContainer = secondaryForeground,
        tertiary = sidebarPrimary,
        onTertiary = sidebarPrimaryForeground,
        tertiaryContainer = accent,
        onTertiaryContainer = accentForeground,
        background = background,
        onBackground = foreground,
        surface = background,
        onSurface = foreground,
        surfaceVariant = muted,
        onSurfaceVariant = mutedForeground,
        surfaceTint = Color.Transparent,
        inverseSurface = foreground,
        inverseOnSurface = background,
        error = destructive,
        onError = Color.White,
        errorContainer = errorContainer,
        onErrorContainer = destructive,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = Color.Black,
        surfaceBright = if (dark) muted else background,
        surfaceDim = if (dark) background else muted,
        surfaceContainerLowest = lowest,
        surfaceContainerLow = low,
        surfaceContainer = mid,
        surfaceContainerHigh = high,
        surfaceContainerHighest = highest,
        // Sem isso as variantes "fixed" herdariam o roxo de base do Material.
        primaryFixed = primary,
        primaryFixedDim = primary,
        onPrimaryFixed = primaryForeground,
        onPrimaryFixedVariant = primaryForeground,
        secondaryFixed = secondary,
        secondaryFixedDim = secondary,
        onSecondaryFixed = secondaryForeground,
        onSecondaryFixedVariant = secondaryForeground,
        tertiaryFixed = accent,
        tertiaryFixedDim = accent,
        onTertiaryFixed = accentForeground,
        onTertiaryFixedVariant = accentForeground,
    )
}
