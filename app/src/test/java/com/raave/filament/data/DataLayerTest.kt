package com.raave.filament.data

import com.raave.filament.data.attachment.readBytesUpTo
import com.raave.filament.data.glpi.parseGlpiInstant
import com.raave.filament.data.network.ApiException
import com.raave.filament.data.network.apiCall
import com.raave.filament.data.network.asHeaderValue
import com.raave.filament.data.network.parseSuccessBody
import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataLayerTest {

    // --- apiCall: tradução de exceções em erro de domínio ---

    @Test
    fun `401 vira Unauthorized`() = runTest {
        val result = apiCall<Unit> { throw ApiException(401, null, "Unauthorized") }
        assertEquals(AppResult.Failure(AppError.Unauthorized), result)
    }

    @Test
    fun `erro HTTP preserva o codigo do backend`() = runTest {
        val result = apiCall<Unit> { throw ApiException(400, "INVALID_OTP", "Invalid OTP") }
        assertEquals(AppResult.Failure(AppError.Server("INVALID_OTP", "Invalid OTP")), result)
    }

    @Test
    fun `falha de rede vira Network e nao encerra a sessao`() = runTest {
        val result = apiCall<Unit> { throw IOException("timeout") }
        assertEquals(AppResult.Failure(AppError.Network), result)
    }

    @Test
    fun `JSON fora do contrato vira UnexpectedResponse em vez de derrubar o app`() = runTest {
        val result = apiCall<Unit> { throw JSONException("No value for tickets") }
        assertEquals(AppResult.Failure(AppError.UnexpectedResponse), result)
    }

    // --- corpo de resposta ---

    @Test
    fun `corpo null do Better Auth vira objeto vazio`() {
        assertEquals(0, parseSuccessBody("null").length())
        assertEquals(0, parseSuccessBody(" null\n").length())
        assertEquals(0, parseSuccessBody("").length())
        assertEquals("Ana", parseSuccessBody("""{"user":{"name":"Ana"}}""").getJSONObject("user").getString("name"))
    }

    @Test(expected = JSONException::class)
    fun `corpo 2xx que nao e JSON lanca JSONException`() {
        parseSuccessBody("<html>502</html>")
    }

    @Test
    fun `nome de arquivo nao consegue injetar headers no multipart`() {
        assertEquals("a%22b.pdf", "a\"b.pdf".asHeaderValue())
        assertEquals("ab.pdf", "a\r\nb.pdf".asHeaderValue())
    }

    // --- datas do GLPI ---

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")

    @Test
    fun `data ISO com offset e respeitada`() {
        assertEquals(Instant.parse("2026-09-15T13:00:00Z"), parseGlpiInstant("2026-09-15T10:00:00-03:00", saoPaulo))
    }

    @Test
    fun `data sem offset assume o fuso informado`() {
        val expected = Instant.parse("2026-09-15T13:00:00Z")
        assertEquals(expected, parseGlpiInstant("2026-09-15T10:00:00", saoPaulo))
        assertEquals(expected, parseGlpiInstant("2026-09-15 10:00:00", saoPaulo))
    }

    @Test
    fun `data vazia ou irreconhecivel vira null`() {
        assertNull(parseGlpiInstant(null, saoPaulo))
        assertNull(parseGlpiInstant("", saoPaulo))
        assertNull(parseGlpiInstant("ontem", saoPaulo))
    }

    // --- leitura limitada de anexos ---

    @Test
    fun `arquivo dentro do limite e lido inteiro`() {
        val bytes = ByteArray(1_000) { it.toByte() }
        assertTrue(bytes.contentEquals(ByteArrayInputStream(bytes).readBytesUpTo(limit = 1_000)))
    }

    @Test
    fun `arquivo acima do limite para de ser lido um byte depois`() {
        val huge = object : java.io.InputStream() {
            var served = 0L
            override fun read(): Int = if (served++ < 10_000_000) 0 else -1
        }
        val read = huge.readBytesUpTo(limit = 50_000)
        assertEquals(50_001, read.size)
        assertTrue(huge.served < 10_000_000)
    }
}
