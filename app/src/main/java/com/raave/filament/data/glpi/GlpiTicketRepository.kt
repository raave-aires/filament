package com.raave.filament.data.glpi

import com.raave.filament.data.auth.AuthorizedApiCall
import com.raave.filament.data.network.HttpClient
import com.raave.filament.data.network.MultipartFile
import com.raave.filament.di.BaseUrl
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.Attachment
import com.raave.filament.domain.model.Message
import com.raave.filament.domain.model.Ticket
import com.raave.filament.domain.repository.TicketRepository
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

private object GlpiPaths {
    const val TICKETS = "/api/glpi/tickets"
    fun ticket(id: Long) = "$TICKETS/$id"
    fun followups(ticketId: Long) = "${ticket(ticketId)}/followups"
}

/**
 * O app nunca fala com o GLPI diretamente, só com o `backbone` (que guarda o App-Token e a conta de
 * serviço do GLPI) — ver NOTES.md do backbone.
 */
@Singleton
class GlpiTicketRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val authorizedApiCall: AuthorizedApiCall,
    @param:BaseUrl private val baseUrl: String,
) : TicketRepository {

    override suspend fun getTickets(): AppResult<List<Ticket>> = authorizedApiCall { headers ->
        val tickets = httpClient.getJson(url = baseUrl + GlpiPaths.TICKETS, headers = headers)
            .body.getJSONArray("tickets")
        List(tickets.length()) { index -> tickets.getJSONObject(index).toTicket() }
    }

    override suspend fun getTicket(id: Long): AppResult<Ticket> = authorizedApiCall { headers ->
        httpClient.getJson(url = baseUrl + GlpiPaths.ticket(id), headers = headers)
            .body.toTicket(includeDescription = true)
    }

    override suspend fun createTicket(title: String, description: String): AppResult<Long> =
        authorizedApiCall { headers ->
            httpClient.postJson(
                url = baseUrl + GlpiPaths.TICKETS,
                body = JSONObject().put("name", title).put("content", description),
                headers = headers,
            ).body.getLong("id")
        }

    override suspend fun getMessages(ticketId: Long): AppResult<List<Message>> = authorizedApiCall { headers ->
        val followups = httpClient.getJson(url = baseUrl + GlpiPaths.followups(ticketId), headers = headers)
            .body.getJSONArray("followups")
        (0 until followups.length())
            .map { index -> followups.getJSONObject(index) }
            .filterNot { it.isPrivateFollowup() }
            .map { it.toMessage() }
    }

    override suspend fun sendMessage(
        ticketId: Long,
        text: String,
        attachments: List<Attachment>,
    ): AppResult<Message> = authorizedApiCall { headers ->
        val url = baseUrl + GlpiPaths.followups(ticketId)
        // Contrato do upload ainda em negociação com o backbone: multipart só quando há anexo.
        val response = if (attachments.isEmpty()) {
            httpClient.postJson(url = url, body = JSONObject().put("content", text), headers = headers)
        } else {
            httpClient.postMultipart(
                url = url,
                fields = mapOf("content" to text),
                files = attachments.map { MultipartFile("files", it.fileName, it.mimeType, it.bytes) },
                headers = headers,
            )
        }
        response.body.toMessage()
    }
}
