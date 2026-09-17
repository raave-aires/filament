package com.raave.filament.ui.login

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.fontawesome.brands.R as FontAwesomeBrandsR
import com.composables.icons.lucide.R as LucideR
import com.raave.filament.R
import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.usecase.VerifyEmailCodeUseCase
import com.raave.filament.ui.common.toMessage
import com.raave.filament.ui.navigation.ExternalAuthCallback
import com.raave.filament.ui.theme.FilamentTheme
import com.raave.filament.ui.theme.filamentTextButtonColors
import com.raave.filament.ui.theme.filamentTextFieldColors
import com.raave.filament.util.HapticUtil

/**
 * @param authCallback retorno do login no navegador recebido pela Activity, ainda não tratado.
 * @param onAuthCallbackConsumed chamado assim que o retorno é repassado ao ViewModel.
 */
@Composable
fun LoginScreen(
    authCallback: ExternalAuthCallback?,
    onAuthCallbackConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    // Activity atual, lida no momento do uso: nunca guardada no ViewModel.
    val context = LocalContext.current

    LaunchedEffect(authCallback) {
        if (authCallback != null) {
            viewModel.onExternalSignInCallback(authCallback.oneTimeToken)
            onAuthCallbackConsumed()
        }
    }

    LaunchedEffect(uiState.pendingAction) {
        when (val action = uiState.pendingAction) {
            null -> Unit
            is LoginAction.OpenBrowser -> {
                CustomTabsIntent.Builder().build().launchUrl(context, action.url.toUri())
                viewModel.onBrowserOpened()
            }
            // Se a tela for recriada com o seletor aberto, o efeito reinicia e o seletor é pedido de
            // novo — em vez de o botão ficar preso em loading.
            is LoginAction.RequestPasskey -> try {
                val response = CredentialManager.create(context).getCredential(
                    context = context,
                    request = GetCredentialRequest(listOf(GetPublicKeyCredentialOption(action.optionsJson))),
                )
                val credential = response.credential as? PublicKeyCredential
                if (credential != null) {
                    viewModel.onPasskeyCredential(credential.authenticationResponseJson)
                } else {
                    viewModel.onPasskeyFailed(AppError.ExternalSignInFailed)
                }
            } catch (e: GetCredentialCancellationException) {
                viewModel.onPasskeyCancelled()
            } catch (e: NoCredentialException) {
                viewModel.onPasskeyFailed(AppError.NoPasskeyAvailable)
            } catch (e: GetCredentialException) {
                viewModel.onPasskeyFailed(AppError.ExternalSignInFailed)
            }
        }
    }

    // Haptics centralizados nas ações, em vez de repetidos em cada botão.
    LoginScreenContent(
        modifier = modifier,
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onCodeChange = viewModel::onCodeChange,
        onSendCodeClick = { HapticUtil.performUIHaptic(view); viewModel.onSendCodeClick() },
        onVerifyCodeClick = { HapticUtil.performUIHaptic(view); viewModel.onVerifyCodeClick() },
        onChangeEmailClick = { HapticUtil.performLightHaptic(view); viewModel.onBackToEmailClick() },
        onPasskeyClick = { HapticUtil.performUIHaptic(view); viewModel.onPasskeyClick() },
        onMicrosoftClick = { HapticUtil.performUIHaptic(view); viewModel.onMicrosoftClick() },
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
    onPasskeyClick: () -> Unit,
    onMicrosoftClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                // Formulário estreito centralizado: em tablet/paisagem os botões esticados na
                // largura toda ficavam desproporcionais e longe demais do campo.
                .widthIn(max = FormMaxWidth)
                .fillMaxSize()
                // safeDrawing = barras do sistema + recorte da câmera + teclado (o manifest usa
                // adjustResize), então o campo em foco nunca fica coberto.
                .safeDrawingPadding()
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
                style = MaterialTheme.typography.headlineMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .semantics { heading() },
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

            if (uiState.error != null) {
                Text(
                    text = uiState.error.toMessage(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        // Sem isso o erro só aparece na tela: quem usa TalkBack não fica sabendo.
                        .semantics { liveRegion = LiveRegionMode.Polite },
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
                    onClick = onPasskeyClick,
                    enabled = !uiState.isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    if (uiState.isPasskeyLoading) {
                        ButtonProgressIndicator()
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
                    onClick = onMicrosoftClick,
                    enabled = !uiState.isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    if (uiState.isMicrosoftLoading) {
                        ButtonProgressIndicator()
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
        colors = filamentTextFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(
            onSend = { if (uiState.email.isNotBlank() && !uiState.isBusy) onSendCodeClick() },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
            .semantics { contentType = ContentType.EmailAddress },
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
            ButtonProgressIndicator()
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
    val codeLength = VerifyEmailCodeUseCase.CODE_LENGTH

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
        onValueChange = { input -> onCodeChange(input.filter(Char::isDigit).take(codeLength)) },
        label = { Text(stringResource(R.string.login_code_hint)) },
        singleLine = true,
        enabled = !uiState.isBusy,
        colors = filamentTextFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { if (uiState.code.length == codeLength && !uiState.isBusy) onVerifyCodeClick() },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            // Permite ao teclado sugerir o código recebido por SMS/e-mail.
            .semantics { contentType = ContentType.SmsOtpCode },
    )

    Button(
        onClick = onVerifyCodeClick,
        enabled = !uiState.isBusy && uiState.code.length == codeLength,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        if (uiState.isVerifyingCode) {
            ButtonProgressIndicator()
        } else {
            Text(stringResource(R.string.login_action_verify_code))
        }
    }

    TextButton(
        onClick = onChangeEmailClick,
        enabled = !uiState.isBusy,
        colors = filamentTextButtonColors(),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Text(stringResource(R.string.login_action_change_email))
    }
}

@Composable
private fun ButtonProgressIndicator() {
    // Herda a cor de conteúdo do botão: o padrão (primary) some sobre o Button preenchido.
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        color = LocalContentColor.current,
        strokeWidth = 2.dp,
    )
}

private val FormMaxWidth = 480.dp

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
            uiState = LoginUiState(step = LoginStep.CODE, email = "usuario@elinsa.com.br", error = AppError.Server("INVALID_OTP", null)),
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
