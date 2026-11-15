package com.example.data.storage

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.NoteEntity
import com.example.data.model.VaultItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

/**
 * Enforces the cloud storage size limit rule for audio recordings:
 * Any audio recording strictly larger than 900 MB (943,718,400 bytes)
 * MUST NOT be stored in Cloud Firestore and remains local-only on the device.
 */
object AudioCloudPolicy {
    const val MAX_CLOUD_AUDIO_MB: Long = 900L
    const val MAX_CLOUD_AUDIO_BYTES: Long = MAX_CLOUD_AUDIO_MB * 1024L * 1024L

    private val AUDIO_EXTENSIONS = setOf(
        "m4a", "mp3", "wav", "aac", "ogg", "opus", "flac", "3gp", "amr", "wma", "weba", "aiff"
    )

    fun isAudioOverCloudLimit(byteSize: Long): Boolean = byteSize > MAX_CLOUD_AUDIO_BYTES

    fun canStoreAudioInCloud(byteSize: Long): Boolean = byteSize <= MAX_CLOUD_AUDIO_BYTES

    fun isAudioFile(mimeType: String, fileName: String): Boolean {
        if (mimeType.startsWith("audio/", ignoreCase = true)) return true
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.US)
        return ext in AUDIO_EXTENSIONS
    }

    fun effectiveAudioByteSize(storedByteSize: Long, audioPath: String): Long {
        if (audioPath.isBlank()) return storedByteSize.coerceAtLeast(0L)
        if (!audioPath.startsWith("content://") && !audioPath.startsWith("file://")) {
            val file = File(audioPath)
            if (file.exists()) {
                return maxOf(storedByteSize, file.length())
            }
        }
        return storedByteSize.coerceAtLeast(0L)
    }

    fun canStoreNoteAudioInCloud(note: NoteEntity): Boolean {
        val effectiveSize = effectiveAudioByteSize(note.audioByteSize, note.audioFilePath)
        return canStoreAudioInCloud(effectiveSize)
    }

    fun canStoreVaultItemInCloud(item: VaultItemEntity): Boolean {
        if (!isAudioFile(item.mimeType, item.fileName)) return true
        val effectiveSize = effectiveAudioByteSize(item.byteSize, item.localFilePath)
        return canStoreAudioInCloud(effectiveSize)
    }

    fun formatByteSize(bytes: Long, isArabic: Boolean): String {
        if (bytes <= 0L) return if (isArabic) "0 ب" else "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f %s", bytes / gb, if (isArabic) "ج.ب" else "GB")
            bytes >= mb -> String.format(Locale.US, "%.1f %s", bytes / mb, if (isArabic) "م.ب" else "MB")
            bytes >= kb -> String.format(Locale.US, "%.0f %s", bytes / kb, if (isArabic) "ك.ب" else "KB")
            else -> "$bytes ${if (isArabic) "ب" else "B"}"
        }
    }
}

data class ImportedAudioInfo(
    val displayTitle: String,
    val filePathOrUri: String,
    val byteSize: Long,
    val durationSec: Int
) {
    val isLocalOnlyOverLimit: Boolean
        get() = AudioCloudPolicy.isAudioOverCloudLimit(byteSize)
}

object AudioStorageManager {

    private const val AUDIO_DIR = "fluffy_persistent_store/audio"
    private const val MAX_INTERNAL_COPY_BYTES = 150L * 1024L * 1024L

    fun audioDir(context: Context): File =
        File(context.applicationContext.filesDir, AUDIO_DIR).apply { if (!exists()) mkdirs() }

    fun createNewRecordingFile(context: Context, extension: String = "wav"): File {
        val id = UUID.randomUUID().toString().take(8)
        return File(audioDir(context), "voice_${System.currentTimeMillis()}_$id.$extension")
    }

    fun getFileByteSize(context: Context, pathOrUri: String): Long {
        if (pathOrUri.isBlank()) return 0L
        return runCatching {
            if (pathOrUri.startsWith("content://")) {
                val uri = Uri.parse(pathOrUri)
                context.applicationContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeCol = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeCol >= 0 && cursor.moveToFirst()) cursor.getLong(sizeCol) else 0L
                } ?: 0L
            } else {
                val f = File(pathOrUri)
                if (f.exists()) f.length() else 0L
            }
        }.getOrDefault(0L)
    }

    fun computeDurationSec(context: Context, pathOrUri: String): Int {
        if (pathOrUri.isBlank()) return 0
        val retrieved = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                if (pathOrUri.startsWith("content://")) {
                    retriever.setDataSource(context.applicationContext, Uri.parse(pathOrUri))
                } else {
                    retriever.setDataSource(pathOrUri)
                }
                val durationMs = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                if (durationMs > 0L) ((durationMs + 500L) / 1000L).toInt().coerceAtLeast(1) else 0
            } finally {
                retriever.release()
            }
        }.getOrDefault(0)
        if (retrieved > 0) return retrieved

        // Fallback: parse RIFF/WAV header byte rate if local .wav file
        if (!pathOrUri.startsWith("content://") && pathOrUri.endsWith(".wav", ignoreCase = true)) {
            val f = File(pathOrUri)
            if (f.exists() && f.length() > 44L) {
                return runCatching {
                    java.io.RandomAccessFile(f, "r").use { raf ->
                        raf.seek(28L)
                        val b0 = raf.read()
                        val b1 = raf.read()
                        val b2 = raf.read()
                        val b3 = raf.read()
                        val byteRate = b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
                        val dataBytes = (f.length() - 44L).coerceAtLeast(0L)
                        if (byteRate > 0) {
                            ((dataBytes + byteRate / 2L) / byteRate.toLong()).toInt().coerceAtLeast(1)
                        } else 1
                    }
                }.getOrDefault(1)
            }
        }
        return 0
    }

    suspend fun importAudioFromUri(context: Context, uri: Uri): ImportedAudioInfo =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val resolver = app.contentResolver

            var displayName = "Audio_${System.currentTimeMillis()}"
            var queriedSize = -1L
            runCatching {
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) {
                            cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
                        }
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            queriedSize = cursor.getLong(sizeIdx)
                        }
                    }
                }
            }

            val cleanTitle = displayName.substringBeforeLast('.').ifBlank { displayName }

            // For large audio files (e.g., > 150 MB, including > 900 MB), take persistable URI
            // permission and reference in place to avoid unnecessary multi-hundred-MB copies.
            if (queriedSize > MAX_INTERNAL_COPY_BYTES) {
                val persistableGranted = runCatching {
                    resolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    true
                }.getOrDefault(false)

                if (persistableGranted) {
                    val dur = computeDurationSec(app, uri.toString()).coerceAtLeast(1)
                    return@withContext ImportedAudioInfo(
                        displayTitle = cleanTitle,
                        filePathOrUri = uri.toString(),
                        byteSize = queriedSize,
                        durationSec = dur
                    )
                }
            }

            // Copy locally into app audio storage for reliable playback across restarts
            val safeName = displayName.replace(Regex("""[^\w.\-() ]"""), "_")
            val destFile = File(audioDir(app), "${System.currentTimeMillis()}_$safeName")
            val tmpFile = File(audioDir(app), "${destFile.name}.part")
            var copiedBytes = 0L

            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        copiedBytes += read
                    }
                    output.flush()
                }
            } ?: throw IllegalStateException("Cannot open selected audio file")

            if (!tmpFile.renameTo(destFile)) {
                tmpFile.copyTo(destFile, overwrite = true)
                tmpFile.delete()
            }

            val finalSize = maxOf(copiedBytes, destFile.length(), queriedSize.coerceAtLeast(0L))
            val dur = computeDurationSec(app, destFile.absolutePath).coerceAtLeast(1)

            ImportedAudioInfo(
                displayTitle = cleanTitle,
                filePathOrUri = destFile.absolutePath,
                byteSize = finalSize,
                durationSec = dur
            )
        }
}
