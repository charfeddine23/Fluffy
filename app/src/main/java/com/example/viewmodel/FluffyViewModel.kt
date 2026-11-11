package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FluffyDatabase
import com.example.data.model.ChecklistItem
import com.example.data.model.GlassTint
import com.example.data.model.NoteEntity
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FluffyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    init {
        val db = FluffyDatabase.getDatabase(application)
        repository = NoteRepository(db.noteDao())
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
        }
    }

    private val prefs = application.getSharedPreferences("fluffy_prefs", android.content.Context.MODE_PRIVATE)

    // App Preferences
    val isDarkMode = MutableStateFlow(true)
    val isArabic = MutableStateFlow(true)
    val isGridView = MutableStateFlow(true)

    // Biometric Authentication & App Lock
    val isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))
    val isAppLocked = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))

    // Settings Drawer
    val isSettingsDrawerOpen = MutableStateFlow(false)

    // Filter and Search
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("ALL") // ALL, PINNED, FAVORITES, or specific tag

    // Active Editor & Sheets
    val editingNote = MutableStateFlow<NoteEntity?>(null)
    val isCreatingNew = MutableStateFlow(false)
    val showStatsSheet = MutableStateFlow(false)
    val showVoiceRecorder = MutableStateFlow(false)
    val snackbarMessage = MutableStateFlow<String?>(null)

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
        isDarkMode.value = !isDarkMode.value
    }

    fun toggleLanguage() {
        isArabic.value = !isArabic.value
    }

    fun toggleViewMode() {
        isGridView.value = !isGridView.value
    }

    fun setSearch(query: String) {
        searchQuery.value = query
    }

    fun selectCategory(cat: String) {
        selectedCategory.value = cat
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
        if (title.isBlank() && content.isBlank() && checklist.isEmpty() && audioDurationSec == 0) {
            closeEditor()
            return
        }

        val updatedNote = current.copy(
            title = title.trim(),
            content = content.trim(),
            category = category.ifBlank { if (isArabic.value) "عام" else "General" },
            colorPreset = colorPreset,
            isPinned = isPinned,
            isFavorite = isFavorite,
            checklistJson = ChecklistItem.listToJson(checklist.filter { it.text.isNotBlank() }),
            audioDurationSec = audioDurationSec,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.insertOrUpdate(updatedNote)
            closeEditor()
            showSnackbar(if (isArabic.value) "تم حفظ الملاحظة بنجاح ✨" else "Note saved successfully ✨")
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

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        isBiometricEnabled.value = enabled
        if (!enabled) {
            isAppLocked.value = false
        }
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

    fun showSnackbar(message: String) {
        snackbarMessage.value = message
    }

    fun clearSnackbar() {
        snackbarMessage.value = null
    }
}
