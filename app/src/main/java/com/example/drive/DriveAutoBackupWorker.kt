
package com.example.drive

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.FluffyDatabase
import com.example.data.storage.FluffySettingsSnapshot
import com.example.data.storage.LocalDeviceStorageManager
import java.util.concurrent.TimeUnit

class DriveAutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_AUTO_BACKUP_24H, false)) {
            return Result.success()
        }

        val db = FluffyDatabase.getDatabase(applicationContext)
        val notes = db.noteDao().getAllNotesOnce()
        // استبدل أو أكمل جلب البيانات حسب ما يناسب مشروعك
        
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "fluffy_google_drive_24h_backup"
        private const val PREFS_NAME = "fluffy_prefs"
        const val KEY_AUTO_BACKUP_24H = "auto_drive_backup_24h"
        const val KEY_LAST_DRIVE_BACKUP_AT = "last_drive_backup_at"
        const val KEY_LAST_DRIVE_BACKUP_STATUS = "last_drive_backup_status"

        fun scheduleEvery24Hours(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<DriveAutoBackupWorker>(
                24,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
        }

        fun cancelScheduledBackup(context: Context) {
            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
