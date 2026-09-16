package com.raave.filament.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.raave.filament.R
import com.raave.filament.data.glpi.GlpiFollowup
import com.raave.filament.ui.home.ChatUiState
import com.raave.filament.ui.theme.FilamentTheme
import com.raave.filament.util.HapticUtil

/**
 * Chat/acompanhamento de um chamado (followups do GLPI). Tela cheia, sem a barra flutuante de
 * navegação — ver o `else if (uiState.chatState != null)` em HomeScreen.
 */
@Composable
fun ChatScreen(
    chatState: ChatUiState,
    currentUserEmail: String?,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(modifier = modifier.fillMaxSize()) {
        ChatTopBar(
            title = chatState.ticketName,
            onBackClick = onBackClick,
            modifier = Modifier.padding(top = statusBarHeight),
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                chatState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                chatState.loadError != null -> {
                    ChatErrorState(
                        message = chatState.loadError,
                        onRetryClick = onRetryClick,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                    )
                }
                chatState.followups.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.chat_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                    )
                }
                else -> {
                    ChatMessageList(
                        followups = chatState.followups,
                        currentUserEmail = currentUserEmail,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        ChatInputBar(
            value = chatState.draftMessage,
            onValueChange = onMessageChange,
            onSendClick = onSendClick,
            isSending = chatState.isSending,
            error = chatState.sendError,
            modifier = Modifier
                .imePadding()
                .padding(bottom = navigationBarHeight),
        )
    }
}

@Composable
private fun ChatTopBar(title: String, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        IconButton(
            onClick = {
                HapticUtil.performUIHaptic(view)
                onBackClick()
            },
        ) {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_arrow_left),
                contentDescription = stringResource(R.string.chat_action_back),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun ChatErrorState(message: String?, onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = stringResource(R.string.chat_error_title),
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

@Composable
private fun ChatMessageList(
    followups: List<GlpiFollowup>,
    currentUserEmail: String?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(followups.size) {
        if (followups.isNotEmpty()) listState.animateScrollToItem(followups.lastIndex)
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(followups, key = { _, followup -> followup.id }) { _, followup ->
            val isMine = currentUserEmail != null && followup.authorEmail?.equals(currentUserEmail, ignoreCase = true) == true
            ChatBubble(followup = followup, isMine = isMine)
        }
    }
}

@Composable
private fun ChatBubble(followup: GlpiFollowup, isMine: Boolean) {
    val containerColor = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isMine) 16.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 16.dp,
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(containerColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (!isMine && !followup.authorName.isNullOrBlank()) {
                Text(
                    text = followup.authorName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = followup.content,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isSending: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Surface(tonalElevation = 2.dp, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text(stringResource(R.string.chat_input_placeholder)) },
                    enabled = !isSending,
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onSendClick()
                    },
                    enabled = !isSending && value.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_send),
                            contentDescription = stringResource(R.string.chat_action_send),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    FilamentTheme {
        ChatScreen(
            chatState = ChatUiState(
                ticketId = 42,
                ticketName = "Impressora do 3º andar sem tinta",
                isLoading = false,
                followups = listOf(
                    GlpiFollowup(
                        id = 1,
                        content = "Abri esse chamado porque a impressora do 3º andar parou de imprimir.",
                        date = "2026-09-15T10:00:00Z",
                        authorName = "Raave Aires",
                        authorEmail = "raave@elinsadobrasil.com.br",
                    ),
                    GlpiFollowup(
                        id = 2,
                        content = "Bom dia! Já estamos verificando o suprimento de tinta, retornamos em breve.",
                        date = "2026-09-15T10:05:00Z",
                        authorName = "Suporte TI",
                        authorEmail = "ti@elinsadobrasil.com.br",
                    ),
                ),
            ),
            currentUserEmail = "raave@elinsadobrasil.com.br",
            onBackClick = {},
            onRetryClick = {},
            onMessageChange = {},
            onSendClick = {},
        )
    }
}
