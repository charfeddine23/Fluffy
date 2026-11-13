package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.NoteEntity
import com.example.data.model.VaultItemEntity

@Database(
    entities = [NoteEntity::class, VaultItemEntity::class],
    version = 2,
    exportSchema = false
)
abstract class FluffyDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: FluffyDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vault_items` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `literalContent` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `localFilePath` TEXT NOT NULL,
                        `byteSize` INTEGER NOT NULL,
                        `sha256` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `isImmutable` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): FluffyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FluffyDatabase::class.java,
                    "fluffy_notes.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
