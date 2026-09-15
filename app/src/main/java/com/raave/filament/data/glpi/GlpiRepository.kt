package com.raave.filament.data.glpi

import com.raave.filament.BuildConfig
import com.raave.filament.data.auth.AuthTokenStore
import com.raave.filament.data.network.ApiException
import com.raave.filament.data.network.HttpClient
import java.io.IOException
import org.json.JSONObject

private object GlpiPaths {
    const val STATUS = "/api/glpi/status"
    const val TICKETS = "/api/glpi/tickets"
}

/**
 * Fundação da comunicação com o GLPI: o app nunca fala com o GLPI diretamente, só com o
 * `backbone` (que guarda o App-Token e a conta de serviço do GLPI) — ver NOTES.md do backbone.
 */
class GlpiRepository(
    private val baseUrl: String = BuildConfig.API_BASE_URL,
    private val tokenStore: AuthTokenStore,
) {

    /** Confirma que o backend consegue abrir/fechar sessão no GLPI, usando a sessão atual do app. */
    suspend fun checkConnection(): Result<Boolean> {
        val token = tokenStore.getToken() ?: return Result.failure(IllegalStateException("Sem sessão ativa"))
        return try {
            val response = HttpClient.getJson(
                url = baseUrl + GlpiPaths.STATUS,
                headers = mapOf("Authorization" to "Bearer $token"),
            )
            Result.success(response.body.optBoolean("connected"))
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(IOException(NETWORK_ERROR_MESSAGE, e))
        }
    }

    /** Abre um chamado no GLPI em nome do usuário logado. Devolve o número do chamado criado. */
    suspend fun createTicket(name: String, content: String): Result<Long> {
        val token = tokenStore.getToken() ?: return Result.failure(IllegalStateException("Sem sessão ativa"))
        return try {
            val response = HttpClient.postJson(
                url = baseUrl + GlpiPaths.TICKETS,
                body = JSONObject().put("name", name).put("content", content),
                headers = mapOf("Authorization" to "Bearer $token"),
            )
            Result.success(response.body.getLong("id"))
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(IOException(NETWORK_ERROR_MESSAGE, e))
        }
    }

    private companion object {
        const val NETWORK_ERROR_MESSAGE = "Falha de conexão. Verifique sua internet e tente novamente."
    }
}
