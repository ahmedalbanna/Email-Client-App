package com.example.data.local

import android.content.Context
import com.example.data.AttachmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Phase 2.4 — Attachment cache handling.
 * Real implementation would stream from IMAP FETCH BODY[] and save to cacheDir / MediaStore.
 * Here we simulate download and create empty placeholder files.
 */
class AttachmentCache(private val context: Context) {

    suspend fun cacheAttachment(attachment: AttachmentEntity, data: ByteArray = ByteArray(0)): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "attachments").apply { mkdirs() }
            val file = File(dir, attachment.fileName)
            if (!file.exists()) {
                file.writeBytes(data)
            }
            file
        }

    fun getCachedFile(attachment: AttachmentEntity): File? {
        val file = File(File(context.cacheDir, "attachments"), attachment.fileName)
        return if (file.exists()) file else null
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        File(context.cacheDir, "attachments").deleteRecursively()
    }
}
