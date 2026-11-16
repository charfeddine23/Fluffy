package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FluffyDatabase
import com.example.data.model.ChecklistItem
import com.example.data.model.GlassTint
import com.example.data.model.NoteEntity
import com.example.data.model.NotePagesCodec
import com.example.data.model.NotesViewMode
import com.example.data.model.VaultItemEntity
import com.example.data.repository.CloudSyncStatus
import com.example.data.repository.FirebaseSyncRepository
import com.example.data.repository.FirebaseUserInfo
import com.example.data.repository.NoteRepository
import com.example.data.storage.AudioCloudPolicy
import com.example.data.storage.AudioStorageManager
import com.example.data.storage.FluffySettingsSnapshot
import com.example.data.storage.LocalDeviceStorageManager
import com.example.drive.DriveAutoBackupWorker
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.AppThemeRegistry
import com.example.ui.theme.translateText
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
    private val initialIsArabic = prefs.getBoolean("is_arabic", true)
    val appLanguage = MutableStateFlow(
        AppLanguage.fromCode(
            code = prefs.getString("app_language_code", if (initialIsArabic) "ar" else "en"),
            fallbackArabic = initialIsArabic
        )
    )
    val isArabic = MutableStateFlow(appLanguage.value == AppLanguage.ARABIC)
    private val initialViewMode = NotesViewMode.fromId(
        id = prefs.getString("notes_view_mode", null),
        fallbackGrid = prefs.getBoolean("is_grid_view", true)
    )
    val viewMode = MutableStateFlow(initialViewMode)
    val isGridView = MutableStateFlow(initialViewMode.isGridLike)
    val selectedThemeId = MutableStateFlow(
        prefs.getString(
            "selected_theme_id",
            if (isDarkMode.value) AppThemePreset.DEFAULT_DARK_ID else AppThemePreset.DEFAULT_LIGHT_ID
        ) ?: AppThemePreset.DEFAULT_DARK_ID
    )
    val isColoredBordersEnabled = MutableStateFlow(prefs.getBoolean("colored_container_borders", true))

    // Biometric Authentication, App Lock, Non-Screenshot & Vault Lock (Biometric + Passkey)
    val isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))
    val isAppLocked = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))
    val isScreenshotProtectionEnabled = MutableStateFlow(prefs.getBoolean("non_screenshot_enabled", false))
    val isVaultLockEnabled = MutableStateFlow(prefs.getBoolean("vault_lock_enabled", false))
    val hasVaultPasskey = MutableStateFlow(!prefs.getString("vault_passkey_hash", "").isNullOrBlank())
    val isVaultUnlockedForSession = MutableStateFlow(false)

    private var openedTrashFromSettings = false
    private var openedArchiveFromSettings = false
    private var openedThemesFromSettings = false

    // Settings Drawer, Themes Gallery, Trash, Archive, Audio Player & Account Profile
    val isSettingsDrawerOpen = MutableStateFlow(false)
    val requestedSettingsPage = MutableStateFlow<String?>(null)
    val isThemesScreenOpen = MutableStateFlow(false)
    val isTrashScreenOpen = MutableStateFlow(false)
    val isArchiveScreenOpen = MutableStateFlow(false)
    val isAccountScreenOpen = MutableStateFlow(false)
    val isAudioPlayerScreenOpen = MutableStateFlow(false)

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
    val showPremiumUpgradeDialog = MutableStateFlow(false)
    val isPremium = MutableStateFlow(prefs.getBoolean("is_premium", false))
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
    val authErrorMessage = MutableStateFlow<String?>(null)
    val authInfoMessage = MutableStateFlow<String?>(null)
    val cloudSyncStatus = MutableStateFlow<CloudSyncStatus>(CloudSyncStatus.Disconnected)
    val profileFirstName = MutableStateFlow(prefs.getString("profile_first_name", "") ?: "")
    val profileLastName = MutableStateFlow(prefs.getString("profile_last_name", "") ?: "")
    val profileAvatarChoice = MutableStateFlow(prefs.getString("profile_avatar_choice", "") ?: "")
    val useGoogleAccountName = MutableStateFlow(prefs.getBoolean("use_google_account_name", false))
    val isAutoDriveBackupEnabled = MutableStateFlow(
        prefs.getBoolean(DriveAutoBackupWorker.KEY_AUTO_BACKUP_24H, false)
    )

    private var cloudSyncJob: Job? = null
    private var lastAppliedRemoteSettingsAt = prefs.getLong("last_remote_settings_at", 0L)
    private val noteSaveMutex = Mutex()

    val vaultItems: StateFlow<List<VaultItemEntity>> = vaultDao.getAllVaultItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val archivedNotes: StateFlow<List<NoteEntity>> = repository.archivedNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val trashedNotes: StateFlow<List<NoteEntity>> = repository.trashedNotes
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
            purgeExpiredTrashNotes()

            // Observe Firebase Authentication & automatically bind real-time Cloud Firestore listeners
            launch {
                FirebaseSyncRepository.observeAuthState(getApplication()).collect { user ->
                    val previousUid = currentUser.value?.uid
                    currentUser.value = user
                    if (user != null) {
                        syncProfileNameFromUser(user.displayName, forceIfGoogleNaming = useGoogleAccountName.value)
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

            // Keep local literal snapshot synced automatically (active + archived + trashed)
            combine(
                repository.allNotes,
                repository.archivedNotes,
                repository.trashedNotes
            ) { active, archived, trashed ->
                active + archived + trashed
            }.collect { notes ->
                withContext(Dispatchers.IO) {
                    LocalDeviceStorageManager.saveNotesLiteralSnapshot(getApplication(), notes)
                }
            }
        }
        persistSettingsSnapshot(syncToCloud = false)
        checkForUpdates(isManual = false)
    }

    suspend fun purgeExpiredTrashNotes() = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - TRASH_RETENTION_MILLIS
        val expired = repository.getExpiredTrashNotes(cutoff)
        if (expired.isNotEmpty()) {
            repository.deleteExpiredTrash(cutoff)
            val uid = currentUser.value?.uid
            if (uid != null) {
                expired.forEach { note ->
                    if (note.syncId.isNotBlank()) {
                        runCatching { FirebaseSyncRepository.deleteNote(getApplication(), uid, note.syncId) }
                    }
                }
            }
        }
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
                                    // Preserve local device audio attachment if local audio exists or exceeds 900 MB cloud limit
                                    val keepLocalAudio = existing.audioFilePath.isNotBlank() &&
                                        (remoteNote.audioFilePath.isBlank() || !AudioCloudPolicy.canStoreNoteAudioInCloud(existing))
                                    val merged = if (keepLocalAudio) {
                                        remoteNote.copy(
                                            id = existing.id,
                                            audioFilePath = existing.audioFilePath,
                                            audioByteSize = existing.audioByteSize,
                                            audioDurationSec = maxOf(existing.audioDurationSec, remoteNote.audioDurationSec)
                                        )
                                    } else {
                                        remoteNote.copy(id = existing.id)
                                    }
                                    repository.insertOrUpdate(merged)
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
                val keepLocalAudio = local.audioFilePath.isNotBlank() &&
                    (remote.audioFilePath.isBlank() || !AudioCloudPolicy.canStoreNoteAudioInCloud(local))
                val merged = if (keepLocalAudio) {
                    remote.copy(
                        id = local.id,
                        audioFilePath = local.audioFilePath,
                        audioByteSize = local.audioByteSize,
                        audioDurationSec = maxOf(local.audioDurationSec, remote.audioDurationSec)
                    )
                } else {
                    remote.copy(id = local.id)
                }
                repository.insertOrUpdate(merged)
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
            if (item.id !in remoteVaultIds && AudioCloudPolicy.canStoreVaultItemInCloud(item)) {
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
                    NotePagesCodec.searchableBody(it).lowercase().contains(q) ||
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

    val audioNotes: StateFlow<List<NoteEntity>> = combine(
        repository.allNotes,
        archivedNotes
    ) { active, archived ->
        (active + archived)
            .filter { !it.isDeleted && (it.audioDurationSec > 0 || it.audioFilePath.isNotBlank()) }
            .sortedByDescending { it.updatedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleDarkMode() {
        val next = !isDarkMode.value
        isDarkMode.value = next

        val currentTheme = AppThemeRegistry.resolve(selectedThemeId.value, !next)
        val switchedTheme = currentTheme.pairedThemeId?.let { AppThemeRegistry.findById(it) }
            ?: if (next) AppThemeRegistry.defaultDark else AppThemeRegistry.defaultLight

        selectedThemeId.value = switchedTheme.id
        prefs.edit()
            .putBoolean("is_dark_mode", next)
            .putString("selected_theme_id", switchedTheme.id)
            .apply()
        persistSettingsSnapshot(syncToCloud = true)
    }

    fun selectTheme(preset: AppThemePreset) {
        selectedThemeId.value = preset.id
        isDarkMode.value = preset.isDark
        prefs.edit()
            .putString("selected_theme_id", preset.id)
            .putBoolean("is_dark_mode", preset.isDark)
            .apply()
        persistSettingsSnapshot(syncToCloud = true)
    }

    fun setColoredBordersEnabled(enabled: Boolean) {
        isColoredBordersEnabled.value = enabled
        prefs.edit().putBoolean("colored_container_borders", enabled).apply()
        persistSettingsSnapshot(syncToCloud = true)
    }

    fun openThemesScreen(fromSettings: Boolean = false) {
        openedThemesFromSettings = fromSettings || isSettingsDrawerOpen.value
        isSettingsDrawerOpen.value = false
        isTrashScreenOpen.value = false
        isArchiveScreenOpen.value = false
        isAccountScreenOpen.value = false
        isAudioPlayerScreenOpen.value = false
        isThemesScreenOpen.value = true
    }

    fun closeThemesScreen() {
        isThemesScreenOpen.value = false
        if (openedThemesFromSettings) {
            openedThemesFromSettings = false
            requestedSettingsPage.value = "APPEARANCE"
            isSettingsDrawerOpen.value = true
        }
    }

    fun clearRequestedSettingsPage() {
        requestedSettingsPage.value = null
    }

    fun openTrashScreen(fromSettings: Boolean = false) {
        openedTrashFromSettings = fromSettings || isSettingsDrawerOpen.value
        isSettingsDrawerOpen.value = false
        isThemesScreenOpen.value = false
        isArchiveScreenOpen.value = false
        isAccountScreenOpen.value = false
        isAudioPlayerScreenOpen.value = false
        isTrashScreenOpen.value = true
        viewModelScope.launch {
            purgeExpiredTrashNotes()
        }
    }

    fun closeTrashScreen() {
        isTrashScreenOpen.value = false
        if (openedTrashFromSettings) {
            openedTrashFromSettings = false
            isSettingsDrawerOpen.value = true
        }
    }

    fun openArchiveScreen(fromSettings: Boolean = false) {
        openedArchiveFromSettings = fromSettings || isSettingsDrawerOpen.value
        isSettingsDrawerOpen.value = false
        isThemesScreenOpen.value = false
        isTrashScreenOpen.value = false
        isAccountScreenOpen.value = false
        isAudioPlayerScreenOpen.value = false
        isArchiveScreenOpen.value = true
    }

    fun closeArchiveScreen() {
        isArchiveScreenOpen.value = false
        if (openedArchiveFromSettings) {
            openedArchiveFromSettings = false
            isSettingsDrawerOpen.value = true
        }
    }

    fun openAccountScreen() {
        isThemesScreenOpen.value = false
        isTrashScreenOpen.value = false
        isArchiveScreenOpen.value = false
        isAudioPlayerScreenOpen.value = false
        isAccountScreenOpen.value = false
        requestedSettingsPage.value = "ACCOUNT_CLOUD"
        isSettingsDrawerOpen.value = true
    }

    fun closeAccountScreen() {
        isAccountScreenOpen.value = false
    }

    fun openAudioPlayerScreen() {
        isSettingsDrawerOpen.value = false
        isThemesScreenOpen.value = false
        isTrashScreenOpen.value = false
        isArchiveScreenOpen.value = false
        isAccountScreenOpen.value = false
        isAudioPlayerScreenOpen.value = true
    }

    fun closeAudioPlayerScreen() {
        isAudioPlayerScreenOpen.value = false
    }

    private fun syncProfileNameFromUser(displayName: String?, forceIfGoogleNaming: Boolean = false) {
        if (!forceIfGoogleNaming && (profileFirstName.value.isNotBlank() || profileLastName.value.isNotBlank())) return
        val raw = displayName?.trim().orEmpty()
        if (raw.isBlank()) return
        val parts = raw.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val first = parts.firstOrNull().orEmpty()
        val last = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
        profileFirstName.value = first
        profileLastName.value = last
        prefs.edit()
            .putString("profile_first_name", first)
            .putString("profile_last_name", last)
            .apply()
    }

    fun setUseGoogleAccountName(enabled: Boolean) {
        useGoogleAccountName.value = enabled
        prefs.edit().putBoolean("use_google_account_name", enabled).apply()
        if (enabled) {
            val googleName = currentUser.value?.displayName?.trim().orEmpty()
            if (googleName.isNotBlank()) {
                syncProfileNameFromUser(googleName, forceIfGoogleNaming = true)
                showSnackbar(
                    if (isArabic.value) "تم تفعيل التسمية حسب حساب Google ✨"
                    else "Using Google account name ✨"
                )
            } else {
                showSnackbar(
                    if (isArabic.value) "تم تفعيل التسمية التلقائية من Google ✨"
                    else "Google account naming enabled ✨"
                )
            }
        } else {
            showSnackbar(
                if (isArabic.value) "يمكنك الآن كتابة الاسم واللقب المخصص"
                else "Custom Name & Surname enabled"
            )
        }
    }

    fun updateProfileName(firstName: String, lastName: String) {
        val cleanFirst = firstName.trim()
        val cleanLast = lastName.trim()
        useGoogleAccountName.value = false
        profileFirstName.value = cleanFirst
        profileLastName.value = cleanLast
        prefs.edit()
            .putBoolean("use_google_account_name", false)
            .putString("profile_first_name", cleanFirst)
            .putString("profile_last_name", cleanLast)
            .apply()

        val fullName = listOf(cleanFirst, cleanLast).filter { it.isNotBlank() }.joinToString(" ")
        if (currentUser.value != null && fullName.isNotBlank()) {
            viewModelScope.launch {
                FirebaseSyncRepository.updateUserDisplayName(getApplication(), fullName)
                    .onSuccess { updatedUser ->
                        currentUser.value = updatedUser
                    }
            }
        }
        showSnackbar(if (isArabic.value) "تم حفظ الاسم واللقب بنجاح ✨" else "Profile name updated ✨")
    }

    fun selectPresetAvatar(presetId: String) {
        val choice = "preset:$presetId"
        profileAvatarChoice.value = choice
        prefs.edit().putString("profile_avatar_choice", choice).apply()
        showSnackbar(if (isArabic.value) "تم تحديث صورة الأفاتار ✨" else "Avatar updated ✨")
    }

    fun useGoogleProfilePhoto() {
        profileAvatarChoice.value = "google"
        prefs.edit().putString("profile_avatar_choice", "google").apply()
        showSnackbar(if (isArabic.value) "تم تفعيل صورة حساب Google ✨" else "Google profile photo enabled ✨")
    }

    fun resetProfileAvatarToInitials() {
        profileAvatarChoice.value = "initials"
        prefs.edit().putString("profile_avatar_choice", "initials").apply()
        showSnackbar(if (isArabic.value) "تم تعيين الأحرف الأولى للصورة ✨" else "Monogram avatar enabled ✨")
    }

    fun selectCustomProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    val app = getApplication<Application>()
                    val dir = java.io.File(app.filesDir, "profile_avatars").apply {
                        if (!exists()) mkdirs()
                    }
                    // Clean up older custom avatar files
                    dir.listFiles()?.forEach { oldFile ->
                        if (oldFile.name.startsWith("custom_avatar_")) {
                            oldFile.delete()
                        }
                    }

                    val outFile = java.io.File(dir, "custom_avatar_${System.currentTimeMillis()}.jpg")
                    val rawBytes = app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@runCatching null

                    val decoded = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                    if (decoded != null) {
                        val maxDim = 512
                        val w = decoded.width
                        val h = decoded.height
                        val scale = if (w > maxDim || h > maxDim) {
                            maxDim.toFloat() / maxOf(w, h).toFloat()
                        } else {
                            1f
                        }
                        val finalBmp = if (scale < 1f) {
                            android.graphics.Bitmap.createScaledBitmap(
                                decoded,
                                (w * scale).toInt().coerceAtLeast(1),
                                (h * scale).toInt().coerceAtLeast(1),
                                true
                            )
                        } else {
                            decoded
                        }
                        outFile.outputStream().use { stream ->
                            finalBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, stream)
                        }
                        if (finalBmp !== decoded) {
                            finalBmp.recycle()
                        }
                        decoded.recycle()
                    } else {
                        outFile.writeBytes(rawBytes)
                    }
                    outFile.absolutePath
                }.getOrNull()
            }

            if (!savedPath.isNullOrBlank()) {
                val choice = "custom:$savedPath"
                profileAvatarChoice.value = choice
                prefs.edit().putString("profile_avatar_choice", choice).apply()
                showSnackbar(
                    if (isArabic.value) "تم تعيين صورة البروفايل بنجاح ✨"
                    else "Profile picture updated ✨"
                )
            } else {
                showSnackbar(
                    if (isArabic.value) "تعذر قراءة الصورة المختارة"
                    else "Unable to load selected image"
                )
            }
        }
    }

    fun toggleAutoDriveBackup(enabled: Boolean) {
        isAutoDriveBackupEnabled.value = enabled
        prefs.edit().putBoolean(DriveAutoBackupWorker.KEY_AUTO_BACKUP_24H, enabled).apply()
        if (enabled) {
            DriveAutoBackupWorker.scheduleEvery24Hours(getApplication())
            showSnackbar(
                if (isArabic.value) "تم تفعيل النسخ الاحتياطي التلقائي كل 24 ساعة"
                else "24h automatic backup enabled"
            )
        } else {
            DriveAutoBackupWorker.cancelScheduledBackup(getApplication())
            showSnackbar(
                if (isArabic.value) "تم إيقاف النسخ الاحتياطي التلقائي"
                else "Automatic backup paused"
            )
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        val arabicFlag = language == AppLanguage.ARABIC
        appLanguage.value = language
        isArabic.value = arabicFlag
        prefs.edit()
            .putString("app_language_code", language.code)
            .putBoolean("is_arabic", arabicFlag)
            .apply()
        persistSettingsSnapshot(syncToCloud = true)
    }

    fun toggleLanguage() {
        val next = if (appLanguage.value == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
        setAppLanguage(next)
    }

    fun setViewMode(mode: NotesViewMode) {
        viewMode.value = mode
        isGridView.value = mode.isGridLike
        prefs.edit()
            .putString("notes_view_mode", mode.id)
            .putBoolean("is_grid_view", mode.isGridLike)
            .apply()
        persistSettingsSnapshot(syncToCloud = true)
    }

    fun toggleViewMode() {
        setViewMode(viewMode.value.next())
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
        cloudSyncEnabled = true,
        selectedThemeId = selectedThemeId.value,
        appLanguageCode = appLanguage.value.code,
        nonScreenshotEnabled = isScreenshotProtectionEnabled.value,
        coloredContainerBorders = isColoredBordersEnabled.value,
        notesViewMode = viewMode.value.id,
        isPremium = isPremium.value
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
        val resolvedTheme = AppThemeRegistry.resolve(s.selectedThemeId, s.isDarkMode)
        val resolvedLanguage = AppLanguage.fromCode(s.appLanguageCode, fallbackArabic = s.isArabic)
        val resolvedViewMode = NotesViewMode.fromId(s.notesViewMode, fallbackGrid = s.isGridView)
        val arabicFlag = resolvedLanguage == AppLanguage.ARABIC
        val resolvedPremium = isPremium.value || s.isPremium

        isDarkMode.value = s.isDarkMode
        appLanguage.value = resolvedLanguage
        isArabic.value = arabicFlag
        viewMode.value = resolvedViewMode
        isGridView.value = resolvedViewMode.isGridLike
        isBiometricEnabled.value = s.isBiometricEnabled
        isScreenshotProtectionEnabled.value = s.nonScreenshotEnabled
        isColoredBordersEnabled.value = s.coloredContainerBorders
        selectedCategory.value = s.selectedCategory
        selectedThemeId.value = resolvedTheme.id
        isPremium.value = resolvedPremium

        prefs.edit()
            .putBoolean("is_dark_mode", s.isDarkMode)
            .putBoolean("is_arabic", arabicFlag)
            .putString("app_language_code", resolvedLanguage.code)
            .putString("notes_view_mode", resolvedViewMode.id)
            .putBoolean("is_grid_view", resolvedViewMode.isGridLike)
            .putBoolean("biometric_enabled", s.isBiometricEnabled)
            .putBoolean("non_screenshot_enabled", s.nonScreenshotEnabled)
            .putBoolean("colored_container_borders", s.coloredContainerBorders)
            .putString("selected_category", s.selectedCategory)
            .putString("selected_theme_id", resolvedTheme.id)
            .putBoolean("is_premium", resolvedPremium)
            .apply()

        persistSettingsSnapshot(syncToCloud = !fromRemote)
    }

    fun openPremiumUpgradeDialog() {
        showPremiumUpgradeDialog.value = true
    }

    fun closePremiumUpgradeDialog() {
        showPremiumUpgradeDialog.value = false
    }

    fun redeemPremiumCode(rawCode: String): Boolean {
        if (rawCode.trim() != PREMIUM_REDEEM_CODE) {
            return false
        }
        isPremium.value = true
        prefs.edit().putBoolean("is_premium", true).apply()
        persistSettingsSnapshot(syncToCloud = true)
        showSnackbar(
            if (isArabic.value) "تمت ترقية حسابك إلى وضع Premium بنجاح 👑✨"
            else "Account upgraded to Premium successfully 👑✨"
        )
        return true
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
        pagesJson: String = "",
        category: String,
        colorPreset: String,
        backgroundId: String = "none",
        customColorArgb: Long = 0L,
        isPinned: Boolean,
        isFavorite: Boolean,
        checklist: List<ChecklistItem>,
        audioDurationSec: Int,
        audioFilePath: String = "",
        audioByteSize: Long = 0L
    ) {
        if (editingNote.value == null) return
        val activeChecklist = checklist.filter { it.text.isNotEmpty() }

        // Only skip when completely empty (never touch non-empty literal whitespace)
        if (title.isEmpty() && !NotePagesCodec.hasAnyContent(content, pagesJson) && activeChecklist.isEmpty() && audioDurationSec == 0 && audioFilePath.isBlank()) {
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
                val resolvedAudioSize = AudioCloudPolicy.effectiveAudioByteSize(audioByteSize, audioFilePath)
                val updatedNote = current.copy(
                    title = title,
                    content = content,
                    pagesJson = pagesJson,
                    category = category.ifBlank { if (isArabic.value) "عام" else "General" },
                    colorPreset = colorPreset,
                    backgroundId = backgroundId,
                    customColorArgb = customColorArgb,
                    isPinned = isPinned,
                    isFavorite = isFavorite,
                    checklistJson = ChecklistItem.listToJson(activeChecklist),
                    audioDurationSec = audioDurationSec,
                    audioFilePath = audioFilePath,
                    audioByteSize = resolvedAudioSize,
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
        pagesJson: String = "",
        category: String,
        colorPreset: String,
        backgroundId: String = "none",
        customColorArgb: Long = 0L,
        isPinned: Boolean,
        isFavorite: Boolean,
        checklist: List<ChecklistItem>,
        audioDurationSec: Int,
        audioFilePath: String = "",
        audioByteSize: Long = 0L
    ) {
        if (editingNote.value == null) return
        val activeChecklist = checklist.filter { it.text.isNotEmpty() }

        if (title.isEmpty() && !NotePagesCodec.hasAnyContent(content, pagesJson) && activeChecklist.isEmpty() && audioDurationSec == 0 && audioFilePath.isBlank()) {
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
                val resolvedAudioSize = AudioCloudPolicy.effectiveAudioByteSize(audioByteSize, audioFilePath)
                val updatedNote = current.copy(
                    title = title,
                    content = content,
                    pagesJson = pagesJson,
                    category = category.ifBlank { if (isArabic.value) "عام" else "General" },
                    colorPreset = colorPreset,
                    backgroundId = backgroundId,
                    customColorArgb = customColorArgb,
                    isPinned = isPinned,
                    isFavorite = isFavorite,
                    checklistJson = ChecklistItem.listToJson(activeChecklist),
                    audioDurationSec = audioDurationSec,
                    audioFilePath = audioFilePath,
                    audioByteSize = resolvedAudioSize,
                    sortOrder = targetSortOrder,
                    updatedAt = System.currentTimeMillis(),
                    syncId = ensuredSyncId
                )
                val id = repository.insertOrUpdate(updatedNote)
                val saved = if (current.id == 0L && id > 0) updatedNote.copy(id = id) else updatedNote
                syncNoteUpsertIfSignedIn(saved)
                closeEditor()
                if (!AudioCloudPolicy.canStoreNoteAudioInCloud(saved)) {
                    showSnackbar(
                        if (isArabic.value) "حجم التسجيل يتعدى 900 م.ب — محفوظ محلياً فقط ولن يُخزَّن سحابياً"
                        else "Audio exceeds 900 MB — saved locally only, excluded from cloud"
                    )
                } else {
                    showSnackbar(if (isArabic.value) "تم حفظ الملاحظة حرفياً بنجاح ✨" else "Note saved verbatim ✨")
                }
            }
        }
    }

    fun createAudioRecordingNote(
        title: String,
        durationSec: Int,
        filePath: String,
        byteSize: Long
    ) {
        viewModelScope.launch {
            val resolvedSize = AudioCloudPolicy.effectiveAudioByteSize(byteSize, filePath)
            val now = System.currentTimeMillis()
            val minOrder = withContext(Dispatchers.IO) { repository.getMinSortOrder() }
            val note = NoteEntity(
                title = title.ifBlank { if (isArabic.value) "تسجيل صوتي" else "Voice Recording" },
                content = "",
                category = if (isArabic.value) "صوتي" else "Audio",
                colorPreset = GlassTint.ROSE.id,
                audioDurationSec = durationSec.coerceAtLeast(1),
                audioFilePath = filePath,
                audioByteSize = resolvedSize,
                createdAt = now,
                updatedAt = now,
                syncId = UUID.randomUUID().toString(),
                sortOrder = minOrder - 1000L
            )
            val id = repository.insertOrUpdate(note)
            val saved = if (id > 0) note.copy(id = id) else note
            syncNoteUpsertIfSignedIn(saved)
            if (AudioCloudPolicy.isAudioOverCloudLimit(resolvedSize)) {
                showSnackbar(
                    if (isArabic.value) "التسجيل أكبر من 900 ميغابايت — تم حفظه محلياً ولن يُخزَّن سحابياً"
                    else "Audio > 900 MB — saved locally only and excluded from cloud"
                )
            } else {
                showSnackbar(if (isArabic.value) "تم حفظ التسجيل الصوتي بنجاح 🎙️" else "Voice recording saved 🎙️")
            }
        }
    }

    fun importAudioFileAsNote(uri: Uri) {
        viewModelScope.launch {
            try {
                val imported = AudioStorageManager.importAudioFromUri(getApplication(), uri)
                val now = System.currentTimeMillis()
                val minOrder = withContext(Dispatchers.IO) { repository.getMinSortOrder() }
                val note = NoteEntity(
                    title = imported.displayTitle,
                    content = "",
                    category = if (isArabic.value) "صوتي" else "Audio",
                    colorPreset = GlassTint.ROSE.id,
                    audioDurationSec = imported.durationSec,
                    audioFilePath = imported.filePathOrUri,
                    audioByteSize = imported.byteSize,
                    createdAt = now,
                    updatedAt = now,
                    syncId = UUID.randomUUID().toString(),
                    sortOrder = minOrder - 1000L
                )
                val id = repository.insertOrUpdate(note)
                val saved = if (id > 0) note.copy(id = id) else note
                syncNoteUpsertIfSignedIn(saved)
                if (imported.isLocalOnlyOverLimit) {
                    showSnackbar(
                        if (isArabic.value) "التسجيل يتعدى 900 ميغابايت — محفوظ محلياً فقط ولن يُخزَّن سحابياً"
                        else "Recording exceeds 900 MB — stored locally only, not in cloud"
                    )
                } else {
                    showSnackbar(
                        if (isArabic.value) "تمت إضافة التسجيل الصوتي إلى القارئ 🎧"
                        else "Audio recording added to player 🎧"
                    )
                }
            } catch (e: Exception) {
                showSnackbar(
                    if (isArabic.value) "تعذر استيراد الملف الصوتي: ${e.localizedMessage}"
                    else "Failed to import audio: ${e.localizedMessage}"
                )
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

            val partitionedGlobal = mergedGlobal.filter { it.isPinned } +
                mergedGlobal.filter { !it.isPinned }

            val hasDuplicateSortOrders = allNotes.map { it.sortOrder }.distinct().size != allNotes.size
            val now = System.currentTimeMillis()
            val changedNotes = mutableListOf<NoteEntity>()
            partitionedGlobal.forEachIndexed { index, note ->
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
                val now = System.currentTimeMillis()
                val trashed = current.copy(
                    isDeleted = true,
                    deletedAt = now,
                    isArchived = false,
                    isPinned = false,
                    updatedAt = now,
                    syncId = current.syncId.ifBlank { UUID.randomUUID().toString() }
                )
                repository.insertOrUpdate(trashed)
                syncNoteUpsertIfSignedIn(trashed)
                showSnackbar(
                    if (isArabic.value) "تم النقل إلى سلة المهملات (يُحذف تلقائياً خلال 30 يوم)"
                    else "Moved to Trash (auto-deletes in 30 days)"
                )
            }
            closeEditor()
        }
    }

    fun deleteNoteDirectly(note: NoteEntity) {
        val now = System.currentTimeMillis()
        val trashed = note.copy(
            isDeleted = true,
            deletedAt = now,
            isArchived = false,
            isPinned = false,
            updatedAt = now,
            syncId = note.syncId.ifBlank { UUID.randomUUID().toString() }
        )
        viewModelScope.launch {
            repository.insertOrUpdate(trashed)
            syncNoteUpsertIfSignedIn(trashed)
            showSnackbar(
                if (isArabic.value) "تم النقل إلى سلة المهملات (يُحذف تلقائياً خلال 30 يوم)"
                else "Moved to Trash (auto-deletes in 30 days)"
            )
        }
    }

    fun restoreFromTrash(note: NoteEntity) {
        val restored = note.copy(
            isDeleted = false,
            deletedAt = 0L,
            updatedAt = System.currentTimeMillis(),
            syncId = note.syncId.ifBlank { UUID.randomUUID().toString() }
        )
        viewModelScope.launch {
            repository.insertOrUpdate(restored)
            syncNoteUpsertIfSignedIn(restored)
            showSnackbar(if (isArabic.value) "تمت استعادة الملاحظة ✨" else "Note restored ✨")
        }
    }

    fun deletePermanentlyFromTrash(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            syncNoteDeleteIfSignedIn(note.syncId)
            showSnackbar(if (isArabic.value) "تم الحذف النهائي" else "Permanently deleted")
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val trashed = withContext(Dispatchers.IO) {
                val items = repository.getTrashedNotesOnce()
                repository.emptyTrash()
                items
            }
            val uid = currentUser.value?.uid
            if (uid != null) {
                withContext(Dispatchers.IO) {
                    trashed.forEach { item ->
                        if (item.syncId.isNotBlank()) {
                            runCatching {
                                FirebaseSyncRepository.deleteNote(getApplication(), uid, item.syncId)
                            }
                        }
                    }
                }
            }
            showSnackbar(if (isArabic.value) "تم تفريغ سلة المهملات نهائياً" else "Trash emptied")
        }
    }

    fun toggleArchive(note: NoteEntity) {
        val nextArchived = !note.isArchived
        val updated = note.copy(
            isArchived = nextArchived,
            isDeleted = false,
            deletedAt = 0L,
            isPinned = if (nextArchived) false else note.isPinned,
            updatedAt = System.currentTimeMillis(),
            syncId = note.syncId.ifBlank { UUID.randomUUID().toString() }
        )
        viewModelScope.launch {
            repository.insertOrUpdate(updated)
            syncNoteUpsertIfSignedIn(updated)
            if (editingNote.value?.id == note.id) {
                closeEditor()
            }
            showSnackbar(
                if (nextArchived) {
                    if (isArabic.value) "تمت أرشفة الملاحظة 📦" else "Note archived 📦"
                } else {
                    if (isArabic.value) "تم إلغاء أرشفة الملاحظة ✨" else "Note unarchived ✨"
                }
            )
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

    fun clearAuthMessages() {
        authErrorMessage.value = null
        authInfoMessage.value = null
    }

    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            authErrorMessage.value = null
            authInfoMessage.value = null
            isAuthBusy.value = true
            val result = FirebaseSyncRepository.signInWithGoogleIdToken(getApplication(), idToken)
            isAuthBusy.value = false
            result.onSuccess { user ->
                isGuestMode.value = false
                useGoogleAccountName.value = true
                prefs.edit()
                    .putBoolean("is_guest_mode", false)
                    .putBoolean("use_google_account_name", true)
                    .apply()
                syncProfileNameFromUser(user.displayName, forceIfGoogleNaming = true)
                currentUser.value = user
                showAuthDialog.value = false
                showSnackbar(
                    if (isArabic.value) "تم إنشاء/ربط حساب Google وتفعيل المزامنة ☁️✨"
                    else "Google account connected — Cloud sync active ☁️✨"
                )
            }.onFailure { err ->
                val msg = if (isArabic.value) {
                    "فشل الدخول عبر Google: ${err.localizedMessage ?: "خطأ غير متوقع"}"
                } else {
                    "Google Sign-In failed: ${err.localizedMessage ?: "Unexpected error"}"
                }
                authErrorMessage.value = msg
                showSnackbar(msg)
            }
        }
    }

    fun signInWithEmailPassword(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            authErrorMessage.value = if (isArabic.value) {
                "يرجى إدخال بريد إلكتروني صحيح"
            } else {
                "Please enter a valid email address"
            }
            return
        }
        if (password.length < 6) {
            authErrorMessage.value = if (isArabic.value) {
                "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
            } else {
                "Password must be at least 6 characters"
            }
            return
        }

        viewModelScope.launch {
            authErrorMessage.value = null
            authInfoMessage.value = null
            isAuthBusy.value = true
            val result = FirebaseSyncRepository.signInWithEmailPassword(getApplication(), trimmedEmail, password)
            isAuthBusy.value = false
            result.onSuccess { user ->
                isGuestMode.value = false
                prefs.edit().putBoolean("is_guest_mode", false).apply()
                currentUser.value = user
                showAuthDialog.value = false
                showSnackbar(
                    if (isArabic.value) "تم تسجيل الدخول ومزامنة السحابة بنجاح ✨"
                    else "Signed in & Cloud sync active ✨"
                )
            }.onFailure { err ->
                authErrorMessage.value = mapFirebaseAuthError(err)
            }
        }
    }

    fun signUpWithEmailPassword(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        val trimmedUsername = username.trim()
        if (trimmedUsername.isEmpty()) {
            authErrorMessage.value = if (isArabic.value) {
                "يرجى كتابة اسم المستخدم (حقل إجباري لإنشاء الحساب)"
            } else {
                "Username is required to create an account"
            }
            return
        }
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            authErrorMessage.value = if (isArabic.value) {
                "يرجى إدخال بريد إلكتروني صحيح"
            } else {
                "Please enter a valid email address"
            }
            return
        }
        if (password.length < 6) {
            authErrorMessage.value = if (isArabic.value) {
                "كلمة المرور يجب أن تتكون من 6 أحرف على الأقل"
            } else {
                "Password must be at least 6 characters"
            }
            return
        }
        if (password != confirmPassword) {
            authErrorMessage.value = if (isArabic.value) {
                "كلمتا المرور غير متطابقتين"
            } else {
                "Passwords do not match"
            }
            return
        }

        viewModelScope.launch {
            authErrorMessage.value = null
            authInfoMessage.value = null
            isAuthBusy.value = true
            val result = FirebaseSyncRepository.signUpWithEmailPassword(getApplication(), trimmedEmail, password)
            if (result.isSuccess) {
                val createdUser = result.getOrNull()!!
                useGoogleAccountName.value = false
                prefs.edit()
                    .putBoolean("is_guest_mode", false)
                    .putBoolean("use_google_account_name", false)
                    .apply()
                syncProfileNameFromUser(trimmedUsername, forceIfGoogleNaming = true)

                val renamedUser = FirebaseSyncRepository.updateUserDisplayName(getApplication(), trimmedUsername)
                    .getOrElse { createdUser }

                isAuthBusy.value = false
                isGuestMode.value = false
                currentUser.value = renamedUser
                showAuthDialog.value = false
                showSnackbar(
                    if (isArabic.value) "تم إنشاء الحساب وتفعيل المزامنة السحابية ✨"
                    else "Account created & Cloud sync enabled ✨"
                )
            } else {
                isAuthBusy.value = false
                val err = result.exceptionOrNull() ?: Exception("Sign-up failed")
                authErrorMessage.value = mapFirebaseAuthError(err)
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            authErrorMessage.value = if (isArabic.value) {
                "أدخل بريدك الإلكتروني لإرسال رابط استعادة كلمة المرور"
            } else {
                "Enter a valid email to receive a password reset link"
            }
            return
        }

        viewModelScope.launch {
            authErrorMessage.value = null
            authInfoMessage.value = null
            isAuthBusy.value = true
            val result = FirebaseSyncRepository.sendPasswordResetEmail(getApplication(), trimmedEmail)
            isAuthBusy.value = false
            result.onSuccess {
                authInfoMessage.value = if (isArabic.value) {
                    "تم إرسال رابط إعادة تعيين كلمة المرور إلى $trimmedEmail 📩"
                } else {
                    "Password reset link sent to $trimmedEmail 📩"
                }
            }.onFailure { err ->
                authErrorMessage.value = mapFirebaseAuthError(err)
            }
        }
    }

    private fun mapFirebaseAuthError(error: Throwable): String {
        val msg = error.message.orEmpty()
        return when {
            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                msg.contains("wrong password", ignoreCase = true) ||
                msg.contains("credential is incorrect", ignoreCase = true) ||
                msg.contains("no user record", ignoreCase = true) ->
                if (isArabic.value) "البريد الإلكتروني أو كلمة المرور غير صحيحة" else "Incorrect email or password"
            msg.contains("email address is already in use", ignoreCase = true) ||
                msg.contains("EMAIL_EXISTS", ignoreCase = true) ->
                if (isArabic.value) "هذا البريد الإلكتروني مسجل مسبقاً، يمكنك تسجيل الدخول مباشرة" else "Email is already registered"
            msg.contains("badly formatted", ignoreCase = true) ||
                msg.contains("INVALID_EMAIL", ignoreCase = true) ->
                if (isArabic.value) "صيغة البريد الإلكتروني غير صالحة" else "Invalid email format"
            msg.contains("network", ignoreCase = true) ->
                if (isArabic.value) "تحقق من اتصالك بالإنترنت وحاول مجدداً" else "Network error, please check your connection"
            msg.contains("WEAK_PASSWORD", ignoreCase = true) ->
                if (isArabic.value) "كلمة المرور ضعيفة جداً، اختر كلمة مرور أقوى" else "Password is too weak"
            else ->
                if (isArabic.value) "تعذر إتمام العملية: ${error.localizedMessage ?: "خطأ غير متوقع"}"
                else error.localizedMessage ?: "Authentication error"
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
                val allowedInCloud = AudioCloudPolicy.canStoreVaultItemInCloud(item)
                if (allowedInCloud) {
                    currentUser.value?.uid?.let { uid ->
                        launch(Dispatchers.IO) {
                            FirebaseSyncRepository.upsertVaultItemIfAbsent(getApplication(), uid, item)
                        }
                    }
                    showSnackbar(
                        if (isArabic.value) "تم حفظ الملف حرفياً في ذاكرة الهاتف والـ Vault 🔒"
                        else "File saved verbatim in phone storage & Vault 🔒"
                    )
                } else {
                    showSnackbar(
                        if (isArabic.value) "التسجيل يتعدى 900 م.ب — محفوظ في الهاتف فقط ولن يُخزَّن سحابياً 🔒"
                        else "Audio > 900 MB — saved locally only, excluded from cloud 🔒"
                    )
                }
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

    fun setScreenshotProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("non_screenshot_enabled", enabled).apply()
        isScreenshotProtectionEnabled.value = enabled
        persistSettingsSnapshot(syncToCloud = true)
        showSnackbar(
            translateText(
                appLanguage.value,
                ar = if (enabled) "تم تفعيل حماية منع لقطة الشاشة 🔒" else "تم إيقاف منع لقطة الشاشة",
                fr = if (enabled) "Protection anti-capture activée 🔒" else "Protection anti-capture désactivée",
                en = if (enabled) "Screenshot protection enabled 🔒" else "Screenshot protection disabled",
                tzm = if (enabled) "Ammesten mgal tuṭṭfa n ugdil yermed 🔒" else "Ammesten mgal tuṭṭfa n ugdil yensa"
            )
        )
    }

    private fun hashPasskey(raw: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val salted = "fluffy_vault_passkey_v1::$raw"
        return digest.digest(salted.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    fun setVaultPasskey(passkey: String) {
        val clean = passkey.trim()
        if (clean.length < 4) return
        val hashed = hashPasskey(clean)
        prefs.edit()
            .putString("vault_passkey_hash", hashed)
            .putBoolean("vault_lock_enabled", true)
            .apply()
        hasVaultPasskey.value = true
        isVaultLockEnabled.value = true
        showSnackbar(
            translateText(
                appLanguage.value,
                ar = "تم تفعيل قفل الـ Vault بالبصمة و Passkey 🔐",
                fr = "Verrouillage Vault (Empreinte & Passkey) activé 🔐",
                en = "Vault Biometric & Passkey lock enabled 🔐",
                tzm = "Asekkeṛ n Vault s uḍad & Passkey yermed 🔐"
            )
        )
    }

    fun setVaultLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vault_lock_enabled", enabled).apply()
        isVaultLockEnabled.value = enabled
        if (!enabled) {
            isVaultUnlockedForSession.value = true
        }
        showSnackbar(
            translateText(
                appLanguage.value,
                ar = if (enabled) "تم تفعيل قفل الـ Vault 🔐" else "تم إيقاف قفل الـ Vault",
                fr = if (enabled) "Verrouillage Vault activé 🔐" else "Verrouillage Vault désactivé",
                en = if (enabled) "Vault lock enabled 🔐" else "Vault lock disabled",
                tzm = if (enabled) "Asekkeṛ n Vault yermed 🔐" else "Asekkeṛ n Vault yensa"
            )
        )
    }

    fun verifyVaultPasskey(input: String): Boolean {
        val stored = prefs.getString("vault_passkey_hash", "") ?: ""
        if (stored.isBlank()) return false
        val matches = stored == hashPasskey(input.trim())
        if (matches) {
            isVaultUnlockedForSession.value = true
        }
        return matches
    }

    fun markVaultUnlocked() {
        isVaultUnlockedForSession.value = true
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
        isVaultUnlockedForSession.value = !isVaultLockEnabled.value
        isVaultOpen.value = true
    }

    fun closeVault() {
        isVaultOpen.value = false
        isVaultUnlockedForSession.value = false
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

    companion object {
        const val TRASH_RETENTION_MILLIS = 30L * 24L * 60L * 60L * 1000L
        const val PREMIUM_REDEEM_CODE = "Charfeddine23a"
    }
}
