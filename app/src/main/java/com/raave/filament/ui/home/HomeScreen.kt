package com.raave.filament.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R as LucideR
import com.raave.filament.R
import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.Ticket
import com.raave.filament.domain.model.User
import com.raave.filament.ui.common.toMessage
import com.raave.filament.ui.components.rememberGlassState
import com.raave.filament.ui.components.FilamentFloatingToolbar
import com.raave.filament.ui.components.StatusBarGlass
import com.raave.filament.ui.components.ToolbarItem
import com.raave.filament.ui.format.toDateLabel
import com.raave.filament.ui.theme.FilamentTheme
import com.raave.filament.ui.theme.filamentCardColors
import com.raave.filament.util.HapticUtil
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.time.Instant

private fun homeTabItems(ticketsBadgeCount: Int?) = listOf(
    ToolbarItem(iconRes = LucideR.drawable.lucide_ic_house, labelRes = R.string.home_nav_inicio),
    ToolbarItem(
        iconRes = LucideR.drawable.lucide_ic_message_circle,
        labelRes = R.string.home_nav_chamados,
        badgeCount = ticketsBadgeCount,
    ),
    ToolbarItem(iconRes = LucideR.drawable.lucide_ic_circle_user, labelRes = R.string.home_nav_conta),
)

/**
 * A partir da largura "medium" do Material 3 a navegação vai pra lateral (rail): numa tela larga, ou
 * num celular deitado, a barra embaixo desperdiça a altura que é justamente o que falta.
 */
private val RailBreakpoint = 600.dp

/**
 * Abaixo da altura "compact" do M3 (celular deitado), um diálogo comum com o teclado aberto some quase
 * inteiro atrás dele — só o título ficava visível, sem a descrição nem os botões.
 */
private val CompactHeightBreakpoint = 480.dp

/** Espaço reservado abaixo do conteúdo para a barra flutuante não cobrir o fim da rolagem. */
private val ToolbarReservedHeight = 96.dp

/** Largura máxima do conteúdo das abas: em tablet/paisagem, cards esticados na tela toda perdem leitura. */
private val ContentMaxWidth = 840.dp

private val ContentMinSidePadding = 24.dp

@Composable
fun HomeScreen(
    onTicketClick: (Ticket) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        modifier = modifier,
        uiState = uiState,
        actions = HomeActions(
            onTabSelected = viewModel::onTabSelected,
            onSignOutClick = viewModel::onSignOutClick,
            onUserRetryClick = viewModel::onUserRetryClick,
            onNewTicketClick = viewModel::onNewTicketClick,
            onNewTicketDismiss = viewModel::onNewTicketDismiss,
            onNewTicketTitleChange = viewModel::onNewTicketTitleChange,
            onNewTicketDescriptionChange = viewModel::onNewTicketDescriptionChange,
            onNewTicketSubmit = viewModel::onNewTicketSubmit,
            onTicketClick = onTicketClick,
            onTicketsRetryClick = viewModel::loadTickets,
            onTicketsRefresh = viewModel::onTicketsRefresh,
        ),
    )
}

/** Agrupa os callbacks da Home, que já passavam de dez parâmetros soltos. */
private class HomeActions(
    val onTabSelected: (HomeTab) -> Unit,
    val onSignOutClick: () -> Unit,
    val onUserRetryClick: () -> Unit,
    val onNewTicketClick: () -> Unit,
    val onNewTicketDismiss: () -> Unit,
    val onNewTicketTitleChange: (String) -> Unit,
    val onNewTicketDescriptionChange: (String) -> Unit,
    val onNewTicketSubmit: () -> Unit,
    val onTicketClick: (Ticket) -> Unit,
    val onTicketsRetryClick: () -> Unit,
    val onTicketsRefresh: () -> Unit,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = maxWidth >= RailBreakpoint
        // Conteúdo das abas (hazeSource) que as superfícies de vidro desfocam ao passar por trás.
        val hazeState = rememberGlassState()

        when {
            uiState.isUserLoading -> {
                // Espera curta de tela inteira: no M3 Expressive é o LoadingIndicator, não o circular.
                LoadingIndicator(
                    color = FilamentTheme.colors.primaryText,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            uiState.userError != null -> {
                UserErrorState(
                    error = uiState.userError,
                    onRetryClick = actions.onUserRetryClick,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = ContentMaxWidth)
                        .padding(horizontal = 24.dp),
                )
            }
            useRail -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    HomeNavigationRail(uiState = uiState, actions = actions)
                    Box(modifier = Modifier.weight(1f)) {
                        HomeTabs(
                            uiState = uiState,
                            actions = actions,
                            hazeState = hazeState,
                            // A rail já cuida do lado inicial; só o lado final precisa de inset.
                            horizontalInsets = WindowInsetsSides.End,
                            bottomReserved = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                        )
                        StatusBarGlass(hazeState)
                    }
                }
            }
            else -> {
                HomeTabs(
                    uiState = uiState,
                    actions = actions,
                    hazeState = hazeState,
                    horizontalInsets = WindowInsetsSides.Horizontal,
                    bottomReserved = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                        ToolbarReservedHeight,
                )
                StatusBarGlass(hazeState)
                HomeFloatingToolbar(
                    uiState = uiState,
                    actions = actions,
                    hazeState = hazeState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                )
            }
        }

        if (uiState.newTicket != null) {
            NewTicketDialog(
                form = uiState.newTicket,
                // Tela inteira em celular (em pé ou deitado); diálogo só quando sobra espaço nos dois eixos.
                fullScreen = !useRail || maxHeight < CompactHeightBreakpoint,
                onDismiss = actions.onNewTicketDismiss,
                onTitleChange = actions.onNewTicketTitleChange,
                onDescriptionChange = actions.onNewTicketDescriptionChange,
                onSubmit = actions.onNewTicketSubmit,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeFloatingToolbar(
    uiState: HomeUiState,
    actions: HomeActions,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    FilamentFloatingToolbar(
        items = remember(uiState.ticketsBadgeCount) { homeTabItems(uiState.ticketsBadgeCount) },
        selectedIndex = uiState.selectedTab.ordinal,
        onItemSelected = { index -> actions.onTabSelected(HomeTab.entries[index]) },
        hazeState = hazeState,
        modifier = modifier,
        floatingActionButton = {
            // Ação principal: preenchimento `primary` do tema (o FAB padrão usa primaryContainer, que
            // no tema é o mesmo azul).
            FloatingActionButton(
                onClick = {
                    HapticUtil.performHeavyHaptic(view)
                    actions.onNewTicketClick()
                },
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_plus),
                    contentDescription = stringResource(R.string.home_action_new_ticket),
                )
            }
        },
    )
}

/** Navegação lateral para telas largas, com a ação principal (novo chamado) no topo, como pede o M3. */
@Composable
private fun HomeNavigationRail(uiState: HomeUiState, actions: HomeActions) {
    val view = LocalView.current
    val items = remember(uiState.ticketsBadgeCount) { homeTabItems(uiState.ticketsBadgeCount) }
    WideNavigationRail(
        colors = WideNavigationRailDefaults.colors(containerColor = FilamentTheme.colors.sidebar),
        header = {
            FloatingActionButton(
                onClick = {
                    HapticUtil.performHeavyHaptic(view)
                    actions.onNewTicketClick()
                },
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_plus),
                    contentDescription = stringResource(R.string.home_action_new_ticket),
                )
            }
        },
    ) {
        items.forEachIndexed { index, item ->
            WideNavigationRailItem(
                selected = index == uiState.selectedTab.ordinal,
                onClick = {
                    HapticUtil.performHeavyHaptic(view)
                    actions.onTabSelected(HomeTab.entries[index])
                },
                icon = {
                    val count = item.badgeCount
                    // O rótulo fica visível abaixo do ícone, então o ícone não se descreve de novo.
                    if (count != null && count > 0) {
                        BadgedBox(badge = { Badge { Text(if (count > 99) "99+" else count.toString()) } }) {
                            Icon(painter = painterResource(item.iconRes), contentDescription = null)
                        }
                    } else {
                        Icon(painter = painterResource(item.iconRes), contentDescription = null)
                    }
                },
                label = { Text(stringResource(item.labelRes)) },
                railExpanded = false,
            )
        }
    }
}

/**
 * Conteúdo das abas. Cada aba tem a própria rolagem — a de Chamados é uma lista preguiçosa com
 * pull-to-refresh, o que não cabia na rolagem única de antes.
 */
@Composable
private fun HomeTabs(
    uiState: HomeUiState,
    actions: HomeActions,
    hazeState: HazeState,
    horizontalInsets: WindowInsetsSides,
    bottomReserved: Dp,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(horizontalInsets)),
    ) {
        // Largura máxima aplicada como padding lateral, não como largura do contêiner: assim a
        // rolagem e o blur continuam ocupando a tela toda e só o conteúdo fica centralizado.
        val sidePadding = max(ContentMinSidePadding, (maxWidth - ContentMaxWidth) / 2)
        val edges = TabEdges(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            bottom = bottomReserved,
            side = sidePadding,
        )

        // transitionSpec não é @Composable, então as specs de motion são resolvidas aqui.
        val slideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

        // Desliza no sentido da aba escolhida, com um deslocamento curto para sugerir a direção sem
        // parecer uma transição de tela inteira.
        AnimatedContent(
            targetState = uiState.selectedTab,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val offset: (Int) -> Int = { width -> width / 6 }
                (
                    slideInHorizontally(slideSpec) { width -> if (forward) offset(width) else -offset(width) } +
                        fadeIn(fadeSpec)
                    ) togetherWith (
                    slideOutHorizontally(slideSpec) { width -> if (forward) -offset(width) else offset(width) } +
                        fadeOut(fadeSpec)
                    )
            },
            label = "tabContent",
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
        ) { tab ->
            when (tab) {
                HomeTab.INICIO -> ScrollableTab(edges) { InicioContent(uiState.user) }
                HomeTab.CHAMADOS -> ChamadosTab(uiState, actions, edges)
                HomeTab.CONTA -> ScrollableTab(edges) {
                    ContaContent(uiState.user, actions.onSignOutClick)
                }
            }
        }
    }
}

/** Faixas que o conteúdo das abas deixa livres: status bar, barra de navegação e margens laterais. */
private class TabEdges(val top: Dp, val bottom: Dp, val side: Dp) {
    fun contentPadding() = PaddingValues(start = side, end = side, top = top + 24.dp, bottom = bottom)
}

@Composable
private fun ScrollableTab(
    edges: TabEdges,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(edges.contentPadding()),
        content = content,
    )
}

@Composable
private fun InicioContent(user: User?) {
    val greetingName = user?.name ?: user?.email.orEmpty()
    Text(
        text = stringResource(R.string.home_welcome, greetingName),
        style = MaterialTheme.typography.headlineSmallEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        text = stringResource(R.string.home_inicio_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChamadosTab(uiState: HomeUiState, actions: HomeActions, edges: TabEdges) {
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()
    val hasTickets = uiState.tickets.isNotEmpty()

    PullToRefreshBox(
        isRefreshing = uiState.isTicketsRefreshing,
        onRefresh = actions.onTicketsRefresh,
        state = pullState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullState,
                isRefreshing = uiState.isTicketsRefreshing,
                // Abaixo da status bar: o conteúdo rola por trás dela, o indicador não.
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = edges.top),
            )
        },
    ) {
        LazyColumn(
            state = listState,
            contentPadding = edges.contentPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") {
                // Mesmo cabeçalho das outras abas: sem ele a lista começava solta sob a status bar.
                Text(
                    text = stringResource(R.string.home_nav_chamados),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (uiState.lastCreatedTicketId != null) {
                item(key = "lastCreated") {
                    Text(
                        text = stringResource(R.string.home_last_ticket_created, uiState.lastCreatedTicketId),
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        color = FilamentTheme.colors.primaryText,
                    )
                }
            }
            when {
                uiState.isTicketsLoading && !hasTickets -> item(key = "loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator(color = FilamentTheme.colors.primaryText)
                    }
                }
                uiState.ticketsError != null && !hasTickets -> item(key = "error") {
                    TicketsErrorState(error = uiState.ticketsError, onRetryClick = actions.onTicketsRetryClick)
                }
                !hasTickets -> item(key = "empty") { TicketsEmptyState() }
                else -> {
                    if (uiState.ticketsError != null) {
                        item(key = "refreshError") { TicketsRefreshError(uiState.ticketsError) }
                    }
                    items(uiState.tickets, key = { it.id }) { ticket ->
                        TicketCard(ticket = ticket, onClick = { actions.onTicketClick(ticket) })
                    }
                }
            }
        }
    }
}

/** Atualização falhou com a lista já na tela: avisa sem esconder os chamados que já estavam lá. */
@Composable
private fun TicketsRefreshError(error: AppError) {
    Text(
        text = stringResource(R.string.tickets_refresh_error, error.toMessage()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun TicketsEmptyState() {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), colors = filamentCardColors()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.tickets_empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.tickets_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun UserErrorState(error: AppError, onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = stringResource(R.string.home_session_error_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = error.toMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onRetryClick, modifier = Modifier.padding(top = 24.dp)) {
            Text(stringResource(R.string.tickets_action_retry))
        }
    }
}

@Composable
private fun TicketsErrorState(error: AppError, onRetryClick: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), colors = filamentCardColors()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.tickets_error_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = error.toMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedButton(onClick = onRetryClick, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.tickets_action_retry))
            }
        }
    }
}

@Composable
private fun TicketCard(ticket: Ticket, onClick: () -> Unit) {
    val view = LocalView.current
    OutlinedCard(
        onClick = {
            HapticUtil.performUIHaptic(view)
            onClick()
        },
        colors = filamentCardColors(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = ticket.title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.tickets_status_label, ticket.status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (ticket.openedAt != null) {
                val dateLabel = remember(ticket.openedAt) { ticket.openedAt.toDateLabel() }
                Text(
                    text = stringResource(R.string.tickets_date_label, dateLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ContaContent(user: User?, onSignOutClick: () -> Unit) {
    val view = LocalView.current
    Text(
        text = user?.name ?: stringResource(R.string.home_nav_conta),
        style = MaterialTheme.typography.headlineSmallEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
    if (user?.email != null) {
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    OutlinedButton(
        onClick = {
            HapticUtil.performUIHaptic(view)
            onSignOutClick()
        },
        modifier = Modifier.padding(top = 24.dp),
    ) {
        Text(stringResource(R.string.home_action_sign_out))
    }
}

private fun previewActions() = HomeActions(
    onTabSelected = {},
    onSignOutClick = {},
    onUserRetryClick = {},
    onNewTicketClick = {},
    onNewTicketDismiss = {},
    onNewTicketTitleChange = {},
    onNewTicketDescriptionChange = {},
    onNewTicketSubmit = {},
    onTicketClick = {},
    onTicketsRetryClick = {},
    onTicketsRefresh = {},
)

private val previewState = HomeUiState(
    isUserLoading = false,
    user = User(name = "Raave Aires", email = "raave@elinsadobrasil.com.br"),
    selectedTab = HomeTab.CHAMADOS,
    tickets = listOf(
        Ticket(id = 42, title = "Impressora do 3º andar sem tinta", status = "Novo", openedAt = Instant.now()),
    ),
)

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    FilamentTheme {
        HomeScreenContent(uiState = previewState, actions = previewActions())
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600)
@Composable
private fun HomeScreenWidePreview() {
    FilamentTheme {
        HomeScreenContent(uiState = previewState, actions = previewActions())
    }
}
