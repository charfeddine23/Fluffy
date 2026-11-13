package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Immutable Vault item persisted literally on device storage and Room.
 * Once saved, its written text and raw file bytes never mutate for any reason.
 */
@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val literalContent: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val localFilePath: String = "",
    val byteSize: Long = 0L,
    val sha256: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isImmutable: Boolean = true
)
