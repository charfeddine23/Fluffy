package com.example.ui.screens

import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.GlassTint
import com.example.data.model.NoteEntity
import com.example.ui.components.CategoryChips
import com.example.ui.components.FirebaseAuthDialog
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
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun FluffyHomeScreen(
    viewModel: FluffyViewModel,
    onToggleBiometricAuth: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
    val currentUser by viewModel.currentUser.collectAsState()
    val isGuestMode by viewModel.isGuestMode.collectAsState()
    val isAuthBusy by viewModel.isAuthBusy.collectAsState()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsState()
    val showAuthDialog by viewModel.showAuthDialog.collectAsState()

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportFullBackupToPhoneUri(it) }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restoreFullBackupFromPhoneUri(it) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val gridState = rememberLazyStaggeredGridState()
    val listState = rememberLazyListState()

    var displayedNotes by remember { mutableStateOf(notes) }
    var draggedNoteId by remember { mutableStateOf<Long?>(null) }
    var currentPointerPos by remember { mutableStateOf(Offset.Zero) }
    var grabOffsetInsideCard by remember { mutableStateOf(Offset.Zero) }
    var pendingCommitOrderIds by remember { mutableStateOf<List<Long>?>(null) }
    var lastSwappedKey by remember { mutableStateOf<Long?>(null) }
    var lastSwapTimeMs by remember { mutableLongStateOf(0L) }
    var lastSwapPointerPos by remember { mutableStateOf(Offset.Zero) }
    val lastKnownTranslationXY = remember { floatArrayOf(0f, 0f) }

    val currentDisplayedNotes by rememberUpdatedState(displayedNotes)
    val currentIsGridView by rememberUpdatedState(isGridView)
    val currentNotes by rememberUpdatedState(notes)

    val listStartPaddingPx = with(density) { 16.dp.toPx() }
    val swapHysteresisPx = with(density) { 28.dp.toPx() }

    LaunchedEffect(notes, draggedNoteId, pendingCommitOrderIds) {
        if (draggedNoteId == null) {
            val pending = pendingCommitOrderIds
            if (pending == null || notes.map { it.id } == pending) {
                displayedNotes = notes
                pendingCommitOrderIds = null
            }
        }
    }

    LaunchedEffect(pendingCommitOrderIds) {
        if (pendingCommitOrderIds != null) {
            delay(600L)
            if (draggedNoteId == null) {
                displayedNotes = currentNotes
                pendingCommitOrderIds = null
            }
        }
    }

    fun trySwapDraggedNote(pointerPos: Offset, activeDragId: Long, inGrid: Boolean) {
        val candidateKey: Long? = if (inGrid) {
            gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                val id = info.key as? Long ?: return@firstOrNull false
                if (id == activeDragId) return@firstOrNull false
                val left = info.offset.x.toFloat()
                val right = left + info.size.width
                val top = info.offset.y.toFloat()
                val bottom = top + info.size.height
                pointerPos.x in left..right && pointerPos.y in top..bottom
            }?.key as? Long
        } else {
            listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                val id = info.key as? Long ?: return@firstOrNull false
                if (id == activeDragId) return@firstOrNull false
                val top = info.offset.toFloat()
                val bottom = top + info.size
                pointerPos.y in top..bottom
            }?.key as? Long
        }

        if (candidateKey == null) return

        val now = SystemClock.uptimeMillis()
        val elapsed = now - lastSwapTimeMs
        val movedDist = (pointerPos - lastSwapPointerPos).getDistance()
        val allowSwap = candidateKey != lastSwappedKey || (elapsed > 260L && movedDist > swapHysteresisPx)
        if (!allowSwap) return

        val currentList = currentDisplayedNotes
        val fromIdx = currentList.indexOfFirst { it.id == activeDragId }
        val toIdx = currentList.indexOfFirst { it.id == candidateKey }
        if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
            val mutable = currentList.toMutableList()
            val moved = mutable.removeAt(fromIdx)
            mutable.add(toIdx, moved)
            displayedNotes = mutable
            lastSwappedKey = candidateKey
            lastSwapTimeMs = now
            lastSwapPointerPos = pointerPos
        }
    }

    // Smooth edge auto-scrolling while holding & dragging a note near the top/bottom edges
    LaunchedEffect(draggedNoteId, isGridView) {
        val activeDragId = draggedNoteId ?: return@LaunchedEffect
        val edgeZonePx = with(density) { 64.dp.toPx() }
        val maxScrollStep = with(density) { 14.dp.toPx() }

        while (draggedNoteId == activeDragId) {
            val viewportHeight = if (currentIsGridView) {
                gridState.layoutInfo.viewportSize.height.toFloat()
            } else {
                listState.layoutInfo.viewportSize.height.toFloat()
            }

            val y = currentPointerPos.y
            val scrollDelta = when {
                viewportHeight <= 0f -> 0f
                y < edgeZonePx -> -((edgeZonePx - y) / edgeZonePx).coerceIn(0f, 1f) * maxScrollStep
                y > viewportHeight - edgeZonePx -> ((y - (viewportHeight - edgeZonePx)) / edgeZonePx).coerceIn(0f, 1f) * maxScrollStep
                else -> 0f
            }

            if (scrollDelta != 0f) {
                if (currentIsGridView) {
                    gridState.scrollBy(scrollDelta)
                } else {
                    listState.scrollBy(scrollDelta)
                }
                trySwapDraggedNote(currentPointerPos, activeDragId, currentIsGridView)
            }
            withFrameNanos { }
        }
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    FrostedBackdrop(isDark = isDark) {
        var dragStartX by remember { mutableFloatStateOf(0f) }
        var totalDragX by remember { mutableFloatStateOf(0f) }
        var drawerTriggered by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Settings drawer swipe gesture: reaches all the way from the edge up to the midpoint of the screen
                .pointerInput(isArabic) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragStartX = offset.x
                            totalDragX = 0f
                            drawerTriggered = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (draggedNoteId != null) return@detectHorizontalDragGestures
                            totalDragX += dragAmount
                            if (!drawerTriggered) {
                                val halfWidth = size.width / 2f
                                if (isArabic) {
                                    // In Arabic: right half (from right edge up to center of screen) swiping left
                                    if (dragStartX >= halfWidth && totalDragX < -24f) {
                                        drawerTriggered = true
                                        viewModel.openSettingsDrawer()
                                    }
                                } else {
                                    // In English: left half (from left edge up to center of screen) swiping right
                                    if (dragStartX <= halfWidth && totalDragX > 24f) {
                                        drawerTriggered = true
                                        viewModel.openSettingsDrawer()
                                    }
                                }
                            }
                        }
                    )
                }
                // Two-finger downward swipe in the middle of the screen opens Vault
                .pointerInput(isSettingsDrawerOpen, showStats, showVoiceRecorder) {
                    if (isSettingsDrawerOpen || showStats || showVoiceRecorder) return@pointerInput
                    val triggerDistancePx = 36.dp.toPx()
                    awaitEachGesture {
                        var twoFingerStartedInCenter = false
                        var startX1 = 0f
                        var startY1 = 0f
                        var startX2 = 0f
                        var startY2 = 0f
                        var id1 = PointerId(-1L)
                        var id2 = PointerId(-1L)
                        var triggered = false

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break

                            if (!twoFingerStartedInCenter && pressed.size == 2) {
                                val p1 = pressed[0]
                                val p2 = pressed[1]
                                val avgX = (p1.position.x + p2.position.x) / 2f
                                val avgY = (p1.position.y + p2.position.y) / 2f
                                val width = size.width.toFloat()
                                val height = size.height.toFloat()

                                val inCenterX = avgX in (width * 0.15f)..(width * 0.85f)
                                val inCenterY = avgY in (height * 0.15f)..(height * 0.85f)
                                if (inCenterX && inCenterY) {
                                    twoFingerStartedInCenter = true
                                    id1 = p1.id
                                    id2 = p2.id
                                    startX1 = p1.position.x
                                    startY1 = p1.position.y
                                    startX2 = p2.position.x
                                    startY2 = p2.position.y
                                }
                            } else if (twoFingerStartedInCenter && !triggered) {
                                val c1 = event.changes.firstOrNull { it.id == id1 }
                                val c2 = event.changes.firstOrNull { it.id == id2 }
                                if (c1 == null || c2 == null || !c1.pressed || !c2.pressed || pressed.size != 2) {
                                    twoFingerStartedInCenter = false
                                } else {
                                    val dy1 = c1.position.y - startY1
                                    val dy2 = c2.position.y - startY2
                                    val dx1 = abs(c1.position.x - startX1)
                                    val dx2 = abs(c2.position.x - startX2)

                                    if (dy1 > 10f && dy2 > 10f) {
                                        c1.consume()
                                        c2.consume()
                                    }
                                    if (dy1 >= triggerDistancePx && dy2 >= triggerDistancePx &&
                                        dx1 < dy1 * 1.2f && dx2 < dy2 * 1.2f
                                    ) {
                                        triggered = true
                                        viewModel.openVault()
                                    }
                                }
                            } else if (triggered) {
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
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
                    onOpenSettings = { viewModel.openSettingsDrawer() },
                    isCloudSignedIn = currentUser != null,
                    isGuestMode = isGuestMode,
                    onOpenCloudAuth = { viewModel.openAuthDialog() }
                )

                // Category Chips
                CategoryChips(
                    isDark = isDark,
                    isArabic = isArabic,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { viewModel.selectCategory(it) }
                )

                // Notes Content: Staggered Grid, List, or clean empty space
                if (displayedNotes.isEmpty()) {
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                val touchSlop = viewConfiguration.touchSlop

                                fun findHitNoteAndOrigin(pos: Offset): Pair<Long, Offset>? {
                                    return if (currentIsGridView) {
                                        gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                            val left = info.offset.x.toFloat()
                                            val right = left + info.size.width
                                            val top = info.offset.y.toFloat()
                                            val bottom = top + info.size.height
                                            pos.x in left..right && pos.y in top..bottom
                                        }?.let { info ->
                                            val id = info.key as? Long ?: return@let null
                                            id to Offset(info.offset.x.toFloat(), info.offset.y.toFloat())
                                        }
                                    } else {
                                        val rightEdge = size.width.toFloat() - listStartPaddingPx
                                        listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                            val top = info.offset.toFloat()
                                            val bottom = top + info.size
                                            pos.x in listStartPaddingPx..rightEdge && pos.y in top..bottom
                                        }?.let { info ->
                                            val id = info.key as? Long ?: return@let null
                                            id to Offset(listStartPaddingPx, info.offset.toFloat())
                                        }
                                    }
                                }

                                awaitEachGesture {
                                    val initialEvent = awaitPointerEvent(PointerEventPass.Initial)
                                    val firstDown = initialEvent.changes.firstOrNull { it.changedToDownIgnoreConsumed() }
                                        ?: return@awaitEachGesture
                                    if (initialEvent.changes.count { it.pressed } != 1) return@awaitEachGesture

                                    val pointerId = firstDown.id
                                    val downPos = firstDown.position
                                    if (findHitNoteAndOrigin(downPos) == null) return@awaitEachGesture

                                    // Wait 1000 ms (1 full second) while stationary before activating drag
                                    var latestPos = downPos
                                    var holdActivated = false
                                    val startTime = SystemClock.uptimeMillis()

                                    while (true) {
                                        val remaining = 1000L - (SystemClock.uptimeMillis() - startTime)
                                        if (remaining <= 0L) {
                                            holdActivated = true
                                            break
                                        }
                                        val nextEvent = withTimeoutOrNull(remaining) {
                                            awaitPointerEvent(PointerEventPass.Initial)
                                        }
                                        if (nextEvent == null) {
                                            holdActivated = true
                                            break
                                        }
                                        val activeCount = nextEvent.changes.count { it.pressed }
                                        val change = nextEvent.changes.firstOrNull { it.id == pointerId }
                                        if (change == null || !change.pressed || activeCount != 1) {
                                            return@awaitEachGesture
                                        }
                                        latestPos = change.position
                                        if ((latestPos - downPos).getDistance() > touchSlop) {
                                            return@awaitEachGesture
                                        }
                                    }

                                    if (!holdActivated) return@awaitEachGesture
                                    val hit = findHitNoteAndOrigin(latestPos) ?: return@awaitEachGesture
                                    if (currentDisplayedNotes.size <= 1) return@awaitEachGesture

                                    val (hitNoteId, cardOrigin) = hit
                                    draggedNoteId = hitNoteId
                                    currentPointerPos = latestPos
                                    grabOffsetInsideCard = latestPos - cardOrigin
                                    lastKnownTranslationXY[0] = 0f
                                    lastKnownTranslationXY[1] = 0f
                                    lastSwappedKey = null
                                    lastSwapTimeMs = 0L
                                    lastSwapPointerPos = latestPos
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                    while (true) {
                                        val dragEvent = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = dragEvent.changes.firstOrNull { it.id == pointerId }
                                        if (change == null || !change.pressed) {
                                            dragEvent.changes.forEach { it.consume() }
                                            break
                                        }
                                        dragEvent.changes.forEach { it.consume() }
                                        latestPos = change.position
                                        currentPointerPos = latestPos
                                        trySwapDraggedNote(latestPos, hitNoteId, currentIsGridView)
                                    }

                                    val committed = currentDisplayedNotes.toList()
                                    pendingCommitOrderIds = committed.map { it.id }
                                    draggedNoteId = null
                                    lastKnownTranslationXY[0] = 0f
                                    lastKnownTranslationXY[1] = 0f
                                    viewModel.reorderNotes(committed)
                                }
                            }
                    ) {
                        if (isGridView) {
                            LazyVerticalStaggeredGrid(
                                state = gridState,
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
                                items(displayedNotes, key = { it.id }) { note ->
                                    val isDragging = draggedNoteId == note.id
                                    val dragScale by animateFloatAsState(
                                        targetValue = if (isDragging) 1.04f else 1f,
                                        label = "grid_drag_scale"
                                    )
                                    val itemModifier = if (isDragging) {
                                        Modifier
                                            .zIndex(10f)
                                            .graphicsLayer {
                                                val origin = gridState.layoutInfo.visibleItemsInfo
                                                    .firstOrNull { it.key == note.id }
                                                    ?.offset
                                                    ?.let { Offset(it.x.toFloat(), it.y.toFloat()) }
                                                val tx: Float
                                                val ty: Float
                                                if (origin != null) {
                                                    val computed = currentPointerPos - origin - grabOffsetInsideCard
                                                    lastKnownTranslationXY[0] = computed.x
                                                    lastKnownTranslationXY[1] = computed.y
                                                    tx = computed.x
                                                    ty = computed.y
                                                } else {
                                                    tx = lastKnownTranslationXY[0]
                                                    ty = lastKnownTranslationXY[1]
                                                }
                                                translationX = tx
                                                translationY = ty
                                                scaleX = dragScale
                                                scaleY = dragScale
                                                shape = RoundedCornerShape(22.dp)
                                                shadowElevation = 24.dp.toPx()
                                            }
                                    } else {
                                        Modifier.animateItem()
                                    }

                                    NoteCard(
                                        note = note,
                                        isDark = isDark,
                                        isArabic = isArabic,
                                        isDragging = isDragging,
                                        onClick = { viewModel.openExistingNote(note) },
                                        onTogglePin = { viewModel.togglePin(note) },
                                        onToggleFavorite = { viewModel.toggleFavorite(note) },
                                        onToggleChecklistItem = { idx ->
                                            viewModel.toggleChecklistItem(note, idx)
                                        },
                                        onDelete = { viewModel.deleteNoteDirectly(note) },
                                        modifier = itemModifier
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
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
                                items(displayedNotes, key = { it.id }) { note ->
                                    val isDragging = draggedNoteId == note.id
                                    val dragScale by animateFloatAsState(
                                        targetValue = if (isDragging) 1.03f else 1f,
                                        label = "list_drag_scale"
                                    )
                                    val itemModifier = if (isDragging) {
                                        Modifier
                                            .zIndex(10f)
                                            .graphicsLayer {
                                                val origin = listState.layoutInfo.visibleItemsInfo
                                                    .firstOrNull { it.key == note.id }
                                                    ?.offset
                                                    ?.let { Offset(listStartPaddingPx, it.toFloat()) }
                                                val tx: Float
                                                val ty: Float
                                                if (origin != null) {
                                                    val computed = currentPointerPos - origin - grabOffsetInsideCard
                                                    lastKnownTranslationXY[0] = computed.x
                                                    lastKnownTranslationXY[1] = computed.y
                                                    tx = computed.x
                                                    ty = computed.y
                                                } else {
                                                    tx = lastKnownTranslationXY[0]
                                                    ty = lastKnownTranslationXY[1]
                                                }
                                                translationX = tx
                                                translationY = ty
                                                scaleX = dragScale
                                                scaleY = dragScale
                                                shape = RoundedCornerShape(22.dp)
                                                shadowElevation = 24.dp.toPx()
                                            }
                                    } else {
                                        Modifier.animateItem()
                                    }

                                    NoteCard(
                                        note = note,
                                        isDark = isDark,
                                        isArabic = isArabic,
                                        isDragging = isDragging,
                                        onClick = { viewModel.openExistingNote(note) },
                                        onTogglePin = { viewModel.togglePin(note) },
                                        onToggleFavorite = { viewModel.toggleFavorite(note) },
                                        onToggleChecklistItem = { idx ->
                                            viewModel.toggleChecklistItem(note, idx)
                                        },
                                        onDelete = { viewModel.deleteNoteDirectly(note) },
                                        modifier = itemModifier
                                    )
                                }
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

            // Settings Drawer (Opened via left/right half-screen swipe or menu icon)
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
                onCheckForUpdates = { viewModel.checkForUpdates(isManual = true) },
                currentUser = currentUser,
                isGuestMode = isGuestMode,
                cloudSyncStatus = cloudSyncStatus,
                onOpenAuthDialog = {
                    viewModel.closeSettingsDrawer()
                    viewModel.openAuthDialog()
                },
                onReturnToWelcomeScreen = { viewModel.returnToWelcomeScreen() },
                onSyncCloudNow = { viewModel.syncCloudNow() },
                onSignOutFirebase = { viewModel.signOutFirebase() },
                onExportLocalBackup = {
                    exportBackupLauncher.launch("Fluffy_Backup_${System.currentTimeMillis()}.json")
                },
                onImportLocalBackup = {
                    importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                }
            )
        }

        // Liquid Glass Google Account & Guest Mode Sync Dialog
        FirebaseAuthDialog(
            visible = showAuthDialog,
            currentUser = currentUser,
            isGuestMode = isGuestMode,
            isAuthBusy = isAuthBusy,
            cloudSyncStatus = cloudSyncStatus,
            isDark = isDark,
            isArabic = isArabic,
            onDismiss = { viewModel.closeAuthDialog() },
            onGoogleIdToken = { idToken -> viewModel.signInWithGoogleIdToken(idToken) },
            onContinueAsGuest = { viewModel.enterGuestMode() },
            onSyncNow = { viewModel.syncCloudNow() },
            onSignOut = { viewModel.signOutFirebase() }
        )

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
