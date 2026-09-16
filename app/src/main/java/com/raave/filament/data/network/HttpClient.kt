package com.raave.filament.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/** Um arquivo a enviar num POST multipart — ver [HttpClient.postMultipart]. */
data class MultipartFile(
    val fieldName: String,
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)

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
    ): ApiResponse = withContext(Dispatchers.IO) {
        val connection = openConnection(url, "POST", headers)
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(StandardCharsets.UTF_8))
            }
            readResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getJson(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): ApiResponse = withContext(Dispatchers.IO) {
        val connection = openConnection(url, "GET", headers)
        try {
            readResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    /** POST `multipart/form-data` — campos de texto simples mais um ou mais arquivos. */
    suspend fun postMultipart(
        url: String,
        fields: Map<String, String>,
        files: List<MultipartFile>,
        headers: Map<String, String> = emptyMap(),
    ): ApiResponse = withContext(Dispatchers.IO) {
        // Único por requisição: um valor fixo deixaria a chamada vulnerável a um campo de texto
        // que por acaso contivesse a própria linha de boundary, corrompendo o corpo.
        val boundary = "FilamentBoundary${java.util.UUID.randomUUID()}"
        val connection = openConnection(url, "POST", headers)
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.outputStream.use { output -> writeMultipartBody(output, boundary, fields, files) }
            readResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String, method: String, headers: Map<String, String>): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")
        headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        return connection
    }

    private fun writeMultipartBody(
        output: java.io.OutputStream,
        boundary: String,
        fields: Map<String, String>,
        files: List<MultipartFile>,
    ) {
        fun writeLine(text: String) = output.write((text + "\r\n").toByteArray(StandardCharsets.UTF_8))
        fields.forEach { (name, value) ->
            writeLine("--$boundary")
            writeLine("Content-Disposition: form-data; name=\"$name\"")
            writeLine("")
            writeLine(value)
        }
        files.forEach { file ->
            writeLine("--$boundary")
            writeLine("Content-Disposition: form-data; name=\"${file.fieldName}\"; filename=\"${file.fileName}\"")
            writeLine("Content-Type: ${file.mimeType}")
            writeLine("")
            output.write(file.bytes)
            writeLine("")
        }
        writeLine("--$boundary--")
    }

    private fun readResponse(connection: HttpURLConnection): ApiResponse {
        try {
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

            return ApiResponse(statusCode, responseBody, connection.headerFields)
        } catch (e: org.json.JSONException) {
            throw IOException("Resposta inválida do servidor", e)
        }
    }
}
