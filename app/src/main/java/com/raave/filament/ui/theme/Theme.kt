package com.raave.filament.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val LocalFilamentColors = staticCompositionLocalOf { MistLight }

/** Acesso aos tokens shadcn que não têm papel no ColorScheme do Material (borda, card). */
object FilamentTheme {
    val colors: FilamentColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFilamentColors.current
}

/**
 * Escala de raio do shadcn (`--radius: 0.625rem` = 10) nos slots do Material: sm 6, md 8, lg 10,
 * xl 14, 2xl 18. Botões e FABs seguem com as formas próprias do Material 3.
 */
private val FilamentShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilamentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Paleta "Mist" do shadcn/ui. Componentes, estados de carregamento e movimento continuam os do
    // Material 3 Expressive — só as cores e os raios vêm do shadcn.
    val colors = if (darkTheme) MistDark else MistLight
    val colorScheme = remember(colors, darkTheme) { colors.toColorScheme(darkTheme) }

    CompositionLocalProvider(LocalFilamentColors provides colors) {
        MaterialExpressiveTheme(colorScheme = colorScheme, shapes = FilamentShapes) {
            // Nenhuma Activity envolve o conteúdo num Surface próprio, então sem isso quem
            // pinta o fundo é o android:windowBackground da theme XML (ver res/values/colors.xml).
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colorScheme.background,
                content = content,
            )
        }
    }
}
