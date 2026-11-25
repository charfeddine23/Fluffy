package com.example

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FluffyDatabase
import com.example.data.model.ChecklistItem
import com.example.data.model.NoteEntity
import com.example.data.model.NotePagesCodec
import com.example.data.storage.LocalDeviceStorageManager
import com.example.reminders.NoteReminderScheduler
import com.example.ui.components.FluffyTopBar
import com.example.ui.components.NoteCard
import com.example.ui.screens.NoteEditorScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoteReminderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var db: FluffyDatabase? = null

    @After
    fun tearDown() {
        db?.close()
    }

    @Test
    fun `noteDao queries active non-deleted reminders in chronological order`() = runBlocking(Dispatchers.IO) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, FluffyDatabase::class.java).build()
        db = database
        val dao = database.noteDao()

        val now = 1_750_000_000_000L
        val later = now + 7_200_000L
        val sooner = now + 1_800_000L

        val idLater = dao.insertNote(NoteEntity(title = "Later", content = "A", reminderAt = later))
        val idSooner = dao.insertNote(NoteEntity(title = "Sooner", content = "B", reminderAt = sooner))
        dao.insertNote(NoteEntity(title = "Past", content = "C", reminderAt = now - 60_000L))
        dao.insertNote(NoteEntity(title = "Zero", content = "D", reminderAt = 0L))
        dao.insertNote(NoteEntity(title = "Deleted", content = "E", reminderAt = later, isDeleted = true))

        val active = dao.getActiveReminderNotesOnce(minTimestamp = now)
        assertEquals(listOf(idSooner, idLater), active.map { it.id })

        dao.updateNoteReminder(noteId = idSooner, reminderAt = 0L, updatedAt = now + 500L)
        val afterClear = dao.getActiveReminderNotesOnce(minTimestamp = now)
        assertEquals(listOf(idLater), afterClear.map { it.id })
    }

    @Test
    fun `notes literal snapshot preserves reminderAt across concurrent writes`() = runBlocking(Dispatchers.IO) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val targetReminder = 1_760_123_456_000L
        val sampleNotes = listOf(
            NoteEntity(
                id = 11L,
                title = "مسودة مع تذكير",
                content = "تفاصيل المهمة",
                reminderAt = targetReminder,
                syncId = "sync-rem-11"
            ),
            NoteEntity(
                id = 12L,
                title = "بدون تذكير",
                content = "نص عادي",
                reminderAt = 0L,
                syncId = "sync-rem-12"
            )
        )

        val jobs = (1..6).map {
            async(Dispatchers.IO) {
                LocalDeviceStorageManager.saveNotesLiteralSnapshot(context, sampleNotes)
            }
        }
        jobs.awaitAll()

        val restored = LocalDeviceStorageManager.loadNotesLiteralSnapshot(context)
        assertEquals(2, restored.size)
        val first = restored.first { it.id == 11L }
        assertEquals(targetReminder, first.reminderAt)
        assertEquals("مسودة مع تذكير", first.title)
        assertEquals(0L, restored.first { it.id == 12L }.reminderAt)
    }

    @Test
    fun `estimateNoteBytes matches UTF-8 byte count without losing surrogate pairs`() {
        val title = "تذكير 🔔"
        val pages = listOf("مرحباً بالعالم 🌍\nسطر ثاني")
        val checklist = listOf(ChecklistItem(id = "c1", text = "مراجعة ✅", isDone = false))

        assertEquals(title.toByteArray(Charsets.UTF_8).size.toLong(), NotePagesCodec.utf8ByteLength(title))
        assertEquals(pages[0].toByteArray(Charsets.UTF_8).size.toLong(), NotePagesCodec.utf8ByteLength(pages[0]))

        val estimated = NotePagesCodec.estimateNoteBytes(title, pages, checklist)
        val expected = title.toByteArray(Charsets.UTF_8).size.toLong() +
            pages[0].toByteArray(Charsets.UTF_8).size.toLong() +
            (checklist[0].text.toByteArray(Charsets.UTF_8).size.toLong() + 28L)
        assertEquals(expected, estimated)

        val checklistJson = ChecklistItem.listToJson(checklist)
        val note = NoteEntity(
            title = title,
            content = pages[0],
            pagesJson = "",
            checklistJson = checklistJson
        )
        val expectedEntityBytes = title.toByteArray(Charsets.UTF_8).size.toLong() +
            pages[0].toByteArray(Charsets.UTF_8).size.toLong() +
            checklistJson.toByteArray(Charsets.UTF_8).size.toLong()
        assertEquals(expectedEntityBytes, NotePagesCodec.estimateNoteBytes(note))
    }

    @Test
    fun `scheduler sets and cancels alarm and formats reminder badge labels`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val futureMillis = System.currentTimeMillis() + 3_600_000L

        NoteReminderScheduler.scheduleReminder(
            context = context,
            noteId = 42L,
            triggerAtMillis = futureMillis,
            syncId = "sync-42"
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        assertNotNull(shadowAlarm.nextScheduledAlarm)

        NoteReminderScheduler.cancelReminder(context, noteId = 42L)
        assertNull(shadowAlarm.nextScheduledAlarm)

        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sameDayReminder = (nowCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 30)
        }.timeInMillis
        val tomorrowReminder = (nowCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 15)
        }.timeInMillis

        assertEquals("Today 15:30", NoteReminderScheduler.formatReminderBadgeText(sameDayReminder, isArabic = false, nowMillis = nowCal.timeInMillis))
        assertEquals("اليوم 15:30", NoteReminderScheduler.formatReminderBadgeText(sameDayReminder, isArabic = true, nowMillis = nowCal.timeInMillis))
        assertEquals("Tomorrow 09:15", NoteReminderScheduler.formatReminderBadgeText(tomorrowReminder, isArabic = false, nowMillis = nowCal.timeInMillis))
    }

    @Test
    fun `noteCard displays reminder chip when reminderAt is set`() {
        val futureReminder = System.currentTimeMillis() + 5_400_000L
        val note = NoteEntity(
            id = 77L,
            title = "Draft with Reminder",
            content = "Body text",
            reminderAt = futureReminder
        )

        composeRule.setContent {
            NoteCard(
                note = note,
                isDark = true,
                isArabic = false,
                onClick = {},
                onTogglePin = {},
                onToggleFavorite = {},
                onToggleChecklistItem = {},
                onDelete = {}
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("note_card_reminder_77", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `contextual top bar exposes reminder action pill when note is selected`() {
        var reminderClickedForId: Long? = null
        val selected = NoteEntity(
            id = 55L,
            title = "Selected Draft",
            content = "Hello"
        )

        composeRule.setContent {
            FluffyTopBar(
                isDark = false,
                isArabic = false,
                isGridView = true,
                searchQuery = "",
                onSearchChange = {},
                onOpenProfile = {},
                onToggleViewMode = {},
                onOpenStats = {},
                onOpenSettings = {},
                selectedNote = selected,
                onReminderSelected = { reminderClickedForId = it.id }
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("note_popup_reminder").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(55L, reminderClickedForId)
    }

    @Test
    fun `editor reminder dialog sets quick +1h reminder and propagates reminderAt on save`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        var savedReminderAt = 0L
        var autoSavedReminderAt = 0L
        val draft = NoteEntity(
            id = 0L,
            title = "New Draft",
            content = "Draft body"
        )

        composeRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                NoteEditorScreen(
                    note = draft,
                    isCreatingNew = true,
                    isDark = false,
                    isArabic = false,
                    onBack = {},
                    onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _, reminderAt ->
                        savedReminderAt = reminderAt
                    },
                    onDelete = {},
                    onAutoSave = { _, _, _, _, _, _, _, _, _, _, _, _, _, reminderAt ->
                        autoSavedReminderAt = reminderAt
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("editor_reminder_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("note_reminder_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("reminder_quick_1h").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("reminder_confirm_button").performClick()
        composeRule.waitForIdle()

        assertTrue("Expected positive autoSavedReminderAt", autoSavedReminderAt > System.currentTimeMillis())
        composeRule.onNodeWithTag("editor_active_reminder_pill").assertIsDisplayed()

        composeRule.onNodeWithTag("editor_back_button").performClick()
        composeRule.waitForIdle()

        assertEquals(autoSavedReminderAt, savedReminderAt)
    }
}
