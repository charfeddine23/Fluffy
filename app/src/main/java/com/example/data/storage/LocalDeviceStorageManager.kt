package com.example.data.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.NoteEntity
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
    val autoDriveBackupEnabled: Boolean
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
 * - Builds and parses portable full JSON backups (Settings + Notes + Vault) for Google Drive and phone storage.
 */
object LocalDeviceStorageManager {

    private const val ROOT_DIR = "fluffy_persistent_store"
    private const val NOTES_DIR = "notes"
    private const val SETTINGS_DIR = "settings"
    private const val VAULT_ITEMS_DIR = "vault/items"
    private const val VAULT_RAW_DIR = "vault/raw"
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
        base64Data: String?
    ): VaultItemEntity {
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
        put("autoDriveBackupEnabled", s.autoDriveBackupEnabled)
    }

    private fun jsonToSettings(obj: JSONObject): FluffySettingsSnapshot = FluffySettingsSnapshot(
        isDarkMode = obj.optBoolean("isDarkMode", true),
        isArabic = obj.optBoolean("isArabic", true),
        isGridView = obj.optBoolean("isGridView", true),
        isBiometricEnabled = obj.optBoolean("isBiometricEnabled", false),
        selectedCategory = obj.optString("selectedCategory", "ALL"),
        autoDriveBackupEnabled = obj.optBoolean("autoDriveBackupEnabled", false)
    )

    private fun noteToJson(n: NoteEntity): JSONObject = JSONObject().apply {
        put("id", n.id)
        put("title", n.title)
        put("content", n.content)
        put("category", n.category)
        put("colorPreset", n.colorPreset)
        put("isPinned", n.isPinned)
        put("isFavorite", n.isFavorite)
        put("isArchived", n.isArchived)
        put("checklistJson", n.checklistJson)
        put("audioDurationSec", n.audioDurationSec)
        put("createdAt", n.createdAt)
        put("updatedAt", n.updatedAt)
    }

    private fun jsonToNote(obj: JSONObject): NoteEntity = NoteEntity(
        id = obj.optLong("id", 0L),
        title = obj.optString("title", ""),
        content = obj.optString("content", ""),
        category = obj.optString("category", "Ideas"),
        colorPreset = obj.optString("colorPreset", "frost"),
        isPinned = obj.optBoolean("isPinned", false),
        isFavorite = obj.optBoolean("isFavorite", false),
        isArchived = obj.optBoolean("isArchived", false),
        checklistJson = obj.optString("checklistJson", ""),
        audioDurationSec = obj.optInt("audioDurationSec", 0),
        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
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
