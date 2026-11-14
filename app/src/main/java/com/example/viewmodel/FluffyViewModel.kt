package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FluffyDatabase
import com.example.data.model.ChecklistItem
import com.example.data.model.GlassTint
import com.example.data.model.NoteEntity
import com.example.data.model.VaultItemEntity
import com.example.data.repository.CloudSyncStatus
import com.example.data.repository.FirebaseSyncRepository
import com.example.data.repository.FirebaseUserInfo
import com.example.data.repository.NoteRepository
import com.example.data.storage.FluffySettingsSnapshot
import com.example.data.storage.LocalDeviceStorageManager
import com.example.update.AppUpdateManager
import com.example.update.ReleaseUpdateInfo
import com.example.update.UpdateUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class FluffyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FluffyDatabase.getDatabase(application)
    private val repository = NoteRepository(db.noteDao())
    private val vaultDao = db.vaultDao()
    private val prefs = application.getSharedPreferences("fluffy_prefs", android.content.Context.MODE_PRIVATE)

    // App Preferences (Persisted across restarts, literal phone storage & Cloud Firestore)
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
    val showAuthDialog = MutableStateFlow(false)
    val snackbarMessage = MutableStateFlow<String?>(null)

    // In-App OTA Update State
    val updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val showUpdateDialog = MutableStateFlow(false)

    // Firebase Auth & Real-Time Cloud Firestore Sync State + Guest Mode
    val currentUser = MutableStateFlow<FirebaseUserInfo?>(
        FirebaseSyncRepository.currentUser(application)
    )
    val isGuestMode = MutableStateFlow(prefs.getBoolean("is_guest_mode", false))
    val isAuthBusy = MutableStateFlow(false)
    val cloudSyncStatus = MutableStateFlow<CloudSyncStatus>(CloudSyncStatus.Disconnected)

    private var cloudSyncJob: Job? = null
    private var lastAppliedRemoteSettingsAt = prefs.getLong("last_remote_settings_at", 0L)
    private val noteSaveMutex = Mutex()

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
            backfillMissingSyncIds()
            backfillSortOrderIfNeeded()

            // Observe Firebase Authentication & automatically bind real-time Cloud Firestore listeners
            launch {
                FirebaseSyncRepository.observeAuthState(getApplication()).collect { user ->
                    val previousUid = currentUser.value?.uid
                    currentUser.value = user
                    if (user != null) {
                        if (isGuestMode.value) {
                            isGuestMode.value = false
                            prefs.edit().putBoolean("is_guest_mode", false).apply()
                        }
                        if (previousUid != user.uid || cloudSyncJob == null || cloudSyncJob?.isActive != true) {
                            startRealTimeCloudSync(user.uid)
                        }
                    } else {
                        cloudSyncJob?.cancel()
                        cloudSyncJob = null
                        cloudSyncStatus.value = CloudSyncStatus.Disconnected
                    }
                }
            }

            // Keep local literal snapshot synced automatically
            repository.allNotes.collect { notes ->
                withContext(Dispatchers.IO) {
                    LocalDeviceStorageManager.saveNotesLiteralSnapshot(getApplication(), notes)
                }
            }
        }
        persistSettingsSnapshot(syncToCloud = false)
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
                applyRestoredSettings(restored, fromRemote = false)
            }
        }
    }

    private suspend fun backfillMissingSyncIds() = withContext(Dispatchers.IO) {
        val notes = repository.getAllNotesOnce()
        notes.filter { it.syncId.isBlank() }.forEach { note ->
            repository.insertOrUpdate(note.copy(syncId = UUID.randomUUID().toString()))
        }
    }

    private suspend fun backfillSortOrderIfNeeded() = withContext(Dispatchers.IO) {
        val notes = repository.getAllNotesOnce()
        if (notes.size > 1 && notes.all { it.sortOrder == 0L }) {
            val indexed = notes.mapIndexed { idx, note ->
                note.copy(sortOrder = idx * 1000L)
            }
            repository.updateSortOrders(indexed)
        }
    }

    private fun startRealTimeCloudSync(uid: String) {
        cloudSyncJob?.cancel()
        cloudSyncJob = viewModelScope.launch {
            cloudSyncStatus.value = CloudSyncStatus.Syncing
            try {
                reconcileLocalAndCloud(uid)
            } catch (e: Exception) {
                cloudSyncStatus.value = CloudSyncStatus.Error(
                    messageAr = "تعذر المزامنة الأولية: ${e.localizedMessage ?: "خطأ"}",
                    messageEn = "Initial sync failed: ${e.localizedMessage ?: "Error"}"
                )
            }

            // 1) Real-time listener for users/{userId}/notes
            launch {
                FirebaseSyncRepository.observeNotes(getApplication(), uid)
                    .catch { e ->
                        cloudSyncStatus.value = CloudSyncStatus.Error(
                            messageAr = "توقفت المزامنة الفورية: ${e.localizedMessage ?: "خطأ"}",
                            messageEn = "Realtime sync error: ${e.localizedMessage ?: "Error"}"
                        )
                    }
                    .collect { remoteSnapshot ->
                        withContext(Dispatchers.IO) {
                            remoteSnapshot.upserted.forEach { remoteNote ->
                                val existing = repository.getNoteBySyncId(remoteNote.syncId)
                                if (existing == null) {
                                    repository.insertOrUpdate(remoteNote.copy(id = 0L))
                                } else if (remoteNote.updatedAt > existing.updatedAt) {
                                    repository.insertOrUpdate(remoteNote.copy(id = existing.id))
                                }
                            }

                            if (!remoteSnapshot.hasPendingWrites) {
                                remoteSnapshot.removedSyncIds.forEach { sid ->
                                    if (editingNote.value?.syncId != sid) {
                                        repository.deleteBySyncId(sid)
                                    }
                                }
                            }
                        }
                        val count = withContext(Dispatchers.IO) {
                            repository.getAllNotesOnce().size
                        }
                        cloudSyncStatus.value = CloudSyncStatus.Synced(
                            lastSyncAt = System.currentTimeMillis(),
                            syncedCount = count
                        )
                    }
            }

            // 2) Real-time listener for users/{userId}/settings/preferences
            launch {
                FirebaseSyncRepository.observeSettings(getApplication(), uid)
                    .catch { /* logged inside repository */ }
                    .collect { payload ->
                        if (payload != null) {
                            val localUpdatedAt = prefs.getLong("local_settings_updated_at", 0L)
                            if (payload.updatedAt > lastAppliedRemoteSettingsAt && payload.updatedAt > localUpdatedAt) {
                                lastAppliedRemoteSettingsAt = payload.updatedAt
                                prefs.edit().putLong("last_remote_settings_at", payload.updatedAt).apply()
                                applyRestoredSettings(payload.snapshot, fromRemote = true)
                            }
                        }
                    }
            }

            // 3) Real-time listener for users/{userId}/vault (immutable write-once)
            launch {
                FirebaseSyncRepository.observeVaultItems(getApplication(), uid)
                    .catch { /* logged inside repository */ }
                    .collect { remotePayloads ->
                        withContext(Dispatchers.IO) {
                            remotePayloads.forEach { payload ->
                                val materialized = LocalDeviceStorageManager.restoreVaultRawFileFromBase64WriteOnce(
                                    context = getApplication(),
                                    item = payload.item,
                                    base64Data = payload.rawFileBase64
                                )
                                vaultDao.insertImmutableItem(materialized)
                            }
                        }
                    }
            }
        }
    }

    private suspend fun reconcileLocalAndCloud(uid: String) = withContext(Dispatchers.IO) {
        val app = getApplication<Application>()
        backfillMissingSyncIds()

        // Reconcile Notes (last-write-wins using updatedAt; literal text untouched)
        val localNotes = repository.getAllNotesOnce()
        val remoteNotes = FirebaseSyncRepository.fetchNotesOnce(app, uid)
        val localBySync = localNotes.associateBy { it.syncId }
        val remoteBySync = remoteNotes.associateBy { it.syncId }

        for (local in localNotes) {
            val remote = remoteBySync[local.syncId]
            if (remote == null || local.updatedAt > remote.updatedAt) {
                FirebaseSyncRepository.upsertNote(app, uid, local)
            }
        }
        for (remote in remoteNotes) {
            val local = localBySync[remote.syncId]
            if (local == null) {
                repository.insertOrUpdate(remote.copy(id = 0L))
            } else if (remote.updatedAt > local.updatedAt) {
                repository.insertOrUpdate(remote.copy(id = local.id))
            }
        }

        // Reconcile Settings
        val remoteSettings = FirebaseSyncRepository.fetchSettingsOnce(app, uid)
        val localSettingsAt = prefs.getLong("local_settings_updated_at", 0L)
        if (remoteSettings == null || localSettingsAt > remoteSettings.updatedAt) {
            FirebaseSyncRepository.upsertSettings(
                context = app,
                userId = uid,
                settings = currentSettingsSnapshot(),
                updatedAt = localSettingsAt.takeIf { it > 0L } ?: System.currentTimeMillis()
            )
        } else if (remoteSettings.updatedAt > localSettingsAt) {
            withContext(Dispatchers.Main) {
                applyRestoredSettings(remoteSettings.snapshot, fromRemote = true)
            }
            lastAppliedRemoteSettingsAt = remoteSettings.updatedAt
            prefs.edit().putLong("last_remote_settings_at", remoteSettings.updatedAt).apply()
        }

        // Reconcile Immutable Vault Items (bidirectional union; never overwrites existing items)
        val localVault = vaultDao.getAllVaultItemsOnce()
        val remoteVault = FirebaseSyncRepository.fetchVaultItemsOnce(app, uid)
        val localVaultIds = localVault.map { it.id }.toHashSet()
        val remoteVaultIds = remoteVault.map { it.item.id }.toHashSet()

        localVault.forEach { item ->
            if (item.id !in remoteVaultIds) {
                FirebaseSyncRepository.upsertVaultItemIfAbsent(app, uid, item)
            }
        }
        remoteVault.forEach { payload ->
            if (payload.item.id !in localVaultIds) {
                val materialized = LocalDeviceStorageManager.restoreVaultRawFileFromBase64WriteOnce(
                    context = app,
                    item = payload.item,
                    base64Data = payload.rawFileBase64
                )
                vaultDao.insertImmutableItem(materialized)
            }
        }

        val finalCount = repository.getAllNotesOnce().size
        cloudSyncStatus.value = CloudSyncStatus.Synced(
            lastSyncAt = System.currentTimeMillis(),
            syncedCount = finalCount
        )
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
        persistSettingsSnapshot(syncToCloud = true)
    }

    fun toggleLanguage() {
        val next = !isArabic.value
        isArabic.value = next
        prefs.edit().putBoolean("is_arabic", next).apply()
        persistSettingsSnapshot(syncToCloud = true)
    }

    fun toggleViewMode() {
        val next = !isGridView.value
        isGridView.value = next
        prefs.edit().putBoolean("is_grid_view", next).apply()
        persistSettingsSnapshot(syncToCloud = true)
    }

    fun setSearch(query: String) {
        searchQuery.value = query
    }

    fun selectCategory(cat: String) {
        selectedCategory.value = cat
        prefs.edit().putString("selected_category", cat).apply()
        persistSettingsSnapshot(syncToCloud = true)
    }

    private fun currentSettingsSnapshot(): FluffySettingsSnapshot = FluffySettingsSnapshot(
        isDarkMode = isDarkMode.value,
        isArabic = isArabic.value,
        isGridView = isGridView.value,
        isBiometricEnabled = isBiometricEnabled.value,
        selectedCategory = selectedCategory.value,
        cloudSyncEnabled = true
    )

    private fun persistSettingsSnapshot(syncToCloud: Boolean = true) {
        val now = System.currentTimeMillis()
        if (syncToCloud) {
            prefs.edit().putLong("local_settings_updated_at", now).apply()
        }
        val snapshot = currentSettingsSnapshot()
        val uid = currentUser.value?.uid
        viewModelScope.launch(Dispatchers.IO) {
            LocalDeviceStorageManager.saveSettingsSnapshot(getApplication(), snapshot)
            if (syncToCloud && uid != null) {
                runCatching {
                    FirebaseSyncRepository.upsertSettings(getApplication(), uid, snapshot, now)
                }
            }
        }
    }

    private fun applyRestoredSettings(s: FluffySettingsSnapshot, fromRemote: Boolean) {
        isDarkMode.value = s.isDarkMode
        isArabic.value = s.isArabic
        isGridView.value = s.isGridView
        isBiometricEnabled.value = s.isBiometricEnabled
        selectedCategory.value = s.selectedCategory

        prefs.edit()
            .putBoolean("is_dark_mode", s.isDarkMode)
            .putBoolean("is_arabic", s.isArabic)
            .putBoolean("is_grid_view", s.isGridView)
            .putBoolean("biometric_enabled", s.isBiometricEnabled)
            .putString("selected_category", s.selectedCategory)
            .apply()

        persistSettingsSnapshot(syncToCloud = !fromRemote)
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
            updatedAt = System.currentTimeMillis(),
            syncId = UUID.randomUUID().toString()
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
     * Saves written text LITERALLY without altering/trimming whitespace, formatting, or newlines,
     * and automatically syncs to Cloud Firestore when authenticated.
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
        if (editingNote.value == null) return
        val activeChecklist = checklist.filter { it.text.isNotEmpty() }

        // Only skip when completely empty (never touch non-empty literal whitespace)
        if (title.isEmpty() && content.isEmpty() && activeChecklist.isEmpty() && audioDurationSec == 0) {
            return
        }

        viewModelScope.launch {
            noteSaveMutex.withLock {
                val current = editingNote.value ?: return@withLock
                val ensuredSyncId = current.syncId.ifBlank { UUID.randomUUID().toString() }
                val targetSortOrder = if (current.id == 0L) {
                    repository.getMinSortOrder() - 1000L
                } else {
                    current.sortOrder
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
                    sortOrder = targetSortOrder,
                    updatedAt = System.currentTimeMillis(),
                    syncId = ensuredSyncId
                )
                val id = repository.insertOrUpdate(updatedNote)
                val saved = if (current.id == 0L && id > 0) updatedNote.copy(id = id) else updatedNote
                editingNote.value = saved
                syncNoteUpsertIfSignedIn(saved)
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
        if (editingNote.value == null) return
        val activeChecklist = checklist.filter { it.text.isNotEmpty() }

        if (title.isEmpty() && content.isEmpty() && activeChecklist.isEmpty() && audioDurationSec == 0) {
            closeEditor()
            return
        }

        viewModelScope.launch {
            noteSaveMutex.withLock {
                val current = editingNote.value ?: return@withLock
                val ensuredSyncId = current.syncId.ifBlank { UUID.randomUUID().toString() }
                val targetSortOrder = if (current.id == 0L) {
                    repository.getMinSortOrder() - 1000L
                } else {
                    current.sortOrder
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
                    sortOrder = targetSortOrder,
                    updatedAt = System.currentTimeMillis(),
                    syncId = ensuredSyncId
                )
                val id = repository.insertOrUpdate(updatedNote)
                val saved = if (current.id == 0L && id > 0) updatedNote.copy(id = id) else updatedNote
                syncNoteUpsertIfSignedIn(saved)
                closeEditor()
                showSnackbar(if (isArabic.value) "تم حفظ الملاحظة حرفياً بنجاح ✨" else "Note saved verbatim ✨")
            }
        }
    }

    fun reorderNotes(reorderedVisibleNotes: List<NoteEntity>) {
        if (reorderedVisibleNotes.size <= 1) return
        viewModelScope.launch(Dispatchers.IO) {
            val allNotes = repository.getAllNotesOnce()
            if (allNotes.isEmpty()) return@launch

            val visibleIds = reorderedVisibleNotes.map { it.id }.toHashSet()
            val queue = ArrayDeque(reorderedVisibleNotes)
            val mergedGlobal = mutableListOf<NoteEntity>()

            for (note in allNotes) {
                if (note.id in visibleIds && queue.isNotEmpty()) {
                    mergedGlobal.add(queue.removeFirst())
                } else {
                    mergedGlobal.add(note)
                }
            }
            while (queue.isNotEmpty()) {
                mergedGlobal.add(queue.removeFirst())
            }

            val hasDuplicateSortOrders = allNotes.map { it.sortOrder }.distinct().size != allNotes.size
            val now = System.currentTimeMillis()
            val changedNotes = mutableListOf<NoteEntity>()
            mergedGlobal.forEachIndexed { index, note ->
                val targetSortOrder = index * 1000L
                if (hasDuplicateSortOrders || note.sortOrder != targetSortOrder) {
                    changedNotes.add(
                        note.copy(
                            sortOrder = targetSortOrder,
                            updatedAt = now,
                            syncId = note.syncId.ifBlank { UUID.randomUUID().toString() }
                        )
                    )
                }
            }

            if (changedNotes.isNotEmpty()) {
                repository.updateSortOrders(changedNotes)
                val uid = currentUser.value?.uid
                if (uid != null) {
                    changedNotes.forEach { changed ->
                        runCatching {
                            FirebaseSyncRepository.upsertNote(getApplication(), uid, changed)
                        }
                    }
                }
            }
        }
    }

    fun deleteCurrentNote() {
        val current = editingNote.value ?: return
        viewModelScope.launch {
            if (current.id != 0L) {
                repository.deleteById(current.id)
                syncNoteDeleteIfSignedIn(current.syncId)
                showSnackbar(if (isArabic.value) "تم حذف الملاحظة" else "Note deleted")
            }
            closeEditor()
        }
    }

    fun deleteNoteDirectly(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            syncNoteDeleteIfSignedIn(note.syncId)
            showSnackbar(if (isArabic.value) "تم حذف الملاحظة" else "Note deleted")
        }
    }

    fun togglePin(note: NoteEntity) {
        val updated = note.copy(
            isPinned = !note.isPinned,
            updatedAt = System.currentTimeMillis(),
            syncId = note.syncId.ifBlank { UUID.randomUUID().toString() }
        )
        viewModelScope.launch {
            repository.insertOrUpdate(updated)
            syncNoteUpsertIfSignedIn(updated)
        }
    }

    fun toggleFavorite(note: NoteEntity) {
        val updated = note.copy(
            isFavorite = !note.isFavorite,
            updatedAt = System.currentTimeMillis(),
            syncId = note.syncId.ifBlank { UUID.randomUUID().toString() }
        )
        viewModelScope.launch {
            repository.insertOrUpdate(updated)
            syncNoteUpsertIfSignedIn(updated)
        }
    }

    fun toggleChecklistItem(note: NoteEntity, itemIndex: Int) {
        val items = ChecklistItem.jsonToList(note.checklistJson).toMutableList()
        if (itemIndex in items.indices) {
            val item = items[itemIndex]
            items[itemIndex] = item.copy(isDone = !item.isDone)
            val updated = note.copy(
                checklistJson = ChecklistItem.listToJson(items),
                updatedAt = System.currentTimeMillis(),
                syncId = note.syncId.ifBlank { UUID.randomUUID().toString() }
            )
            viewModelScope.launch {
                repository.insertOrUpdate(updated)
                syncNoteUpsertIfSignedIn(updated)
            }
        }
    }

    private fun syncNoteUpsertIfSignedIn(note: NoteEntity) {
        val uid = currentUser.value?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                FirebaseSyncRepository.upsertNote(getApplication(), uid, note)
            }.onFailure { e ->
                cloudSyncStatus.value = CloudSyncStatus.Error(
                    messageAr = "تعذر رفع الملاحظة: ${e.localizedMessage ?: "خطأ"}",
                    messageEn = "Upload error: ${e.localizedMessage ?: "Error"}"
                )
            }
        }
    }

    private fun syncNoteDeleteIfSignedIn(syncId: String) {
        val uid = currentUser.value?.uid ?: return
        if (syncId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                FirebaseSyncRepository.deleteNote(getApplication(), uid, syncId)
            }
        }
    }

    // --- Firebase Authentication, Guest Mode & Manual Cloud Sync Triggers ---

    fun openAuthDialog() {
        showAuthDialog.value = true
    }

    fun closeAuthDialog() {
        showAuthDialog.value = false
    }

    fun enterGuestMode() {
        isGuestMode.value = true
        prefs.edit().putBoolean("is_guest_mode", true).apply()
        showAuthDialog.value = false
        showSnackbar(
            if (isArabic.value) "تم الدخول بوضع الضيف — بياناتك محفوظة محلياً على هاتفك 👤"
            else "Guest Mode active — notes saved locally on your device 👤"
        )
    }

    fun returnToWelcomeScreen() {
        isSettingsDrawerOpen.value = false
        showAuthDialog.value = false
        isGuestMode.value = false
        prefs.edit().putBoolean("is_guest_mode", false).apply()
    }

    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            isAuthBusy.value = true
            val result = FirebaseSyncRepository.signInWithGoogleIdToken(getApplication(), idToken)
            isAuthBusy.value = false
            result.onSuccess { user ->
                isGuestMode.value = false
                prefs.edit().putBoolean("is_guest_mode", false).apply()
                currentUser.value = user
                showAuthDialog.value = false
                showSnackbar(
                    if (isArabic.value) "تم إنشاء/ربط حساب Google وتفعيل المزامنة ☁️✨"
                    else "Google account connected — Cloud sync active ☁️✨"
                )
            }.onFailure { err ->
                showSnackbar(
                    if (isArabic.value) "فشل الدخول عبر Google: ${err.localizedMessage}"
                    else "Google Sign-In failed: ${err.localizedMessage}"
                )
            }
        }
    }

    fun syncCloudNow() {
        val uid = currentUser.value?.uid
        if (uid == null) {
            showAuthDialog.value = true
            return
        }
        viewModelScope.launch {
            cloudSyncStatus.value = CloudSyncStatus.Syncing
            try {
                reconcileLocalAndCloud(uid)
                showSnackbar(
                    if (isArabic.value) "تمت المزامنة السحابية الفورية بنجاح ☁️✨"
                    else "Cloud Firestore sync complete ☁️✨"
                )
            } catch (e: Exception) {
                cloudSyncStatus.value = CloudSyncStatus.Error(
                    messageAr = "تعذر المزامنة: ${e.localizedMessage ?: "خطأ"}",
                    messageEn = "Sync failed: ${e.localizedMessage ?: "Error"}"
                )
            }
        }
    }

    fun signOutFirebase() {
        viewModelScope.launch {
            cloudSyncJob?.cancel()
            cloudSyncJob = null
            FirebaseSyncRepository.signOut(getApplication())
            currentUser.value = null
            isGuestMode.value = false
            prefs.edit().putBoolean("is_guest_mode", false).apply()
            cloudSyncStatus.value = CloudSyncStatus.Disconnected
            showAuthDialog.value = false
            isSettingsDrawerOpen.value = false
            showSnackbar(
                if (isArabic.value) "تم تسجيل الخروج من حساب Google"
                else "Signed out from Google account"
            )
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
            currentUser.value?.uid?.let { uid ->
                launch(Dispatchers.IO) {
                    FirebaseSyncRepository.upsertVaultItemIfAbsent(getApplication(), uid, item)
                }
            }
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
                currentUser.value?.uid?.let { uid ->
                    launch(Dispatchers.IO) {
                        FirebaseSyncRepository.upsertVaultItemIfAbsent(getApplication(), uid, item)
                    }
                }
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

    // --- Local Phone Storage Full Literal Export / Import ---

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

    private suspend fun applyRestoredBackupBundle(jsonString: String) = withContext(Dispatchers.IO) {
        val bundle = LocalDeviceStorageManager.parseFullBackupBundle(jsonString)

        bundle.settings?.let { restoredSettings ->
            withContext(Dispatchers.Main) {
                applyRestoredSettings(restoredSettings, fromRemote = false)
            }
        }

        // Restore notes verbatim without losing or altering existing local notes
        val existingNotes = db.noteDao().getAllNotesOnce()
        val existingKeys = existingNotes.map { "${it.createdAt}|${it.title}|${it.content}" }.toHashSet()
        val existingSyncIds = existingNotes.map { it.syncId }.toHashSet()
        val uid = currentUser.value?.uid

        var nextFallbackSortOrder = (existingNotes.maxOfOrNull { it.sortOrder } ?: 0L) + 1000L
        val allBundleSortOrdersZero = bundle.notes.size > 1 && bundle.notes.all { it.sortOrder == 0L }

        bundle.notes.forEach { note ->
            val key = "${note.createdAt}|${note.title}|${note.content}"
            if (key !in existingKeys && note.syncId !in existingSyncIds) {
                val resolvedSortOrder = if (allBundleSortOrdersZero || (note.sortOrder == 0L && existingNotes.isNotEmpty())) {
                    nextFallbackSortOrder.also { nextFallbackSortOrder += 1000L }
                } else {
                    note.sortOrder
                }
                val prepared = note.copy(
                    id = 0L,
                    sortOrder = resolvedSortOrder,
                    syncId = note.syncId.ifBlank { UUID.randomUUID().toString() }
                )
                db.noteDao().insertNote(prepared)
                existingKeys.add(key)
                existingSyncIds.add(prepared.syncId)
                if (uid != null) {
                    runCatching { FirebaseSyncRepository.upsertNote(getApplication(), uid, prepared) }
                }
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
            if (uid != null) {
                runCatching { FirebaseSyncRepository.upsertVaultItemIfAbsent(getApplication(), uid, materialized) }
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        isBiometricEnabled.value = enabled
        if (!enabled) {
            isAppLocked.value = false
        }
        persistSettingsSnapshot(syncToCloud = true)
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
