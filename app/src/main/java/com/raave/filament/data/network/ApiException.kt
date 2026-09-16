package com.raave.filament.data.network

import java.io.IOException

/**
 * Erro HTTP (status != 2xx). [code] é o código estável do backend (ex.: `INVALID_OTP` do Better Auth)
 * quando o corpo trouxer um — formato `{"message": "...", "code": "..."}`.
 */
class ApiException(
    val statusCode: Int,
    val code: String?,
    message: String,
) : IOException(message)
