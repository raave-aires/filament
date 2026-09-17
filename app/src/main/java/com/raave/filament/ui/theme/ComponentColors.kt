package com.raave.filament.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ajustes pontuais de cor em componentes do Material 3 que usam `primary` como cor de TEXTO. No
// shadcn o primary do escuro é só preenchimento (2,2:1 como texto sobre o fundo); estes helpers
// trocam por `primaryText`, sem mudar forma nem comportamento do componente.

/** @param containerColor fundo do campo; transparente por padrão, como o OutlinedTextField do M3. */
@Composable
fun filamentTextFieldColors(containerColor: Color = Color.Transparent): TextFieldColors {
    val colors = FilamentTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = containerColor,
        errorContainerColor = containerColor,
        focusedBorderColor = colors.primaryText,
        focusedLabelColor = colors.primaryText,
        cursorColor = colors.primaryText,
        focusedLeadingIconColor = colors.foreground,
    )
}

@Composable
fun filamentTextButtonColors(): ButtonColors =
    ButtonDefaults.textButtonColors(contentColor = FilamentTheme.colors.primaryText)

/** Card do shadcn: fundo `card` com borda de 1px (a do OutlinedCard, `outlineVariant` = `border`). */
@Composable
fun filamentCardColors(): CardColors {
    val colors = FilamentTheme.colors
    return CardDefaults.outlinedCardColors(containerColor = colors.card, contentColor = colors.cardForeground)
}
