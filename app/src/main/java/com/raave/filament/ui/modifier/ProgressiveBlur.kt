package com.raave.filament.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Desfoca progressivamente as bordas superior e/ou inferior do conteúdo, para que ele suma
 * suavemente atrás da status bar e da barra flutuante em vez de ser cortado.
 *
 * Usa a API nativa do Compose (`Modifier.blur { }`, a partir do ui 1.13.0-alpha03): a gradiente de
 * raio é resolvida no próprio pipeline do Skia, sem shader AGSL manual. As duas bordas são feitas
 * numa única passada, então só existe uma camada de blur mesmo quando ambas estão ativas.
 *
 * Quando [enabled] é falso (ver [com.raave.filament.util.DeviceUtils.isBlurSupported]) o blur é
 * omitido e resta apenas o scrim em gradiente, que preserva a legibilidade sem custo de GPU.
 *
 * @param topHeight altura da faixa desfocada no topo; `0.dp` desliga essa borda.
 * @param bottomHeight altura da faixa desfocada embaixo; `0.dp` desliga essa borda.
 * @param scrimColor cor do gradiente sobreposto nas mesmas faixas; `null` desliga o scrim.
 */
fun Modifier.progressiveBlurEdges(
    enabled: Boolean,
    topHeight: Dp = 0.dp,
    bottomHeight: Dp = 0.dp,
    maxRadius: Dp = 24.dp,
    scrimColor: Color? = null,
): Modifier = this
    .then(if (enabled) Modifier.blurEdges(topHeight, bottomHeight, maxRadius) else Modifier)
    .then(if (scrimColor != null) Modifier.edgeScrim(topHeight, bottomHeight, scrimColor) else Modifier)

private fun Modifier.blurEdges(
    topHeight: Dp,
    bottomHeight: Dp,
    maxRadius: Dp,
): Modifier = blur {
    // BlurScope.size já vem em Dp, então a fração sai da divisão direta.
    val contentHeight = size.height
    if (contentHeight <= 0.dp) return@blur

    val topFraction = (topHeight / contentHeight).coerceIn(0f, 1f)
    val bottomFraction = (bottomHeight / contentHeight).coerceIn(0f, 1f)
    if (topFraction == 0f && bottomFraction == 0f) return@blur

    // Cada faixa vai de maxRadius na borda até zero no fim dela; o miolo fica nítido.
    val stops = buildList {
        if (topFraction > 0f) {
            add(BlurStop(fraction = 0f, radius = maxRadius))
            add(BlurStop(fraction = topFraction, radius = 0.dp))
        } else {
            add(BlurStop(fraction = 0f, radius = 0.dp))
        }
        if (bottomFraction > 0f) {
            val bottomStart = (1f - bottomFraction).coerceAtLeast(topFraction + MinStopGap)
            if (bottomStart < 1f) {
                add(BlurStop(fraction = bottomStart, radius = 0.dp))
            }
            add(BlurStop(fraction = 1f, radius = maxRadius))
        } else {
            add(BlurStop(fraction = 1f, radius = 0.dp))
        }
    }

    radius = BlurRadiusSpec.verticalGradient(stops)
}

private fun Modifier.edgeScrim(
    topHeight: Dp,
    bottomHeight: Dp,
    scrimColor: Color,
): Modifier = drawWithContent {
    drawContent()
    if (topHeight > 0.dp) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(scrimColor, Color.Transparent),
                startY = 0f,
                endY = topHeight.toPx(),
            )
        )
    }
    if (bottomHeight > 0.dp) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, scrimColor),
                startY = size.height - bottomHeight.toPx(),
                endY = size.height,
            )
        )
    }
}

/** Evita que as faixas de topo e base colidam num conteúdo muito curto. */
private const val MinStopGap = 0.001f
