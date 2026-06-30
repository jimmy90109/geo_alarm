package com.github.jimmy90109.geoalarm.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface PlaceReminderAttachmentStore {
    suspend fun copy(reminderId: String, uri: Uri, sortOrder: Int): PlaceReminderAttachment?
    fun delete(localPath: String)
}

@Singleton
class LocalPlaceReminderAttachmentStore @Inject constructor(
    @ApplicationContext private val context: Context
) : PlaceReminderAttachmentStore {
    override suspend fun copy(reminderId: String, uri: Uri, sortOrder: Int): PlaceReminderAttachment? {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: return null
        val type = when {
            mimeType.startsWith("image/") -> PlaceReminderAttachmentType.IMAGE
            mimeType.startsWith("video/") -> PlaceReminderAttachmentType.VIDEO
            else -> return null
        }
        val metadata = resolver.openableMetadata(uri)
        val extension = extensionFor(mimeType, metadata.displayName)
        val fileName = "${System.currentTimeMillis()}-${UUID.randomUUID()}$extension"
        val targetDir = File(context.filesDir, "place_reminder_attachments/$reminderId")
        if (!targetDir.exists()) targetDir.mkdirs()
        val target = File(targetDir, fileName)

        resolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null

        return PlaceReminderAttachment(
            reminderId = reminderId,
            type = type,
            localPath = target.absolutePath,
            mimeType = mimeType,
            displayName = metadata.displayName.ifBlank { fileName },
            sizeBytes = metadata.sizeBytes.takeIf { it >= 0L } ?: target.length(),
            durationMillis = null,
            width = null,
            height = null,
            sortOrder = sortOrder,
            createdAt = System.currentTimeMillis(),
        )
    }

    override fun delete(localPath: String) {
        runCatching { File(localPath).delete() }
    }

    private fun extensionFor(mimeType: String, displayName: String): String {
        val nameExtension = displayName.substringAfterLast('.', missingDelimiterValue = "")
            .takeIf { it.isNotBlank() && it.length <= 8 }
            ?.let { ".$it" }
        if (nameExtension != null) return nameExtension
        return when (mimeType) {
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "video/mp4" -> ".mp4"
            "video/webm" -> ".webm"
            else -> if (mimeType.startsWith("image/")) ".jpg" else ".mp4"
        }
    }

    private fun ContentResolver.openableMetadata(uri: Uri): OpenableMetadata {
        var cursor: Cursor? = null
        return try {
            cursor = query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                OpenableMetadata(
                    displayName = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else "",
                    sizeBytes = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else -1L,
                )
            } else {
                OpenableMetadata()
            }
        } finally {
            cursor?.close()
        }
    }

    private data class OpenableMetadata(
        val displayName: String = "",
        val sizeBytes: Long = -1L,
    )
}
