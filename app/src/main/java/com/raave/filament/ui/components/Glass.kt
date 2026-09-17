package com.raave.filament.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.raave.filament.ui.theme.FilamentTheme
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState

/**
 * Se o aparelho aguenta blur em tempo real. Falso em Samsung com a implementação nativa quebrada e em
 * economia de bateria (ver [com.raave.filament.util.DeviceCapabilities]); aí o vidro vira só a tinta
 * translúcida, mais opaca, sem desfoque. Fornecido pela MainActivity e reavaliado a cada retomada.
 */
val LocalBlurEnabled = compositionLocalOf { true }

/** Desfoque do vidro: forte o bastante pra texto que passa por trás não competir com o da barra. */
private val GlassBlurRadius = 24.dp

/** Sem blur, a tinta sozinha precisa esconder mais do conteúdo pra o texto da barra continuar legível. */
private const val FallbackTintAlpha = 0.9f

/**
 * Estado que liga o conteúdo registrado com `hazeSource` às superfícies de vidro que o desfocam.
 * Já respeita [LocalBlurEnabled].
 */
@Composable
fun rememberGlassState(): HazeState = rememberHazeState(blurEnabled = LocalBlurEnabled.current)

/**
 * Aplica o vidro fosco: o conteúdo de [state] que passa por trás do componente é desfocado e recebe a
 * tinta translúcida do tema.
 *
 * @param progressive intensidade variável ao longo da superfície; `null` = uniforme.
 */
@Composable
fun Modifier.glass(state: HazeState, progressive: HazeProgressive? = null): Modifier {
    val colors = FilamentTheme.colors
    val style = remember(colors) {
        HazeStyle(
            backgroundColor = colors.background,
            tint = HazeTint(colors.glassTint),
            blurRadius = GlassBlurRadius,
            // Sem granulado: o ruído padrão do Haze suja o fundo liso da paleta.
            noiseFactor = 0f,
            fallbackTint = HazeTint(colors.glassTint.copy(alpha = FallbackTintAlpha)),
        )
    }
    return hazeEffect(state = state, style = style) {
        this.progressive = progressive
    }
}

/**
 * Faixa de vidro atrás da status bar, em telas sem barra superior: o conteúdo some desfocado sob os
 * ícones do sistema em vez de ser cortado ou brigar com eles. Esvanece pra baixo, sem borda visível.
 */
@Composable
fun StatusBarGlass(state: HazeState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .glass(
                state = state,
                progressive = remember {
                    HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                },
            ),
    )
}

enum class HairlineEdge { Top, Bottom }

/**
 * Linha de 1px numa das bordas: separa barras de vidro do conteúdo que passa por trás, no lugar
 * da sombra (que atrás de uma superfície translúcida aparece por dentro, como mancha).
 */
fun Modifier.hairline(color: Color, edge: HairlineEdge): Modifier = drawWithContent {
    drawContent()
    val stroke = 1.dp.toPx()
    val y = if (edge == HairlineEdge.Top) stroke / 2 else size.height - stroke / 2
    drawLine(color = color, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = stroke)
}
