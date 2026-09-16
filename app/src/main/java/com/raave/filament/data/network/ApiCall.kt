package com.raave.filament.data.network

import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import java.io.IOException
import org.json.JSONException

/** Traduz as exceções de rede/HTTP/JSON pro erro tipado do domínio. Ponto único dessa conversão. */
internal suspend fun <T> apiCall(block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: ApiException) {
        // Antes do IOException: ApiException é subclasse dele.
        if (e.statusCode == HTTP_UNAUTHORIZED) {
            AppResult.Failure(AppError.Unauthorized)
        } else {
            AppResult.Failure(AppError.Server(code = e.code, message = e.message))
        }
    } catch (e: IOException) {
        AppResult.Failure(AppError.Network)
    } catch (e: JSONException) {
        // Resposta 2xx num formato diferente do combinado — antes isso derrubava o app.
        AppResult.Failure(AppError.UnexpectedResponse)
    }

private const val HTTP_UNAUTHORIZED = 401
