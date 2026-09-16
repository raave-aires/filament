package com.raave.filament.ui

import android.net.Uri
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R as LucideR
import com.raave.filament.R
import com.raave.filament.data.glpi.GlpiTicketSummary
import com.raave.filament.ui.components.FilamentFloatingToolbar
import com.raave.filament.ui.components.ToolbarItem
import com.raave.filament.ui.home.HomeTab
import com.raave.filament.ui.home.HomeUiState
import com.raave.filament.ui.home.HomeViewModel
import com.raave.filament.ui.modifier.progressiveBlurEdges
import com.raave.filament.ui.theme.FilamentTheme
import com.raave.filament.util.HapticUtil

private fun homeTabItems(chamadosBadgeCount: Int?) = listOf(
    ToolbarItem(iconRes = LucideR.drawable.lucide_ic_house, labelRes = R.string.home_nav_inicio),
    ToolbarItem(
        iconRes = LucideR.drawable.lucide_ic_message_circle,
        labelRes = R.string.home_nav_chamados,
        badgeCount = chamadosBadgeCount,
    ),
    ToolbarItem(iconRes = LucideR.drawable.lucide_ic_circle_user, labelRes = R.string.home_nav_conta),
)

/** Espaço reservado abaixo do conteúdo para a barra flutuante não cobrir o fim da rolagem. */
private val ToolbarReservedHeight = 96.dp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onSignedOut: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.signedOut) {
        if (uiState.signedOut) onSignedOut()
    }

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
        onNewTicketClick = viewModel::onNewTicketButtonClick,
        onNewTicketDismiss = viewModel::onNewTicketDismiss,
        onNewTicketNameChange = viewModel::onNewTicketNameChange,
        onNewTicketContentChange = viewModel::onNewTicketContentChange,
        onNewTicketSubmit = viewModel::onNewTicketSubmit,
        onTicketClick = viewModel::onTicketClick,
        onChamadosRetryClick = viewModel::loadChamados,
        onChatBackClick = viewModel::onChatBackClick,
        onChatRetryClick = viewModel::onChatRetryClick,
        onChatMessageChange = viewModel::onChatMessageChange,
        onChatAttachmentsPicked = viewModel::onAttachmentsPicked,
        onChatRemoveAttachment = viewModel::onChatRemoveAttachment,
        onChatSendClick = viewModel::onChatSendClick,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onTabSelected: (HomeTab) -> Unit,
    onSignOutClick: () -> Unit,
    onNewTicketClick: () -> Unit,
    onNewTicketDismiss: () -> Unit,
    onNewTicketNameChange: (String) -> Unit,
    onNewTicketContentChange: (String) -> Unit,
    onNewTicketSubmit: () -> Unit,
    onTicketClick: (GlpiTicketSummary) -> Unit,
    onChamadosRetryClick: () -> Unit,
    onChatBackClick: () -> Unit,
    onChatRetryClick: () -> Unit,
    onChatMessageChange: (String) -> Unit,
    onChatAttachmentsPicked: (List<Uri>) -> Unit,
    onChatRemoveAttachment: (String) -> Unit,
    onChatSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomReserved = navigationBarHeight + ToolbarReservedHeight

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.chatState != null) {
            // Tela cheia por cima das abas: um chat não faz sentido dividindo espaço com a barra
            // flutuante de navegação, então ela some enquanto o chat está aberto.
            ChatScreen(
                chatState = uiState.chatState,
                onBackClick = onChatBackClick,
                onMoreClick = {},
                onRetryClick = onChatRetryClick,
                onMessageChange = onChatMessageChange,
                onAttachmentsPicked = onChatAttachmentsPicked,
                onRemoveAttachment = onChatRemoveAttachment,
                onSendClick = onChatSendClick,
            )
        } else {
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
                    // Insets como padding do conteúdo: ele rola por baixo das barras em vez de
                    // ser recortado por elas.
                    .padding(horizontal = 24.dp)
                    .padding(top = statusBarHeight + 24.dp, bottom = bottomReserved),
            ) {
                // transitionSpec não é @Composable, então as specs de motion são resolvidas aqui.
                val slideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

                // Antes o conteúdo trocava de uma vez. Agora desliza no sentido da aba
                // escolhida, com um deslocamento curto para sugerir a direção sem parecer
                // uma transição de tela inteira.
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
                ) { tab ->
                    Column {
                        when (tab) {
                            HomeTab.INICIO -> InicioTab(uiState)
                            HomeTab.CHAMADOS -> ChamadosTab(uiState, onTicketClick, onChamadosRetryClick)
                            HomeTab.CONTA -> ContaTab(uiState, onSignOutClick)
                        }
                    }
                }
            }
        }

        if (!uiState.isLoading && uiState.chatState == null) {
            FilamentFloatingToolbar(
                items = remember(uiState.chamadosBadgeCount) { homeTabItems(uiState.chamadosBadgeCount) },
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

            if (uiState.isNewTicketDialogOpen) {
                NewTicketDialog(
                    uiState = uiState,
                    onDismiss = onNewTicketDismiss,
                    onNameChange = onNewTicketNameChange,
                    onContentChange = onNewTicketContentChange,
                    onSubmit = onNewTicketSubmit,
                )
            }
        }
    }
}

@Composable
private fun NewTicketDialog(
    uiState: HomeUiState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isCreatingTicket) onDismiss() },
        title = { Text(stringResource(R.string.new_ticket_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = uiState.newTicketName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.new_ticket_field_name)) },
                    singleLine = true,
                    enabled = !uiState.isCreatingTicket,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.newTicketContent,
                    onValueChange = onContentChange,
                    label = { Text(stringResource(R.string.new_ticket_field_content)) },
                    minLines = 3,
                    enabled = !uiState.isCreatingTicket,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                if (uiState.newTicketError != null) {
                    Text(
                        text = uiState.newTicketError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSubmit, enabled = !uiState.isCreatingTicket) {
                if (uiState.isCreatingTicket) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.new_ticket_action_submit))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isCreatingTicket) {
                Text(stringResource(R.string.new_ticket_action_cancel))
            }
        },
    )
}

@Composable
private fun InicioTab(uiState: HomeUiState) {
    val greetingName = uiState.userName ?: uiState.userEmail.orEmpty()
    Text(
        text = stringResource(R.string.home_welcome, greetingName),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = stringResource(R.string.home_inicio_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun ChamadosTab(
    uiState: HomeUiState,
    onTicketClick: (GlpiTicketSummary) -> Unit,
    onRetryClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.lastCreatedTicketId != null) {
            Text(
                text = stringResource(R.string.home_last_ticket_created, uiState.lastCreatedTicketId),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        when {
            uiState.isChamadosLoading && uiState.chamados.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.chamadosError != null && uiState.chamados.isEmpty() -> {
                TicketsErrorState(message = uiState.chamadosError, onRetryClick = onRetryClick)
            }
            uiState.chamados.isEmpty() -> {
                TicketsEmptyState()
            }
            else -> {
                uiState.chamados.forEach { ticket ->
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
private fun TicketsErrorState(message: String?, onRetryClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.tickets_error_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            OutlinedButton(onClick = onRetryClick, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.tickets_action_retry))
            }
        }
    }
}

@Composable
private fun TicketCard(ticket: GlpiTicketSummary, onClick: () -> Unit) {
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
                text = ticket.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.tickets_status_label, ticket.status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (ticket.date != null) {
                Text(
                    text = stringResource(R.string.tickets_date_label, ticket.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ContaTab(uiState: HomeUiState, onSignOutClick: () -> Unit) {
    val view = LocalView.current
    Text(
        text = uiState.userName ?: stringResource(R.string.home_nav_conta),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    if (uiState.userEmail != null) {
        Text(
            text = uiState.userEmail,
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
                isLoading = false,
                userName = "Raave Aires",
                userEmail = "raave@elinsadobrasil.com.br",
            ),
            onTabSelected = {},
            onSignOutClick = {},
            onNewTicketClick = {},
            onNewTicketDismiss = {},
            onNewTicketNameChange = {},
            onNewTicketContentChange = {},
            onNewTicketSubmit = {},
            onTicketClick = {},
            onChamadosRetryClick = {},
            onChatBackClick = {},
            onChatRetryClick = {},
            onChatMessageChange = {},
            onChatAttachmentsPicked = {},
            onChatRemoveAttachment = {},
            onChatSendClick = {},
        )
    }
}
