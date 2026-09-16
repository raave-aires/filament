package com.raave.filament.data.glpi

import androidx.core.text.HtmlCompat
import org.json.JSONObject

/**
 * `status` vem confirmado contra a instância real como `{"id": 1, "name": "Novo"}` — guardamos só
 * o `name` (já traduzido pelo GLPI) para exibição direta, sem mapear os códigos numéricos.
 */
data class GlpiTicketSummary(
    val id: Long,
    val name: String,
    val status: String,
    val date: String?,
    val dateMod: String?,
)

data class GlpiTicketDetail(
    val id: Long,
    val name: String,
    val content: String?,
    val status: String,
    val date: String?,
    val dateMod: String?,
)

data class GlpiFollowup(
    val id: Long,
    val content: String,
    val date: String?,
    val authorName: String?,
    val authorEmail: String?,
    // Calculado no backend a partir do users_id do GLPI, não do nome/e-mail — esses dois podem
    // faltar ou vir estranhos por dado legítimo (ex.: usuário de teste sem e-mail cadastrado no
    // GLPI), então nunca dá pra confiar neles pra decidir o lado da bolha no chat.
    val isMine: Boolean,
)

/** Arquivo já lido pra memória, pronto pra ir num POST multipart — ver [GlpiRepository.sendFollowup]. */
data class PendingAttachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)

internal fun JSONObject.toTicketSummary() = GlpiTicketSummary(
    id = getLong("id"),
    name = optString("name"),
    status = statusName(),
    date = optString("date").takeIf(String::isNotBlank),
    dateMod = optString("date_mod").takeIf(String::isNotBlank),
)

internal fun JSONObject.toTicketDetail() = GlpiTicketDetail(
    id = getLong("id"),
    name = optString("name"),
    content = optString("content").toPlainText().takeIf(String::isNotBlank),
    status = statusName(),
    date = optString("date").takeIf(String::isNotBlank),
    dateMod = optString("date_mod").takeIf(String::isNotBlank),
)

/** Cai para optString("status") se um dia vier no formato antigo (opaco) combinado antes. */
private fun JSONObject.statusName(): String =
    optJSONObject("status")?.optString("name")?.takeIf(String::isNotBlank) ?: optString("status")

internal fun JSONObject.toFollowup() = GlpiFollowup(
    id = getLong("id"),
    content = optString("content").toPlainText(),
    date = optString("date").takeIf(String::isNotBlank),
    authorName = optString("authorName").takeIf(String::isNotBlank),
    authorEmail = optString("authorEmail").takeIf(String::isNotBlank),
    isMine = optBoolean("isMine"),
)

/** O GLPI guarda o conteúdo do followup como HTML (editor rich-text) — o chat só mostra texto. */
private fun String.toPlainText(): String =
    HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()

/** Notas internas do time técnico não deveriam nem chegar do backend pro requerente — filtrado de novo aqui como cinto de segurança. */
internal fun JSONObject.isPrivateFollowup() = optBoolean("isPrivate", false)
