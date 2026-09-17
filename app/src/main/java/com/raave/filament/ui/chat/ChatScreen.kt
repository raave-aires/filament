package com.raave.filament.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R as LucideR
import com.raave.filament.R
import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.Message
import com.raave.filament.ui.common.toMessage
import com.raave.filament.ui.format.toTimeLabel
import com.raave.filament.ui.modifier.progressiveEdgeBlur
import com.raave.filament.ui.theme.FilamentTheme
import com.raave.filament.ui.theme.filamentTextFieldColors
import com.raave.filament.util.HapticUtil
import java.time.Instant

/** Em tablet/paisagem a conversa fica numa coluna central em vez de espalhar bolhas pelas bordas. */
private val ChatMaxWidth = 840.dp

/**
 * Chat/acompanhamento de um chamado (followups do GLPI). Destino de navegação próprio, então o
 * voltar do sistema (inclusive o gesto preditivo) volta pra lista sem tratamento manual.
 */
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Respostas do atendimento chegam enquanto o usuário está em outro app: ao voltar, a conversa se
    // atualiza sozinha. O pull-to-refresh fica como gesto manual, mas numa conversa longa ele exige
    // rolar até o topo, longe das mensagens novas. Na primeira retomada a carga inicial já está em
    // andamento e o onRefresh a ignora.
    LifecycleResumeEffect(viewModel) {
        viewModel.onRefresh()
        onPauseOrDispose {}
    }
    ChatScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onMoreClick = {},
        onRetryClick = viewModel::onRetryClick,
        onRefresh = viewModel::onRefresh,
        onMessageChange = viewModel::onMessageChange,
        onAttachmentsPicked = viewModel::onAttachmentsPicked,
        onRemoveAttachment = viewModel::onRemoveAttachment,
        onSendClick = viewModel::onSendClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChatScreenContent(
    uiState: ChatUiState,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    onRetryClick: () -> Unit,
    onRefresh: () -> Unit,
    onMessageChange: (String) -> Unit,
    onAttachmentsPicked: (List<String>) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // SAF (Storage Access Framework): não precisa de permissão de runtime, concede acesso só ao
    // arquivo escolhido. "*/*" porque o followup do GLPI não restringe tipo de anexo.
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris -> onAttachmentsPicked(uris.map { it.toString() }) },
    )

    // Barras flutuantes (só os controles, sem faixa de fundo): a conversa ocupa a tela toda, rola por
    // trás delas e ganha blur progressivo nas bordas (progressiveEdgeBlur, na lista) — padrão
    // edge-to-edge do Scaffold, com os insets nas barras e as alturas delas como contentPadding.
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        // Cada barra aplica os próprios insets; o conteúdo recebe só as alturas delas.
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ChatTopBar(
                title = uiState.ticketTitle,
                onBackClick = onBackClick,
                onMoreClick = onMoreClick,
                modifier = Modifier
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                    ),
            )
        },
        bottomBar = {
            ChatInputBar(
                uiState = uiState,
                onValueChange = onMessageChange,
                onAttachClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                onSendClick = onSendClick,
                onRemoveAttachment = onRemoveAttachment,
                // safeDrawing já une navigation bar e teclado; somar a altura da navigation bar à
                // mão contava a barra duas vezes com o teclado aberto.
                modifier = Modifier
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    ),
            )
        },
    ) { barsPadding ->
        val pullState = rememberPullToRefreshState()
        // Puxar pra baixo busca respostas novas do atendimento sem sair e voltar do chamado.
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            state = pullState,
            enabled = !uiState.isLoading,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullState,
                    isRefreshing = uiState.isRefreshing,
                    // Abaixo da barra superior: a conversa passa por trás dela, o indicador não.
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = barsPadding.calculateTopPadding()),
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                when {
                    uiState.isLoading -> {
                        LoadingIndicator(
                            color = FilamentTheme.colors.primaryText,
                            modifier = Modifier.align(Alignment.Center).padding(barsPadding),
                        )
                    }
                    uiState.loadError != null -> {
                        ChatErrorState(
                            error = uiState.loadError,
                            onRetryClick = onRetryClick,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(barsPadding)
                                .padding(horizontal = 24.dp),
                        )
                    }
                    uiState.messages.isEmpty() -> {
                        // Rolável (mesmo sem precisar) pra o pull-to-refresh receber o gesto.
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(barsPadding),
                        ) {
                            Text(
                                text = stringResource(R.string.chat_empty_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                        }
                    }
                    else -> {
                        ChatMessageList(
                            messages = uiState.messages,
                            barsPadding = barsPadding,
                            modifier = Modifier
                                .widthIn(max = ChatMaxWidth)
                                .fillMaxSize(),
                        )
                    }
                }
            }
        }
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
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .widthIn(max = ChatMaxWidth)
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
                shape = CircleShape,
                color = FilamentTheme.colors.card,
                // Flutua sobre a conversa: o contorno separa a pílula de bolhas de cor parecida.
                border = BorderStroke(1.dp, FilamentTheme.colors.border),
                // Altura mínima, não fixa: com fonte grande do sistema o título crescia além dos 48.dp e
                // era cortado.
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
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
}

@Composable
private fun ChatErrorState(error: AppError, onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = stringResource(R.string.chat_error_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = error.toMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        OutlinedButton(onClick = onRetryClick, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.tickets_action_retry))
        }
    }
}

/** Espaço entre bolhas do mesmo remetente em sequência — bem menor que entre remetentes diferentes. */
private val GroupedBubbleSpacing = 2.dp
private val SeparateBubbleSpacing = 16.dp

@Composable
private fun ChatMessageList(
    messages: List<Message>,
    barsPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.progressiveEdgeBlur(
            top = barsPadding.calculateTopPadding(),
            bottom = barsPadding.calculateBottomPadding(),
            scrollState = listState,
        ),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = barsPadding.calculateTopPadding() + 12.dp,
            bottom = barsPadding.calculateBottomPadding() + 12.dp,
        ),
    ) {
        itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
            val previous = messages.getOrNull(index - 1)
            // Mesmo grupo = mesmo remetente na mensagem anterior: junta as bolhas visualmente, como
            // um parágrafo por pessoa. Mensagens próprias agrupam só por isMine (a de abertura do
            // chamado não traz nome); recebidas, também pelo nome de quem respondeu.
            val sameGroupAsPrevious = previous != null &&
                previous.isMine == message.isMine &&
                (message.isMine || previous.authorName == message.authorName)
            if (index > 0) {
                Spacer(modifier = Modifier.height(if (sameGroupAsPrevious) GroupedBubbleSpacing else SeparateBubbleSpacing))
            }
            ChatBubble(
                message = message,
                // Nome só na primeira bolha do grupo — repetir em toda mensagem do mesmo remetente
                // era ruído, não informação.
                showAuthorLabel = !message.isMine && !sameGroupAsPrevious && !message.authorName.isNullOrBlank(),
            )
        }
    }
}

@Composable
private fun ChatBubble(message: Message, showAuthorLabel: Boolean) {
    val isMine = message.isMine
    val colors = FilamentTheme.colors
    // Minha: preenchimento `primary`. Recebida: `card` com borda, como os cards do shadcn — num fundo
    // `muted` o horário em `mutedForeground` ficaria em 4,1:1 no tema claro, abaixo do mínimo.
    val containerColor = if (isMine) colors.primary else colors.card
    val contentColor = if (isMine) colors.primaryForeground else colors.cardForeground
    // Horário em cor sólida: com alpha a hierarquia vinha ao custo do contraste; ela fica por conta
    // do tamanho (labelSmall).
    val timeColor = if (isMine) colors.primaryForeground else colors.mutedForeground
    // Cantos do tema (large = 14.dp), com o canto do lado do remetente reduzido.
    val tailCorner = CornerSize(4.dp)
    val bubbleShape = if (isMine) {
        MaterialTheme.shapes.large.copy(bottomEnd = tailCorner)
    } else {
        MaterialTheme.shapes.large.copy(bottomStart = tailCorner)
    }
    val timeLabel = remember(message.sentAt) { message.sentAt?.toTimeLabel() }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(containerColor)
                .then(if (isMine) Modifier else Modifier.border(1.dp, colors.border, bubbleShape))
                // Uma bolha = um anúncio no TalkBack (autor, texto e horário juntos).
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (showAuthorLabel) {
                Text(
                    text = message.authorName.orEmpty(),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    color = colors.mutedForeground,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
            // Ancora cada bolha no tempo — sem isso, bolhas de tamanhos diferentes empilhadas
            // pareciam soltas, sem nenhum ponto de referência em comum entre elas.
            if (timeLabel != null) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = timeColor,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    uiState: ChatUiState,
    onValueChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onSendClick: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = ChatMaxWidth)
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            uiState.refreshError?.let { InputBarError(it) }
            uiState.sendError?.let { InputBarError(it) }
            uiState.attachmentError?.let { InputBarError(it) }
            if (uiState.attachments.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    items(uiState.attachments, key = { it.id }) { attachment ->
                        AttachmentInputChip(
                            attachment = attachment,
                            enabled = !uiState.isSending,
                            onRemove = { onRemoveAttachment(attachment.id) },
                        )
                    }
                }
            }
            // Botões alinhados à base: quando o texto cresce pra várias linhas, eles ficam junto
            // da última linha em vez de flutuar no meio do campo.
            Row(verticalAlignment = Alignment.Bottom) {
                FilledTonalIconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onAttachClick()
                    },
                    enabled = !uiState.isSending,
                    // Desabilitado o padrão é translúcido, e sem faixa atrás a conversa apareceria através.
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier.padding(bottom = InputButtonBottomInset),
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_plus),
                        contentDescription = stringResource(R.string.chat_action_attach),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = uiState.draftMessage,
                    onValueChange = onValueChange,
                    placeholder = { Text(stringResource(R.string.chat_input_placeholder)) },
                    enabled = !uiState.isSending,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    // Sem faixa atrás, o campo precisa de fundo próprio pra o texto não brigar com a conversa.
                    colors = filamentTextFieldColors(containerColor = FilamentTheme.colors.card),
                    // extraLarge (18.dp na escala do tema) arredonda bem numa linha e continua legível
                    // com 4 linhas; 50% virava um estádio que recortava os cantos do texto.
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onSendClick()
                    },
                    enabled = uiState.canSend,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier.padding(bottom = InputButtonBottomInset),
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = LocalContentColor.current,
                            strokeWidth = 2.dp,
                        )
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

/** Centraliza os IconButtons (40.dp) na primeira linha do campo (56.dp) com o Row alinhado à base. */
private val InputButtonBottomInset = 8.dp

@Composable
private fun InputBarError(error: AppError) {
    // Pílula própria: texto solto por cima das mensagens ficaria ilegível sem a faixa da barra.
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        Text(
            text = error.toMessage(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/**
 * InputChip é o componente do M3 pra "informação inserida pelo usuário" como anexos; o chip inteiro
 * remove o anexo, com alvo de toque de 48.dp.
 */
@Composable
private fun AttachmentInputChip(attachment: AttachmentChip, enabled: Boolean, onRemove: () -> Unit) {
    InputChip(
        selected = false,
        onClick = onRemove,
        enabled = enabled,
        label = {
            Text(
                text = attachment.fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp),
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_file),
                contentDescription = null,
                modifier = Modifier.size(InputChipDefaults.IconSize),
            )
        },
        trailingIcon = {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_x),
                contentDescription = stringResource(R.string.chat_action_remove_attachment),
                modifier = Modifier.size(InputChipDefaults.IconSize),
            )
        },
        shape = CircleShape,
        colors = InputChipDefaults.inputChipColors(containerColor = FilamentTheme.colors.card),
    )
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    FilamentTheme {
        ChatScreenContent(
            uiState = ChatUiState(
                ticketTitle = "Impressora do 3º andar sem tinta",
                isLoading = false,
                messages = listOf(
                    Message(
                        id = 0,
                        text = "Abri esse chamado porque a impressora do 3º andar parou de imprimir.",
                        sentAt = Instant.parse("2026-09-15T10:00:00Z"),
                        authorName = null,
                        isMine = true,
                    ),
                    Message(
                        id = 2,
                        text = "Bom dia! Já estamos verificando o suprimento de tinta, retornamos em breve.",
                        sentAt = Instant.parse("2026-09-15T10:05:00Z"),
                        authorName = "Suporte TI",
                        isMine = false,
                    ),
                ),
                attachments = listOf(AttachmentChip(id = "1", fileName = "foto-impressora.jpg")),
            ),
            onBackClick = {},
            onMoreClick = {},
            onRetryClick = {},
            onRefresh = {},
            onMessageChange = {},
            onAttachmentsPicked = {},
            onRemoveAttachment = {},
            onSendClick = {},
        )
    }
}
