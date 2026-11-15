package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val category: String = "Ideas",
    val colorPreset: String = "frost",
    val backgroundId: String = "none",
    val customColorArgb: Long = 0L,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val checklistJson: String = "",
    val audioDurationSec: Int = 0,
    val audioFilePath: String = "",
    val audioByteSize: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncId: String = UUID.randomUUID().toString(),
    val sortOrder: Long = 0L,
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0L
)
