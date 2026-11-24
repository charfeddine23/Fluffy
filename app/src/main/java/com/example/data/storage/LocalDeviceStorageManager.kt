package com.example.data.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.ChecklistItem
import com.example.data.model.GlassTint
import com.example.data.model.NoteEntity
import com.example.data.model.NotePagesCodec
import com.example.data.model.VaultItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

data class FluffySettingsSnapshot(
    val isDarkMode: Boolean,
    val isArabic: Boolean,
    val isGridView: Boolean,
    val isBiometricEnabled: Boolean,
    val selectedCategory: String,
    val cloudSyncEnabled: Boolean = true,
    val selectedThemeId: String = "ayu_dark",
    val appLanguageCode: String = if (isArabic) "ar" else "en",
    val nonScreenshotEnabled: Boolean = false,
    val coloredContainerBorders: Boolean = true,
    val notesViewMode: String = if (isGridView) "grid" else "list",
    val isPremium: Boolean = false,
    val fabMenuStyle: String = "vertical_stack",
    val selectedFontId: String = "sans_serif"
)

data class FluffyBackupBundle(
    val schemaVersion: Int,
    val exportedAt: Long,
    val appVersion: String,
    val settings: FluffySettingsSnapshot?,
    val notes: List<NoteEntity>,
    val vaultItems: List<RestorableVaultItem>
)

data class RestorableVaultItem(
    val entity: VaultItemEntity,
    val rawFileBase64: String?
)

/**
 * Literal, tamper-resistant local phone storage engine.
 * - Stores Notes, Settings, and Vault items verbatim on device internal storage.
 * - Uses write-once semantics for Vault records and raw files (never mutates or trims saved data).
 * - Builds and parses portable full JSON backups (Settings + Notes + Vault) for Firebase Cloud Sync and phone storage.
 */
object LocalDeviceStorageManager {

    private const val ROOT_DIR = "fluffy_persistent_store"
    private const val NOTES_DIR = "notes"
    private const val SETTINGS_DIR = "settings"
    private const val VAULT_ITEMS_DIR = "vault/items"
    private const val VAULT_RAW_DIR = "vault/raw"
    private const val VAULT_NOTE_META_DIR = "vault/note_meta"
    private const val BACKUPS_DIR = "backups"

    private const val NOTES_SNAPSHOT_FILE = "notes_literal_snapshot.json"
    private const val SETTINGS_SNAPSHOT_FILE = "settings_literal.json"
    private const val FULL_BACKUP_FILE = "fluffy_full_backup.json"

    // Up to 12 MB per attached Vault file embedded verbatim in cloud/JSON backup
    private const val MAX_INLINE_BACKUP_FILE_BYTES = 12L * 1024L * 1024L

    private fun rootDir(context: Context): File =
        File(context.applicationContext.filesDir, ROOT_DIR).apply { if (!exists()) mkdirs() }

    private fun subDir(context: Context, relative: String): File =
        File(rootDir(context), relative).apply { if (!exists()) mkdirs() }

    private fun atomicWriteUtf8(target: File, content: String) {
        val parent = target.parentFile ?: return
        if (!parent.exists()) parent.mkdirs()
        val tmp = File(parent, "${target.name}.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
            try {
                out.fd.sync()
            } catch (_: Exception) {
            }
        }
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    /**
     * Write-once file writer: if [target] already exists, NEVER overwrites or alters it.
     */
    private fun writeOnceUtf8(target: File, content: String) {
        if (target.exists()) return
        atomicWriteUtf8(target, content)
    }

    fun saveSettingsSnapshot(context: Context, settings: FluffySettingsSnapshot) {
        runCatching {
            val json = settingsToJson(settings).toString(2)
            val file = File(subDir(context, SETTINGS_DIR), SETTINGS_SNAPSHOT_FILE)
            atomicWriteUtf8(file, json)
        }
    }

    fun loadSettingsSnapshot(context: Context): FluffySettingsSnapshot? {
        return runCatching {
            val file = File(subDir(context, SETTINGS_DIR), SETTINGS_SNAPSHOT_FILE)
            if (!file.exists()) return null
            jsonToSettings(JSONObject(file.readText(Charsets.UTF_8)))
        }.getOrNull()
    }

    fun saveNotesLiteralSnapshot(context: Context, notes: List<NoteEntity>) {
        runCatching {
            val array = JSONArray()
            notes.forEach { array.put(noteToJson(it)) }
            val wrapper = JSONObject().apply {
                put("updatedAt", System.currentTimeMillis())
                put("count", notes.size)
                put("notes", array)
            }
            val file = File(subDir(context, NOTES_DIR), NOTES_SNAPSHOT_FILE)
            atomicWriteUtf8(file, wrapper.toString(2))
        }
    }

    fun loadNotesLiteralSnapshot(context: Context): List<NoteEntity> {
        return runCatching {
            val file = File(subDir(context, NOTES_DIR), NOTES_SNAPSHOT_FILE)
            if (!file.exists()) return emptyList()
            val obj = JSONObject(file.readText(Charsets.UTF_8))
            val arr = obj.optJSONArray("notes") ?: return emptyList()
            buildList {
                for (i in 0 until arr.length()) {
                    add(jsonToNote(arr.getJSONObject(i)))
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun saveLiteralTextToVault(
        context: Context,
        title: String,
        literalContent: String
    ): VaultItemEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val rawBytes = literalContent.toByteArray(Charsets.UTF_8)
        val sha256 = computeSha256(rawBytes)
        val rawFile = File(subDir(context, VAULT_RAW_DIR), "${id}_literal.txt")
        if (!rawFile.exists()) {
            FileOutputStream(rawFile).use { out ->
                out.write(rawBytes)
                out.flush()
                try {
                    out.fd.sync()
                } catch (_: Exception) {
                }
            }
        }

        val entity = VaultItemEntity(
            id = id,
            title = title,
            literalContent = literalContent,
            fileName = "",
            mimeType = "text/plain; charset=utf-8",
            localFilePath = rawFile.absolutePath,
            byteSize = rawBytes.size.toLong(),
            sha256 = sha256,
            createdAt = System.currentTimeMillis(),
            isImmutable = true
        )

        persistVaultEntityWriteOnce(context, entity)
        entity
    }

    suspend fun saveFileUriLiterallyToVault(
        context: Context,
        uri: Uri,
        customTitle: String = ""
    ): VaultItemEntity = withContext(Dispatchers.IO) {
        val resolver = context.applicationContext.contentResolver
        val originalName = queryDisplayName(context, uri) ?: "vault_file_${System.currentTimeMillis()}"
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val id = UUID.randomUUID().toString()
        val safeName = originalName.replace(Regex("""[^\w.\-() ]"""), "_")
        val destFile = File(subDir(context, VAULT_RAW_DIR), "${id}_$safeName")

        val digest = MessageDigest.getInstance("SHA-256")
        var totalBytes = 0L

        if (!destFile.exists()) {
            val tmpFile = File(destFile.parentFile, "${destFile.name}.part")
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        totalBytes += read
                    }
                    output.flush()
                    try {
                        output.fd.sync()
                    } catch (_: Exception) {
                    }
                }
            } ?: throw IllegalStateException("Cannot open selected file")
            tmpFile.renameTo(destFile)
        } else {
            totalBytes = destFile.length()
        }

        val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
        val entity = VaultItemEntity(
            id = id,
            title = customTitle.ifEmpty { originalName },
            literalContent = "",
            fileName = originalName,
            mimeType = mimeType,
            localFilePath = destFile.absolutePath,
            byteSize = totalBytes,
            sha256 = sha256,
            createdAt = System.currentTimeMillis(),
            isImmutable = true
        )

        persistVaultEntityWriteOnce(context, entity)
        entity
    }

    fun persistVaultEntityWriteOnce(context: Context, item: VaultItemEntity) {
        val metaFile = File(subDir(context, VAULT_ITEMS_DIR), "${item.id}.json")
        writeOnceUtf8(metaFile, vaultItemToJson(item, includeRawFile = false).toString(2))
    }

    fun persistVaultNoteMetaJson(context: Context, vaultId: String, noteJson: String?) {
        if (noteJson.isNullOrBlank()) return
        runCatching {
            val file = File(subDir(context, VAULT_NOTE_META_DIR), "$vaultId.json")
            writeOnceUtf8(file, noteJson)
        }
    }

    fun loadVaultNoteMetaJson(context: Context, vaultId: String): String? {
        return runCatching {
            val file = File(subDir(context, VAULT_NOTE_META_DIR), "$vaultId.json")
            if (file.exists()) file.readText(Charsets.UTF_8) else null
        }.getOrNull()
    }

    fun loadVaultNoteMetadata(context: Context, vaultId: String): NoteEntity? {
        val raw = loadVaultNoteMetaJson(context, vaultId) ?: return null
        return runCatching { jsonToNote(JSONObject(raw)) }.getOrNull()
    }

    fun loadAllVaultNoteMetadata(context: Context): Map<String, NoteEntity> {
        return runCatching {
            val dir = subDir(context, VAULT_NOTE_META_DIR)
            dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                ?.mapNotNull { file ->
                    val id = file.name.removeSuffix(".json")
                    runCatching { id to jsonToNote(JSONObject(file.readText(Charsets.UTF_8))) }.getOrNull()
                }
                ?.toMap()
                ?: emptyMap()
        }.getOrDefault(emptyMap())
    }

    private fun buildReadableVaultLiteral(note: NoteEntity): String {
        val draftPages = NotePagesCodec.decodeDraftPages(note.content, note.pagesJson)
        val checklist = ChecklistItem.jsonToList(note.checklistJson)
        if (draftPages.size <= 1 && checklist.isEmpty()) {
            val p1 = draftPages.firstOrNull()
            return if (p1 != null && p1.title.isNotBlank()) {
                "${p1.title}\n${p1.body}".trimEnd()
            } else {
                note.content
            }
        }
        val sb = StringBuilder()
        val nonEmptyPages = draftPages.filterIndexed { index, page ->
            index == 0 || page.title.isNotBlank() || page.body.isNotEmpty()
        }
        val body = nonEmptyPages.joinToString("\n\n") { page ->
            if (page.title.isNotBlank()) "${page.title}\n${page.body}".trimEnd() else page.body.trimEnd()
        }.trimEnd()
        if (body.isNotEmpty()) {
            sb.append(body)
        }
        if (checklist.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append("\n\n")
            checklist.forEachIndexed { idx, item ->
                if (idx > 0) sb.append('\n')
                sb.append(if (item.isDone) "[✓] " else "[ ] ")
                sb.append(item.text)
            }
        }
        return sb.toString()
    }

    private fun guessAudioMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "ogg", "opus" -> "audio/ogg"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "3gp" -> "audio/3gpp"
            "amr" -> "audio/amr"
            "weba" -> "audio/webm"
            else -> "audio/wav"
        }
    }

    suspend fun saveNoteAsVaultItem(
        context: Context,
        note: NoteEntity
    ): VaultItemEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val literalText = buildReadableVaultLiteral(note)
        val resolver = context.applicationContext.contentResolver

        val hasAudio = note.audioFilePath.isNotBlank() || note.audioDurationSec > 0
        var resolvedFileName = ""
        var resolvedMimeType = "text/plain; charset=utf-8"
        var resolvedLocalPath = ""
        var resolvedByteSize: Long
        var resolvedSha256 = ""

        if (hasAudio && note.audioFilePath.isNotBlank()) {
            val sourcePath = note.audioFilePath
            if (sourcePath.startsWith("content://")) {
                val uri = Uri.parse(sourcePath)
                val origName = queryDisplayName(context, uri)
                    ?: (note.title.ifBlank { "voice_$now" }.replace(Regex("""[^\w.\-() ]"""), "_") + ".m4a")
                val safeName = origName.replace(Regex("""[^\w.\-() ]"""), "_")
                val queriedSize = AudioStorageManager.getFileByteSize(context, sourcePath)
                val canCopyLocally = queriedSize in 1L..(150L * 1024L * 1024L)
                if (canCopyLocally) {
                    val destFile = File(subDir(context, VAULT_RAW_DIR), "${id}_$safeName")
                    val digest = MessageDigest.getInstance("SHA-256")
                    var total = 0L
                    val copied = runCatching {
                        val tmpFile = File(destFile.parentFile, "${destFile.name}.part")
                        resolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(tmpFile).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                    digest.update(buffer, 0, read)
                                    total += read
                                }
                                output.flush()
                            }
                        } ?: error("Cannot open audio URI")
                        if (!tmpFile.renameTo(destFile)) {
                            tmpFile.copyTo(destFile, overwrite = true)
                            tmpFile.delete()
                        }
                        true
                    }.getOrDefault(false)

                    if (copied && destFile.exists()) {
                        resolvedFileName = origName
                        resolvedMimeType = resolver.getType(uri) ?: guessAudioMimeType(origName)
                        resolvedLocalPath = destFile.absolutePath
                        resolvedByteSize = maxOf(total, destFile.length())
                        resolvedSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                    } else {
                        resolvedFileName = origName
                        resolvedMimeType = resolver.getType(uri) ?: guessAudioMimeType(origName)
                        resolvedLocalPath = sourcePath
                        resolvedByteSize = AudioCloudPolicy.effectiveAudioByteSize(note.audioByteSize, sourcePath)
                        resolvedSha256 = ""
                    }
                } else {
                    resolvedFileName = origName
                    resolvedMimeType = resolver.getType(uri) ?: guessAudioMimeType(origName)
                    resolvedLocalPath = sourcePath
                    resolvedByteSize = AudioCloudPolicy.effectiveAudioByteSize(note.audioByteSize, sourcePath)
                    resolvedSha256 = ""
                }
            } else {
                val srcFile = File(sourcePath)
                val origName = srcFile.name.ifBlank { "voice_$now.wav" }
                val safeName = origName.replace(Regex("""[^\w.\-() ]"""), "_")
                resolvedFileName = origName
                resolvedMimeType = guessAudioMimeType(origName)
                if (srcFile.exists()) {
                    val destFile = File(subDir(context, VAULT_RAW_DIR), "${id}_$safeName")
                    val digest = MessageDigest.getInstance("SHA-256")
                    var total = 0L
                    val tmpFile = File(destFile.parentFile, "${destFile.name}.part")
                    FileInputStream(srcFile).use { input ->
                        FileOutputStream(tmpFile).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                digest.update(buffer, 0, read)
                                total += read
                            }
                            output.flush()
                            try {
                                output.fd.sync()
                            } catch (_: Exception) {
                            }
                        }
                    }
                    if (!tmpFile.renameTo(destFile)) {
                        tmpFile.copyTo(destFile, overwrite = true)
                        tmpFile.delete()
                    }
                    resolvedLocalPath = destFile.absolutePath
                    resolvedByteSize = maxOf(total, destFile.length())
                    resolvedSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                } else {
                    resolvedLocalPath = sourcePath
                    resolvedByteSize = AudioCloudPolicy.effectiveAudioByteSize(note.audioByteSize, sourcePath)
                    resolvedSha256 = ""
                }
            }
        } else {
            val rawBytes = literalText.toByteArray(Charsets.UTF_8)
            resolvedSha256 = computeSha256(rawBytes)
            val rawFile = File(subDir(context, VAULT_RAW_DIR), "${id}_literal.txt")
            if (!rawFile.exists()) {
                FileOutputStream(rawFile).use { out ->
                    out.write(rawBytes)
                    out.flush()
                    try {
                        out.fd.sync()
                    } catch (_: Exception) {
                    }
                }
            }
            resolvedLocalPath = rawFile.absolutePath
            resolvedByteSize = rawBytes.size.toLong()
        }

        val resolvedDuration = if (hasAudio && note.audioDurationSec <= 0 && resolvedLocalPath.isNotBlank()) {
            AudioStorageManager.computeDurationSec(context, resolvedLocalPath)
        } else {
            note.audioDurationSec
        }

        val normalizedNote = note.copy(
            audioDurationSec = resolvedDuration,
            audioByteSize = if (hasAudio) resolvedByteSize else note.audioByteSize
        )
        persistVaultNoteMetaJson(context, id, noteToJson(normalizedNote).toString(2))

        val entity = VaultItemEntity(
            id = id,
            title = note.title.ifEmpty {
                if (hasAudio) "Voice Recording" else ""
            },
            literalContent = literalText,
            fileName = resolvedFileName,
            mimeType = resolvedMimeType,
            localFilePath = resolvedLocalPath,
            byteSize = resolvedByteSize,
            sha256 = resolvedSha256,
            createdAt = now,
            isImmutable = true
        )

        persistVaultEntityWriteOnce(context, entity)
        entity
    }

    /**
     * Updates an existing Vault note, blog, or voice note directly inside the Vault:
     * - Updates title, multi-page content, checklist, styling metadata, and literal storage file.
     * - Updates or replaces/removes attached audio recording in vault/raw & recalculates byteSize + sha256.
     */
    suspend fun updateVaultNoteAndAudioItem(
        context: Context,
        existingItem: VaultItemEntity,
        existingMeta: NoteEntity?,
        title: String,
        content: String,
        pagesJson: String,
        category: String,
        colorPreset: String,
        backgroundId: String,
        customColorArgb: Long,
        isPinned: Boolean,
        checklist: List<ChecklistItem>,
        audioDurationSec: Int,
        audioFilePath: String,
        audioByteSize: Long
    ): VaultItemEntity = withContext(Dispatchers.IO) {
        val id = existingItem.id
        val now = System.currentTimeMillis()
        val rawDir = subDir(context, VAULT_RAW_DIR)
        val checklistJson = ChecklistItem.listToJson(checklist)

        val baseMeta = existingMeta ?: NoteEntity(
            id = 0L,
            title = existingItem.title,
            content = existingItem.literalContent,
            category = category,
            colorPreset = colorPreset,
            backgroundId = backgroundId,
            customColorArgb = customColorArgb,
            isPinned = isPinned,
            createdAt = existingItem.createdAt,
            updatedAt = now,
            syncId = id
        )

        val updatedNoteTemp = baseMeta.copy(
            title = title,
            content = content,
            pagesJson = pagesJson,
            category = category,
            colorPreset = colorPreset,
            backgroundId = backgroundId,
            customColorArgb = customColorArgb,
            isPinned = isPinned,
            checklistJson = checklistJson,
            audioDurationSec = audioDurationSec,
            audioFilePath = audioFilePath,
            audioByteSize = audioByteSize,
            updatedAt = now
        )
        val literalText = buildReadableVaultLiteral(updatedNoteTemp)

        val wasNonAudioBinaryFile = existingItem.fileName.isNotEmpty() &&
            !AudioCloudPolicy.isAudioFile(existingItem.mimeType, existingItem.fileName) &&
            audioFilePath.isBlank()

        var resolvedFileName = existingItem.fileName
        var resolvedMimeType = existingItem.mimeType
        var resolvedLocalPath = existingItem.localFilePath
        var resolvedByteSize = existingItem.byteSize
        var resolvedSha256 = existingItem.sha256
        var resolvedDuration = audioDurationSec

        if (wasNonAudioBinaryFile) {
            // Keep imported non-audio binary file untouched while persisting title/note annotations
        } else if (audioFilePath.isNotBlank()) {
            if (audioFilePath == existingItem.localFilePath) {
                // Keep existing audio file inside VAULT_RAW_DIR
                val existingFile = File(audioFilePath)
                if (existingFile.exists()) {
                    resolvedByteSize = existingFile.length()
                    resolvedSha256 = existingItem.sha256.ifBlank { computeFileSha256(existingFile) }
                } else {
                    resolvedByteSize = AudioCloudPolicy.effectiveAudioByteSize(audioByteSize, audioFilePath)
                }
                if (resolvedDuration <= 0) {
                    resolvedDuration = AudioStorageManager.computeDurationSec(context, audioFilePath)
                }
                if (resolvedMimeType.isBlank() || resolvedMimeType.startsWith("text/")) {
                    resolvedMimeType = guessAudioMimeType(resolvedFileName.ifBlank { existingFile.name })
                }
            } else if (audioFilePath.startsWith("content://")) {
                val resolver = context.applicationContext.contentResolver
                val uri = Uri.parse(audioFilePath)
                val origName = queryDisplayName(context, uri)
                    ?: (title.ifBlank { "voice_$now" }.replace(Regex("""[^\w.\-() ]"""), "_") + ".m4a")
                val safeName = origName.replace(Regex("""[^\w.\-() ]"""), "_")
                val queriedSize = AudioStorageManager.getFileByteSize(context, audioFilePath)
                val canCopyLocally = queriedSize in 1L..(150L * 1024L * 1024L)
                if (canCopyLocally) {
                    val destFile = File(rawDir, "${id}_${now}_$safeName")
                    val tmpFile = File(destFile.parentFile, "${destFile.name}.part")
                    val digest = MessageDigest.getInstance("SHA-256")
                    var total = 0L
                    val copied = runCatching {
                        resolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(tmpFile).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                    digest.update(buffer, 0, read)
                                    total += read
                                }
                                output.flush()
                            }
                        } ?: error("Cannot open audio URI")
                        if (!tmpFile.renameTo(destFile)) {
                            tmpFile.copyTo(destFile, overwrite = true)
                            tmpFile.delete()
                        }
                        true
                    }.getOrDefault(false)

                    if (copied && destFile.exists()) {
                        if (existingItem.localFilePath.isNotBlank() && existingItem.localFilePath != destFile.absolutePath) {
                            runCatching { File(existingItem.localFilePath).takeIf { it.parentFile?.absolutePath == rawDir.absolutePath }?.delete() }
                        }
                        resolvedFileName = origName
                        resolvedMimeType = resolver.getType(uri) ?: guessAudioMimeType(origName)
                        resolvedLocalPath = destFile.absolutePath
                        resolvedByteSize = maxOf(total, destFile.length())
                        resolvedSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                    }
                } else {
                    resolvedFileName = origName
                    resolvedMimeType = resolver.getType(uri) ?: guessAudioMimeType(origName)
                    resolvedLocalPath = audioFilePath
                    resolvedByteSize = AudioCloudPolicy.effectiveAudioByteSize(audioByteSize, audioFilePath)
                    resolvedSha256 = ""
                }
                if (resolvedDuration <= 0) {
                    resolvedDuration = AudioStorageManager.computeDurationSec(context, resolvedLocalPath)
                }
            } else {
                val srcFile = File(audioFilePath)
                val origName = srcFile.name.ifBlank { "voice_$now.wav" }
                val safeName = origName.replace(Regex("""[^\w.\-() ]"""), "_")
                resolvedFileName = origName
                resolvedMimeType = guessAudioMimeType(origName)
                if (srcFile.exists()) {
                    val destFile = File(rawDir, "${id}_${now}_$safeName")
                    val tmpFile = File(destFile.parentFile, "${destFile.name}.part")
                    val digest = MessageDigest.getInstance("SHA-256")
                    var total = 0L
                    FileInputStream(srcFile).use { input ->
                        FileOutputStream(tmpFile).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                digest.update(buffer, 0, read)
                                total += read
                            }
                            output.flush()
                            try {
                                output.fd.sync()
                            } catch (_: Exception) {
                            }
                        }
                    }
                    if (!tmpFile.renameTo(destFile)) {
                        tmpFile.copyTo(destFile, overwrite = true)
                        tmpFile.delete()
                    }
                    if (existingItem.localFilePath.isNotBlank() && existingItem.localFilePath != destFile.absolutePath) {
                        runCatching { File(existingItem.localFilePath).takeIf { it.parentFile?.absolutePath == rawDir.absolutePath }?.delete() }
                    }
                    resolvedLocalPath = destFile.absolutePath
                    resolvedByteSize = maxOf(total, destFile.length())
                    resolvedSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                    val audioDir = AudioStorageManager.audioDir(context)
                    if (srcFile.parentFile?.absolutePath == audioDir.absolutePath) {
                        runCatching { srcFile.delete() }
                    }
                } else {
                    resolvedLocalPath = audioFilePath
                    resolvedByteSize = AudioCloudPolicy.effectiveAudioByteSize(audioByteSize, audioFilePath)
                    resolvedSha256 = ""
                }
                if (resolvedDuration <= 0) {
                    resolvedDuration = AudioStorageManager.computeDurationSec(context, resolvedLocalPath)
                }
            }
        } else {
            // Audio removed or plain rich note/blog edit -> write literal UTF-8 file
            if (existingItem.localFilePath.isNotBlank() && existingItem.fileName.isNotEmpty()) {
                runCatching {
                    File(existingItem.localFilePath).takeIf { it.parentFile?.absolutePath == rawDir.absolutePath }?.delete()
                }
            }
            resolvedFileName = ""
            resolvedMimeType = "text/plain; charset=utf-8"
            val rawBytes = literalText.toByteArray(Charsets.UTF_8)
            resolvedSha256 = computeSha256(rawBytes)
            val rawFile = File(rawDir, "${id}_literal.txt")
            val tmpFile = File(rawDir, "${id}_literal.txt.part")
            FileOutputStream(tmpFile).use { out ->
                out.write(rawBytes)
                out.flush()
                try {
                    out.fd.sync()
                } catch (_: Exception) {
                }
            }
            if (rawFile.exists()) rawFile.delete()
            if (!tmpFile.renameTo(rawFile)) {
                tmpFile.copyTo(rawFile, overwrite = true)
                tmpFile.delete()
            }
            resolvedLocalPath = rawFile.absolutePath
            resolvedByteSize = rawBytes.size.toLong()
            resolvedDuration = 0
        }

        val finalMetaNote = updatedNoteTemp.copy(
            audioDurationSec = resolvedDuration,
            audioFilePath = if (audioFilePath.isNotBlank()) resolvedLocalPath else "",
            audioByteSize = if (audioFilePath.isNotBlank()) resolvedByteSize else 0L
        )
        val noteMetaFile = File(subDir(context, VAULT_NOTE_META_DIR), "$id.json")
        atomicWriteUtf8(noteMetaFile, noteToJson(finalMetaNote).toString(2))

        val updatedEntity = existingItem.copy(
            title = title.ifEmpty {
                if (resolvedFileName.isNotEmpty() && audioFilePath.isNotBlank()) "Voice Recording" else ""
            },
            literalContent = literalText,
            fileName = resolvedFileName,
            mimeType = resolvedMimeType,
            localFilePath = resolvedLocalPath,
            byteSize = resolvedByteSize,
            sha256 = resolvedSha256
        )

        val itemMetaFile = File(subDir(context, VAULT_ITEMS_DIR), "$id.json")
        atomicWriteUtf8(itemMetaFile, vaultItemToJson(updatedEntity, includeRawFile = false).toString(2))
        updatedEntity
    }

    suspend fun buildNoteFromVaultItem(
        context: Context,
        item: VaultItemEntity,
        isArabic: Boolean,
        targetSortOrder: Long
    ): NoteEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val meta = loadVaultNoteMetadata(context, item.id)
        val isAudioItem = AudioCloudPolicy.isAudioFile(item.mimeType, item.fileName) ||
            (meta != null && (meta.audioFilePath.isNotBlank() || meta.audioDurationSec > 0))

        var restoredAudioPath = ""
        var restoredAudioBytes = 0L
        var restoredAudioDuration = meta?.audioDurationSec ?: 0

        if (isAudioItem) {
            val sourcePath = item.localFilePath.ifBlank { meta?.audioFilePath.orEmpty() }
            if (sourcePath.isNotBlank() && !sourcePath.startsWith("content://")) {
                val srcFile = File(sourcePath)
                if (srcFile.exists()) {
                    val extName = item.fileName.ifBlank { srcFile.name }.replace(Regex("""[^\w.\-() ]"""), "_")
                    val destFile = File(AudioStorageManager.audioDir(context), "voice_${now}_$extName")
                    val tmpFile = File(destFile.parentFile, "${destFile.name}.part")
                    FileInputStream(srcFile).use { input ->
                        FileOutputStream(tmpFile).use { output ->
                            input.copyTo(output, bufferSize = 64 * 1024)
                            output.flush()
                        }
                    }
                    if (!tmpFile.renameTo(destFile)) {
                        tmpFile.copyTo(destFile, overwrite = true)
                        tmpFile.delete()
                    }
                    restoredAudioPath = destFile.absolutePath
                    restoredAudioBytes = destFile.length()
                    if (restoredAudioDuration <= 0) {
                        restoredAudioDuration = AudioStorageManager.computeDurationSec(context, restoredAudioPath).coerceAtLeast(1)
                    }
                } else {
                    restoredAudioPath = sourcePath
                    restoredAudioBytes = AudioCloudPolicy.effectiveAudioByteSize(item.byteSize, sourcePath)
                }
            } else if (sourcePath.isNotBlank()) {
                restoredAudioPath = sourcePath
                restoredAudioBytes = AudioCloudPolicy.effectiveAudioByteSize(item.byteSize, sourcePath)
                if (restoredAudioDuration <= 0) {
                    restoredAudioDuration = AudioStorageManager.computeDurationSec(context, sourcePath).coerceAtLeast(1)
                }
            }
        }

        if (meta != null) {
            return@withContext meta.copy(
                id = 0L,
                isArchived = false,
                isDeleted = false,
                deletedAt = 0L,
                audioFilePath = if (isAudioItem) restoredAudioPath else meta.audioFilePath,
                audioByteSize = if (isAudioItem) restoredAudioBytes else meta.audioByteSize,
                audioDurationSec = if (isAudioItem) restoredAudioDuration else meta.audioDurationSec,
                sortOrder = targetSortOrder,
                updatedAt = now,
                syncId = UUID.randomUUID().toString()
            )
        }

        if (isAudioItem) {
            val resolvedTitle = item.title.ifBlank {
                item.fileName.substringBeforeLast('.').ifBlank {
                    if (isArabic) "تسجيل صوتي" else "Voice Recording"
                }
            }
            return@withContext NoteEntity(
                title = resolvedTitle,
                content = "",
                category = if (isArabic) "صوتي" else "Audio",
                colorPreset = GlassTint.ROSE.id,
                audioDurationSec = restoredAudioDuration.coerceAtLeast(1),
                audioFilePath = restoredAudioPath,
                audioByteSize = restoredAudioBytes,
                createdAt = item.createdAt,
                updatedAt = now,
                syncId = UUID.randomUUID().toString(),
                sortOrder = targetSortOrder
            )
        }

        // Fallback for standalone Vault text or Vault file item created inside Vault
        val fallbackBody = when {
            item.literalContent.isNotEmpty() -> item.literalContent
            item.mimeType.startsWith("text/", ignoreCase = true) && item.localFilePath.isNotBlank() -> {
                val f = File(item.localFilePath)
                if (f.exists() && f.length() <= NotePagesCodec.MAX_NOTE_BYTES) {
                    runCatching { f.readText(Charsets.UTF_8) }.getOrDefault("")
                } else ""
            }
            item.fileName.isNotEmpty() -> {
                val sizeLabel = AudioCloudPolicy.formatByteSize(item.byteSize, isArabic)
                if (isArabic) "ملف مستورد من الـ Vault: ${item.fileName} ($sizeLabel)"
                else "Imported from Vault: ${item.fileName} ($sizeLabel)"
            }
            else -> ""
        }

        val (fittedBody, _) = NotePagesCodec.fitWithinByteBudget(fallbackBody, NotePagesCodec.MAX_NOTE_BYTES)

        return@withContext NoteEntity(
            title = item.title.ifBlank { item.fileName.ifBlank { if (isArabic) "ملاحظة من الـ Vault" else "Vault Note" } },
            content = fittedBody,
            category = if (isArabic) "أفكار" else "Ideas",
            colorPreset = GlassTint.AMETHYST.id,
            createdAt = item.createdAt,
            updatedAt = now,
            syncId = UUID.randomUUID().toString(),
            sortOrder = targetSortOrder
        )
    }

    fun loadAllLiteralVaultItemsFromDisk(context: Context): List<VaultItemEntity> {
        return runCatching {
            val dir = subDir(context, VAULT_ITEMS_DIR)
            dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                ?.mapNotNull { file ->
                    runCatching {
                        jsonToVaultItem(JSONObject(file.readText(Charsets.UTF_8))).entity
                    }.getOrNull()
                }
                ?.sortedByDescending { it.createdAt }
                ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun restoreVaultRawFileFromBase64WriteOnce(
        context: Context,
        item: VaultItemEntity,
        base64Data: String?,
        noteMetaJson: String? = null
    ): VaultItemEntity {
        persistVaultNoteMetaJson(context, item.id, noteMetaJson)
        val rawDir = subDir(context, VAULT_RAW_DIR)
        if (!base64Data.isNullOrEmpty() && item.fileName.isNotEmpty()) {
            val safeName = item.fileName.replace(Regex("""[^\w.\-() ]"""), "_")
            val target = File(rawDir, "${item.id}_$safeName")
            if (!target.exists()) {
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                FileOutputStream(target).use { out ->
                    out.write(bytes)
                    out.flush()
                }
            }
            val updated = item.copy(localFilePath = target.absolutePath)
            persistVaultEntityWriteOnce(context, updated)
            return updated
        } else if (item.literalContent.isNotEmpty()) {
            val target = File(rawDir, "${item.id}_literal.txt")
            if (!target.exists()) {
                writeOnceUtf8(target, item.literalContent)
            }
            val updated = item.copy(localFilePath = target.absolutePath)
            persistVaultEntityWriteOnce(context, updated)
            return updated
        }
        persistVaultEntityWriteOnce(context, item)
        return item
    }

    fun verifyVaultItemIntegrity(item: VaultItemEntity): Boolean {
        if (item.sha256.isBlank()) return true
        val file = item.localFilePath.takeIf { it.isNotBlank() }?.let(::File)
        val actualHash = if (file != null && file.exists()) {
            computeFileSha256(file)
        } else {
            computeSha256(item.literalContent.toByteArray(Charsets.UTF_8))
        }
        return actualHash.equals(item.sha256, ignoreCase = true)
    }

    suspend fun exportVaultItemToUri(
        context: Context,
        item: VaultItemEntity,
        destUri: Uri
    ) = withContext(Dispatchers.IO) {
        val resolver = context.applicationContext.contentResolver
        resolver.openOutputStream(destUri)?.use { out ->
            val rawFile = item.localFilePath.takeIf { it.isNotBlank() }?.let(::File)
            if (rawFile != null && rawFile.exists()) {
                FileInputStream(rawFile).use { input ->
                    input.copyTo(out)
                }
            } else {
                out.write(item.literalContent.toByteArray(Charsets.UTF_8))
            }
            out.flush()
        } ?: throw IllegalStateException("Unable to write to destination")
    }

    suspend fun deleteVaultItemFromDisk(context: Context, item: VaultItemEntity) = withContext(Dispatchers.IO) {
        runCatching {
            item.localFilePath.takeIf { it.isNotBlank() }?.let(::File)?.let { file ->
                if (file.exists()) file.delete()
            }
        }
        runCatching {
            val rawDir = subDir(context, VAULT_RAW_DIR)
            rawDir.listFiles { f -> f.isFile && f.name.startsWith("${item.id}_") }?.forEach { it.delete() }
        }
        runCatching {
            val metaFile = File(subDir(context, VAULT_ITEMS_DIR), "${item.id}.json")
            if (metaFile.exists()) metaFile.delete()
        }
        runCatching {
            val noteMetaFile = File(subDir(context, VAULT_NOTE_META_DIR), "${item.id}.json")
            if (noteMetaFile.exists()) noteMetaFile.delete()
        }
    }

    suspend fun buildAndPersistFullBackupJson(
        context: Context,
        settings: FluffySettingsSnapshot,
        notes: List<NoteEntity>,
        vaultItems: List<VaultItemEntity>
    ): String = withContext(Dispatchers.IO) {
        saveSettingsSnapshot(context, settings)
        saveNotesLiteralSnapshot(context, notes)

        val notesArr = JSONArray()
        notes.forEach { notesArr.put(noteToJson(it)) }

        val vaultArr = JSONArray()
        vaultItems.forEach { item ->
            persistVaultEntityWriteOnce(context, item)
            vaultArr.put(vaultItemToJson(item, includeRawFile = true))
        }

        val root = JSONObject().apply {
            put("schemaVersion", 2)
            put("exportedAt", System.currentTimeMillis())
            put("appVersion", BuildConfig.VERSION_NAME)
            put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("settings", settingsToJson(settings))
            put("notes", notesArr)
            put("vaultItems", vaultArr)
        }

        val jsonText = root.toString(2)
        val localBackupFile = File(subDir(context, BACKUPS_DIR), FULL_BACKUP_FILE)
        atomicWriteUtf8(localBackupFile, jsonText)
        jsonText
    }

    suspend fun exportJsonToUri(context: Context, jsonContent: String, destUri: Uri) =
        withContext(Dispatchers.IO) {
            context.applicationContext.contentResolver.openOutputStream(destUri)?.use { out ->
                out.write(jsonContent.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: throw IllegalStateException("Cannot write backup file")
        }

    suspend fun readUtf8FromUri(context: Context, srcUri: Uri): String =
        withContext(Dispatchers.IO) {
            context.applicationContext.contentResolver.openInputStream(srcUri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: throw IllegalStateException("Cannot read selected backup file")
        }

    fun parseFullBackupBundle(jsonString: String): FluffyBackupBundle {
        val root = JSONObject(jsonString)
        val settingsObj = root.optJSONObject("settings")
        val settings = settingsObj?.let(::jsonToSettings)

        val notesList = buildList {
            val arr = root.optJSONArray("notes")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    add(jsonToNote(arr.getJSONObject(i)))
                }
            }
        }

        val vaultList = buildList {
            val arr = root.optJSONArray("vaultItems")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    add(jsonToVaultItem(arr.getJSONObject(i)))
                }
            }
        }

        return FluffyBackupBundle(
            schemaVersion = root.optInt("schemaVersion", 1),
            exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
            appVersion = root.optString("appVersion", ""),
            settings = settings,
            notes = notesList,
            vaultItems = vaultList
        )
    }

    fun getStorageFolderDisplayPath(context: Context): String =
        rootDir(context).absolutePath

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.applicationContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        }.getOrNull()
    }

    private fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun computeFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(8192)
            var read: Int
            while (input.read(buf).also { read = it } != -1) {
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun settingsToJson(s: FluffySettingsSnapshot): JSONObject = JSONObject().apply {
        put("isDarkMode", s.isDarkMode)
        put("isArabic", s.isArabic)
        put("isGridView", s.isGridView)
        put("isBiometricEnabled", s.isBiometricEnabled)
        put("selectedCategory", s.selectedCategory)
        put("cloudSyncEnabled", s.cloudSyncEnabled)
        put("selectedThemeId", s.selectedThemeId)
        put("appLanguageCode", s.appLanguageCode)
        put("nonScreenshotEnabled", s.nonScreenshotEnabled)
        put("coloredContainerBorders", s.coloredContainerBorders)
        put("notesViewMode", s.notesViewMode)
        put("isPremium", s.isPremium)
        put("fabMenuStyle", s.fabMenuStyle)
        put("selectedFontId", s.selectedFontId)
    }

    private fun jsonToSettings(obj: JSONObject): FluffySettingsSnapshot {
        val dark = obj.optBoolean("isDarkMode", true)
        val arabic = obj.optBoolean("isArabic", true)
        val gridFlag = obj.optBoolean("isGridView", true)
        return FluffySettingsSnapshot(
            isDarkMode = dark,
            isArabic = arabic,
            isGridView = gridFlag,
            isBiometricEnabled = obj.optBoolean("isBiometricEnabled", false),
            selectedCategory = obj.optString("selectedCategory", "ALL"),
            cloudSyncEnabled = obj.optBoolean("cloudSyncEnabled", true),
            selectedThemeId = obj.optString(
                "selectedThemeId",
                if (dark) "ayu_dark" else "frosted_arctic_glass"
            ),
            appLanguageCode = obj.optString(
                "appLanguageCode",
                if (arabic) "ar" else "en"
            ),
            nonScreenshotEnabled = obj.optBoolean("nonScreenshotEnabled", false),
            coloredContainerBorders = obj.optBoolean("coloredContainerBorders", true),
            notesViewMode = obj.optString(
                "notesViewMode",
                if (gridFlag) "grid" else "list"
            ),
            isPremium = obj.optBoolean("isPremium", false),
            fabMenuStyle = obj.optString("fabMenuStyle", "vertical_stack"),
            selectedFontId = obj.optString("selectedFontId", "sans_serif")
        )
    }

    private fun noteToJson(n: NoteEntity): JSONObject = JSONObject().apply {
        put("id", n.id)
        put("syncId", n.syncId)
        put("sortOrder", n.sortOrder)
        put("title", n.title)
        put("content", n.content)
        put("pagesJson", n.pagesJson)
        put("category", n.category)
        put("colorPreset", n.colorPreset)
        put("backgroundId", n.backgroundId)
        put("customColorArgb", n.customColorArgb)
        put("isPinned", n.isPinned)
        put("isFavorite", n.isFavorite)
        put("isArchived", n.isArchived)
        put("isDeleted", n.isDeleted)
        put("deletedAt", n.deletedAt)
        put("checklistJson", n.checklistJson)
        put("audioDurationSec", n.audioDurationSec)
        put("audioFilePath", n.audioFilePath)
        put("audioByteSize", n.audioByteSize)
        put("createdAt", n.createdAt)
        put("updatedAt", n.updatedAt)
    }

    private fun jsonToNote(obj: JSONObject): NoteEntity = NoteEntity(
        id = obj.optLong("id", 0L),
        title = obj.optString("title", ""),
        content = obj.optString("content", ""),
        pagesJson = obj.optString("pagesJson", ""),
        category = obj.optString("category", "Ideas"),
        colorPreset = obj.optString("colorPreset", "frost"),
        backgroundId = obj.optString("backgroundId", "none"),
        customColorArgb = obj.optLong("customColorArgb", 0L),
        isPinned = obj.optBoolean("isPinned", false),
        isFavorite = obj.optBoolean("isFavorite", false),
        isArchived = obj.optBoolean("isArchived", false),
        checklistJson = obj.optString("checklistJson", ""),
        audioDurationSec = obj.optInt("audioDurationSec", 0),
        audioFilePath = obj.optString("audioFilePath", ""),
        audioByteSize = obj.optLong("audioByteSize", 0L),
        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
        syncId = obj.optString("syncId", "").ifBlank { UUID.randomUUID().toString() },
        sortOrder = obj.optLong("sortOrder", 0L),
        isDeleted = obj.optBoolean("isDeleted", false),
        deletedAt = obj.optLong("deletedAt", 0L)
    )

    private fun vaultItemToJson(v: VaultItemEntity, includeRawFile: Boolean): JSONObject = JSONObject().apply {
        put("id", v.id)
        put("title", v.title)
        put("literalContent", v.literalContent)
        put("fileName", v.fileName)
        put("mimeType", v.mimeType)
        put("localFilePath", v.localFilePath)
        put("byteSize", v.byteSize)
        put("sha256", v.sha256)
        put("createdAt", v.createdAt)
        put("isImmutable", v.isImmutable)
        if (includeRawFile && v.fileName.isNotEmpty() && v.localFilePath.isNotBlank()) {
            val file = File(v.localFilePath)
            if (file.exists() && file.length() <= MAX_INLINE_BACKUP_FILE_BYTES) {
                put("rawFileBase64", Base64.encodeToString(file.readBytes(), Base64.NO_WRAP))
            }
        }
    }

    private fun jsonToVaultItem(obj: JSONObject): RestorableVaultItem {
        val entity = VaultItemEntity(
            id = obj.optString("id", UUID.randomUUID().toString()),
            title = obj.optString("title", ""),
            literalContent = obj.optString("literalContent", ""),
            fileName = obj.optString("fileName", ""),
            mimeType = obj.optString("mimeType", ""),
            localFilePath = obj.optString("localFilePath", ""),
            byteSize = obj.optLong("byteSize", 0L),
            sha256 = obj.optString("sha256", ""),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            isImmutable = obj.optBoolean("isImmutable", true)
        )
        val b64 = obj.optString("rawFileBase64", "").ifBlank { null }
        return RestorableVaultItem(entity = entity, rawFileBase64 = b64)
    }
}
