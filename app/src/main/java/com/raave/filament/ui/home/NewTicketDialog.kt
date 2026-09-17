package com.raave.filament.ui.home

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.composables.icons.lucide.R as LucideR
import com.raave.filament.R
import com.raave.filament.ui.common.toMessage
import com.raave.filament.ui.theme.FilamentTheme
import com.raave.filament.ui.theme.filamentTextButtonColors
import com.raave.filament.ui.theme.filamentTextFieldColors

/**
 * Formulário de novo chamado. Em tela estreita ocupa a tela inteira (diálogo full-screen do M3): a
 * descrição tem espaço de verdade e o teclado não espreme o formulário dentro de um cartão. Em tela
 * larga, um diálogo comum basta.
 */
@Composable
internal fun NewTicketDialog(
    form: NewTicketFormState,
    fullScreen: Boolean,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    if (fullScreen) {
        FullScreenNewTicketDialog(form, onDismiss, onTitleChange, onDescriptionChange, onSubmit)
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.new_ticket_dialog_title)) },
            text = {
                NewTicketFields(
                    form = form,
                    onTitleChange = onTitleChange,
                    onDescriptionChange = onDescriptionChange,
                    descriptionMinLines = 3,
                )
            },
            confirmButton = {
                Button(onClick = onSubmit, enabled = !form.isSubmitting) {
                    SubmitLabel(isSubmitting = form.isSubmitting)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, enabled = !form.isSubmitting, colors = filamentTextButtonColors()) {
                    Text(stringResource(R.string.new_ticket_action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenNewTicketDialog(
    form: NewTicketFormState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        // Borda a borda como o resto do app; os insets são aplicados no conteúdo abaixo.
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        DialogSystemBarsAppearance()
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.new_ticket_dialog_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !form.isSubmitting) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_x),
                                contentDescription = stringResource(R.string.new_ticket_action_cancel),
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = onSubmit,
                            enabled = !form.isSubmitting,
                            colors = filamentTextButtonColors(),
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            SubmitLabel(isSubmitting = form.isSubmitting)
                        }
                    },
                    windowInsets = TopAppBarDefaults.windowInsets,
                )
                NewTicketFields(
                    form = form,
                    onTitleChange = onTitleChange,
                    onDescriptionChange = onDescriptionChange,
                    descriptionMinLines = 8,
                    modifier = Modifier
                        .weight(1f)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * O diálogo tem janela própria, que não herda a aparência das barras de sistema da Activity: sem isso,
 * no tema claro os ícones da status bar ficavam brancos sobre o fundo claro do formulário.
 */
@Composable
private fun DialogSystemBarsAppearance() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val view = LocalView.current
    val lightBars = !isSystemInDarkTheme()
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBars
            isAppearanceLightNavigationBars = lightBars
        }
    }
}

@Composable
private fun NewTicketFields(
    form: NewTicketFormState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    descriptionMinLines: Int,
    modifier: Modifier = Modifier,
) {
    val titleFocus = remember { FocusRequester() }
    // Quem abre o formulário vai digitar: o teclado já sobe no título.
    LaunchedEffect(Unit) { titleFocus.requestFocus() }

    Column(modifier = modifier) {
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
            colors = filamentTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(titleFocus),
        )
        OutlinedTextField(
            value = form.description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.new_ticket_field_content)) },
            minLines = descriptionMinLines,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            enabled = !form.isSubmitting,
            colors = filamentTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
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
}

@Composable
private fun SubmitLabel(isSubmitting: Boolean) {
    if (isSubmitting) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = LocalContentColor.current,
            strokeWidth = 2.dp,
        )
    } else {
        Text(stringResource(R.string.new_ticket_action_submit))
    }
}

@Preview(showBackground = true)
@Composable
private fun NewTicketFieldsPreview() {
    FilamentTheme {
        NewTicketFields(
            form = NewTicketFormState(title = "Impressora sem tinta"),
            onTitleChange = {},
            onDescriptionChange = {},
            descriptionMinLines = 8,
            modifier = Modifier.padding(16.dp),
        )
    }
}
