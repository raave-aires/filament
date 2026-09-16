package com.raave.filament.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarColors
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarHorizontalFabPosition
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.raave.filament.util.HapticUtil

data class ToolbarItem(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int,
    /** `null` ou zero escondem o badge; a partir de 1 o número aparece sobre o ícone (teto 99+). */
    val badgeCount: Int? = null,
)

/**
 * Barra de navegação flutuante (Material 3 Expressive), com FAB acoplado.
 *
 * Só o item selecionado exibe rótulo, expandindo com animação — isso mantém a barra equilibrada
 * independentemente do comprimento de cada rótulo. Em fontes muito grandes ou telas estreitas os
 * rótulos somem por completo, para os itens nunca se espremerem.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilamentFloatingToolbar(
    items: List<ToolbarItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
    floatingActionButton: (@Composable () -> Unit)? = null,
) {
    val density = LocalDensity.current
    val fontScale = density.fontScale
    val screenWidth = LocalWindowInfo.current.containerDpSize.width
    val hideLabels = fontScale > 1.25f || (screenWidth < 400.dp && items.size > 3)

    // Todos os itens reservam a largura do maior rótulo. Assim, quando a seleção muda, o que um
    // item ganha o outro perde e a barra inteira mantém a mesma largura — sem isso, o toolbar, o
    // FAB e a centralização eram remedidos a cada frame da animação, que era o que travava.
    val labelStyle = MaterialTheme.typography.labelLarge
    val textMeasurer = rememberTextMeasurer()
    val labels = items.map { stringResource(it.labelRes) }
    val labelSlotWidth = remember(labels, labelStyle, density) {
        val widestPx = labels.maxOfOrNull { textMeasurer.measure(it, labelStyle).size.width } ?: 0
        with(density) { widestPx.toDp() }
    }

    HorizontalFloatingToolbar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp),
        expanded = expanded,
        scrollBehavior = scrollBehavior,
        colors = colors,
        floatingActionButton = floatingActionButton ?: {},
        floatingActionButtonPosition = FloatingToolbarHorizontalFabPosition.End,
        expandedShadowElevation = ToolbarShadowElevation,
        collapsedShadowElevation = ToolbarShadowElevation,
    ) {
        items.forEachIndexed { index, item ->
            ToolbarNavItem(
                item = item,
                label = labels[index],
                labelStyle = labelStyle,
                labelSlotWidth = if (hideLabels) 0.dp else labelSlotWidth,
                selected = index == selectedIndex,
                onClick = { onItemSelected(index) },
            )
            if (index < items.lastIndex) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

// O FAB do toolbar (FloatingToolbarDefaults.*FloatingActionButton) fixa elevação nível 2 e não
// expõe parâmetro pra mudar, enquanto o container com FAB vem em nível 1 expandido e 0 recolhido —
// era daí que vinha a sombra só no FAB. Igualar os dois no nível 2 faz barra e FAB lerem como uma
// peça flutuante só, que é o que o Material especifica pro par.
private val ToolbarShadowElevation = 3.dp

private val IconSize = 24.dp
private val ItemHorizontalPadding = 14.dp
private val LabelSpacing = 8.dp
private val IconOnlyWidth = IconSize + ItemHorizontalPadding * 2

/** Deslocamento inicial do rótulo, pra ele emergir de trás do ícone em vez de só surgir no lugar. */
private val LabelRevealSlide = 12.dp

@Composable
private fun ToolbarNavItem(
    item: ToolbarItem,
    label: String,
    labelStyle: TextStyle,
    labelSlotWidth: Dp,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    val motionScheme = MaterialTheme.motionScheme
    val showLabel = selected && labelSlotWidth > 0.dp
    val labelSlot = LabelSpacing + labelSlotWidth

    // Uma única animação de Dp governa a largura da pílula, em vez de o layout do rótulo ser
    // recalculado quadro a quadro. O texto é medido uma vez e apenas recortado pela pílula.
    val itemWidth by animateDpAsState(
        targetValue = if (showLabel) IconOnlyWidth + labelSlot else IconOnlyWidth,
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "navItemWidth",
    )

    // Cor e opacidade usam a spec de "effects" (sem overshoot) e o que se move usa a "spatial",
    // como o Material 3 Expressive separa. Selecionado = surfaceContainer/onSurface, conforme
    // FloatingToolbarTokens.VibrantButtonSelected* (antes era `surface`, que no escuro AMOLED
    // virava uma pílula preta chapada sobre o primaryContainer).
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "navItemContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else LocalContentColor.current,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "navItemContent",
    )

    // Surface "selectable": o TalkBack anuncia a aba como selecionada, e o papel Tab a identifica
    // como destino de navegação em vez de botão genérico.
    Surface(
        selected = selected,
        onClick = {
            HapticUtil.performHeavyHaptic(view)
            onClick()
        },
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .height(48.dp)
            .width(itemWidth)
            .semantics { role = Role.Tab },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // Mede na largura natural mesmo quando a pílula está estreita: o excedente é
            // recortado pela Surface, então o texto não é remedido durante a animação.
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .padding(horizontal = ItemHorizontalPadding),
        ) {
            // A Box é fixada no tamanho do ícone para o badge transbordar só no desenho: se ele
            // entrasse na medição, empurraria o rótulo e quebraria a largura fixa da pílula.
            Box(modifier = Modifier.size(IconSize)) {
                // O Text do rótulo fica sempre na árvore de semântica (só é recortado/transparente),
                // então descrever o ícone também fazia o TalkBack ler o nome duas vezes.
                Icon(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
                val count = item.badgeCount
                if (count != null && count > 0) {
                    // Posicionado à mão em vez de via BadgedBox: o BadgedBox afasta o badge até
                    // 12.dp da borda do ícone, mais do que os 8.dp que separam ícone e rótulo, e
                    // com isso o "99+" cobria o começo do texto. Ancorado no canto, ele cresce
                    // por cima do ícone e nunca encosta no rótulo.
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-4).dp)
                            // A Box acima fixa 24.dp, o que espremia o badge e quebrava o "99+"
                            // em duas linhas. Medido solto, ele cresce pra esquerda por cima do
                            // ícone e continua sem ocupar espaço no layout.
                            .wrapContentSize(align = Alignment.TopEnd, unbounded = true),
                    ) {
                        Text(
                            text = if (count > 99) "99+" else count.toString(),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(LabelSpacing))
            Text(
                text = label,
                style = labelStyle,
                maxLines = 1,
                softWrap = false,
                // O rótulo é derivado da largura já animada da pílula em vez de ter uma animação
                // própria: ele emerge junto com o espaço que abre e termina exatamente com ele.
                // Antes eram duas molas soltas (largura na spatial, opacidade na effects), e o
                // texto aparecia recortado no meio do caminho ou terminava fora de hora.
                // Leitura na fase de desenho: não recompõe nem refaz layout.
                modifier = Modifier.graphicsLayer {
                    val revealed = ((itemWidth - IconOnlyWidth) / labelSlot).coerceIn(0f, 1f)
                    alpha = revealed
                    translationX = -(1f - revealed) * LabelRevealSlide.toPx()
                },
            )
        }
    }
}
