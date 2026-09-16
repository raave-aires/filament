package com.raave.filament.data.glpi

import com.raave.filament.BuildConfig
import com.raave.filament.data.auth.AuthTokenStore
import com.raave.filament.data.network.ApiException
import com.raave.filament.data.network.HttpClient
import com.raave.filament.data.network.MultipartFile
import java.io.IOException
import org.json.JSONObject

private object GlpiPaths {
    const val STATUS = "/api/glpi/status"
    const val TICKETS = "/api/glpi/tickets"
    fun ticket(id: Long) = "$TICKETS/$id"
    fun followups(ticketId: Long) = "${ticket(ticketId)}/followups"
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

    /** Chamados abertos pelo usuário logado. */
    suspend fun getTickets(): Result<List<GlpiTicketSummary>> {
        val token = tokenStore.getToken() ?: return Result.failure(IllegalStateException("Sem sessão ativa"))
        return try {
            val response = HttpClient.getJson(
                url = baseUrl + GlpiPaths.TICKETS,
                headers = mapOf("Authorization" to "Bearer $token"),
            )
            val tickets = response.body.getJSONArray("tickets")
            Result.success(List(tickets.length()) { index -> tickets.getJSONObject(index).toTicketSummary() })
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(IOException(NETWORK_ERROR_MESSAGE, e))
        }
    }

    /** Detalhe do chamado — usado pra recuperar a mensagem de abertura (`content`), que o GLPI não trata como followup. */
    suspend fun getTicket(id: Long): Result<GlpiTicketDetail> {
        val token = tokenStore.getToken() ?: return Result.failure(IllegalStateException("Sem sessão ativa"))
        return try {
            val response = HttpClient.getJson(
                url = baseUrl + GlpiPaths.ticket(id),
                headers = mapOf("Authorization" to "Bearer $token"),
            )
            Result.success(response.body.toTicketDetail())
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(IOException(NETWORK_ERROR_MESSAGE, e))
        }
    }

    /** Mensagens (followups) públicas do chamado, em ordem cronológica. */
    suspend fun getFollowups(ticketId: Long): Result<List<GlpiFollowup>> {
        val token = tokenStore.getToken() ?: return Result.failure(IllegalStateException("Sem sessão ativa"))
        return try {
            val response = HttpClient.getJson(
                url = baseUrl + GlpiPaths.followups(ticketId),
                headers = mapOf("Authorization" to "Bearer $token"),
            )
            val followups = response.body.getJSONArray("followups")
            val visible = (0 until followups.length())
                .map { index -> followups.getJSONObject(index) }
                .filterNot { it.isPrivateFollowup() }
            Result.success(visible.map { it.toFollowup() })
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(IOException(NETWORK_ERROR_MESSAGE, e))
        }
    }

    /** Envia uma nova mensagem (com anexos opcionais) no chamado em nome do usuário logado. */
    suspend fun sendFollowup(
        ticketId: Long,
        content: String,
        attachments: List<PendingAttachment> = emptyList(),
    ): Result<GlpiFollowup> {
        val token = tokenStore.getToken() ?: return Result.failure(IllegalStateException("Sem sessão ativa"))
        return try {
            val response = if (attachments.isEmpty()) {
                HttpClient.postJson(
                    url = baseUrl + GlpiPaths.followups(ticketId),
                    body = JSONObject().put("content", content),
                    headers = mapOf("Authorization" to "Bearer $token"),
                )
            } else {
                HttpClient.postMultipart(
                    url = baseUrl + GlpiPaths.followups(ticketId),
                    fields = mapOf("content" to content),
                    files = attachments.map { MultipartFile("files", it.fileName, it.mimeType, it.bytes) },
                    headers = mapOf("Authorization" to "Bearer $token"),
                )
            }
            Result.success(response.body.toFollowup())
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
