package com.example.reminders

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.FluffyDatabase
import com.example.data.model.NotePagesCodec
import com.example.data.repository.FirebaseSyncRepository
import com.example.data.storage.LocalDeviceStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(NoteReminderScheduler.EXTRA_NOTE_ID, -1L)
        if (noteId <= 0L) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val action = intent.action

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (action == NoteReminderScheduler.ACTION_DISMISS_REMINDER) {
                    val dao = FluffyDatabase.getDatabase(appContext).noteDao()
                    val now = System.currentTimeMillis()
                    dao.updateNoteReminder(noteId, 0L, now)
                    NoteReminderScheduler.cancelReminder(appContext, noteId)
                    runCatching {
                        LocalDeviceStorageManager.saveNotesLiteralSnapshot(appContext, dao.getAllNotesOnce())
                    }
                    val updatedNote = dao.getNoteByIdOnce(noteId)
                    val uid = FirebaseSyncRepository.currentUser(appContext)?.uid
                    if (updatedNote != null && uid != null) {
                        runCatching { FirebaseSyncRepository.upsertNote(appContext, uid, updatedNote) }
                    }
                    return@launch
                }

                NoteReminderScheduler.ensureNotificationChannel(appContext)
                val dao = FluffyDatabase.getDatabase(appContext).noteDao()
                val note = dao.getNoteByIdOnce(noteId) ?: return@launch
                if (note.isDeleted || note.reminderAt <= 0L) return@launch

                val prefs = appContext.getSharedPreferences("fluffy_prefs", Context.MODE_PRIVATE)
                val isArabic = prefs.getBoolean("is_arabic", true)

                val title = note.title.ifBlank {
                    if (isArabic) "تذكير بمسودة Fluffy" else "Fluffy Draft Reminder"
                }
                val snippet = NotePagesCodec.plainTextSnippet(note).trim().ifBlank {
                    if (isArabic) "اضغط لفتح المسودة ومراجعتها" else "Tap to open and review your draft"
                }

                val openIntent = Intent(appContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(NoteReminderScheduler.EXTRA_NOTE_ID, note.id)
                    putExtra(NoteReminderScheduler.EXTRA_NOTE_SYNC_ID, note.syncId)
                }
                val openPendingIntent = PendingIntent.getActivity(
                    appContext,
                    NoteReminderScheduler.requestCodeForNote(note.id),
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val dismissIntent = Intent(appContext, ReminderAlarmReceiver::class.java).apply {
                    this.action = NoteReminderScheduler.ACTION_DISMISS_REMINDER
                    putExtra(NoteReminderScheduler.EXTRA_NOTE_ID, note.id)
                }
                val dismissPendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    NoteReminderScheduler.requestCodeForNote(note.id) + 20000,
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(appContext, NoteReminderScheduler.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_reminder_bell)
                    .setContentTitle(title)
                    .setContentText(snippet)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(snippet))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setContentIntent(openPendingIntent)
                    .addAction(
                        0,
                        if (isArabic) "إنهاء التذكير" else "Done",
                        dismissPendingIntent
                    )
                    .build()

                val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        appContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                if (canNotify) {
                    runCatching {
                        NotificationManagerCompat.from(appContext)
                            .notify(NoteReminderScheduler.notificationIdForNote(note.id), notification)
                    }.onFailure {
                        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                        nm?.notify(NoteReminderScheduler.notificationIdForNote(note.id), notification)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
