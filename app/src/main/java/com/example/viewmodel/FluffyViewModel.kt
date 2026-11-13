package com.example.viewmodel

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FluffyDatabase
import com.example.data.model.ChecklistItem
import com.example.data.model.GlassTint
import com.example.data.model.NoteEntity
import com.example.data.model.VaultItemEntity
import com.example.data.repository.NoteRepository
import com.example.data.storage.FluffySettingsSnapshot
import com.example.data.storage.LocalDeviceStorageManager
import com.example.drive.DriveAuthOutcome
import com.example.drive.DriveAutoBackupWorker
import com.example.drive.DriveSyncStatus
import com.example.drive.GoogleDriveBackupManager
import com.example.update.AppUpdateManager
import com.example.update.ReleaseUpdateInfo
import com.example.update.UpdateUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PendingDriveAction {
    BACKUP,
    RESTORE,
    AUTHORIZE_ONLY
}

class FluffyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FluffyDatabase.getDatabase(application)
    private val repository = NoteRepository(db.noteDao())
    private val vaultDao = db.vaultDao()
    private val prefs = application.getSharedPreferences("fluffy_prefs", android.content.Context.MODE_PRIVATE)

    // App Preferences (Persisted across restarts & literal phone storage)
    val isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isArabic = MutableStateFlow(prefs.getBoolean("is_arabic", true))
    val isGridView = MutableStateFlow(prefs.getBoolean("is_grid_view", true))

    // Biometric Authentication & App Lock
    val isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))
    val isAppLocked = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))

    // Settings Drawer
    val isSettingsDrawerOpen = MutableStateFlow(false)

    // Filter and Search
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow(prefs.getString("selected_category", "ALL") ?: "ALL")

    // Active Editor, Vault & Sheets
    val editingNote = MutableStateFlow<NoteEntity?>(null)
    val isCreatingNew = MutableStateFlow(false)
    val isVaultOpen = MutableStateFlow(false)
    val showStatsSheet = MutableStateFlow(false)
    val showVoiceRecorder = MutableStateFlow(false)
    val snackbarMessage = MutableStateFlow<String?>(null)

    // In-App OTA Update State
    val updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val showUpdateDialog = MutableStateFlow(false)

    // Google Drive Backup & 24h Auto-Backup State
    val driveSyncStatus = MutableStateFlow<DriveSyncStatus>(DriveSyncStatus.Idle)
    val isAutoDriveBackupEnabled = MutableStateFlow(
        prefs.getBoolean(DriveAutoBackupWorker.KEY_AUTO_BACKUP_24H, false)
    )
    val lastDriveBackupAt = MutableStateFlow(
        prefs.getLong(DriveAutoBackupWorker.KEY_LAST_DRIVE_BACKUP_AT, 0L)
    )
    private var pendingDriveAction: PendingDriveAction? = null

    val vaultItems: StateFlow<List<VaultItemEntity>> = vaultDao.getAllVaultItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            // Reconcile and guarantee literal recovery from phone storage if DB was reset
            recoverLiteralStorageIfNeeded()
            // Keep literal snapshot synced automatically
            repository.allNotes.collect { notes ->
                withContext(Dispatchers.IO) {
                    LocalDeviceStorageManager.saveNotesLiteralSnapshot(getApplication(), notes)
                }
            }
        }
        persistSettingsSnapshot()
        if (isAutoDriveBackupEnabled.value) {
            DriveAutoBackupWorker.scheduleEvery24Hours(application)
        }
        checkForUpdates(isManual = false)
    }

    private suspend fun recoverLiteralStorageIfNeeded() = withContext(Dispatchers.IO) {
        val app = getApplication<Application>()
        val existingNotes = db.noteDao().getAllNotesOnce()
        if (existingNotes.isEmpty()) {
            val savedNotes = LocalDeviceStorageManager.loadNotesLiteralSnapshot(app)
            savedNotes.forEach { note ->
                db.noteDao().insertNoteIfAbsent(note)
            }
        }

        // Recover any immutable Vault entries persisted on phone storage
        val diskVaultItems = LocalDeviceStorageManager.loadAllLiteralVaultItemsFromDisk(app)
        diskVaultItems.forEach { item ->
            vaultDao.insertImmutableItem(item)
        }

        // Recover settings if SharedPreferences were cleared while local snapshot survived
        if (!prefs.contains("is_dark_mode")) {
            LocalDeviceStorageManager.loadSettingsSnapshot(app)?.let { restored ->
                applyRestoredSettings(restored)
            }
        }
    }

    // Filtered Notes Flow
    val filteredNotes: StateFlow<List<NoteEntity>> = combine(
        repository.allNotes,
        searchQuery,
        selectedCategory
    ) { notes, query, category ->
        var list = notes

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                    it.content.lowercase().contains(q) ||
                    it.category.lowercase().contains(q)
            }
        }

        when (category) {
            "ALL" -> list
            "PINNED" -> list.filter { it.isPinned }
            "FAVORITES" -> list.filter { it.isFavorite }
            "TODO" -> list.filter { it.checklistJson.isNotBlank() }
            else -> list.filter { it.category.equals(category, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allNotesCount: StateFlow<Int> = repository.allNotes.combine(searchQuery) { notes, _ ->
        notes.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun toggleDarkMode() {
        val next = !isDarkMode.value
        isDarkMode.value = next
        prefs.edit().putBoolean("is_dark_mode", next).apply()
        persistSettingsSnapshot()
    }

    fun toggleLanguage() {
        val next = !isArabic.value
        isArabic.value = next
        prefs.edit().putBoolean("is_arabic", next).apply()
        persistSettingsSnapshot()
    }

    fun toggleViewMode() {
        val next = !isGridView.value
        isGridView.value = next
        prefs.edit().putBoolean("is_grid_view", next).apply()
        persistSettingsSnapshot()
    }

    fun setSearch(query: String) {
        searchQuery.value = query
    }

    fun selectCategory(cat: String) {
        selectedCategory.value = cat
        prefs.edit().putString("selected_category", cat).apply()
        persistSettingsSnapshot()
    }

    private fun currentSettingsSnapshot(): FluffySettingsSnapshot = FluffySettingsSnapshot(
        isDarkMode = isDarkMode.value,
        isArabic = isArabic.value,
        isGridView = isGridView.value,
        isBiometricEnabled = isBiometricEnabled.value,
        selectedCategory = selectedCategory.value,
        autoDriveBackupEnabled = isAutoDriveBackupEnabled.value
    )

    private fun persistSettingsSnapshot() {
        val snapshot = currentSettingsSnapshot()
        viewModelScope.launch(Dispatchers.IO) {
            LocalDeviceStorageManager.saveSettingsSnapshot(getApplication(), snapshot)
        }
    }

    private fun applyRestoredSettings(s: FluffySettingsSnapshot) {
        isDarkMode.value = s.isDarkMode
        isArabic.value = s.isArabic
        isGridView.value = s.isGridView
        isBiometricEnabled.value = s.isBiometricEnabled
        selectedCategory.value = s.selectedCategory
        isAutoDriveBackupEnabled.value = s.autoDriveBackupEnabled

        prefs.edit()
            .putBoolean("is_dark_mode", s.isDarkMode)
            .putBoolean("is_arabic", s.isArabic)
            .putBoolean("is_grid_view", s.isGridView)
            .putBoolean("biometric_enabled", s.isBiometricEnabled)
            .putString("selected_category", s.selectedCategory)
            .putBoolean(DriveAutoBackupWorker.KEY_AUTO_BACKUP_24H, s.autoDriveBackupEnabled)
            .apply()

        if (s.autoDriveBackupEnabled) {
            DriveAutoBackupWorker.scheduleEvery24Hours(getApplication())
        } else {
            DriveAutoBackupWorker.cancelScheduledBackup(getApplication())
        }
        persistSettingsSnapshot()
    }

    fun openNewNote(initialCategory: String = "أفكار", startWithChecklist: Boolean = false) {
        val emptyChecklist = if (startWithChecklist) {
            ChecklistItem.listToJson(listOf(ChecklistItem(text = "", isDone = false)))
        } else ""

        val defaultCat = if (isArabic.value) {
            if (startWithChecklist) "مهام" else initialCategory
        } else {
            if (startWithChecklist) "Todo" else "Ideas"
        }

        editingNote.value = NoteEntity(
            title = "",
            content = "",
            category = defaultCat,
            colorPreset = GlassTint.FROST.id,
            checklistJson = emptyChecklist,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        isCreatingNew.value = true
    }

    fun openExistingNote(note: NoteEntity) {
        editingNote.value = note
        isCreatingNew.value = false
    }

    fun closeEditor() {
        editingNote.value = null
        isCreatingNew.value = false
    }

    /**
     * Silent real-time autosave while editing:
     * Saves written text LITERALLY without altering/trimming whitespace, formatting, or newlines.
     */
    fun autoSaveNote(
        title: String,
        content: String,
        category: String,
        colorPreset: String,
        isPinned: Boolean,
        isFavorite: Boolean,
        checklist: List<ChecklistItem>,
        audioDurationSec: Int
    ) {
        val current = editingNote.value ?: return
        val activeChecklist = checklist.filter { it.text.isNotEmpty() }

        // Only skip when completely empty (never touch non-empty literal whitespace)
        if (title.isEmpty() && content.isEmpty() && activeChecklist.isEmpty() && audioDurationSec == 0) {
            return
        }

        val updatedNote = current.copy(
            title = title,
            content = content,
            category = category.ifBlank { if (isArabic.value) "عام" else "General" },
            colorPreset = colorPreset,
            isPinned = isPinned,
            isFavorite = isFavorite,
            checklistJson = ChecklistItem.listToJson(activeChecklist),
            audioDurationSec = audioDurationSec,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            val id = repository.insertOrUpdate(updatedNote)
            if (current.id == 0L && id > 0) {
                editingNote.value = updatedNote.copy(id = id)
            } else {
                editingNote.value = updatedNote
            }
        }
    }

    fun saveNote(
        title: String,
        content: String,
        category: String,
        colorPreset: String,
        isPinned: Boolean,
        isFavorite: Boolean,
        checklist: List<ChecklistItem>,
        audioDurationSec: Int
    ) {
        val current = editingNote.value ?: return
        val activeChecklist = checklist.filter { it.text.isNotEmpty() }

        if (title.isEmpty() && content.isEmpty() && activeChecklist.isEmpty() && audioDurationSec == 0) {
            closeEditor()
            return
        }

        val updatedNote = current.copy(
            title = title,
            content = content,
            category = category.ifBlank { if (isArabic.value) "عام" else "General" },
            colorPreset = colorPreset,
            isPinned = isPinned,
            isFavorite = isFavorite,
            checklistJson = ChecklistItem.listToJson(activeChecklist),
            audioDurationSec = audioDurationSec,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.insertOrUpdate(updatedNote)
            closeEditor()
            showSnackbar(if (isArabic.value) "تم حفظ الملاحظة حرفياً بنجاح ✨" else "Note saved verbatim ✨")
        }
    }

    fun deleteCurrentNote() {
        val current = editingNote.value ?: return
        viewModelScope.launch {
            if (current.id != 0L) {
                repository.deleteById(current.id)
                showSnackbar(if (isArabic.value) "تم حذف الملاحظة" else "Note deleted")
            }
            closeEditor()
        }
    }

    fun deleteNoteDirectly(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            showSnackbar(if (isArabic.value) "تم حذف الملاحظة" else "Note deleted")
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.togglePin(note)
        }
    }

    fun toggleFavorite(note: NoteEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(note)
        }
    }

    fun toggleChecklistItem(note: NoteEntity, itemIndex: Int) {
        val items = ChecklistItem.jsonToList(note.checklistJson).toMutableList()
        if (itemIndex in items.indices) {
            val item = items[itemIndex]
            items[itemIndex] = item.copy(isDone = !item.isDone)
            val updated = note.copy(
                checklistJson = ChecklistItem.listToJson(items),
                updatedAt = System.currentTimeMillis()
            )
            viewModelScope.launch {
                repository.insertOrUpdate(updated)
            }
        }
    }

    // --- Immutable Literal Vault Operations ---

    fun saveLiteralTextToVault(title: String, literalContent: String) {
        if (title.isEmpty() && literalContent.isEmpty()) return
        viewModelScope.launch {
            val item = LocalDeviceStorageManager.saveLiteralTextToVault(
                context = getApplication(),
                title = title,
                literalContent = literalContent
            )
            vaultDao.insertImmutableItem(item)
            showSnackbar(
                if (isArabic.value) "تم الحفظ حرفياً في الـ Vault وذاكرة الهاتف 🔒"
                else "Saved verbatim in Vault & phone storage 🔒"
            )
        }
    }

    fun saveFileToVault(uri: Uri) {
        viewModelScope.launch {
            try {
                val item = LocalDeviceStorageManager.saveFileUriLiterallyToVault(
                    context = getApplication(),
                    uri = uri
                )
                vaultDao.insertImmutableItem(item)
                showSnackbar(
                    if (isArabic.value) "تم حفظ الملف حرفياً في ذاكرة الهاتف والـ Vault 🔒"
                    else "File saved verbatim in phone storage & Vault 🔒"
                )
            } catch (e: Exception) {
                showSnackbar(
                    if (isArabic.value) "تعذر حفظ الملف: ${e.localizedMessage}"
                    else "Unable to save file: ${e.localizedMessage}"
                )
            }
        }
    }

    fun exportVaultItemToUri(item: VaultItemEntity, destUri: Uri) {
        viewModelScope.launch {
            try {
                LocalDeviceStorageManager.exportVaultItemToUri(getApplication(), item, destUri)
                showSnackbar(
                    if (isArabic.value) "تم تصدير النسخة الحرفية إلى ذاكرة الهاتف 📁"
                    else "Literal copy exported to phone storage 📁"
                )
            } catch (e: Exception) {
                showSnackbar(
                    if (isArabic.value) "تعذر التصدير: ${e.localizedMessage}"
                    else "Export failed: ${e.localizedMessage}"
                )
            }
        }
    }

    // --- Local Storage & Google Drive Backup / Restore ---

    fun exportFullBackupToPhoneUri(destUri: Uri) {
        viewModelScope.launch {
            try {
                val json = buildCurrentFullBackupJson()
                LocalDeviceStorageManager.exportJsonToUri(getApplication(), json, destUri)
                showSnackbar(
                    if (isArabic.value) "تم حفظ نسخة كاملة في ذاكرة الهاتف 📁✨"
                    else "Full literal backup saved to phone storage 📁✨"
                )
            } catch (e: Exception) {
                showSnackbar(
                    if (isArabic.value) "تعذر الحفظ: ${e.localizedMessage}"
                    else "Save failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun restoreFullBackupFromPhoneUri(srcUri: Uri) {
        viewModelScope.launch {
            try {
                val json = LocalDeviceStorageManager.readUtf8FromUri(getApplication(), srcUri)
                applyRestoredBackupBundle(json)
                showSnackbar(
                    if (isArabic.value) "تمت الاستعادة الحرفية من ذاكرة الهاتف بنجاح ✨"
                    else "Restored verbatim from phone storage ✨"
                )
            } catch (e: Exception) {
                showSnackbar(
                    if (isArabic.value) "ملف غير صالح للاستعادة: ${e.localizedMessage}"
                    else "Invalid backup file: ${e.localizedMessage}"
                )
            }
        }
    }

    fun performManualDriveBackup(onNeedConsent: (IntentSender) -> Unit) {
        if (driveSyncStatus.value is DriveSyncStatus.BackingUp ||
            driveSyncStatus.value is DriveSyncStatus.Restoring
        ) return

        viewModelScope.launch {
            driveSyncStatus.value = DriveSyncStatus.BackingUp
            when (val auth = GoogleDriveBackupManager.authorize(getApplication())) {
                is DriveAuthOutcome.Authorized -> executeDriveBackupWithToken(auth.accessToken)
                is DriveAuthOutcome.NeedsConsent -> {
                    pendingDriveAction = PendingDriveAction.BACKUP
                    driveSyncStatus.value = DriveSyncStatus.Idle
                    onNeedConsent(auth.intentSender)
                }
                is DriveAuthOutcome.Failed -> {
                    driveSyncStatus.value = DriveSyncStatus.Error(
                        messageAr = "تعذر تفويض Google Drive: ${auth.message}",
                        messageEn = "Google Drive auth error: ${auth.message}"
                    )
                }
            }
        }
    }

    fun performManualDriveRestore(onNeedConsent: (IntentSender) -> Unit) {
        if (driveSyncStatus.value is DriveSyncStatus.BackingUp ||
            driveSyncStatus.value is DriveSyncStatus.Restoring
        ) return

        viewModelScope.launch {
            driveSyncStatus.value = DriveSyncStatus.Restoring
            when (val auth = GoogleDriveBackupManager.authorize(getApplication())) {
                is DriveAuthOutcome.Authorized -> executeDriveRestoreWithToken(auth.accessToken)
                is DriveAuthOutcome.NeedsConsent -> {
                    pendingDriveAction = PendingDriveAction.RESTORE
                    driveSyncStatus.value = DriveSyncStatus.Idle
                    onNeedConsent(auth.intentSender)
                }
                is DriveAuthOutcome.Failed -> {
                    driveSyncStatus.value = DriveSyncStatus.Error(
                        messageAr = "تعذر تفويض Google Drive: ${auth.message}",
                        messageEn = "Google Drive auth error: ${auth.message}"
                    )
                }
            }
        }
    }

    fun setAutoDriveBackup24h(enabled: Boolean, onNeedConsent: (IntentSender) -> Unit) {
        isAutoDriveBackupEnabled.value = enabled
        prefs.edit().putBoolean(DriveAutoBackupWorker.KEY_AUTO_BACKUP_24H, enabled).apply()
        persistSettingsSnapshot()

        if (enabled) {
            DriveAutoBackupWorker.scheduleEvery24Hours(getApplication())
            // Authorize once upfront so background 24h runs can execute silently without UI
            viewModelScope.launch {
                when (val auth = GoogleDriveBackupManager.authorize(getApplication())) {
                    is DriveAuthOutcome.NeedsConsent -> {
                        pendingDriveAction = PendingDriveAction.AUTHORIZE_ONLY
                        onNeedConsent(auth.intentSender)
                    }
                    is DriveAuthOutcome.Authorized -> {
                        showSnackbar(
                            if (isArabic.value) "تم تفعيل النسخ التلقائي على Google Drive كل 24 ساعة ☁️"
                            else "24h automatic Google Drive backup enabled ☁️"
                        )
                    }
                    is DriveAuthOutcome.Failed -> {
                        showSnackbar(
                            if (isArabic.value) "تم جدولة النسخ كل 24 ساعة (تحتاج ربط Google Drive عند الاتصال)"
                            else "24h schedule saved (connect Google Drive when ready)"
                        )
                    }
                }
            }
        } else {
            DriveAutoBackupWorker.cancelScheduledBackup(getApplication())
            showSnackbar(
                if (isArabic.value) "تم إيقاف النسخ التلقائي كل 24 ساعة"
                else "24h automatic backup disabled"
            )
        }
    }

    fun onDriveConsentGranted(accessToken: String) {
        val action = pendingDriveAction
        pendingDriveAction = null
        viewModelScope.launch {
            when (action) {
                PendingDriveAction.BACKUP -> executeDriveBackupWithToken(accessToken)
                PendingDriveAction.RESTORE -> executeDriveRestoreWithToken(accessToken)
                PendingDriveAction.AUTHORIZE_ONLY -> {
                    showSnackbar(
                        if (isArabic.value) "تم ربط Google Drive والنسخ التلقائي كل 24 ساعة بنجاح ☁️"
                        else "Google Drive connected for 24h auto-backup ☁️"
                    )
                }
                null -> Unit
            }
        }
    }

    fun onDriveConsentCancelled() {
        pendingDriveAction = null
        driveSyncStatus.value = DriveSyncStatus.Idle
        showSnackbar(
            if (isArabic.value) "تم إلغاء ربط Google Drive"
            else "Google Drive authorization cancelled"
        )
    }

    private suspend fun buildCurrentFullBackupJson(): String = withContext(Dispatchers.IO) {
        val notes = db.noteDao().getAllNotesOnce()
        val vault = vaultDao.getAllVaultItemsOnce()
        LocalDeviceStorageManager.buildAndPersistFullBackupJson(
            context = getApplication(),
            settings = currentSettingsSnapshot(),
            notes = notes,
            vaultItems = vault
        )
    }

    private suspend fun executeDriveBackupWithToken(accessToken: String) {
        driveSyncStatus.value = DriveSyncStatus.BackingUp
        try {
            val json = buildCurrentFullBackupJson()
            GoogleDriveBackupManager.uploadBackupJson(accessToken, json)
            val now = System.currentTimeMillis()
            lastDriveBackupAt.value = now
            prefs.edit()
                .putLong(DriveAutoBackupWorker.KEY_LAST_DRIVE_BACKUP_AT, now)
                .putString(DriveAutoBackupWorker.KEY_LAST_DRIVE_BACKUP_STATUS, "manual_ok")
                .apply()

            driveSyncStatus.value = DriveSyncStatus.Success(
                messageAr = "تم حفظ الإعدادات والملاحظات والـ Vault في Google Drive بنجاح ☁️✨",
                messageEn = "Settings, Notes & Vault backed up to Google Drive ☁️✨"
            )
            showSnackbar(
                if (isArabic.value) "تم النسخ الاحتياطي إلى Google Drive بنجاح ☁️"
                else "Google Drive backup complete ☁️"
            )
        } catch (e: Exception) {
            driveSyncStatus.value = DriveSyncStatus.Error(
                messageAr = "فشل النسخ إلى Google Drive: ${e.localizedMessage}",
                messageEn = "Google Drive backup failed: ${e.localizedMessage}"
            )
        }
    }

    private suspend fun executeDriveRestoreWithToken(accessToken: String) {
        driveSyncStatus.value = DriveSyncStatus.Restoring
        try {
            val json = GoogleDriveBackupManager.downloadLatestBackupJson(accessToken)
            if (json.isNullOrBlank()) {
                driveSyncStatus.value = DriveSyncStatus.Error(
                    messageAr = "لا توجد نسخة احتياطية سابقة في Google Drive",
                    messageEn = "No existing backup found in Google Drive"
                )
                return
            }

            applyRestoredBackupBundle(json)
            driveSyncStatus.value = DriveSyncStatus.Success(
                messageAr = "تمت استعادة الإعدادات والملاحظات والـ Vault من Google Drive بنجاح ✨",
                messageEn = "Restored Settings, Notes & Vault from Google Drive ✨"
            )
            showSnackbar(
                if (isArabic.value) "تمت الاستعادة من Google Drive بنجاح ✨"
                else "Restored from Google Drive ✨"
            )
        } catch (e: Exception) {
            driveSyncStatus.value = DriveSyncStatus.Error(
                messageAr = "فشلت الاستعادة من Google Drive: ${e.localizedMessage}",
                messageEn = "Google Drive restore failed: ${e.localizedMessage}"
            )
        }
    }

    private suspend fun applyRestoredBackupBundle(jsonString: String) = withContext(Dispatchers.IO) {
        val bundle = LocalDeviceStorageManager.parseFullBackupBundle(jsonString)

        bundle.settings?.let { restoredSettings ->
            withContext(Dispatchers.Main) {
                applyRestoredSettings(restoredSettings)
            }
        }

        // Restore notes verbatim without losing or altering existing local notes
        val existingNotes = db.noteDao().getAllNotesOnce()
        val existingKeys = existingNotes.map { "${it.createdAt}|${it.title}|${it.content}" }.toHashSet()
        bundle.notes.forEach { note ->
            val key = "${note.createdAt}|${note.title}|${note.content}"
            if (key !in existingKeys) {
                db.noteDao().insertNote(note.copy(id = 0L))
                existingKeys.add(key)
            }
        }

        // Restore Vault items with strict IGNORE / write-once semantics so existing Vault items never change
        bundle.vaultItems.forEach { restorable ->
            val materialized = LocalDeviceStorageManager.restoreVaultRawFileFromBase64WriteOnce(
                context = getApplication(),
                item = restorable.entity,
                base64Data = restorable.rawFileBase64
            )
            vaultDao.insertImmutableItem(materialized)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        isBiometricEnabled.value = enabled
        if (!enabled) {
            isAppLocked.value = false
        }
        persistSettingsSnapshot()
    }

    fun lockApp() {
        if (isBiometricEnabled.value) {
            isAppLocked.value = true
        }
    }

    fun unlockApp() {
        isAppLocked.value = false
    }

    fun openSettingsDrawer() {
        isSettingsDrawerOpen.value = true
    }

    fun closeSettingsDrawer() {
        isSettingsDrawerOpen.value = false
    }

    fun openVault() {
        isSettingsDrawerOpen.value = false
        isVaultOpen.value = true
    }

    fun closeVault() {
        isVaultOpen.value = false
    }

    fun showSnackbar(message: String) {
        snackbarMessage.value = message
    }

    fun clearSnackbar() {
        snackbarMessage.value = null
    }

    fun checkForUpdates(isManual: Boolean) {
        if (updateState.value is UpdateUiState.Checking || updateState.value is UpdateUiState.Downloading) {
            if (isManual && updateState.value is UpdateUiState.Downloading) {
                showUpdateDialog.value = true
            }
            return
        }

        viewModelScope.launch {
            updateState.value = UpdateUiState.Checking
            when (val result = AppUpdateManager.checkForUpdate()) {
                is AppUpdateManager.CheckResult.UpdateAvailable -> {
                    updateState.value = UpdateUiState.Available(result.info)
                    showUpdateDialog.value = true
                    if (isManual) {
                        closeSettingsDrawer()
                    }
                }
                is AppUpdateManager.CheckResult.AlreadyLatest -> {
                    updateState.value = UpdateUiState.UpToDate
                    if (isManual) {
                        showSnackbar(
                            if (isArabic.value) "أنت تستخدم أحدث إصدار بالفعل ✨" else "You're already on the latest version ✨"
                        )
                    }
                }
                is AppUpdateManager.CheckResult.Failure -> {
                    updateState.value = UpdateUiState.Idle
                    if (isManual) {
                        showSnackbar(if (isArabic.value) result.messageAr else result.messageEn)
                    }
                }
            }
        }
    }

    fun startUpdateDownload(info: ReleaseUpdateInfo) {
        if (updateState.value is UpdateUiState.Downloading) return
        val appContext = getApplication<Application>()

        viewModelScope.launch {
            updateState.value = UpdateUiState.Downloading(
                info = info,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = info.apkSizeBytes
            )
            try {
                val apkFile = AppUpdateManager.downloadApk(
                    context = appContext,
                    info = info
                ) { progress, downloaded, total ->
                    updateState.value = UpdateUiState.Downloading(
                        info = info,
                        progress = progress,
                        downloadedBytes = downloaded,
                        totalBytes = total
                    )
                }

                if (AppUpdateManager.canInstallPackages(appContext)) {
                    updateState.value = UpdateUiState.ReadyToInstall(info, apkFile)
                    AppUpdateManager.installApk(appContext, apkFile)
                } else {
                    updateState.value = UpdateUiState.NeedsInstallPermission(info, apkFile)
                }
            } catch (e: Exception) {
                updateState.value = UpdateUiState.Error(
                    messageAr = "تعذر تحميل التحديث: تأكد من اتصال الإنترنت وحاول مرة أخرى",
                    messageEn = e.localizedMessage ?: "Failed to download update",
                    info = info
                )
            }
        }
    }

    fun installDownloadedUpdate() {
        val appContext = getApplication<Application>()
        val currentState = updateState.value
        val (info, file) = when (currentState) {
            is UpdateUiState.ReadyToInstall -> currentState.info to currentState.apkFile
            is UpdateUiState.NeedsInstallPermission -> currentState.info to currentState.apkFile
            else -> return
        }

        if (AppUpdateManager.canInstallPackages(appContext)) {
            updateState.value = UpdateUiState.ReadyToInstall(info, file)
            AppUpdateManager.installApk(appContext, file)
        } else {
            updateState.value = UpdateUiState.NeedsInstallPermission(info, file)
        }
    }

    fun retryInstallAfterPermissionIfReady() {
        val currentState = updateState.value
        if (currentState is UpdateUiState.NeedsInstallPermission) {
            val appContext = getApplication<Application>()
            if (AppUpdateManager.canInstallPackages(appContext)) {
                updateState.value = UpdateUiState.ReadyToInstall(currentState.info, currentState.apkFile)
                AppUpdateManager.installApk(appContext, currentState.apkFile)
            }
        }
    }

    fun dismissUpdateDialog() {
        showUpdateDialog.value = false
    }
}
