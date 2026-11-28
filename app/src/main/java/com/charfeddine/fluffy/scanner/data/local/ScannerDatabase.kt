package com.charfeddine.fluffy.scanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.charfeddine.fluffy.scanner.data.model.SavedSignatureEntity
import com.charfeddine.fluffy.scanner.data.model.ScannedDocumentEntity
import com.charfeddine.fluffy.scanner.data.model.ScannedPageEntity
import com.charfeddine.fluffy.scanner.data.model.ScannerFolderEntity

@Database(
    entities = [
        ScannerFolderEntity::class,
        ScannedDocumentEntity::class,
        ScannedPageEntity::class,
        SavedSignatureEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ScannerDatabase : RoomDatabase() {
    abstract fun scannerDao(): ScannerDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scanned_documents ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scanned_documents ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scanned_pages ADD COLUMN shadows REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE scanned_pages ADD COLUMN highlights REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE scanned_pages ADD COLUMN saturation REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE scanned_pages ADD COLUMN temperature REAL NOT NULL DEFAULT 0.0")
            }
        }

        @Volatile
        private var INSTANCE: ScannerDatabase? = null

        fun getDatabase(context: Context): ScannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScannerDatabase::class.java,
                    "fluffy_scanner.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
