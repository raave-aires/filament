package com.raave.filament.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.fontawesome.brands.R as FontAwesomeBrandsR
import com.composables.icons.lucide.R as LucideR
import com.raave.filament.R
import com.raave.filament.ui.login.LoginStep
import com.raave.filament.ui.login.LoginUiState
import com.raave.filament.ui.login.LoginViewModel
import com.raave.filament.ui.theme.FilamentTheme

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) onLoginSuccess()
    }

    LoginScreenContent(
        modifier = modifier,
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onCodeChange = viewModel::onCodeChange,
        onSendCodeClick = viewModel::onSendCodeClick,
        onVerifyCodeClick = viewModel::onVerifyCodeClick,
        onChangeEmailClick = viewModel::onBackToEmailClick,
        onPasskeyClick = viewModel::onPasskeyClick,
        onMicrosoftClick = viewModel::onMicrosoftClick,
    )
}

@Composable
private fun LoginScreenContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSendCodeClick: () -> Unit,
    onVerifyCodeClick: () -> Unit,
    onChangeEmailClick: () -> Unit,
    onPasskeyClick: (android.content.Context) -> Unit,
    onMicrosoftClick: (android.content.Context) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 72.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.height(96.dp),
            )

            Text(
                text = stringResource(R.string.login_brand_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 20.dp),
            )

            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 20.dp, end = 20.dp),
            )

            when (uiState.step) {
                LoginStep.EMAIL -> EmailStep(
                    uiState = uiState,
                    onEmailChange = onEmailChange,
                    onSendCodeClick = onSendCodeClick,
                )
                LoginStep.CODE -> CodeStep(
                    uiState = uiState,
                    onCodeChange = onCodeChange,
                    onVerifyCodeClick = onVerifyCodeClick,
                    onChangeEmailClick = onChangeEmailClick,
                )
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }

            if (uiState.step == LoginStep.EMAIL) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.login_divider_or),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                FilledTonalButton(
                    onClick = { onPasskeyClick(context) },
                    enabled = !uiState.isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    if (uiState.isPasskeyLoading) {
                        LoadingIndicator()
                    } else {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_fingerprint_pattern),
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(R.string.login_action_passkey),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onMicrosoftClick(context) },
                    enabled = !uiState.isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    if (uiState.isMicrosoftLoading) {
                        LoadingIndicator()
                    } else {
                        Icon(
                            painter = painterResource(FontAwesomeBrandsR.drawable.fontawesome_ic_microsoft_brands),
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(R.string.login_action_microsoft),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.login_terms_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
            )
        }
    }
}

@Composable
private fun EmailStep(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onSendCodeClick: () -> Unit,
) {
    OutlinedTextField(
        value = uiState.email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(R.string.login_email_hint)) },
        leadingIcon = {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_mail),
                contentDescription = null,
            )
        },
        singleLine = true,
        enabled = !uiState.isBusy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
    )

    Button(
        onClick = onSendCodeClick,
        enabled = !uiState.isBusy && uiState.email.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        if (uiState.isSendingCode) {
            LoadingIndicator()
        } else {
            Text(stringResource(R.string.login_action_send_code))
        }
    }
}

@Composable
private fun CodeStep(
    uiState: LoginUiState,
    onCodeChange: (String) -> Unit,
    onVerifyCodeClick: () -> Unit,
    onChangeEmailClick: () -> Unit,
) {
    Text(
        text = stringResource(R.string.login_email_helper),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
    )

    OutlinedTextField(
        value = uiState.code,
        onValueChange = { input -> onCodeChange(input.filter(Char::isDigit).take(6)) },
        label = { Text(stringResource(R.string.login_code_hint)) },
        singleLine = true,
        enabled = !uiState.isBusy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
    )

    Button(
        onClick = onVerifyCodeClick,
        enabled = !uiState.isBusy && uiState.code.length == 6,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        if (uiState.isVerifyingCode) {
            LoadingIndicator()
        } else {
            Text(stringResource(R.string.login_action_verify_code))
        }
    }

    TextButton(
        onClick = onChangeEmailClick,
        enabled = !uiState.isBusy,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Text(stringResource(R.string.login_action_change_email))
    }
}

@Composable
private fun LoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.dp,
    )
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenEmailStepPreview() {
    FilamentTheme {
        LoginScreenContent(
            uiState = LoginUiState(),
            onEmailChange = {},
            onCodeChange = {},
            onSendCodeClick = {},
            onVerifyCodeClick = {},
            onChangeEmailClick = {},
            onPasskeyClick = {},
            onMicrosoftClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenCodeStepPreview() {
    FilamentTheme {
        LoginScreenContent(
            uiState = LoginUiState(step = LoginStep.CODE, email = "usuario@elinsa.com.br"),
            onEmailChange = {},
            onCodeChange = {},
            onSendCodeClick = {},
            onVerifyCodeClick = {},
            onChangeEmailClick = {},
            onPasskeyClick = {},
            onMicrosoftClick = {},
        )
    }
}
