package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.GlassTint
import com.example.data.model.NoteEntity
import com.example.ui.components.CategoryChips
import com.example.ui.components.FluffyBottomBar
import com.example.ui.components.FluffyTopBar
import com.example.ui.components.NoteCard
import com.example.ui.components.SettingsDrawer
import com.example.ui.components.StatsBottomSheet
import com.example.ui.components.VoiceRecorderDialog
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
import com.example.update.UpdateUiState
import com.example.viewmodel.FluffyViewModel

@Composable
fun FluffyHomeScreen(
    viewModel: FluffyViewModel,
    onToggleBiometricAuth: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark by viewModel.isDarkMode.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isSettingsDrawerOpen by viewModel.isSettingsDrawerOpen.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val notes by viewModel.filteredNotes.collectAsState()
    val showStats by viewModel.showStatsSheet.collectAsState()
    val showVoiceRecorder by viewModel.showVoiceRecorder.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    FrostedBackdrop(isDark = isDark) {
        var dragStartX by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isArabic) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragStartX = offset.x
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            val screenWidth = size.width
                            if (isArabic) {
                                // In Arabic: swipe from right edge towards left
                                val isRightEdge = dragStartX > (screenWidth - 380f) || change.position.x > (screenWidth - 380f)
                                if (isRightEdge && dragAmount < -18f) {
                                    viewModel.openSettingsDrawer()
                                }
                            } else {
                                // In English: swipe from left edge towards right
                                val isLeftEdge = dragStartX < 380f || change.position.x < 380f
                                if (isLeftEdge && dragAmount > 18f) {
                                    viewModel.openSettingsDrawer()
                                }
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // Top Liquid Glass Bar
                FluffyTopBar(
                    isDark = isDark,
                    isArabic = isArabic,
                    isGridView = isGridView,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearch(it) },
                    onToggleTheme = { viewModel.toggleDarkMode() },
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onToggleViewMode = { viewModel.toggleViewMode() },
                    onOpenStats = { viewModel.showStatsSheet.value = true },
                    onOpenSettings = { viewModel.openSettingsDrawer() }
                )

                // Category Chips
                CategoryChips(
                    isDark = isDark,
                    isArabic = isArabic,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { viewModel.selectCategory(it) }
                )

                // Notes Content: Staggered Grid, List, or clean empty space
                if (notes.isEmpty()) {
                    if (searchQuery.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "لا توجد نتائج مطابقة" else "No matching notes found",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    } else {
                        // Completely empty clean canvas with no placeholder, frame, or text
                        Box(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    if (isGridView) {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("notes_grid"),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 100.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = 12.dp
                        ) {
                            items(notes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    isDark = isDark,
                                    isArabic = isArabic,
                                    onClick = { viewModel.openExistingNote(note) },
                                    onTogglePin = { viewModel.togglePin(note) },
                                    onToggleFavorite = { viewModel.toggleFavorite(note) },
                                    onToggleChecklistItem = { idx ->
                                        viewModel.toggleChecklistItem(note, idx)
                                    },
                                    onDelete = { viewModel.deleteNoteDirectly(note) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("notes_list"),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(notes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    isDark = isDark,
                                    isArabic = isArabic,
                                    onClick = { viewModel.openExistingNote(note) },
                                    onTogglePin = { viewModel.togglePin(note) },
                                    onToggleFavorite = { viewModel.toggleFavorite(note) },
                                    onToggleChecklistItem = { idx ->
                                        viewModel.toggleChecklistItem(note, idx)
                                    },
                                    onDelete = { viewModel.deleteNoteDirectly(note) }
                                )
                            }
                        }
                    }
                }
            }

            // Floating Glass Bottom Bar & Expanding Action Sheet
            FluffyBottomBar(
                isDark = isDark,
                isArabic = isArabic,
                onNewNote = { viewModel.openNewNote() },
                onNewChecklist = { viewModel.openNewNote(startWithChecklist = true) },
                onNewVoiceNote = { viewModel.showVoiceRecorder.value = true },
                modifier = Modifier.fillMaxSize()
            )

            // Custom Frosted Glass Snackbar Host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp, start = 20.dp, end = 20.dp)
            ) { data ->
                LiquidGlassCard(
                    isDark = isDark,
                    tint = GlassTint.FROST,
                    shape = RoundedCornerShape(18.dp),
                    borderWidth = 1.2.dp
                ) {
                    Text(
                        text = data.visuals.message,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    )
                }
            }

            // Settings Drawer (Opened via left swipe or menu icon)
            SettingsDrawer(
                isOpen = isSettingsDrawerOpen,
                onClose = { viewModel.closeSettingsDrawer() },
                isDark = isDark,
                isArabic = isArabic,
                isGridView = isGridView,
                isBiometricEnabled = isBiometricEnabled,
                onToggleBiometric = { onToggleBiometricAuth(it) },
                onLockAppNow = { viewModel.lockApp() },
                onToggleTheme = { viewModel.toggleDarkMode() },
                onToggleLanguage = { viewModel.toggleLanguage() },
                onToggleViewMode = { viewModel.toggleViewMode() },
                isCheckingUpdates = updateState is UpdateUiState.Checking,
                hasPendingUpdate = updateState is UpdateUiState.Available ||
                    updateState is UpdateUiState.ReadyToInstall ||
                    updateState is UpdateUiState.NeedsInstallPermission,
                onCheckForUpdates = { viewModel.checkForUpdates(isManual = true) }
            )
        }

        // Stats Bottom Sheet
        if (showStats) {
            StatsBottomSheet(
                notes = notes,
                isDark = isDark,
                isArabic = isArabic,
                onDismiss = { viewModel.showStatsSheet.value = false }
            )
        }

        // Voice Recorder Dialog
        if (showVoiceRecorder) {
            VoiceRecorderDialog(
                isDark = isDark,
                isArabic = isArabic,
                onDismiss = { viewModel.showVoiceRecorder.value = false },
                onSaveVoiceNote = { duration ->
                    viewModel.showVoiceRecorder.value = false
                    viewModel.openNewNote()
                    // Set duration on newly created note
                    viewModel.editingNote.value = viewModel.editingNote.value?.copy(
                        audioDurationSec = duration
                    )
                }
            )
        }
    }
}
