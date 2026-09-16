package com.raave.filament.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent

// Transições entre telas. Os padrões do Navigation 3 eram um crossfade de 700 ms (as duas telas
// meio transparentes, uma sobre a outra) e, no gesto de voltar, a tela encolhendo pra 70% sem sumir.
//
// Tween em vez das springs do MaterialTheme.motionScheme: o esquema Expressive tem overshoot, e numa
// tela inteira deslizando isso abre uma fresta na borda; além disso o gesto de voltar "arrasta" a
// transição pelo tempo, o que pede duração fixa.

/** Emphasized decelerate do Material 3: entra rápido e assenta devagar. */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** Duração de transição de tela inteira (token medium4 do M3). */
private const val SCREEN_DURATION_MS = 400

/** Quanto a tela de baixo se desloca enquanto a de cima entra/sai por inteiro: sugere profundidade. */
private const val PARALLAX_DIVISOR = 4

/** Abrir um nível abaixo (Home → Chat): a nova tela entra pela direita, a atual recua um pouco. */
fun <T : Any> forwardTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    val spec = tween<IntOffset>(SCREEN_DURATION_MS, easing = EmphasizedDecelerate)
    slideInHorizontally(spec) { fullWidth -> fullWidth } togetherWith
        slideOutHorizontally(spec) { fullWidth -> -fullWidth / PARALLAX_DIVISOR }
}

/** Voltar (botão da tela ou do sistema): exatamente o caminho inverso de [forwardTransition]. */
fun <T : Any> backTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    val spec = tween<IntOffset>(SCREEN_DURATION_MS, easing = EmphasizedDecelerate)
    slideInHorizontally(spec) { fullWidth -> -fullWidth / PARALLAX_DIVISOR } togetherWith
        slideOutHorizontally(spec) { fullWidth -> fullWidth }
}

/**
 * Gesto de voltar preditivo: a mesma saída de [backTransition], mas acompanhando o dedo. Easing linear
 * porque o progresso do gesto já é a curva — com easing, a tela andaria mais rápido que o dedo no
 * começo. A tela sai pro lado de onde o gesto começou.
 */
fun <T : Any> predictiveBackTransition():
    AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = { swipeEdge ->
    val spec = tween<IntOffset>(SCREEN_DURATION_MS, easing = LinearEasing)
    val fromRightEdge = swipeEdge == NavigationEvent.EDGE_RIGHT
    slideInHorizontally(spec) { fullWidth ->
        if (fromRightEdge) fullWidth / PARALLAX_DIVISOR else -fullWidth / PARALLAX_DIVISOR
    } togetherWith slideOutHorizontally(spec) { fullWidth ->
        if (fromRightEdge) -fullWidth else fullWidth
    }
}

/**
 * Troca entre Login e Home, que não são níveis de uma hierarquia (fade through do M3): a tela atual
 * some rápido e a nova aparece em seguida, crescendo de leve. Deslizar ali sugeriria um "voltar" que
 * não existe. Tipado com `Scene<*>` porque é aplicado via metadata da entrada (NavDisplay.transitionSpec).
 */
val FadeThroughTransition: AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform = {
    val enterSpec = tween<Float>(durationMillis = 210, delayMillis = 90, easing = EmphasizedDecelerate)
    (fadeIn(enterSpec) + scaleIn(enterSpec, initialScale = 0.92f)) togetherWith
        fadeOut(tween(durationMillis = 90, easing = LinearEasing))
}
