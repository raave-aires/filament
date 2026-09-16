package com.raave.filament.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.raave.filament.R
import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.Attachment

/**
 * Texto exibido pra cada erro do domínio. Único lugar que decide a redação: ViewModels e repositórios
 * não montam mais mensagens (antes havia textos soltos em Kotlin e o `message` cru do backend na tela).
 */
@Composable
@ReadOnlyComposable
fun AppError.toMessage(): String = when (this) {
    AppError.Network -> stringResource(R.string.error_network)
    AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
    AppError.UnexpectedResponse -> stringResource(R.string.error_unexpected_response)
    is AppError.Server -> serverMessage()
    AppError.InvalidEmail -> stringResource(R.string.error_invalid_email)
    AppError.InvalidCode -> stringResource(R.string.error_invalid_code)
    AppError.EmptyTicketFields -> stringResource(R.string.new_ticket_error_empty)
    is AppError.AttachmentTooLarge ->
        stringResource(R.string.error_attachment_too_large, fileName, Attachment.MAX_SIZE_BYTES / BYTES_PER_MB)
    AppError.AttachmentUnreadable -> stringResource(R.string.error_attachment_unreadable)
    AppError.NoPasskeyAvailable -> stringResource(R.string.error_no_passkey)
    AppError.ExternalSignInFailed -> stringResource(R.string.error_external_sign_in)
}

/**
 * Códigos conhecidos do Better Auth ganham texto próprio em português. Sem código conhecido, usa a
 * mensagem do backend — as rotas do GLPI no backbone já respondem em português.
 */
@Composable
@ReadOnlyComposable
private fun AppError.Server.serverMessage(): String = when (code) {
    "INVALID_OTP" -> stringResource(R.string.error_invalid_otp)
    "OTP_EXPIRED" -> stringResource(R.string.error_otp_expired)
    "TOO_MANY_ATTEMPTS" -> stringResource(R.string.error_too_many_attempts)
    else -> message ?: stringResource(R.string.error_generic)
}

private const val BYTES_PER_MB = 1024 * 1024
