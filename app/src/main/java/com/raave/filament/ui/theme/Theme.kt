package com.raave.filament.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Cantos generosos são parte do visual do Material 3 Expressive; o extraLarge de 28.dp é o
// arredondamento "assinatura" usado em cards e containers.
private val FilamentShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilamentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Paleta sempre derivada do azul da Elinsa (nunca do wallpaper/dynamic color do
    // Android). Ver Color.kt: no escuro, background/surface são preto puro em vez do
    // cinza-azulado padrão do Material, e o "primary" já é uma tonalidade mais clara/
    // dessaturada do mesmo matiz pra manter contraste — o #24A3DD cru só aparece no claro.
    val colorScheme = if (darkTheme) ElinsaDarkColorScheme else ElinsaLightColorScheme

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        shapes = FilamentShapes,
    ) {
        // Nenhuma Activity envolve o conteúdo num Surface próprio, então sem isso quem
        // pinta o fundo é o android:windowBackground da theme XML — e no OneUI da Samsung
        // esse fallback é cinza-escuro, não o preto AMOLED do ColorScheme.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}
