package com.raave.filament.domain.model

/** Falhas que o app sabe tratar. A UI converte cada uma em texto (ver ui/common/AppErrorMessage.kt). */
sealed interface AppError {

    /** Sem conexão, timeout ou servidor inalcançável. */
    data object Network : AppError

    /** Sessão ausente, expirada ou recusada pelo backend. O token local já foi descartado. */
    data object Unauthorized : AppError

    /** O backend respondeu algo que o app não soube interpretar. */
    data object UnexpectedResponse : AppError

    /** Recusa do backend. [code] é o código estável (ex.: `INVALID_OTP`) quando o backend manda um. */
    data class Server(val code: String?, val message: String?) : AppError

    data object InvalidEmail : AppError

    data object InvalidCode : AppError

    data object EmptyTicketFields : AppError

    data class AttachmentTooLarge(val fileName: String) : AppError

    data object AttachmentUnreadable : AppError

    /** O aparelho não tem passkey cadastrada para este app. */
    data object NoPasskeyAvailable : AppError

    /** Login por passkey ou provedor externo (Microsoft) não concluiu por um motivo não específico. */
    data object ExternalSignInFailed : AppError
}
