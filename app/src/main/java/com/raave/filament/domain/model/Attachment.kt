package com.raave.filament.domain.model

/**
 * Arquivo já lido pra memória, pronto pra envio. Não é `data class` de propósito: o `equals` gerado
 * compararia o [ByteArray] por referência, dando a falsa impressão de comparar conteúdo.
 */
class Attachment(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    companion object {
        const val MAX_SIZE_BYTES = 15 * 1024 * 1024
    }
}
