package com.raave.filament.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.raave.filament.util.HapticUtil

data class ToolbarItem(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int,
    val hasBadge: Boolean = false,
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

private val IconSize = 24.dp
private val ItemHorizontalPadding = 14.dp
private val LabelSpacing = 8.dp
private val IconOnlyWidth = IconSize + ItemHorizontalPadding * 2

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

    // Uma única animação de Dp governa a largura da pílula, em vez de o layout do rótulo ser
    // recalculado quadro a quadro. O texto é medido uma vez e apenas recortado pela pílula.
    val itemWidth by animateDpAsState(
        targetValue = if (showLabel) IconOnlyWidth + LabelSpacing + labelSlotWidth else IconOnlyWidth,
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "navItemWidth",
    )
    val labelAlpha = animateFloatAsState(
        targetValue = if (showLabel) 1f else 0f,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "navItemLabelAlpha",
    )

    // Cor e opacidade usam a spec de "effects" (sem overshoot) e o que se move usa a "spatial",
    // como o Material 3 Expressive separa.
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "navItemContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else LocalContentColor.current,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "navItemContent",
    )

    Surface(
        onClick = {
            HapticUtil.performHeavyHaptic(view)
            onClick()
        },
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .height(48.dp)
            .width(itemWidth),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // Mede na largura natural mesmo quando a pílula está estreita: o excedente é
            // recortado pela Surface, então o texto não é remedido durante a animação.
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .padding(horizontal = ItemHorizontalPadding),
        ) {
            Box {
                Icon(
                    painter = painterResource(item.iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(IconSize),
                )
                if (item.hasBadge) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.TopEnd)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.width(LabelSpacing))
            Text(
                text = label,
                style = labelStyle,
                maxLines = 1,
                softWrap = false,
                // Leitura adiada: a opacidade muda só na fase de desenho, sem recompor nem
                // refazer layout.
                modifier = Modifier.graphicsLayer { alpha = labelAlpha.value },
            )
        }
    }
}
