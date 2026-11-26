package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.VaultItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY createdAt DESC")
    fun getAllVaultItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items ORDER BY createdAt DESC")
    suspend fun getAllVaultItemsOnce(): List<VaultItemEntity>

    /**
     * IGNORE conflict strategy guarantees initial import/restore never overwrites existing entries.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImmutableItem(item: VaultItemEntity): Long

    /**
     * Explicit user-initiated update for notes and voice notes edited inside the Vault.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEditedVaultItem(item: VaultItemEntity): Long

    @Query("DELETE FROM vault_items WHERE id = :itemId")
    suspend fun deleteVaultItemById(itemId: String): Int
}
