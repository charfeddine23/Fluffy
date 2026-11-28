package com.charfeddine.fluffy.scanner.ui.screens

import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil.compose.SubcomposeAsyncImage
import com.charfeddine.fluffy.scanner.data.model.ScanMode
import com.charfeddine.fluffy.scanner.data.model.ScannedDocumentEntity
import com.charfeddine.fluffy.scanner.data.model.ScannerFolderEntity
import com.charfeddine.fluffy.scanner.data.model.ScannerTagCatalog
import com.charfeddine.fluffy.scanner.data.model.SortOption
import com.charfeddine.fluffy.scanner.domain.ScannerViewModel
import com.charfeddine.fluffy.scanner.ui.components.PdfToolsSheet
import com.charfeddine.fluffy.scanner.ui.theme.ScannerDarkLiquidBackdrop
import com.charfeddine.fluffy.scanner.ui.theme.ScannerGlassPalette
import com.charfeddine.fluffy.scanner.ui.theme.ScannerLaserIcon
import com.charfeddine.fluffy.scanner.ui.theme.formatScannerBytes
import com.charfeddine.fluffy.scanner.ui.theme.iosLiquidGlass
import com.charfeddine.fluffy.scanner.ui.theme.scannerGlassSurface
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun ScannerHubScreen(
    scannerViewModel: ScannerViewModel,
    isArabic: Boolean,
    onLaunchPrimaryScan: () -> Unit = { scannerViewModel.openCameraScanner(appendToDocId = null, initialMode = ScanMode.ID_CARD) },
    onLaunchAppIdScanner: () -> Unit = { scannerViewModel.openCameraScanner(appendToDocId = null, initialMode = ScanMode.ID_CARD) },
    onAddMoreCamera: (String) -> Unit = { docId -> scannerViewModel.openCameraScanner(appendToDocId = docId, initialMode = ScanMode.ID_CARD) },
    onRequestProtectedAccess: (targetId: String, onUnlocked: () -> Unit) -> Unit,
    onExitEcosystem: () -> Unit
) {
    val currentFolderId by scannerViewModel.currentFolderId.collectAsState()
    val allFolders by scannerViewModel.allFolders.collectAsState()
    val visibleDocs by scannerViewModel.visibleDocuments.collectAsState()
    val trashedDocs by scannerViewModel.trashedDocs.collectAsState()
    val searchQuery by scannerViewModel.searchQuery.collectAsState()
    val selectedTag by scannerViewModel.selectedTagFilter.collectAsState()
    val onlyStarred by scannerViewModel.onlyStarredFilter.collectAsState()
    val showTrash by scannerViewModel.showTrashView.collectAsState()
    val sortOption by scannerViewModel.sortOption.collectAsState()
    val isGridView by scannerViewModel.isGridView.collectAsState()
    val unlockedIds by scannerViewModel.unlockedProtectedIds.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showTagMenu by remember { mutableStateOf(false) }
    var showPdfToolsSheet by remember { mutableStateOf(false) }
    var selectedDocIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val selectedDocs = remember(selectedDocIds, visibleDocs) {
        visibleDocs.filter { it.id in selectedDocIds }
    }
    val isSelectionMode = selectedDocs.isNotEmpty()
    var showAddMoreSourceChoices by remember { mutableStateOf(false) }
    var docToRename by remember { mutableStateOf<ScannedDocumentEntity?>(null) }
    var pendingAppendGalleryDocId by remember { mutableStateOf<String?>(null) }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val docGridState = rememberLazyGridState()
    var displayedDocs by remember { mutableStateOf(visibleDocs) }
    var draggedDocId by remember { mutableStateOf<String?>(null) }
    var pressedDocId by remember { mutableStateOf<String?>(null) }
    var currentPointerPos by remember { mutableStateOf(Offset.Zero) }
    var grabOffsetInsideCard by remember { mutableStateOf(Offset.Zero) }
    var pendingCommitOrderIds by remember { mutableStateOf<List<String>?>(null) }
    var lastSwappedKey by remember { mutableStateOf<String?>(null) }
    var lastSwapTimeMs by remember { mutableLongStateOf(0L) }
    var lastSwapPointerPos by remember { mutableStateOf(Offset.Zero) }
    val lastKnownTranslationXY = remember { floatArrayOf(0f, 0f) }
    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val docPhysicalRects = remember { mutableMapOf<String, Rect>() }

    val currentDisplayedDocs by rememberUpdatedState(displayedDocs)
    val currentVisibleDocs by rememberUpdatedState(visibleDocs)
    val currentIsGridView by rememberUpdatedState(isGridView)
    val swapHysteresisPx = with(density) { 28.dp.toPx() }

    LaunchedEffect(visibleDocs, draggedDocId, pendingCommitOrderIds) {
        if (draggedDocId == null) {
            val pending = pendingCommitOrderIds
            if (pending == null || visibleDocs.map { it.id } == pending) {
                displayedDocs = visibleDocs
                pendingCommitOrderIds = null
            }
        }
        val visibleIdSet = visibleDocs.map { it.id }.toSet()
        val pruned = selectedDocIds.intersect(visibleIdSet)
        if (pruned != selectedDocIds) {
            selectedDocIds = pruned
        }
    }

    LaunchedEffect(pendingCommitOrderIds) {
        if (pendingCommitOrderIds != null) {
            delay(600L)
            if (draggedDocId == null) {
                displayedDocs = currentVisibleDocs
                pendingCommitOrderIds = null
            }
        }
    }

    fun visibleDocKeys(): Set<String> {
        return docGridState.layoutInfo.visibleItemsInfo.mapNotNullTo(HashSet()) { it.key as? String }
    }

    fun trySwapDraggedDoc(pointerPos: Offset, activeDragId: String, gridMode: Boolean) {
        val currentList = currentDisplayedDocs
        val draggedDoc = currentList.firstOrNull { it.id == activeDragId } ?: return
        val visibleKeys = visibleDocKeys()

        val candidateKey: String? = docPhysicalRects.entries.firstOrNull { (id, rect) ->
            if (id == activeDragId || id !in visibleKeys) return@firstOrNull false
            if (gridMode) {
                rect.contains(pointerPos)
            } else {
                pointerPos.y in rect.top..rect.bottom
            }
        }?.key

        if (candidateKey == null) return
        val candidateDoc = currentList.firstOrNull { it.id == candidateKey } ?: return
        if (draggedDoc.isPinned != candidateDoc.isPinned) return

        val now = SystemClock.uptimeMillis()
        val elapsed = now - lastSwapTimeMs
        val movedDist = (pointerPos - lastSwapPointerPos).getDistance()
        val allowSwap = candidateKey != lastSwappedKey || (elapsed > 260L && movedDist > swapHysteresisPx)
        if (!allowSwap) return

        val fromIdx = currentList.indexOfFirst { it.id == activeDragId }
        val toIdx = currentList.indexOfFirst { it.id == candidateKey }
        if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
            val mutable = currentList.toMutableList()
            val moved = mutable.removeAt(fromIdx)
            mutable.add(toIdx, moved)
            displayedDocs = mutable
            lastSwappedKey = candidateKey
            lastSwapTimeMs = now
            lastSwapPointerPos = pointerPos
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    LaunchedEffect(draggedDocId) {
        val activeDragId = draggedDocId ?: return@LaunchedEffect
        val edgeZonePx = with(density) { 64.dp.toPx() }
        val maxScrollStep = with(density) { 14.dp.toPx() }

        while (draggedDocId == activeDragId) {
            val viewportHeight = docGridState.layoutInfo.viewportSize.height.toFloat()
            val y = currentPointerPos.y
            val scrollDelta = when {
                viewportHeight <= 0f -> 0f
                y < edgeZonePx -> -((edgeZonePx - y) / edgeZonePx).coerceIn(0f, 1f) * maxScrollStep
                y > viewportHeight - edgeZonePx -> ((y - (viewportHeight - edgeZonePx)) / edgeZonePx).coerceIn(0f, 1f) * maxScrollStep
                else -> 0f
            }

            if (scrollDelta != 0f) {
                docGridState.scrollBy(scrollDelta)
                trySwapDraggedDoc(currentPointerPos, activeDragId, currentIsGridView)
            }
            withFrameNanos { }
        }
    }

    BackHandler {
        when {
            selectedDocIds.isNotEmpty() -> {
                showAddMoreSourceChoices = false
                selectedDocIds = emptySet()
            }
            showTrash -> scannerViewModel.showTrashView.value = false
            currentFolderId != null -> {
                val parent = allFolders.firstOrNull { it.id == currentFolderId }?.parentFolderId
                scannerViewModel.setCurrentFolderId(parent)
            }
            else -> onExitEcosystem()
        }
    }

    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        val appendDocId = pendingAppendGalleryDocId
        pendingAppendGalleryDocId = null
        if (uris.isNotEmpty()) {
            scannerViewModel.importDeviceFiles(uris, appendToDocId = appendDocId, isArabic = isArabic)
        }
    }

    val childFolders = remember(allFolders, currentFolderId) {
        allFolders.filter { it.parentFolderId == currentFolderId }
    }

    val currentFolderObj = remember(allFolders, currentFolderId) {
        allFolders.firstOrNull { it.id == currentFolderId }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScannerDarkLiquidBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Clean Minimal Top Bar (Back + Title + View Toggle + Trash)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                when {
                                    selectedDocIds.isNotEmpty() -> {
                                        showAddMoreSourceChoices = false
                                        selectedDocIds = emptySet()
                                    }
                                    showTrash -> scannerViewModel.showTrashView.value = false
                                    currentFolderId != null -> {
                                        val parent = allFolders.firstOrNull { it.id == currentFolderId }?.parentFolderId
                                        scannerViewModel.setCurrentFolderId(parent)
                                    }
                                    else -> onExitEcosystem()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("scanner_hub_back_btn")
                                .scannerGlassSurface(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isArabic) "رجوع" else "Back",
                                tint = ScannerGlassPalette.TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    showTrash -> if (isArabic) "سلة المهملات" else "Recycle Bin"
                                    currentFolderObj != null -> currentFolderObj.name
                                    else -> "Doc Scanner"
                                },
                                color = ScannerGlassPalette.TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitleText = when {
                                showTrash -> if (isArabic) "تُحذف تلقائياً بعد 30 يوماً" else "Auto-deletes after 30 days"
                                currentFolderObj != null -> if (isArabic) "مجلد خاص" else "Folder"
                                else -> null
                            }
                            if (subtitleText != null) {
                                Text(
                                    text = subtitleText,
                                    color = ScannerGlassPalette.TextSecondary,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!showTrash) {
                            IconButton(
                                onClick = { scannerViewModel.toggleGridView() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("hub_view_toggle_btn")
                                    .scannerGlassSurface(CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                    contentDescription = if (isArabic) "تغيير العرض" else "Toggle View",
                                    tint = ScannerGlassPalette.TextPrimary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                showAddMoreSourceChoices = false
                                selectedDocIds = emptySet()
                                scannerViewModel.showTrashView.value = !showTrash
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("hub_trash_toggle_btn")
                                .scannerGlassSurface(CircleShape, glowBorder = showTrash)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = if (isArabic) "سلة المهملات" else "Recycle Bin",
                                tint = if (showTrash) ScannerGlassPalette.RoseGlow else ScannerGlassPalette.TextSecondary
                            )
                        }
                    }
                }

                if (showTrash) {
                    if (trashedDocs.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "${trashedDocs.size} عنصر في السلة" else "${trashedDocs.size} trashed item${if (trashedDocs.size == 1) "" else "s"}",
                                color = ScannerGlassPalette.TextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("hub_empty_trash_btn")
                                    .clip(CircleShape)
                                    .background(ScannerGlassPalette.RoseGlow.copy(alpha = 0.16f))
                                    .border(1.dp, ScannerGlassPalette.RoseGlow.copy(alpha = 0.65f), CircleShape)
                                    .clickable { scannerViewModel.emptyTrash(isArabic) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = if (isArabic) "مسح الكل" else "Clear All",
                                    tint = ScannerGlassPalette.RoseGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isArabic) "مسح الكل" else "Clear All",
                                    color = ScannerGlassPalette.RoseGlow,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Clean Recycle Bin View
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (trashedDocs.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 48.dp)
                                        .scannerGlassSurface(RoundedCornerShape(24.dp))
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = ScannerGlassPalette.TextSecondary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "سلة المهملات فارغة" else "Recycle Bin is empty",
                                        color = ScannerGlassPalette.TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        } else {
                            items(trashedDocs, key = { it.id }) { doc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scannerGlassSurface(RoundedCornerShape(18.dp))
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.title,
                                            color = ScannerGlassPalette.TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isArabic) "${doc.pageCount} صفحة • ${formatScannerBytes(doc.totalSizeBytes, true)}" else "${doc.pageCount} pages • ${formatScannerBytes(doc.totalSizeBytes, false)}",
                                            color = ScannerGlassPalette.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { scannerViewModel.restoreFromTrash(doc.id) }) {
                                            Icon(Icons.Default.RestoreFromTrash, contentDescription = "Restore", tint = ScannerGlassPalette.EmeraldGlow)
                                        }
                                        IconButton(onClick = { scannerViewModel.deletePermanently(doc.id) }) {
                                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete Permanently", tint = ScannerGlassPalette.RoseGlow)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 2. Clean Search Bar <-> In-Place Contextual / Batch Selection Bar (exact same slot & height)
                    AnimatedContent(
                        targetState = selectedDocs,
                        transitionSpec = {
                            (fadeIn(tween(200)) + slideInVertically(tween(200)) { -it / 8 }) togetherWith
                                (fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 8 })
                        },
                        contentKey = { list -> if (list.isEmpty()) 0 else if (list.size == 1) 1 else 2 },
                        modifier = Modifier.fillMaxWidth(),
                        label = "scanner_search_or_contextual_bar"
                    ) { activeDocs ->
                        if (activeDocs.size == 1) {
                            val activeDoc = activeDocs.first()
                            val isDocUnlocked = !activeDoc.isLocked || activeDoc.id in unlockedIds
                            fun withDocAccess(action: () -> Unit) {
                                if (!isDocUnlocked) {
                                    onRequestProtectedAccess(activeDoc.id) {
                                        scannerViewModel.markProtectedUnlocked(activeDoc.id)
                                        action()
                                    }
                                } else {
                                    action()
                                }
                            }

                            ScannerContextualTopActionBar(
                                doc = activeDoc,
                                isArabic = isArabic,
                                showAddMoreChoices = showAddMoreSourceChoices,
                                onDismiss = {
                                    showAddMoreSourceChoices = false
                                    selectedDocIds = emptySet()
                                },
                                onTogglePin = {
                                    scannerViewModel.togglePinned(activeDoc, isArabic)
                                    showAddMoreSourceChoices = false
                                    selectedDocIds = emptySet()
                                },
                                onRename = {
                                    withDocAccess {
                                        showAddMoreSourceChoices = false
                                        selectedDocIds = emptySet()
                                        docToRename = activeDoc
                                    }
                                },
                                onToggleAddMore = {
                                    showAddMoreSourceChoices = !showAddMoreSourceChoices
                                },
                                onAddMoreCamera = {
                                    withDocAccess {
                                        showAddMoreSourceChoices = false
                                        selectedDocIds = emptySet()
                                        onAddMoreCamera(activeDoc.id)
                                    }
                                },
                                onAddMoreFiles = {
                                    withDocAccess {
                                        showAddMoreSourceChoices = false
                                        selectedDocIds = emptySet()
                                        pendingAppendGalleryDocId = activeDoc.id
                                        fileImportLauncher.launch(arrayOf("image/*", "application/pdf"))
                                    }
                                },
                                onDelete = {
                                    withDocAccess {
                                        scannerViewModel.moveToTrash(activeDoc.id, isArabic)
                                        showAddMoreSourceChoices = false
                                        selectedDocIds = emptySet()
                                    }
                                }
                            )
                        } else if (activeDocs.size > 1) {
                            val allVisibleSelected = displayedDocs.isNotEmpty() &&
                                displayedDocs.all { it.id in selectedDocIds }
                            ScannerBatchSelectionTopBar(
                                selectedCount = activeDocs.size,
                                allSelected = allVisibleSelected,
                                isArabic = isArabic,
                                onDismiss = {
                                    showAddMoreSourceChoices = false
                                    selectedDocIds = emptySet()
                                },
                                onToggleSelectAll = {
                                    selectedDocIds = if (allVisibleSelected) {
                                        emptySet()
                                    } else {
                                        displayedDocs.mapTo(HashSet()) { it.id }
                                    }
                                },
                                onBatchDelete = {
                                    val lockedPending = activeDocs.firstOrNull { it.isLocked && it.id !in unlockedIds }
                                    if (lockedPending != null) {
                                        onRequestProtectedAccess(lockedPending.id) {
                                            scannerViewModel.markProtectedUnlocked(lockedPending.id)
                                            scannerViewModel.moveDocumentsToTrash(activeDocs.map { it.id }, isArabic)
                                            showAddMoreSourceChoices = false
                                            selectedDocIds = emptySet()
                                        }
                                    } else {
                                        scannerViewModel.moveDocumentsToTrash(activeDocs.map { it.id }, isArabic)
                                        showAddMoreSourceChoices = false
                                        selectedDocIds = emptySet()
                                    }
                                }
                            )
                        } else {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { scannerViewModel.searchQuery.value = it },
                                placeholder = {
                                    Text(
                                        text = if (isArabic) "ابحث في المستندات أو نصوص OCR..." else "Search documents or OCR text...",
                                        fontSize = 13.sp,
                                        color = ScannerGlassPalette.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = ScannerGlassPalette.CyanGlow
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { scannerViewModel.searchQuery.value = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = if (isArabic) "مسح البحث" else "Clear search",
                                                tint = ScannerGlassPalette.TextSecondary
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(22.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ScannerGlassPalette.CyanGlow.copy(alpha = 0.7f),
                                    unfocusedBorderColor = ScannerGlassPalette.SubtleStroke,
                                    focusedContainerColor = ScannerGlassPalette.FrostedCardDark,
                                    unfocusedContainerColor = ScannerGlassPalette.FrostedCardDark,
                                    focusedTextColor = ScannerGlassPalette.TextPrimary,
                                    unfocusedTextColor = ScannerGlassPalette.TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scanner_search_bar")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Structured Scrollable Content (supports hold-for-actions + keep-holding-to-drag-reorder)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                containerCoords = coords
                            }
                            .pointerInput(Unit) {
                                val touchSlop = viewConfiguration.touchSlop

                                fun findHitDocAndOrigin(pos: Offset): Pair<String, Offset>? {
                                    val visibleKeys = visibleDocKeys()
                                    for ((id, rect) in docPhysicalRects) {
                                        if (id in visibleKeys && rect.contains(pos)) {
                                            return id to rect.topLeft
                                        }
                                    }
                                    return null
                                }

                                awaitEachGesture {
                                    val initialEvent = awaitPointerEvent(PointerEventPass.Initial)
                                    val firstDown = initialEvent.changes.firstOrNull { it.changedToDownIgnoreConsumed() }
                                        ?: return@awaitEachGesture
                                    if (initialEvent.changes.count { it.pressed } != 1) return@awaitEachGesture

                                    val pointerId = firstDown.id
                                    val downPos = firstDown.position
                                    val initialHit = findHitDocAndOrigin(downPos) ?: return@awaitEachGesture

                                    pressedDocId = initialHit.first
                                    var latestPos = downPos
                                    var holdActivated = false
                                    val startTime = SystemClock.uptimeMillis()

                                    try {
                                        while (true) {
                                            val remaining = 500L - (SystemClock.uptimeMillis() - startTime)
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
                                        pressedDocId = null

                                        val hit = findHitDocAndOrigin(latestPos) ?: initialHit
                                        val (hitDocId, cardOrigin) = hit
                                        val listSnapshot = currentDisplayedDocs
                                        val hitDoc = listSnapshot.firstOrNull { it.id == hitDocId } ?: return@awaitEachGesture
                                        val canReorderInGroup = listSnapshot.count { it.isPinned == hitDoc.isPinned } > 1

                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showAddMoreSourceChoices = false
                                        selectedDocIds = if (selectedDocIds.isEmpty()) {
                                            setOf(hitDoc.id)
                                        } else if (hitDoc.id !in selectedDocIds) {
                                            selectedDocIds + hitDoc.id
                                        } else {
                                            selectedDocIds
                                        }

                                        val grabAnchorPos = latestPos
                                        val dragStartSlop = maxOf(touchSlop * 1.35f, with(density) { 12.dp.toPx() })
                                        var startedDragging = false

                                        // Keep-holding loop:
                                        // - Lifting without dragging keeps contextual / selection bar open
                                        // - Moving finger without lifting enters Drag & Reorder
                                        while (true) {
                                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                                            val ch = ev.changes.firstOrNull { it.id == pointerId }
                                            ev.changes.forEach { it.consume() }
                                            if (ch == null || !ch.pressed || ev.changes.none { it.pressed }) {
                                                break
                                            }
                                            latestPos = ch.position
                                            if (canReorderInGroup && (latestPos - grabAnchorPos).getDistance() > dragStartSlop) {
                                                startedDragging = true
                                                break
                                            }
                                        }

                                        if (!startedDragging) return@awaitEachGesture

                                        selectedDocIds = emptySet()
                                        draggedDocId = hitDocId
                                        grabOffsetInsideCard = grabAnchorPos - cardOrigin
                                        currentPointerPos = latestPos
                                        lastSwappedKey = null
                                        lastSwapTimeMs = 0L
                                        lastSwapPointerPos = latestPos
                                        lastKnownTranslationXY[0] = latestPos.x - grabAnchorPos.x
                                        lastKnownTranslationXY[1] = latestPos.y - grabAnchorPos.y
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        trySwapDraggedDoc(latestPos, hitDocId, currentIsGridView)

                                        while (true) {
                                            val dragEvent = awaitPointerEvent(PointerEventPass.Initial)
                                            val change = dragEvent.changes.firstOrNull { it.id == pointerId }
                                            dragEvent.changes.forEach { it.consume() }
                                            if (change == null || !change.pressed || dragEvent.changes.none { it.pressed }) {
                                                break
                                            }
                                            latestPos = change.position
                                            currentPointerPos = latestPos
                                            trySwapDraggedDoc(latestPos, hitDocId, currentIsGridView)
                                        }

                                        val committed = currentDisplayedDocs.toList()
                                        pendingCommitOrderIds = committed.map { it.id }
                                        draggedDocId = null
                                        lastKnownTranslationXY[0] = 0f
                                        lastKnownTranslationXY[1] = 0f
                                        scannerViewModel.reorderDocuments(committed)
                                    } finally {
                                        pressedDocId = null
                                        if (draggedDocId != null) {
                                            draggedDocId = null
                                            lastKnownTranslationXY[0] = 0f
                                            lastKnownTranslationXY[1] = 0f
                                        }
                                    }
                                }
                            }
                    ) {
                        LazyVerticalGrid(
                            state = docGridState,
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 112.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Quick Actions Bento Row (shown cleanly at top when not searching)
                            if (searchQuery.isBlank() && currentFolderId == null) {
                                item(
                                    key = "quick_actions",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        HubQuickActionTile(
                                            icon = Icons.Default.Badge,
                                            label = if (isArabic) "ماسح ID" else "ID Scanner",
                                            accentColor = ScannerGlassPalette.CyanGlow,
                                            highlighted = false,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("hub_open_camera_scanner_btn"),
                                            onClick = onLaunchAppIdScanner
                                        )
                                        HubQuickActionTile(
                                            icon = Icons.AutoMirrored.Filled.MenuBook,
                                            label = if (isArabic) "قراءة PDF" else "Read PDF",
                                            accentColor = ScannerGlassPalette.VioletGlow,
                                            highlighted = false,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("hub_read_pdf_btn"),
                                            onClick = { scannerViewModel.openPdfLibrary() }
                                        )
                                        HubQuickActionTile(
                                            icon = Icons.Default.PictureAsPdf,
                                            label = if (isArabic) "أدوات PDF" else "PDF Tools",
                                            accentColor = ScannerGlassPalette.EmeraldGlow,
                                            highlighted = false,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("hub_pdf_tools_btn"),
                                            onClick = { showPdfToolsSheet = true }
                                        )
                                    }
                                }
                            }

                            // Compact Segmented Filter Bar (All / Starred / Tag Dropdown)
                            item(
                                key = "filter_bar",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChipPill(
                                        label = if (isArabic) "الكل" else "All",
                                        selected = !onlyStarred && selectedTag == null,
                                        accentColor = ScannerGlassPalette.CyanGlow,
                                        onClick = {
                                            scannerViewModel.clearFilters()
                                        }
                                    )

                                    FilterChipPill(
                                        label = if (isArabic) "المميزة" else "Starred",
                                        icon = if (onlyStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                        selected = onlyStarred,
                                        accentColor = ScannerGlassPalette.AmberGlow,
                                        onClick = { scannerViewModel.setOnlyStarredFilter(!onlyStarred) }
                                    )

                                    Box {
                                        val activePreset = ScannerTagCatalog.presets.firstOrNull { it.name == selectedTag }
                                        val tagAccent = activePreset?.let { Color(it.colorArgb.toInt()) } ?: ScannerGlassPalette.VioletGlow
                                        Row(
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .clip(CircleShape)
                                                .scannerGlassSurface(CircleShape, glowBorder = selectedTag != null)
                                                .clickable { showTagMenu = true }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            if (activePreset != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(tagAccent)
                                                )
                                            }
                                            Text(
                                                text = selectedTag ?: if (isArabic) "الوسوم" else "Tags",
                                                color = if (selectedTag != null) tagAccent else ScannerGlassPalette.TextSecondary,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = ScannerGlassPalette.TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showTagMenu,
                                            onDismissRequest = { showTagMenu = false },
                                            containerColor = ScannerGlassPalette.MidnightNavy
                                        ) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = if (isArabic) "كل الوسوم" else "All Tags",
                                                        color = if (selectedTag == null) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextPrimary,
                                                        fontWeight = if (selectedTag == null) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    scannerViewModel.setSelectedTagFilter(null)
                                                    showTagMenu = false
                                                }
                                            )
                                            ScannerTagCatalog.presets.forEach { preset ->
                                                val dotColor = Color(preset.colorArgb.toInt())
                                                val isSelected = selectedTag == preset.name
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(dotColor)
                                                        )
                                                    },
                                                    text = {
                                                        Text(
                                                            text = preset.name,
                                                            color = if (isSelected) dotColor else ScannerGlassPalette.TextPrimary,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    },
                                                    onClick = {
                                                        scannerViewModel.setSelectedTagFilter(preset.name)
                                                        showTagMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Folders Section (Clean Header + Horizontal Cards)
                            if (searchQuery.isBlank() && selectedTag == null && !onlyStarred) {
                                item(
                                    key = "folders_section",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isArabic) "المجلدات" else "Folders",
                                                color = ScannerGlassPalette.TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable { showCreateFolderDialog = true }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CreateNewFolder,
                                                    contentDescription = null,
                                                    tint = ScannerGlassPalette.CyanGlow,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = if (isArabic) "مجلد جديد" else "New Folder",
                                                    color = ScannerGlassPalette.CyanGlow,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        if (childFolders.isNotEmpty()) {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items(childFolders, key = { it.id }) { folder ->
                                                    FolderCard(
                                                        folder = folder,
                                                        onClick = {
                                                            if (folder.isLocked && folder.id !in unlockedIds) {
                                                                onRequestProtectedAccess(folder.id) {
                                                                    scannerViewModel.markProtectedUnlocked(folder.id)
                                                                    scannerViewModel.setCurrentFolderId(folder.id)
                                                                }
                                                            } else {
                                                                scannerViewModel.setCurrentFolderId(folder.id)
                                                            }
                                                        },
                                                        onToggleLock = {
                                                            if (folder.isLocked && folder.id !in unlockedIds) {
                                                                onRequestProtectedAccess(folder.id) {
                                                                    scannerViewModel.markProtectedUnlocked(folder.id)
                                                                    scannerViewModel.toggleFolderLock(folder, false, isArabic)
                                                                }
                                                            } else {
                                                                scannerViewModel.toggleFolderLock(folder, !folder.isLocked, isArabic)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Documents Section Header + Sort Picker
                            item(
                                key = "docs_header",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isArabic) "المستندات (${displayedDocs.size})" else "Documents (${displayedDocs.size})",
                                        color = ScannerGlassPalette.TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )

                                    Box {
                                        Row(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .scannerGlassSurface(CircleShape)
                                                .clickable { showSortMenu = true }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Sort,
                                                contentDescription = "Sort",
                                                tint = ScannerGlassPalette.VioletGlow,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = if (isArabic) sortOption.labelAr else sortOption.labelEn,
                                                color = ScannerGlassPalette.TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showSortMenu,
                                            onDismissRequest = { showSortMenu = false },
                                            containerColor = ScannerGlassPalette.MidnightNavy
                                        ) {
                                            SortOption.entries.forEach { opt ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = if (isArabic) opt.labelAr else opt.labelEn,
                                                            color = if (sortOption == opt) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextPrimary,
                                                            fontWeight = if (sortOption == opt) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    },
                                                    onClick = {
                                                        scannerViewModel.setSortOption(opt)
                                                        showSortMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Documents Body: Empty State, Clean 2-Col Grid, or List Cards
                            if (displayedDocs.isEmpty()) {
                                item(
                                    key = "empty_docs",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .scannerGlassSurface(RoundedCornerShape(24.dp))
                                            .padding(horizontal = 24.dp, vertical = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        ScannerLaserIcon(modifier = Modifier.size(56.dp))
                                        Text(
                                            text = if (isArabic) "لا توجد مستندات بعد" else "No Documents Yet",
                                            color = ScannerGlassPalette.TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isArabic) {
                                                "صوّر المستندات والبطاقات والكتب أو استوردها من المعرض للبدء."
                                            } else {
                                                "Scan receipts, ID cards, contracts, or import photos from your gallery."
                                            },
                                            color = ScannerGlassPalette.TextSecondary,
                                            fontSize = 12.5.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                gridItems(
                                    items = displayedDocs,
                                    key = { it.id },
                                    span = if (isGridView) null else { _ -> GridItemSpan(maxLineSpan) }
                                ) { doc ->
                                    val isDragging = draggedDocId == doc.id
                                    val isCardPressed = pressedDocId == doc.id && !isDragging
                                    val dragScale by animateFloatAsState(
                                        targetValue = if (isDragging) 1.04f else 1f,
                                        label = "doc_drag_scale"
                                    )
                                    val slotRectState = remember { mutableStateOf<Rect?>(null) }
                                    DisposableEffect(doc.id) {
                                        onDispose {
                                            docPhysicalRects.remove(doc.id)
                                        }
                                    }

                                    val outerSlotModifier = (if (isDragging) {
                                        Modifier.zIndex(10f)
                                    } else {
                                        Modifier.animateItem()
                                    }).onGloballyPositioned { coords ->
                                        val container = containerCoords
                                        if (container != null && container.isAttached && coords.isAttached) {
                                            val topLeft = container.localPositionOf(coords, Offset.Zero)
                                            val rect = Rect(topLeft, coords.size.toSize())
                                            docPhysicalRects[doc.id] = rect
                                            if (isDragging && slotRectState.value != rect) {
                                                slotRectState.value = rect
                                            }
                                        }
                                    }

                                    val cardDragModifier = if (isDragging) {
                                        Modifier.graphicsLayer {
                                            val origin = slotRectState.value?.topLeft
                                                ?: docPhysicalRects[doc.id]?.topLeft
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
                                            shape = RoundedCornerShape(if (isGridView) 20.dp else 18.dp)
                                            shadowElevation = 24.dp.toPx()
                                        }
                                    } else {
                                        Modifier
                                    }

                                    val handleDocClick = {
                                        showAddMoreSourceChoices = false
                                        if (selectedDocIds.isNotEmpty()) {
                                            selectedDocIds = if (doc.id in selectedDocIds) {
                                                selectedDocIds - doc.id
                                            } else {
                                                selectedDocIds + doc.id
                                            }
                                        } else if (doc.isLocked && doc.id !in unlockedIds) {
                                            onRequestProtectedAccess(doc.id) {
                                                scannerViewModel.markProtectedUnlocked(doc.id)
                                                scannerViewModel.openDocumentDetail(doc.id)
                                            }
                                        } else {
                                            scannerViewModel.openDocumentDetail(doc.id)
                                        }
                                    }

                                    val cardSelected = doc.id in selectedDocIds
                                    Box(modifier = outerSlotModifier) {
                                        if (isGridView) {
                                            DocumentGridCard(
                                                doc = doc,
                                                isArabic = isArabic,
                                                isUnlocked = !doc.isLocked || doc.id in unlockedIds,
                                                isSelectionMode = isSelectionMode,
                                                isSelected = cardSelected,
                                                isDragging = isDragging,
                                                isPressed = isCardPressed,
                                                modifier = cardDragModifier.fillMaxWidth(),
                                                onClick = handleDocClick,
                                                onToggleStar = { scannerViewModel.toggleStarred(doc) }
                                            )
                                        } else {
                                            DocumentListCard(
                                                doc = doc,
                                                isArabic = isArabic,
                                                isUnlocked = !doc.isLocked || doc.id in unlockedIds,
                                                isSelectionMode = isSelectionMode,
                                                isSelected = cardSelected,
                                                isDragging = isDragging,
                                                isPressed = isCardPressed,
                                                modifier = cardDragModifier,
                                                onClick = handleDocClick,
                                                onToggleStar = { scannerViewModel.toggleStarred(doc) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Primary Flagship Bottom Scan Button (Google ML Kit Document Scanner)
            if (!showTrash) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .height(56.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("hub_primary_scan_btn")
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(ScannerGlassPalette.CyanGlow, ScannerGlassPalette.VioletGlow)
                            )
                        )
                        .border(1.2.dp, ScannerGlassPalette.SubtleStroke, CircleShape)
                        .clickable { onLaunchPrimaryScan() }
                        .padding(horizontal = 30.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = if (isArabic) "مسح مستند" else "Scan",
                        tint = ScannerGlassPalette.OnAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (isArabic) "مسح مستند" else "Scan",
                        color = ScannerGlassPalette.OnAccent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.5.sp
                    )
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        var folderColor by remember { mutableLongStateOf(0xFF22D3EEL) }
        Dialog(onDismissRequest = { showCreateFolderDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scannerGlassSurface(RoundedCornerShape(24.dp), glowBorder = true, fillAlpha = 0.95f)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "إنشاء مجلد جديد" else "Create New Folder",
                    color = ScannerGlassPalette.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(if (isArabic) "اسم المجلد" else "Folder Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScannerGlassPalette.CyanGlow,
                        unfocusedBorderColor = ScannerGlassPalette.SubtleStroke,
                        focusedTextColor = ScannerGlassPalette.TextPrimary,
                        unfocusedTextColor = ScannerGlassPalette.TextPrimary,
                        focusedLabelColor = ScannerGlassPalette.CyanGlow,
                        unfocusedLabelColor = ScannerGlassPalette.TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(0xFF22D3EEL, 0xFFA855F7L, 0xFF34D399L, 0xFFF59E0BL, 0xFFF43F5EL).forEach { argb ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(argb.toInt()))
                                .border(if (folderColor == argb) 2.5.dp else 0.dp, ScannerGlassPalette.TextPrimary, CircleShape)
                                .clickable { folderColor = argb }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showCreateFolderDialog = false }) {
                        Text(if (isArabic) "إلغاء" else "Cancel", color = ScannerGlassPalette.TextSecondary)
                    }
                    Button(
                        enabled = folderName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                        onClick = {
                            scannerViewModel.createFolder(folderName, folderColor, isArabic)
                            showCreateFolderDialog = false
                        }
                    ) {
                        Text(if (isArabic) "إنشاء" else "Create", color = ScannerGlassPalette.OnAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // PDF Utility Studio (Full-Screen iOS 27 Dark Liquid Glass Overlay)
    PdfToolsSheet(
        visible = showPdfToolsSheet,
        isArabic = isArabic,
        onMergePdfs = { uris, outName -> scannerViewModel.mergePdfs(uris, outName, isArabic) },
        onSplitPdf = { uri, outName -> scannerViewModel.splitPdf(uri, outName, isArabic) },
        onCompressPdf = { uri, quality, outName -> scannerViewModel.compressPdf(uri, quality, outName, isArabic) },
        onProtectPdf = { uri, pwd, outName -> scannerViewModel.passwordProtectPdf(uri, pwd, outName, isArabic) },
        onEditPdf = { uris -> scannerViewModel.importDeviceFiles(uris, appendToDocId = null, isArabic = isArabic) },
        onDismiss = { showPdfToolsSheet = false }
    )

    // Rename Document Dialog triggered from Contextual Top Bar
    docToRename?.let { targetDoc ->
        var draftTitle by remember(targetDoc.id) { mutableStateOf(targetDoc.title) }
        Dialog(onDismissRequest = { docToRename = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosLiquidGlass(RoundedCornerShape(24.dp), glowAccent = true, fillAlpha = 0.94f)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "إعادة تسمية المستند" else "Rename Document",
                    color = ScannerGlassPalette.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScannerGlassPalette.CyanGlow,
                        unfocusedBorderColor = ScannerGlassPalette.SubtleStroke,
                        focusedTextColor = ScannerGlassPalette.TextPrimary,
                        unfocusedTextColor = ScannerGlassPalette.TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { docToRename = null }) {
                        Text(if (isArabic) "إلغاء" else "Cancel", color = ScannerGlassPalette.TextSecondary)
                    }
                    Button(
                        enabled = draftTitle.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                        onClick = {
                            scannerViewModel.renameDocument(targetDoc, draftTitle.trim())
                            docToRename = null
                        }
                    ) {
                        Text(if (isArabic) "حفظ" else "Save", color = ScannerGlassPalette.OnAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun ScannerContextualTopActionBar(
    doc: ScannedDocumentEntity,
    isArabic: Boolean,
    showAddMoreChoices: Boolean,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onToggleAddMore: () -> Unit,
    onAddMoreCamera: () -> Unit,
    onAddMoreFiles: () -> Unit,
    onDelete: () -> Unit
) {
    val deleteRed = ScannerGlassPalette.RoseGlow

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scanner_contextual_top_bar"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Exact 56.dp height pill matching OutlinedTextField search bar slot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .scannerGlassSurface(RoundedCornerShape(22.dp), glowBorder = true)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ScannerGlassPalette.TextPrimary.copy(alpha = 0.08f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClickLabel = if (isArabic) "إلغاء التحديد" else "Cancel selection",
                        onClick = onDismiss
                    )
                    .testTag("context_menu_close_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = if (isArabic) "إلغاء التحديد" else "Cancel selection",
                    tint = ScannerGlassPalette.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            ScannerContextualActionPill(
                icon = Icons.Default.PushPin,
                label = if (doc.isPinned) {
                    if (isArabic) "إلغاء" else "Unpin"
                } else {
                    if (isArabic) "تثبيت" else "Pin"
                },
                contentDescription = if (doc.isPinned) {
                    if (isArabic) "إلغاء التثبيت" else "Unpin"
                } else {
                    if (isArabic) "تثبيت في الأعلى" else "Pin to top"
                },
                tint = ScannerGlassPalette.CyanGlow,
                labelColor = ScannerGlassPalette.TextPrimary,
                fillColor = Color.Transparent,
                testTag = "context_menu_pin_btn",
                onClick = onTogglePin,
                modifier = Modifier.weight(1f)
            )

            ScannerContextualActionPill(
                icon = Icons.Default.Edit,
                label = if (isArabic) "تعديل" else "Rename",
                contentDescription = if (isArabic) "إعادة التسمية" else "Rename",
                tint = ScannerGlassPalette.VioletGlow,
                labelColor = ScannerGlassPalette.TextPrimary,
                fillColor = Color.Transparent,
                testTag = "context_menu_rename_btn",
                onClick = onRename,
                modifier = Modifier.weight(1f)
            )

            ScannerContextualActionPill(
                icon = Icons.Default.AddCircleOutline,
                label = if (isArabic) "إضافة" else "Add",
                contentDescription = if (isArabic) "إضافة صفحات المزيد" else "Add More",
                tint = ScannerGlassPalette.EmeraldGlow,
                labelColor = ScannerGlassPalette.TextPrimary,
                fillColor = if (showAddMoreChoices) ScannerGlassPalette.EmeraldGlow.copy(alpha = 0.18f) else Color.Transparent,
                testTag = "context_menu_add_more_btn",
                onClick = onToggleAddMore,
                modifier = Modifier.weight(1f)
            )

            ScannerContextualActionPill(
                icon = Icons.Default.DeleteOutline,
                label = if (isArabic) "حذف" else "Delete",
                contentDescription = if (isArabic) "حذف" else "Delete",
                tint = deleteRed,
                labelColor = deleteRed,
                fillColor = deleteRed.copy(alpha = 0.16f),
                testTag = "context_menu_delete_btn",
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedVisibility(
            visible = showAddMoreChoices,
            enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { -it / 4 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it / 4 }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .minimumInteractiveComponentSize()
                        .scannerGlassSurface(RoundedCornerShape(16.dp), glowBorder = true)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = ScannerGlassPalette.CyanGlow),
                            onClick = onAddMoreCamera
                        )
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = ScannerGlassPalette.CyanGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "الكاميرا" else "Camera",
                        color = ScannerGlassPalette.TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .minimumInteractiveComponentSize()
                        .scannerGlassSurface(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = ScannerGlassPalette.VioletGlow),
                            onClick = onAddMoreFiles
                        )
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = ScannerGlassPalette.VioletGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "ملفات الهاتف" else "Files",
                        color = ScannerGlassPalette.TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerBatchSelectionTopBar(
    selectedCount: Int,
    allSelected: Boolean,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onBatchDelete: () -> Unit
) {
    val deleteRed = ScannerGlassPalette.RoseGlow

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("scanner_batch_selection_top_bar")
            .scannerGlassSurface(RoundedCornerShape(22.dp), glowBorder = true)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(16.dp))
                .background(ScannerGlassPalette.TextPrimary.copy(alpha = 0.08f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClickLabel = if (isArabic) "إلغاء التحديد" else "Cancel selection",
                    onClick = onDismiss
                )
                .testTag("context_menu_close_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = if (isArabic) "إلغاء التحديد" else "Cancel selection",
                tint = ScannerGlassPalette.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = if (isArabic) "$selectedCount محدد" else "$selectedCount selected",
            color = ScannerGlassPalette.CyanGlow,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .testTag("context_menu_selected_count")
        )

        ScannerContextualActionPill(
            icon = Icons.Default.DoneAll,
            label = if (allSelected) {
                if (isArabic) "إلغاء الكل" else "None"
            } else {
                if (isArabic) "الكل" else "All"
            },
            contentDescription = if (allSelected) {
                if (isArabic) "إلغاء تحديد الكل" else "Deselect all"
            } else {
                if (isArabic) "تحديد الكل" else "Select all"
            },
            tint = ScannerGlassPalette.CyanGlow,
            labelColor = ScannerGlassPalette.TextPrimary,
            fillColor = ScannerGlassPalette.CyanGlow.copy(alpha = 0.14f),
            testTag = "context_menu_select_all_btn",
            onClick = onToggleSelectAll,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        ScannerContextualActionPill(
            icon = Icons.Default.DeleteOutline,
            label = if (isArabic) "حذف ($selectedCount)" else "Delete ($selectedCount)",
            contentDescription = if (isArabic) "حذف المحدد" else "Delete selected",
            tint = deleteRed,
            labelColor = deleteRed,
            fillColor = deleteRed.copy(alpha = 0.18f),
            testTag = "context_menu_batch_delete_btn",
            onClick = onBatchDelete,
            modifier = Modifier.padding(end = 2.dp)
        )
    }
}

@Composable
private fun ScannerContextualActionPill(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    tint: Color,
    labelColor: Color,
    fillColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fillColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = tint),
                onClickLabel = contentDescription,
                onClick = onClick
            )
            .padding(horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HubQuickActionTile(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(84.dp)
            .minimumInteractiveComponentSize()
            .scannerGlassSurface(RoundedCornerShape(20.dp), glowBorder = highlighted)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = ScannerGlassPalette.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(if (selected) accentColor.copy(alpha = 0.18f) else ScannerGlassPalette.FrostedCardDark)
            .border(
                width = 1.dp,
                color = if (selected) accentColor.copy(alpha = 0.70f) else ScannerGlassPalette.SubtleStroke,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accentColor else ScannerGlassPalette.TextSecondary,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = label,
            color = if (selected) accentColor else ScannerGlassPalette.TextSecondary,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun FolderCard(
    folder: ScannerFolderEntity,
    onClick: () -> Unit,
    onToggleLock: () -> Unit
) {
    val color = Color(folder.colorHex.toInt())
    Row(
        modifier = Modifier
            .width(156.dp)
            .minimumInteractiveComponentSize()
            .scannerGlassSurface(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Text(
                text = folder.name,
                color = ScannerGlassPalette.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onToggleLock,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = if (folder.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Lock folder",
                tint = if (folder.isLocked) ScannerGlassPalette.EmeraldGlow else ScannerGlassPalette.TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun DocumentGridCard(
    doc: ScannedDocumentEntity,
    isArabic: Boolean,
    isUnlocked: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isDragging: Boolean = false,
    isPressed: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onToggleStar: () -> Unit
) {
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !isDragging) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f),
        label = "grid_card_press_scale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .testTag("doc_card_${doc.id}")
            .scannerGlassSurface(RoundedCornerShape(20.dp), glowBorder = doc.isPinned || isSelected || isDragging)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ScannerGlassPalette.ThumbnailFrame),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = ScannerGlassPalette.EmeraldGlow, modifier = Modifier.size(36.dp))
            } else if (doc.coverThumbnailPath.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = remember(doc.coverThumbnailPath) { File(doc.coverThumbnailPath) },
                    contentDescription = doc.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { ScannerLaserIcon(modifier = Modifier.size(40.dp)) },
                    error = { ScannerLaserIcon(modifier = Modifier.size(40.dp)) }
                )
            } else {
                ScannerLaserIcon(modifier = Modifier.size(40.dp))
            }

            if (doc.isPinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(ScannerGlassPalette.FrostedElevated)
                        .padding(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = if (isArabic) "مثبت" else "Pinned",
                        tint = ScannerGlassPalette.CyanGlow,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ScannerGlassPalette.FrostedElevated)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("${doc.pageCount}p", color = ScannerGlassPalette.CyanGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                if (isSelectionMode) {
                    ScannerSelectionCircleBadge(
                        isSelected = isSelected,
                        isArabic = isArabic,
                        modifier = Modifier.testTag("doc_select_badge_${doc.id}")
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    color = ScannerGlassPalette.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatScannerBytes(doc.totalSizeBytes, isArabic),
                    color = ScannerGlassPalette.TextSecondary,
                    fontSize = 11.sp
                )
            }
            if (!isSelectionMode) {
                IconButton(onClick = onToggleStar, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (doc.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star",
                        tint = if (doc.isStarred) ScannerGlassPalette.AmberGlow else ScannerGlassPalette.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentListCard(
    doc: ScannedDocumentEntity,
    isArabic: Boolean,
    isUnlocked: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isDragging: Boolean = false,
    isPressed: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onToggleStar: () -> Unit
) {
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !isDragging) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f),
        label = "list_card_press_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .testTag("doc_card_${doc.id}")
            .scannerGlassSurface(RoundedCornerShape(18.dp), glowBorder = doc.isPinned || isSelected || isDragging)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ScannerGlassPalette.ThumbnailFrame),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = ScannerGlassPalette.EmeraldGlow)
            } else if (doc.coverThumbnailPath.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = remember(doc.coverThumbnailPath) { File(doc.coverThumbnailPath) },
                    contentDescription = doc.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { ScannerLaserIcon(modifier = Modifier.size(28.dp)) },
                    error = { ScannerLaserIcon(modifier = Modifier.size(28.dp)) }
                )
            } else {
                ScannerLaserIcon(modifier = Modifier.size(28.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (doc.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = if (isArabic) "مثبت" else "Pinned",
                        tint = ScannerGlassPalette.CyanGlow,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = doc.title,
                    color = ScannerGlassPalette.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Text(
                text = if (isArabic) "${doc.pageCount} صفحات • ${formatScannerBytes(doc.totalSizeBytes, true)}" else "${doc.pageCount} pages • ${formatScannerBytes(doc.totalSizeBytes, false)}",
                color = ScannerGlassPalette.TextSecondary,
                fontSize = 11.5.sp
            )
            if (doc.tagsCsv.isNotBlank()) {
                Text(
                    text = doc.tagsCsv.replace(",", " • "),
                    color = ScannerGlassPalette.CyanGlow,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .testTag("doc_select_badge_${doc.id}"),
                contentAlignment = Alignment.Center
            ) {
                ScannerSelectionCircleBadge(
                    isSelected = isSelected,
                    isArabic = isArabic
                )
            }
        } else {
            IconButton(onClick = onToggleStar) {
                Icon(
                    imageVector = if (doc.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Star",
                    tint = if (doc.isStarred) ScannerGlassPalette.AmberGlow else ScannerGlassPalette.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ScannerSelectionCircleBadge(
    isSelected: Boolean,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) ScannerGlassPalette.CyanGlow
                else ScannerGlassPalette.MidnightNavy.copy(alpha = 0.72f)
            )
            .border(
                width = 1.5.dp,
                color = if (isSelected) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextPrimary.copy(alpha = 0.7f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (isArabic) "محدد" else "Selected",
                tint = ScannerGlassPalette.OnAccent,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
