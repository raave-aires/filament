package com.raave.filament.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.raave.filament.R
import com.raave.filament.data.glpi.GlpiFollowup
import com.raave.filament.data.glpi.PendingAttachment
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
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    onRetryClick: () -> Unit,
    onMessageChange: (String) -> Unit,
    onAttachmentsPicked: (List<Uri>) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // SAF (Storage Access Framework): não precisa de permissão de runtime, concede acesso só ao
    // arquivo escolhido. "*/*" porque o followup do GLPI não restringe tipo de anexo.
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = onAttachmentsPicked,
    )

    Column(modifier = modifier.fillMaxSize()) {
        ChatTopBar(
            title = chatState.ticketName,
            onBackClick = onBackClick,
            onMoreClick = onMoreClick,
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
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        ChatInputBar(
            value = chatState.draftMessage,
            onValueChange = onMessageChange,
            onAttachClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            onSendClick = onSendClick,
            isSending = chatState.isSending,
            error = chatState.sendError,
            attachments = chatState.pendingAttachments,
            attachmentError = chatState.attachmentError,
            onRemoveAttachment = onRemoveAttachment,
            modifier = Modifier
                .imePadding()
                .padding(bottom = navigationBarHeight),
        )
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        FilledTonalIconButton(
            onClick = {
                HapticUtil.performUIHaptic(view)
                onBackClick()
            },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_arrow_left),
                contentDescription = stringResource(R.string.chat_action_back),
            )
        }
        // Título em pílula: mesma linguagem "flutuante" da FilamentFloatingToolbar, só que fixa
        // no topo em vez de sobrepor o conteúdo.
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Sem ação ainda — reservado pra opções do chamado (ver detalhes, encerrar) mais adiante.
        FilledTonalIconButton(
            onClick = {
                HapticUtil.performUIHaptic(view)
                onMoreClick()
            },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_ellipsis_vertical),
                contentDescription = stringResource(R.string.chat_action_more),
            )
        }
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

/** Espaço entre bolhas do mesmo remetente em sequência — bem menor que entre remetentes diferentes. */
private val GroupedBubbleSpacing = 3.dp
private val SeparateBubbleSpacing = 14.dp

@Composable
private fun ChatMessageList(
    followups: List<GlpiFollowup>,
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
    ) {
        itemsIndexed(followups, key = { _, followup -> followup.id }) { index, followup ->
            val previous = followups.getOrNull(index - 1)
            // Mesmo grupo = mesmo remetente na mensagem anterior: junta as bolhas visualmente,
            // como um parágrafo por pessoa, em vez de um espaçamento uniforme pra tudo. Agrupa por
            // isMine + authorName, não por e-mail — e-mail pode faltar por dado legítimo do GLPI.
            val sameGroupAsPrevious = previous != null &&
                previous.isMine == followup.isMine &&
                previous.authorName == followup.authorName
            if (index > 0) {
                Spacer(modifier = Modifier.height(if (sameGroupAsPrevious) GroupedBubbleSpacing else SeparateBubbleSpacing))
            }
            ChatBubble(
                followup = followup,
                // Nome só na primeira bolha do grupo — repetir em toda mensagem do mesmo remetente
                // era ruído, não informação.
                showAuthorLabel = !followup.isMine && !sameGroupAsPrevious && !followup.authorName.isNullOrBlank(),
            )
        }
    }
}

@Composable
private fun ChatBubble(followup: GlpiFollowup, showAuthorLabel: Boolean) {
    val isMine = followup.isMine
    val containerColor = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isMine) 16.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 16.dp,
    )
    val timeLabel = remember(followup.date) { followup.date.toTimeLabel() }

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
            if (showAuthorLabel) {
                Text(
                    text = followup.authorName.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Text(
                text = followup.content,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
            // Ancora cada bolha no tempo — sem isso, bolhas de tamanhos diferentes empilhadas
            // pareciam soltas, sem nenhum ponto de referência em comum entre elas.
            if (timeLabel != null) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                )
            }
        }
    }
}

/** Formata pro fuso do aparelho — a string do backend vem com o offset de onde o GLPI está, não do celular. */
private fun String?.toTimeLabel(): String? {
    if (this == null) return null
    return runCatching {
        val instant = java.time.OffsetDateTime.parse(this).toInstant()
        java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrNull()
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onSendClick: () -> Unit,
    isSending: Boolean,
    error: String?,
    attachments: List<PendingAttachment>,
    attachmentError: String?,
    onRemoveAttachment: (String) -> Unit,
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
            if (attachmentError != null) {
                Text(
                    text = attachmentError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            if (attachments.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    items(attachments, key = { it.id }) { attachment ->
                        AttachmentChip(
                            attachment = attachment,
                            enabled = !isSending,
                            onRemove = { onRemoveAttachment(attachment.id) },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onAttachClick()
                    },
                    enabled = !isSending,
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_plus),
                        contentDescription = stringResource(R.string.chat_action_attach),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text(stringResource(R.string.chat_input_placeholder)) },
                    enabled = !isSending,
                    maxLines = 4,
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onSendClick()
                    },
                    enabled = !isSending && (value.isNotBlank() || attachments.isNotEmpty()),
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

@Composable
private fun AttachmentChip(attachment: PendingAttachment, enabled: Boolean, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_file),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 120.dp)
                    .padding(start = 6.dp),
            )
            IconButton(onClick = onRemove, enabled = enabled, modifier = Modifier.size(28.dp)) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_x),
                    contentDescription = stringResource(R.string.chat_action_remove_attachment),
                    modifier = Modifier.size(14.dp),
                )
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
                        isMine = true,
                    ),
                    GlpiFollowup(
                        id = 2,
                        content = "Bom dia! Já estamos verificando o suprimento de tinta, retornamos em breve.",
                        date = "2026-09-15T10:05:00Z",
                        authorName = "Suporte TI",
                        authorEmail = "ti@elinsadobrasil.com.br",
                        isMine = false,
                    ),
                ),
            ),
            onBackClick = {},
            onMoreClick = {},
            onRetryClick = {},
            onMessageChange = {},
            onAttachmentsPicked = {},
            onRemoveAttachment = {},
            onSendClick = {},
        )
    }
}
