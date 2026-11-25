package com.example.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.local.FluffyDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object NoteReminderScheduler {

    const val CHANNEL_ID = "fluffy_note_reminders"
    const val EXTRA_NOTE_ID = "extra_reminder_note_id"
    const val EXTRA_NOTE_SYNC_ID = "extra_reminder_note_sync_id"
    const val ACTION_REMINDER_TRIGGERED = "com.aistudio.fluffy.ACTION_NOTE_REMINDER"
    const val ACTION_DISMISS_REMINDER = "com.aistudio.fluffy.ACTION_DISMISS_NOTE_REMINDER"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Draft Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scheduled reminders for your Fluffy drafts and notes"
            enableVibration(true)
            enableLights(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun requestCodeForNote(noteId: Long): Int {
        return ((noteId xor (noteId ushr 32)).toInt() and 0x3FFFFFFF) + 1000
    }

    fun notificationIdForNote(noteId: Long): Int {
        return ((noteId xor (noteId ushr 32)).toInt() and 0x3FFFFFFF) + 50000
    }

    private fun buildAlarmPendingIntent(
        context: Context,
        noteId: Long,
        syncId: String = "",
        flags: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    ): PendingIntent? {
        val intent = Intent(context.applicationContext, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_TRIGGERED
            putExtra(EXTRA_NOTE_ID, noteId)
            putExtra(EXTRA_NOTE_SYNC_ID, syncId)
        }
        return PendingIntent.getBroadcast(
            context.applicationContext,
            requestCodeForNote(noteId),
            intent,
            flags
        )
    }

    fun scheduleReminder(
        context: Context,
        noteId: Long,
        triggerAtMillis: Long,
        syncId: String = ""
    ) {
        if (noteId <= 0L || triggerAtMillis <= 0L) return
        ensureNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(context, noteId, syncId) ?: return
        val effectiveTrigger = maxOf(triggerAtMillis, System.currentTimeMillis() + 1000L)

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(false)
        } else {
            true
        }

        if (canExact) {
            runCatching {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    effectiveTrigger,
                    pendingIntent
                )
            }.onFailure {
                runCatching {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        effectiveTrigger,
                        pendingIntent
                    )
                }
            }
        } else {
            runCatching {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    effectiveTrigger,
                    pendingIntent
                )
            }
        }
    }

    fun cancelReminder(context: Context, noteId: Long) {
        if (noteId <= 0L) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val pendingIntent = buildAlarmPendingIntent(
            context = context,
            noteId = noteId,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (alarmManager != null && pendingIntent != null) {
            runCatching { alarmManager.cancel(pendingIntent) }
            runCatching { pendingIntent.cancel() }
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        runCatching { notificationManager?.cancel(notificationIdForNote(noteId)) }
    }

    suspend fun rescheduleAllActiveReminders(context: Context) {
        val now = System.currentTimeMillis()
        val notes = runCatching {
            FluffyDatabase.getDatabase(context.applicationContext)
                .noteDao()
                .getActiveReminderNotesOnce(minTimestamp = now)
        }.getOrDefault(emptyList())

        notes.forEach { note ->
            if (note.id > 0L && note.reminderAt > now) {
                scheduleReminder(
                    context = context,
                    noteId = note.id,
                    triggerAtMillis = note.reminderAt,
                    syncId = note.syncId
                )
            }
        }
    }

    fun formatReminderBadgeText(
        reminderAt: Long,
        isArabic: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        if (reminderAt <= 0L) return ""
        val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
        val targetCal = Calendar.getInstance().apply { timeInMillis = reminderAt }
        val todayCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val tomorrowCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }

        val timeText = SimpleDateFormat("HH:mm", Locale.US).format(Date(reminderAt))

        val sameYearDay = targetCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
        val isTomorrow = targetCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)

        return when {
            sameYearDay -> if (isArabic) "اليوم $timeText" else "Today $timeText"
            isTomorrow -> if (isArabic) "غداً $timeText" else "Tomorrow $timeText"
            else -> {
                val datePart = SimpleDateFormat("dd MMM", locale).format(Date(reminderAt))
                "$datePart • $timeText"
            }
        }
    }

    fun formatReminderFullText(
        reminderAt: Long,
        isArabic: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        if (reminderAt <= 0L) return ""
        val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
        val badge = formatReminderBadgeText(reminderAt, isArabic, nowMillis)
        val dayName = SimpleDateFormat("EEE", locale).format(Date(reminderAt))
        return "$dayName • $badge"
    }
}
