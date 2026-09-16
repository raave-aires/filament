package com.raave.filament.data.attachment

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.raave.filament.di.IoDispatcher
import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.Attachment
import com.raave.filament.domain.repository.AttachmentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Lê arquivos escolhidos pelo seletor do sistema (SAF). O resto do app não lida com `ContentResolver`. */
@Singleton
class ContentResolverAttachmentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AttachmentRepository {

    override suspend fun read(uri: String): AppResult<Attachment> = withContext(ioDispatcher) {
        val resolver = context.contentResolver
        val contentUri = uri.toUri()
        try {
            val fileName = resolver.queryDisplayName(contentUri) ?: contentUri.lastPathSegment ?: DEFAULT_FILE_NAME
            val bytes = resolver.openInputStream(contentUri)?.use { it.readBytesUpTo(Attachment.MAX_SIZE_BYTES) }
                ?: return@withContext AppResult.Failure(AppError.AttachmentUnreadable)
            if (bytes.size > Attachment.MAX_SIZE_BYTES) {
                return@withContext AppResult.Failure(AppError.AttachmentTooLarge(fileName))
            }
            AppResult.Success(
                Attachment(
                    id = UUID.randomUUID().toString(),
                    fileName = fileName,
                    mimeType = resolver.getType(contentUri) ?: DEFAULT_MIME_TYPE,
                    bytes = bytes,
                ),
            )
        } catch (e: IOException) {
            AppResult.Failure(AppError.AttachmentUnreadable)
        } catch (e: SecurityException) {
            // Permissão de leitura concedida pelo seletor pode ter expirado.
            AppResult.Failure(AppError.AttachmentUnreadable)
        }
    }

    private fun ContentResolver.queryDisplayName(uri: Uri): String? {
        query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
        }
        return null
    }

    private companion object {
        const val DEFAULT_FILE_NAME = "arquivo"
        const val DEFAULT_MIME_TYPE = "application/octet-stream"
    }
}

/**
 * Lê no máximo [limit] + 1 bytes: o byte extra basta pra saber que o arquivo passou do limite, sem
 * nunca manter mais que isso em memória (o tamanho informado pelo provider pode faltar ou mentir).
 * Antes o arquivo era lido inteiro e só depois comparado — um vídeo de alguns GB estourava a memória.
 */
internal fun InputStream.readBytesUpTo(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (output.size() <= limit) {
        val read = read(buffer, 0, minOf(buffer.size, limit + 1 - output.size()))
        if (read == -1) break
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}
