package com.raave.filament.data.network

import org.json.JSONObject

data class ApiResponse(
    val statusCode: Int,
    val body: JSONObject,
    // A entrada da status-line em HttpURLConnection.headerFields tem chave null — mantenha o tipo nullable.
    val headers: Map<String?, List<String>>,
) {
    /** Nomes de header HTTP são case-insensitive; [HttpURLConnection.headerFields] não normaliza. */
    fun header(name: String): String? = headerValues(name).firstOrNull()

    private fun headerValues(name: String): List<String> =
        headers.entries.firstOrNull { it.key?.equals(name, ignoreCase = true) == true }?.value.orEmpty()

    /**
     * Monta o valor pro header `Cookie` de uma futura requisição a partir dos `Set-Cookie` desta
     * resposta (pode haver mais de um). Descarta atributos (`Path`, `HttpOnly`, `Max-Age`...) e
     * mantém só os pares `nome=valor`, que é tudo que o header `Cookie` de request aceita.
     */
    fun cookieHeaderValue(): String? {
        val pairs = headerValues("Set-Cookie").map { it.substringBefore(';') }
        return pairs.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }
}
