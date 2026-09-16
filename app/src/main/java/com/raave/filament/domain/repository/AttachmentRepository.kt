package com.raave.filament.domain.repository

import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.Attachment

interface AttachmentRepository {

    /**
     * Lê um arquivo escolhido pelo usuário. [uri] é a forma textual do `content://` devolvido pelo
     * seletor do sistema — texto pra manter o domínio livre de `android.net.Uri`.
     * Falha com [com.raave.filament.domain.model.AppError.AttachmentTooLarge] acima de
     * [Attachment.MAX_SIZE_BYTES], sem carregar o arquivo inteiro pra descobrir.
     */
    suspend fun read(uri: String): AppResult<Attachment>
}
