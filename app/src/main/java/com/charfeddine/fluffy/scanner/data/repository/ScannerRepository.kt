package com.charfeddine.fluffy.scanner.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.charfeddine.fluffy.scanner.data.local.ScannerDao
import com.charfeddine.fluffy.scanner.data.model.SavedSignatureEntity
import com.charfeddine.fluffy.scanner.data.model.ScannedDocumentEntity
import com.charfeddine.fluffy.scanner.data.model.ScannedPageEntity
import com.charfeddine.fluffy.scanner.data.model.ScannerFolderEntity
import com.charfeddine.fluffy.scanner.utils.CryptoVaultHelper
import com.charfeddine.fluffy.scanner.utils.OfflineOcrAndBarcodeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class ScannerRepository(
    private val context: Context,
    private val dao: ScannerDao
) {
    init {
        OfflineOcrAndBarcodeEngine.init(context)
    }

    val folders: Flow<List<ScannerFolderEntity>> = dao.observeAllFolders()
    val activeDocuments: Flow<List<ScannedDocumentEntity>> = dao.observeActiveDocuments()
    val trashedDocuments: Flow<List<ScannedDocumentEntity>> = dao.observeTrashedDocuments()
    val savedSignatures: Flow<List<SavedSignatureEntity>> = dao.observeSavedSignatures()

    val docsRootDir: File by lazy {
        File(context.filesDir, "scanner_docs").apply { mkdirs() }
    }

    val signaturesDir: File by lazy {
        File(docsRootDir, "signatures").apply { mkdirs() }
    }

    val exportsDir: File by lazy {
        File(context.filesDir, "scanner_exports").apply { mkdirs() }
    }

    val shareCacheDir: File by lazy {
        File(context.cacheDir, "scanner_share").apply { mkdirs() }
    }

    fun observeDocument(docId: String): Flow<ScannedDocumentEntity?> = dao.observeDocumentById(docId)

    fun observePages(docId: String): Flow<List<ScannedPageEntity>> = dao.observePagesForDocument(docId)

    suspend fun getPagesOnce(docId: String): List<ScannedPageEntity> = dao.getPagesForDocumentOnce(docId)

    suspend fun getDocumentOnce(docId: String): ScannedDocumentEntity? = dao.getDocumentById(docId)

    suspend fun upsertFolder(folder: ScannerFolderEntity) = dao.upsertFolder(folder)

    suspend fun deleteFolderAndUnassignDocs(folderId: String) {
        dao.clearFolderFromDocuments(folderId)
        dao.deleteFolderById(folderId)
    }

    suspend fun upsertDocument(doc: ScannedDocumentEntity) = dao.upsertDocument(doc)

    suspend fun updateDocument(doc: ScannedDocumentEntity) = dao.updateDocument(doc)

    suspend fun moveDocumentToFolder(docId: String, targetFolderId: String?) =
        dao.moveDocumentToFolder(docId, targetFolderId)

    suspend fun upsertPage(page: ScannedPageEntity) = dao.upsertPage(page)

    suspend fun upsertPages(pages: List<ScannedPageEntity>) = dao.upsertPages(pages)

    suspend fun deletePageAndRecalculate(docId: String, pageId: String) = withContext(Dispatchers.IO) {
        val currentPages = dao.getPagesForDocumentOnce(docId)
        val removed = currentPages.firstOrNull { it.id == pageId }
        dao.deletePageById(pageId)
        removed?.let {
            runCatching { File(it.originalImagePath).delete() }
            runCatching { File(it.processedImagePath).delete() }
        }
        val remaining = dao.getPagesForDocumentOnce(docId)
        val reindexed = remaining.mapIndexed { idx, p -> p.copy(pageOrder = idx) }
        if (reindexed.isNotEmpty()) {
            dao.upsertPages(reindexed)
        }
        syncDocumentStats(docId)
    }

    suspend fun syncDocumentStats(docId: String) = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentById(docId) ?: return@withContext
        val pages = dao.getPagesForDocumentOnce(docId)
        val totalBytes = pages.sumOf { File(it.processedImagePath).takeIf { f -> f.exists() }?.length() ?: 0L } +
            (File(doc.pdfPath).takeIf { doc.pdfPath.isNotBlank() && it.exists() }?.length() ?: 0L)
        val combinedOcr = pages.mapNotNull { it.ocrText.takeIf(String::isNotBlank) }.joinToString("\n\n")
        val coverPath = pages.firstOrNull()?.processedImagePath ?: doc.coverThumbnailPath
        dao.updateDocument(
            doc.copy(
                pageCount = pages.size,
                totalSizeBytes = totalBytes,
                coverThumbnailPath = coverPath,
                ocrFullText = combinedOcr,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun moveToTrash(docId: String) = dao.moveToTrash(docId)

    suspend fun restoreFromTrash(docId: String) = dao.restoreFromTrash(docId)

    suspend fun deleteDocumentPermanently(docId: String) = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentById(docId)
        val pages = dao.getPagesForDocumentOnce(docId)
        pages.forEach { page ->
            runCatching { File(page.originalImagePath).delete() }
            runCatching { File(page.processedImagePath).delete() }
        }
        doc?.pdfPath?.takeIf { it.isNotBlank() }?.let { runCatching { File(it).delete() } }
        dao.deleteAllPagesForDocument(docId)
        dao.deleteDocumentPermanently(docId)
    }

    suspend fun purgeExpiredTrash(retentionMillis: Long = THIRTY_DAYS_MS) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - retentionMillis
        val expired = dao.getExpiredTrashedDocuments(cutoff)
        expired.forEach { deleteDocumentPermanently(it.id) }
    }

    suspend fun toggleDocumentLock(docId: String, locked: Boolean) = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentById(docId) ?: return@withContext
        val pages = dao.getPagesForDocumentOnce(docId)
        pages.forEach { page ->
            val file = File(page.processedImagePath)
            val origFile = File(page.originalImagePath)
            if (locked) {
                encryptFileInPlaceIfPlain(file)
                if (origFile.absolutePath != file.absolutePath) {
                    encryptFileInPlaceIfPlain(origFile)
                }
            } else {
                decryptFileInPlaceIfEncrypted(file)
                if (origFile.absolutePath != file.absolutePath) {
                    decryptFileInPlaceIfEncrypted(origFile)
                }
            }
        }
        dao.updateDocument(doc.copy(isLocked = locked, updatedAt = System.currentTimeMillis()))
    }

    private fun encryptFileInPlaceIfPlain(file: File) {
        if (!file.exists()) return
        val bytes = file.readBytes()
        if (bytes.size >= MAGIC_ENC_HEADER.size &&
            bytes.copyOfRange(0, MAGIC_ENC_HEADER.size).contentEquals(MAGIC_ENC_HEADER)
        ) {
            return
        }
        val encrypted = CryptoVaultHelper.encryptBytes(bytes)
        file.writeBytes(MAGIC_ENC_HEADER + encrypted)
    }

    private fun decryptFileInPlaceIfEncrypted(file: File) {
        if (!file.exists()) return
        val bytes = file.readBytes()
        if (bytes.size > MAGIC_ENC_HEADER.size &&
            bytes.copyOfRange(0, MAGIC_ENC_HEADER.size).contentEquals(MAGIC_ENC_HEADER)
        ) {
            val payload = bytes.copyOfRange(MAGIC_ENC_HEADER.size, bytes.size)
            val plain = CryptoVaultHelper.decryptBytes(payload)
            if (plain.isNotEmpty()) file.writeBytes(plain)
        }
    }

    fun decodePageBitmap(path: String): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        val bytes = file.readBytes()
        val plain = if (bytes.size > MAGIC_ENC_HEADER.size &&
            bytes.copyOfRange(0, MAGIC_ENC_HEADER.size).contentEquals(MAGIC_ENC_HEADER)
        ) {
            CryptoVaultHelper.decryptBytes(bytes.copyOfRange(MAGIC_ENC_HEADER.size, bytes.size))
        } else {
            bytes
        }
        return BitmapFactory.decodeByteArray(plain, 0, plain.size)
    }

    suspend fun saveEncryptedSignature(name: String, bitmap: Bitmap, colorArgb: Long): SavedSignatureEntity =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val outFile = File(signaturesDir, "sig_$id.enc")
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            CryptoVaultHelper.writeEncryptedFile(outFile, baos.toByteArray())
            val entity = SavedSignatureEntity(
                id = id,
                name = name,
                encryptedFilePath = outFile.absolutePath,
                colorArgb = colorArgb
            )
            dao.upsertSignature(entity)
            entity
        }

    fun loadEncryptedSignatureBitmap(signature: SavedSignatureEntity): Bitmap? {
        val plain = CryptoVaultHelper.readEncryptedFile(File(signature.encryptedFilePath)) ?: return null
        return BitmapFactory.decodeByteArray(plain, 0, plain.size)
    }

    suspend fun deleteSignature(signature: SavedSignatureEntity) = withContext(Dispatchers.IO) {
        runCatching { File(signature.encryptedFilePath).delete() }
        dao.deleteSignatureById(signature.id)
    }

    companion object {
        private const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L
        private val MAGIC_ENC_HEADER = "FLFENC1".toByteArray(Charsets.US_ASCII)
    }
}
