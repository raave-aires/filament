package com.raave.filament.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R as LucideR
import com.raave.filament.R
import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.Ticket
import com.raave.filament.domain.model.User
import com.raave.filament.ui.common.toMessage
import com.raave.filament.ui.components.FilamentFloatingToolbar
import com.raave.filament.ui.components.ToolbarItem
import com.raave.filament.ui.format.toDateLabel
import com.raave.filament.ui.modifier.progressiveBlurEdges
import com.raave.filament.ui.theme.FilamentTheme
import com.raave.filament.util.HapticUtil
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

/** Espaço reservado abaixo do conteúdo para a barra flutuante não cobrir o fim da rolagem. */
private val ToolbarReservedHeight = 96.dp

/** Largura máxima do conteúdo das abas: em tablet/paisagem, cards esticados na tela toda perdem leitura. */
private val ContentMaxWidth = 840.dp

@Composable
fun HomeScreen(
    onTicketClick: (Ticket) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // A economia de bateria pode ser ligada com o app aberto.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshBlurState()
        onPauseOrDispose {}
    }

    HomeScreenContent(
        modifier = modifier,
        uiState = uiState,
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
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onTabSelected: (HomeTab) -> Unit,
    onSignOutClick: () -> Unit,
    onUserRetryClick: () -> Unit,
    onNewTicketClick: () -> Unit,
    onNewTicketDismiss: () -> Unit,
    onNewTicketTitleChange: (String) -> Unit,
    onNewTicketDescriptionChange: (String) -> Unit,
    onNewTicketSubmit: () -> Unit,
    onTicketClick: (Ticket) -> Unit,
    onTicketsRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomReserved = navigationBarHeight + ToolbarReservedHeight

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isUserLoading -> {
                // Espera curta de tela inteira: no M3 Expressive é o LoadingIndicator, não o circular.
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.userError != null -> {
                UserErrorState(
                    error = uiState.userError,
                    onRetryClick = onUserRetryClick,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = ContentMaxWidth)
                        .padding(horizontal = 24.dp),
                )
            }
            else -> {
                val scrollState = rememberScrollState()
                // Sem rolagem nada passa por trás das barras, então o blur seria custo de GPU por
                // nada — e era justamente durante as animações que esse custo aparecia como travada.
                val contentOverflows by remember {
                    derivedStateOf { scrollState.canScrollForward || scrollState.canScrollBackward }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // Desfoca o que passa por trás da status bar e da barra flutuante. Sem blur
                        // (Samsung com bug conhecido / economia de bateria), fica só o scrim.
                        .progressiveBlurEdges(
                            enabled = uiState.isBlurEnabled && contentOverflows,
                            topHeight = statusBarHeight,
                            bottomHeight = bottomReserved,
                            scrimColor = when {
                                !contentOverflows -> null
                                uiState.isBlurEnabled -> null
                                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            },
                        )
                        .verticalScroll(scrollState)
                        // Laterais: navigation bar de 3 botões e recorte da câmera em paisagem.
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        // Insets como padding do conteúdo: ele rola por baixo das barras em vez de
                        // ser recortado por elas.
                        .padding(horizontal = 24.dp)
                        .padding(top = statusBarHeight + 24.dp, bottom = bottomReserved),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // transitionSpec não é @Composable, então as specs de motion são resolvidas aqui.
                    val slideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                    val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

                    // Desliza no sentido da aba escolhida, com um deslocamento curto para sugerir a
                    // direção sem parecer uma transição de tela inteira.
                    AnimatedContent(
                        targetState = uiState.selectedTab,
                        transitionSpec = {
                            val forward = targetState.ordinal > initialState.ordinal
                            val offset: (Int) -> Int = { width -> width / 6 }
                            (
                                slideInHorizontally(slideSpec) { width ->
                                    if (forward) offset(width) else -offset(width)
                                } + fadeIn(fadeSpec)
                                ) togetherWith (
                                slideOutHorizontally(slideSpec) { width ->
                                    if (forward) -offset(width) else offset(width)
                                } + fadeOut(fadeSpec)
                                )
                        },
                        contentAlignment = Alignment.TopStart,
                        label = "tabContent",
                        modifier = Modifier
                            .widthIn(max = ContentMaxWidth)
                            .fillMaxWidth(),
                    ) { tab ->
                        Column {
                            when (tab) {
                                HomeTab.INICIO -> InicioTab(uiState.user)
                                HomeTab.CHAMADOS -> ChamadosTab(uiState, onTicketClick, onTicketsRetryClick)
                                HomeTab.CONTA -> ContaTab(uiState.user, onSignOutClick)
                            }
                        }
                    }
                }

                FilamentFloatingToolbar(
                    items = remember(uiState.ticketsBadgeCount) { homeTabItems(uiState.ticketsBadgeCount) },
                    selectedIndex = uiState.selectedTab.ordinal,
                    onItemSelected = { index -> onTabSelected(HomeTab.entries[index]) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    floatingActionButton = {
                        FloatingToolbarDefaults.StandardFloatingActionButton(
                            onClick = {
                                HapticUtil.performHeavyHaptic(view)
                                onNewTicketClick()
                            },
                        ) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_plus),
                                contentDescription = stringResource(R.string.home_action_new_ticket),
                            )
                        }
                    },
                )

                if (uiState.newTicket != null) {
                    NewTicketDialog(
                        form = uiState.newTicket,
                        onDismiss = onNewTicketDismiss,
                        onTitleChange = onNewTicketTitleChange,
                        onDescriptionChange = onNewTicketDescriptionChange,
                        onSubmit = onNewTicketSubmit,
                    )
                }
            }
        }
    }
}

@Composable
private fun NewTicketDialog(
    form: NewTicketFormState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_ticket_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = form.title,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(R.string.new_ticket_field_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    enabled = !form.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.new_ticket_field_content)) },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    enabled = !form.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                if (form.error != null) {
                    Text(
                        text = form.error.toMessage(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSubmit, enabled = !form.isSubmitting) {
                if (form.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.new_ticket_action_submit))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !form.isSubmitting) {
                Text(stringResource(R.string.new_ticket_action_cancel))
            }
        },
    )
}

@Composable
private fun InicioTab(user: User?) {
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
private fun ChamadosTab(
    uiState: HomeUiState,
    onTicketClick: (Ticket) -> Unit,
    onRetryClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Mesmo cabeçalho das outras abas: sem ele a lista começava solta sob a status bar.
        Text(
            text = stringResource(R.string.home_nav_chamados),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        if (uiState.lastCreatedTicketId != null) {
            Text(
                text = stringResource(R.string.home_last_ticket_created, uiState.lastCreatedTicketId),
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        when {
            uiState.isTicketsLoading && uiState.tickets.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
            uiState.ticketsError != null && uiState.tickets.isEmpty() -> {
                TicketsErrorState(error = uiState.ticketsError, onRetryClick = onRetryClick)
            }
            uiState.tickets.isEmpty() -> {
                TicketsEmptyState()
            }
            else -> {
                uiState.tickets.forEach { ticket ->
                    TicketCard(ticket = ticket, onClick = { onTicketClick(ticket) })
                }
            }
        }
    }
}

@Composable
private fun TicketsEmptyState() {
    Card(modifier = Modifier.fillMaxWidth()) {
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
    Card(modifier = Modifier.fillMaxWidth()) {
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
    Card(
        onClick = {
            HapticUtil.performUIHaptic(view)
            onClick()
        },
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
private fun ContaTab(user: User?, onSignOutClick: () -> Unit) {
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

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    FilamentTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                isUserLoading = false,
                user = User(name = "Raave Aires", email = "raave@elinsadobrasil.com.br"),
                selectedTab = HomeTab.CHAMADOS,
                tickets = listOf(
                    Ticket(id = 42, title = "Impressora do 3º andar sem tinta", status = "Novo", openedAt = Instant.now()),
                ),
            ),
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
        )
    }
}
