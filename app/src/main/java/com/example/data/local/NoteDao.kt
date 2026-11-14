package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun getAllNotesOnce(): List<NoteEntity>

    @Query("SELECT MIN(sortOrder) FROM notes")
    suspend fun getMinSortOrder(): Long?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE syncId = :syncId LIMIT 1")
    suspend fun getNoteBySyncId(syncId: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteIfAbsent(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Update
    suspend fun updateNotes(notes: List<NoteEntity>)

    @Transaction
    suspend fun updateSortOrders(reorderedNotes: List<NoteEntity>) {
        updateNotes(reorderedNotes)
    }

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notes WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("DELETE FROM notes WHERE title IN ('مرحباً بك في Fluffy ✨', 'قائمة مهام الأسبوع 🎯', 'خاطرة إبداعية: انعكاس الضوء 🌈', 'Liquid Glass Architecture 💎')")
    suspend fun clearSampleNotes()

    @Query("""
        SELECT * FROM notes 
        WHERE isArchived = 0 
          AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        ORDER BY sortOrder ASC, createdAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isFavorite = 1 ORDER BY sortOrder ASC, createdAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isPinned = 1 ORDER BY sortOrder ASC, createdAt DESC")
    fun getPinnedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND category = :category ORDER BY sortOrder ASC, createdAt DESC")
    fun getNotesByCategory(category: String): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM notes WHERE isArchived = 0")
    fun getNotesCount(): Flow<Int>
}
