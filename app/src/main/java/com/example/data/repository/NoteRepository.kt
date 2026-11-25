package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.model.NoteEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val trashedNotes: Flow<List<NoteEntity>> = noteDao.getTrashedNotes()

    fun getNote(id: Long): Flow<NoteEntity?> = noteDao.getNoteById(id)

    suspend fun getNoteByIdOnce(id: Long): NoteEntity? = noteDao.getNoteByIdOnce(id)

    suspend fun getActiveReminderNotesOnce(minTimestamp: Long = 0L): List<NoteEntity> =
        noteDao.getActiveReminderNotesOnce(minTimestamp)

    suspend fun updateNoteReminder(noteId: Long, reminderAt: Long, updatedAt: Long = System.currentTimeMillis()) =
        noteDao.updateNoteReminder(noteId, reminderAt, updatedAt)

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    suspend fun getAllNotesOnce(): List<NoteEntity> = noteDao.getAllNotesOnce()

    suspend fun getTrashedNotesOnce(): List<NoteEntity> = noteDao.getTrashedNotesOnce()

    suspend fun getExpiredTrashNotes(cutoffTimestamp: Long): List<NoteEntity> =
        noteDao.getExpiredTrashNotes(cutoffTimestamp)

    suspend fun deleteExpiredTrash(cutoffTimestamp: Long) =
        noteDao.deleteExpiredTrash(cutoffTimestamp)

    suspend fun emptyTrash() = noteDao.emptyTrash()

    suspend fun getMinSortOrder(): Long = noteDao.getMinSortOrder() ?: 0L

    suspend fun updateSortOrders(notes: List<NoteEntity>) = noteDao.updateSortOrders(notes)

    suspend fun getNoteBySyncId(syncId: String): NoteEntity? = noteDao.getNoteBySyncId(syncId)

    suspend fun insertOrUpdate(note: NoteEntity): Long {
        val normalized = if (note.syncId.isBlank()) {
            note.copy(syncId = UUID.randomUUID().toString())
        } else {
            note
        }
        return if (normalized.id == 0L) {
            noteDao.insertNote(normalized)
        } else {
            noteDao.updateNote(normalized)
            normalized.id
        }
    }

    suspend fun deleteNote(note: NoteEntity) = noteDao.deleteNote(note)

    suspend fun deleteById(id: Long) = noteDao.deleteById(id)

    suspend fun deleteBySyncId(syncId: String) = noteDao.deleteBySyncId(syncId)

    suspend fun togglePin(note: NoteEntity) {
        noteDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleFavorite(note: NoteEntity) {
        noteDao.updateNote(note.copy(isFavorite = !note.isFavorite, updatedAt = System.currentTimeMillis()))
    }
}
