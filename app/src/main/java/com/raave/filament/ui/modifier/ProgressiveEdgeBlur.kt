package com.raave.filament.ui.modifier

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.raave.filament.ui.theme.FilamentTheme

/**
 * Se o aparelho aguenta blur em tempo real. Falso em Samsung com a implementação nativa quebrada e em
 * economia de bateria (ver [com.raave.filament.util.DeviceCapabilities]); aí as bordas ficam só com o
 * degradê. Fornecido pela MainActivity e reavaliado a cada retomada.
 */
val LocalBlurEnabled = compositionLocalOf { true }

/** Desfoque máximo junto à borda da tela; cai a zero no fim da faixa ocupada pelos controles. */
private val EdgeBlurRadius = 24.dp

/** Opacidade do fundo no degradê da borda: mantém ícones da status bar legíveis sobre o conteúdo. */
private const val ScrimAlpha = 0.8f

/** Sem blur, o degradê sozinho precisa esconder mais do que passa por trás. */
private const val FallbackScrimAlpha = 0.94f

/**
 * Único tratamento de transparência do app: o conteúdo rolável passa por trás de controles flutuantes
 * (sem faixa de fundo) e, nas bordas, ganha blur progressivo — máximo na borda, zero em [top] /
 * [bottom] a partir dela — com um degradê do fundo por cima.
 *
 * Aplicar no viewport da rolagem (antes do `verticalScroll`, ou na própria `LazyColumn`), nunca nas
 * barras. Blur nativo do Compose 1.13 com raio variável (Android 13+).
 *
 * @param top altura da faixa superior (status bar + barra superior, se houver); `0.dp` desliga.
 * @param bottom altura da faixa inferior (barra inferior + teclado/insets); `0.dp` desliga.
 * @param scrollState quando informado, o efeito só existe enquanto há conteúdo pra rolar por trás dos
 *   controles — sem rolagem nada passa por trás, e a camada de blur seria custo de GPU à toa (era nas
 *   animações de troca de aba que esse custo aparecia como travada).
 */
@Composable
fun Modifier.progressiveEdgeBlur(top: Dp, bottom: Dp, scrollState: ScrollableState? = null): Modifier {
    val overflows by remember(scrollState) {
        derivedStateOf { scrollState == null || scrollState.canScrollForward || scrollState.canScrollBackward }
    }
    if (!overflows) return this

    val blurEnabled = LocalBlurEnabled.current
    val scrim = FilamentTheme.colors.background.copy(alpha = if (blurEnabled) ScrimAlpha else FallbackScrimAlpha)

    val blurModifier = if (blurEnabled) {
        Modifier.blur {
            val height = size.height
            if (height <= 0.dp) return@blur
            // Frações da altura: blur cai de EdgeBlurRadius a 0 em cada faixa e fica zerado no meio.
            // Com teclado aberto as faixas podem se sobrepor; a de baixo nunca começa antes do fim
            // da de cima.
            val topEnd = (top / height).coerceIn(0f, 1f)
            val bottomStart = (1f - bottom / height).coerceIn(topEnd, 1f)
            radius = BlurRadiusSpec.verticalGradient(
                listOf(
                    BlurStop(fraction = 0f, radius = if (top > 0.dp) EdgeBlurRadius else 0.dp),
                    BlurStop(fraction = topEnd, radius = 0.dp),
                    BlurStop(fraction = bottomStart, radius = 0.dp),
                    BlurStop(fraction = 1f, radius = if (bottom > 0.dp) EdgeBlurRadius else 0.dp),
                ),
            )
        }
    } else {
        Modifier
    }

    // Degradê desenhado por fora do blur: fica nítido, sem ser desfocado junto com o conteúdo.
    return drawWithContent {
        drawContent()
        val topPx = top.toPx().coerceAtMost(size.height)
        val bottomPx = bottom.toPx().coerceAtMost(size.height)
        if (topPx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(listOf(scrim, Color.Transparent), startY = 0f, endY = topPx),
                size = Size(size.width, topPx),
            )
        }
        if (bottomPx > 0f) {
            val startY = size.height - bottomPx
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, scrim), startY = startY, endY = size.height),
                topLeft = Offset(0f, startY),
                size = Size(size.width, bottomPx),
            )
        }
    }.then(blurModifier)
}
