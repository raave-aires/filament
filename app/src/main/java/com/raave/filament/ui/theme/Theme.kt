package com.raave.filament.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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

    // Shapes e typography ficam nos padrões do Material 3 Expressive (cantos 4/8/12/16/28.dp e
    // estilos *Emphasized para ênfase). Customizar aqui, nunca com RoundedCornerShape ou
    // fontWeight soltos nas telas.
    MaterialExpressiveTheme(colorScheme = colorScheme) {
        // Nenhuma Activity envolve o conteúdo num Surface próprio, então sem isso quem
        // pinta o fundo é o android:windowBackground da theme XML (ver res/values/colors.xml).
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}
