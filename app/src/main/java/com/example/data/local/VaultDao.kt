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
     * IGNORE conflict strategy guarantees an already-saved immutable Vault entry
     * can never be overwritten or altered once stored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImmutableItem(item: VaultItemEntity): Long

    @Query("SELECT COUNT(*) FROM vault_items")
    suspend fun getVaultCount(): Int
}
