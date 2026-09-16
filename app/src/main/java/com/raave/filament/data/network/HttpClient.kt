package com.raave.filament.data.network

import com.raave.filament.di.IoDispatcher
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Um arquivo a enviar num POST multipart — ver [HttpClient.postMultipart]. */
class MultipartFile(
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
 * O cookie de challenge do passkey (entre `generate-authenticate-options` e
 * `verify-authentication`) é lido do `Set-Cookie` da resposta e reenviado como `Cookie`
 * manualmente, só nessa chamada — ver [com.raave.filament.data.auth.BetterAuthRepository].
 *
 * Lança [ApiException] para status != 2xx, [java.io.IOException] para falha de rede e
 * [org.json.JSONException] para corpo 2xx que não seja JSON — ver [apiCall].
 */
@Singleton
class HttpClient @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun postJson(
        url: String,
        body: JSONObject,
        headers: Map<String, String> = emptyMap(),
    ): ApiResponse = withContext(ioDispatcher) {
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
    ): ApiResponse = withContext(ioDispatcher) {
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
    ): ApiResponse = withContext(ioDispatcher) {
        // Único por requisição: um valor fixo deixaria a chamada vulnerável a um campo de texto
        // que por acaso contivesse a própria linha de boundary, corrompendo o corpo.
        val boundary = "FilamentBoundary${UUID.randomUUID()}"
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
        output: OutputStream,
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
            writeLine(
                "Content-Disposition: form-data; name=\"${file.fieldName}\"; " +
                    "filename=\"${file.fileName.asHeaderValue()}\"",
            )
            writeLine("Content-Type: ${file.mimeType}")
            writeLine("")
            output.write(file.bytes)
            writeLine("")
        }
        writeLine("--$boundary--")
    }

    private fun readResponse(connection: HttpURLConnection): ApiResponse {
        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream?.use { it.bufferedReader(StandardCharsets.UTF_8).readText() }.orEmpty()

        if (statusCode !in 200..299) {
            // Corpo de erro pode não ser JSON (ex.: página HTML de um proxy num 502): leitura
            // tolerante, pra ainda virar ApiException com o status em vez de erro de parse.
            val errorBody = runCatching { JSONObject(responseText) }.getOrNull()
            throw ApiException(
                statusCode = statusCode,
                code = errorBody?.optString("code")?.takeIf(String::isNotBlank),
                message = errorBody?.optString("message")?.takeIf(String::isNotBlank) ?: "HTTP $statusCode",
            )
        }

        return ApiResponse(statusCode, parseSuccessBody(responseText), connection.headerFields)
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 15_000
    }
}

/**
 * Nome de arquivo vem do usuário: aspas fechariam o `filename="..."` antes da hora e quebra de
 * linha injetaria headers no meio do corpo multipart.
 */
internal fun String.asHeaderValue(): String =
    replace("\"", "%22").replace("\r", "").replace("\n", "")

/**
 * `null` literal é resposta legítima do Better Auth (ex.: get-session sem sessão válida devolve 200
 * com corpo `null`) e vira objeto vazio. Qualquer outro corpo que não seja JSON lança JSONException.
 */
internal fun parseSuccessBody(responseText: String): JSONObject =
    if (responseText.isBlank() || responseText.trim() == "null") JSONObject() else JSONObject(responseText)
