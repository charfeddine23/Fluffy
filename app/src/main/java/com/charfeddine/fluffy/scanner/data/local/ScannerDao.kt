package com.charfeddine.fluffy.scanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.charfeddine.fluffy.scanner.data.model.SavedSignatureEntity
import com.charfeddine.fluffy.scanner.data.model.ScannedDocumentEntity
import com.charfeddine.fluffy.scanner.data.model.ScannedPageEntity
import com.charfeddine.fluffy.scanner.data.model.ScannerFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannerDao {
    // Folders
    @Query("SELECT * FROM scanner_folders ORDER BY name COLLATE NOCASE ASC")
    fun observeAllFolders(): Flow<List<ScannerFolderEntity>>

    @Query("SELECT * FROM scanner_folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: String): ScannerFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: ScannerFolderEntity)

    @Query("DELETE FROM scanner_folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: String)

    // Documents
    @Query("SELECT * FROM scanned_documents WHERE isTrashed = 0 ORDER BY updatedAt DESC")
    fun observeActiveDocuments(): Flow<List<ScannedDocumentEntity>>

    @Query("SELECT * FROM scanned_documents WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun observeTrashedDocuments(): Flow<List<ScannedDocumentEntity>>

    @Query("SELECT * FROM scanned_documents WHERE id = :documentId LIMIT 1")
    fun observeDocumentById(documentId: String): Flow<ScannedDocumentEntity?>

    @Query("SELECT * FROM scanned_documents WHERE id = :documentId LIMIT 1")
    suspend fun getDocumentById(documentId: String): ScannedDocumentEntity?

    @Query("SELECT * FROM scanned_documents WHERE folderId = :folderId AND isTrashed = 0")
    suspend fun getDocumentsInFolder(folderId: String): List<ScannedDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: ScannedDocumentEntity)

    @Update
    suspend fun updateDocument(document: ScannedDocumentEntity)

    @Query("UPDATE scanned_documents SET folderId = :targetFolderId, updatedAt = :now WHERE id = :documentId")
    suspend fun moveDocumentToFolder(documentId: String, targetFolderId: String?, now: Long = System.currentTimeMillis())

    @Query("UPDATE scanned_documents SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolderFromDocuments(folderId: String)

    @Query("UPDATE scanned_documents SET isTrashed = 1, trashedAt = :trashedAt WHERE id = :documentId")
    suspend fun moveToTrash(documentId: String, trashedAt: Long = System.currentTimeMillis())

    @Query("UPDATE scanned_documents SET isTrashed = 0, trashedAt = 0, updatedAt = :now WHERE id = :documentId")
    suspend fun restoreFromTrash(documentId: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM scanned_documents WHERE id = :documentId")
    suspend fun deleteDocumentPermanently(documentId: String)

    @Query("SELECT * FROM scanned_documents WHERE isTrashed = 1 AND trashedAt > 0 AND trashedAt < :cutoffMs")
    suspend fun getExpiredTrashedDocuments(cutoffMs: Long): List<ScannedDocumentEntity>

    // Pages
    @Query("SELECT * FROM scanned_pages WHERE documentId = :documentId ORDER BY pageOrder ASC")
    fun observePagesForDocument(documentId: String): Flow<List<ScannedPageEntity>>

    @Query("SELECT * FROM scanned_pages WHERE documentId = :documentId ORDER BY pageOrder ASC")
    suspend fun getPagesForDocumentOnce(documentId: String): List<ScannedPageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPage(page: ScannedPageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPages(pages: List<ScannedPageEntity>)

    @Query("DELETE FROM scanned_pages WHERE id = :pageId")
    suspend fun deletePageById(pageId: String)

    @Query("DELETE FROM scanned_pages WHERE documentId = :documentId")
    suspend fun deleteAllPagesForDocument(documentId: String)

    // Saved E-Signatures
    @Query("SELECT * FROM saved_signatures ORDER BY createdAt DESC")
    fun observeSavedSignatures(): Flow<List<SavedSignatureEntity>>

    @Query("SELECT * FROM saved_signatures WHERE id = :signatureId LIMIT 1")
    suspend fun getSignatureById(signatureId: String): SavedSignatureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSignature(signature: SavedSignatureEntity)

    @Query("DELETE FROM saved_signatures WHERE id = :signatureId")
    suspend fun deleteSignatureById(signatureId: String)
}
