package com.charfeddine.fluffy.scanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = false
)
abstract class ScannerDatabase : RoomDatabase() {
    abstract fun scannerDao(): ScannerDao

    companion object {
        @Volatile
        private var INSTANCE: ScannerDatabase? = null

        fun getDatabase(context: Context): ScannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScannerDatabase::class.java,
                    "fluffy_scanner.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
