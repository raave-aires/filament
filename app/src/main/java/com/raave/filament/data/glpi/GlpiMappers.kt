package com.raave.filament.data.glpi

import androidx.core.text.HtmlCompat
import com.raave.filament.domain.model.Message
import com.raave.filament.domain.model.Ticket
import org.json.JSONObject

// Contrato JSON dos endpoints /api/glpi/* do backbone. Os modelos do GLPI não passam desta camada.

/**
 * `status` vem confirmado contra a instância real como `{"id": 1, "name": "Novo"}` — guardamos só o
 * `name` (já traduzido pelo GLPI) para exibição direta, sem mapear os códigos numéricos.
 */
internal fun JSONObject.toTicket(includeDescription: Boolean = false) = Ticket(
    id = getLong("id"),
    title = optString("name"),
    status = statusName(),
    openedAt = parseGlpiInstant(optString("date")),
    description = if (includeDescription) optString("content").toPlainText().takeIf(String::isNotBlank) else null,
)

/** Cai para optString("status") se um dia vier no formato antigo (opaco) combinado antes. */
private fun JSONObject.statusName(): String =
    optJSONObject("status")?.optString("name")?.takeIf(String::isNotBlank) ?: optString("status")

internal fun JSONObject.toMessage() = Message(
    id = getLong("id"),
    text = optString("content").toPlainText(),
    sentAt = parseGlpiInstant(optString("date")),
    authorName = optString("authorName").takeIf(String::isNotBlank),
    // Calculado no backend a partir do users_id do GLPI, não do nome/e-mail.
    isMine = optBoolean("isMine"),
)

/**
 * Notas internas do time técnico não deveriam nem chegar do backend pro requerente — filtrado de
 * novo aqui como cinto de segurança.
 */
internal fun JSONObject.isPrivateFollowup() = optBoolean("isPrivate", false)

/** O GLPI guarda o conteúdo como HTML (editor rich-text) — o app só mostra texto. */
private fun String.toPlainText(): String =
    HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
