package com.raave.filament.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Client HTTP mínimo baseado em [HttpURLConnection] (nativo do Android), sem dependências
 * externas de rede. Corpos são JSON via [org.json], também nativo do Android SDK.
 *
 * De propósito, NÃO instala um [java.net.CookieHandler] padrão: um cookie handler de processo
 * reenviaria automaticamente qualquer cookie já visto pra esse host em TODAS as chamadas
 * seguintes — inclusive nas que não deveriam levar cookie nenhum (ex.: `sign-in/email-otp`,
 * que usa bearer token puro). O Better Auth trata a presença de um header `Cookie` como sinal
 * de sessão de browser e passa a exigir `Origin`, o que quebra esse client nativo.
 * O cookie de challenge do passkey (entre `generate-authenticate-options`/`-register-options` e
 * `verify-authentication`/`-registration`) é lido do `Set-Cookie` da resposta e reenviado como
 * `Cookie` manualmente, só nessas duas chamadas — ver [AuthRepository].
 */
object HttpClient {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 15_000

    suspend fun postJson(
        url: String,
        body: JSONObject,
        headers: Map<String, String> = emptyMap(),
    ): ApiResponse = request(url, "POST", body, headers)

    suspend fun getJson(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): ApiResponse = request(url, "GET", body = null, headers)

    private suspend fun request(
        url: String,
        method: String,
        body: JSONObject?,
        headers: Map<String, String>,
    ): ApiResponse = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { output ->
                    output.write(body.toString().toByteArray(StandardCharsets.UTF_8))
                }
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.use { it.bufferedReader(StandardCharsets.UTF_8).readText() }.orEmpty()
            val responseBody = if (responseText.isBlank()) JSONObject() else JSONObject(responseText)

            if (statusCode !in 200..299) {
                // Formato confirmado com o backend: {"message": "...", "code": "..."} direto no corpo.
                val message = responseBody.optString("message").takeIf { it.isNotBlank() }
                    ?: responseText.ifBlank { "HTTP $statusCode" }
                throw ApiException(statusCode, message)
            }

            ApiResponse(statusCode, responseBody, connection.headerFields)
        } catch (e: org.json.JSONException) {
            throw IOException("Resposta inválida do servidor", e)
        } finally {
            connection.disconnect()
        }
    }
}
