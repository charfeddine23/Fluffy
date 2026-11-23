package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.charfeddine.fluffy.scanner.utils.MlKitDocumentScannerHelper
import com.charfeddine.fluffy.scanner.utils.OfflineOcrAndBarcodeEngine
import com.example.audio.rememberAudioPlaybackController
import com.example.data.model.ChecklistItem
import com.example.data.model.EditorPageSegment
import com.example.data.model.GlassTint
import com.example.data.model.InsertFeatureType
import com.example.data.model.MarkdownTableData
import com.example.data.model.NoteBackgroundCanvas
import com.example.data.model.NoteBackgroundCatalog
import com.example.data.model.NoteEntity
import com.example.data.model.NotePagesCodec
import com.example.data.model.RichFormatAction
import com.example.data.model.RichTextCodec
import com.example.data.storage.AudioCloudPolicy
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.DocumentStyleBottomSheet
import com.example.ui.components.EditorNoteHeader
import com.example.ui.components.EditorPageTitleField
import com.example.ui.components.EditorTopAppBar
import com.example.ui.components.InsertFeaturesBottomSheet
import com.example.ui.components.RichContentView
import com.example.ui.components.SegmentedRichPageEditor
import com.example.ui.components.TableEditorDialog
import com.example.ui.components.UnifiedWritingBottomBar
import com.example.ui.components.VoiceRecorderDialog
import com.example.ui.components.applyActionToSegmentedPage
import com.example.ui.components.insertSnippetInSegmentedPage
import com.example.ui.components.insertTableInSegmentedPage
import com.example.ui.components.resolveActiveTextSegment
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NoteEditorScreen(
    note: NoteEntity,
    isCreatingNew: Boolean = note.id == 0L,
    isDark: Boolean,
    isArabic: Boolean,
    isPremium: Boolean = false,
    onRequestPremiumUpgrade: () -> Unit = {},
    onBack: () -> Unit,
    onSave: (
        title: String,
        content: String,
        pagesJson: String,
        category: String,
        colorPreset: String,
        backgroundId: String,
        customColorArgb: Long,
        isPinned: Boolean,
        isFavorite: Boolean,
        checklist: List<ChecklistItem>,
        audioDurationSec: Int,
        audioFilePath: String,
        audioByteSize: Long
    ) -> Unit,
    onDelete: () -> Unit,
    onToggleArchive: () -> Unit = {},
    onMoveToVault: (NoteEntity) -> Unit = {},
    onAutoSave: ((
        title: String,
        content: String,
        pagesJson: String,
        category: String,
        colorPreset: String,
        backgroundId: String,
        customColorArgb: Long,
        isPinned: Boolean,
        isFavorite: Boolean,
        checklist: List<ChecklistItem>,
        audioDurationSec: Int,
        audioFilePath: String,
        audioByteSize: Long
    ) -> Unit)? = null
) {
    val context = LocalContext.current
    val playbackController = rememberAudioPlaybackController()
    val coroutineScope = rememberCoroutineScope()

    // New drafts open in Edit/Writing mode; saved notes reopen in View-Only mode
    var isEditMode by remember { mutableStateOf(isCreatingNew || note.id == 0L) }

    // Local mutable state for literal multi-page editing with per-page titles (up to 100 pages or 900 KB)
    var title by remember { mutableStateOf(note.title) }
    val initialDraftPages = remember(note.id, note.syncId) {
        NotePagesCodec.decodeDraftPages(note.content, note.pagesJson)
    }
    val pages = remember(note.id, note.syncId) {
        mutableStateListOf<String>().apply {
            addAll(initialDraftPages.map { it.body }.ifEmpty { listOf("") })
        }
    }
    val pageTitles = remember(note.id, note.syncId) {
        mutableStateListOf<String>().apply {
            addAll(initialDraftPages.map { it.title }.ifEmpty { listOf("") })
        }
    }
    val activeSegmentByPage = remember(note.id, note.syncId) { mutableStateMapOf<Int, Int>() }
    val segmentSelections = remember(note.id, note.syncId) { mutableStateMapOf<String, TextRange>() }
    val segmentCompositions = remember(note.id, note.syncId) { mutableStateMapOf<String, TextRange?>() }
    val pendingStylesByPage = remember(note.id, note.syncId) { mutableStateMapOf<Int, Set<RichFormatAction>>() }

    var category by remember { mutableStateOf(note.category) }
    var selectedTint by remember { mutableStateOf(GlassTint.fromId(note.colorPreset)) }
    var backgroundId by remember { mutableStateOf(note.backgroundId) }
    var customColorArgb by remember { mutableLongStateOf(note.customColorArgb) }
    var isPinned by remember { mutableStateOf(note.isPinned) }
    var isFavorite by remember { mutableStateOf(note.isFavorite) }
    var audioDurationSec by remember { mutableIntStateOf(note.audioDurationSec) }
    var audioFilePath by remember { mutableStateOf(note.audioFilePath) }
    var audioByteSize by remember {
        mutableLongStateOf(AudioCloudPolicy.effectiveAudioByteSize(note.audioByteSize, note.audioFilePath))
    }

    var showVoiceRecorderDialog by remember { mutableStateOf(false) }
    var showStyleSheet by remember { mutableStateOf(false) }
    var styleSheetInitialTab by remember { mutableIntStateOf(0) }
    var showPagePickerDialog by remember { mutableStateOf(false) }
    var showInsertSheet by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var showImageScanDialog by remember { mutableStateOf(false) }
    var isExtractingOcr by remember { mutableStateOf(false) }
    var isFocusMode by remember { mutableStateOf(false) }
    var editorTextAlign by remember(note.id) { mutableStateOf(TextAlign.Start) }
    var editingTableTarget by remember { mutableStateOf<MarkdownTableData?>(null) }
    var isRemovingPage by remember { mutableStateOf(false) }
    var limitWarningMessage by remember { mutableStateOf<String?>(null) }
    var copyFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var justCopiedPage by remember { mutableStateOf(false) }
    var readZoomScale by remember { mutableFloatStateOf(1f) }
    var readPanOffset by remember { mutableStateOf(Offset.Zero) }
    var readViewportSize by remember { mutableStateOf(IntSize.Zero) }
    var writingFontSizeSp by remember(note.id) { mutableFloatStateOf(16f) }

    val checklistItems = remember {
        mutableStateListOf<ChecklistItem>().apply {
            addAll(ChecklistItem.jsonToList(note.checklistJson))
        }
    }

    val maxAllowedPages = NotePagesCodec.maxPagesForTier(isPremium)
    val pagesSnapshot = pages.toList()
    val pageTitlesSnapshot = pageTitles.toList()
    val encodedPageEntries = remember(pagesSnapshot, pageTitlesSnapshot) {
        pagesSnapshot.indices.map { idx ->
            NotePagesCodec.encodePageEntry(pageTitlesSnapshot.getOrElse(idx) { "" }, pagesSnapshot[idx])
        }
    }
    val page1Content = encodedPageEntries.firstOrNull().orEmpty()
    val encodedExtraPages = remember(encodedPageEntries) { NotePagesCodec.encodeExtraPages(encodedPageEntries) }
    val totalNoteBytes = remember(title, encodedPageEntries, checklistItems.toList()) {
        NotePagesCodec.estimateNoteBytes(title, encodedPageEntries, checklistItems.toList())
    }
    val canAppendAnotherPage = pages.size < maxAllowedPages &&
        totalNoteBytes + 8L <= NotePagesCodec.MAX_NOTE_BYTES

    val pagerPageCount = if (isEditMode && canAppendAnotherPage) {
        pages.size + 1
    } else {
        pages.size.coerceAtLeast(1)
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pagerPageCount })
    val pageChipListState = rememberLazyListState()

    // Reset zoom & pan cleanly when toggling Edit mode or switching pages
    LaunchedEffect(isEditMode, pagerState.currentPage) {
        readZoomScale = 1f
        readPanOffset = Offset.Zero
        if (!isEditMode) {
            pendingStylesByPage.clear()
        }
    }

    // Auto-create page ONLY when user genuinely settles onto the trailing virtual slot (never during deletion)
    LaunchedEffect(
        pagerState.settledPage,
        pagerState.currentPage,
        pagerState.isScrollInProgress,
        isEditMode,
        pages.size,
        canAppendAnotherPage,
        isPremium,
        isRemovingPage
    ) {
        if (isEditMode &&
            !isRemovingPage &&
            !pagerState.isScrollInProgress &&
            pagerState.currentPage == pages.size &&
            pagerState.settledPage == pages.size
        ) {
            if (canAppendAnotherPage) {
                pages.add("")
                pageTitles.add("")
            } else {
                limitWarningMessage = if (pages.size >= maxAllowedPages) {
                    if (isPremium) {
                        if (isArabic) "تم الوصول إلى الحد الأقصى: 100 صفحة في المسودة" else "Maximum limit reached: 100 pages per note"
                    } else {
                        if (isArabic) "الحد الأقصى في الوضع المجاني 5 صفحات — قم بالترقية لإضافة حتى 100 صفحة" else "Free limit: 5 pages max — Upgrade to Premium for up to 100 pages"
                    }
                } else {
                    if (isArabic) "تم الوصول إلى الحد الأقصى للحجم: 900 كيلوبايت للمسودة" else "Note size limit reached: 900 KB max"
                }
                pagerState.animateScrollToPage((pages.size - 1).coerceAtLeast(0))
            }
        }
    }

    // Keep horizontal page pill strip scrolled to active page
    LaunchedEffect(pagerState.currentPage, pages.size) {
        val target = pagerState.currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        if (pages.isNotEmpty()) {
            pageChipListState.animateScrollToItem(target)
        }
    }

    // Auto-clear limit warning & copy feedback banners
    LaunchedEffect(limitWarningMessage) {
        if (limitWarningMessage != null) {
            kotlinx.coroutines.delay(3400)
            limitWarningMessage = null
        }
    }

    LaunchedEffect(justCopiedPage, copyFeedbackMessage) {
        if (justCopiedPage || copyFeedbackMessage != null) {
            kotlinx.coroutines.delay(2200)
            justCopiedPage = false
            copyFeedbackMessage = null
        }
    }

    fun triggerLimitWarning(isCountLimit: Boolean = false) {
        limitWarningMessage = if (isCountLimit) {
            if (isPremium) {
                if (isArabic) "الحد الأقصى 100 صفحة لكل مسودة" else "Maximum 100 pages per note"
            } else {
                if (isArabic) "الحد الأقصى في الوضع المجاني 5 صفحات — قم بالترقية لـ 100 صفحة" else "Free mode limit: 5 pages max — Upgrade for 100 pages"
            }
        } else {
            if (isArabic) "تم الوصول للحد الأقصى لحجم المسودة (900 كيلوبايت)" else "Note size limit reached (900 KB)"
        }
    }

    fun addPageAndNavigate() {
        if (pages.size >= maxAllowedPages) {
            triggerLimitWarning(isCountLimit = true)
            return
        }
        if (totalNoteBytes + 8L > NotePagesCodec.MAX_NOTE_BYTES) {
            triggerLimitWarning(isCountLimit = false)
            return
        }
        pages.add("")
        pageTitles.add("")
        val newIndex = pages.lastIndex
        coroutineScope.launch {
            pagerState.animateScrollToPage(newIndex)
        }
    }

    fun removePageAt(index: Int) {
        if (pages.size <= 1 || index !in pages.indices || isRemovingPage) return
        val curPage = pagerState.currentPage
        val nextTarget = when {
            curPage == index -> (index - 1).coerceAtLeast(0)
            curPage > index -> curPage - 1
            else -> curPage
        }
        coroutineScope.launch {
            isRemovingPage = true
            try {
                // Pre-scroll away BEFORE shrinking `pages` so `settledPage == pages.size` never triggers auto-append
                if (pagerState.currentPage >= index) {
                    val safePreScroll = nextTarget.coerceIn(0, (pages.size - 2).coerceAtLeast(0))
                    pagerState.scrollToPage(safePreScroll)
                }
                pages.removeAt(index)
                if (index in pageTitles.indices) {
                    pageTitles.removeAt(index)
                }
                val activeSnap = activeSegmentByPage.toMap()
                val pendingSnap = pendingStylesByPage.toMap()
                val selSnap = segmentSelections.toMap()
                val compSnap = segmentCompositions.toMap()
                activeSegmentByPage.clear()
                pendingStylesByPage.clear()
                segmentSelections.clear()
                segmentCompositions.clear()
                activeSnap.forEach { (k, v) ->
                    if (k < index) activeSegmentByPage[k] = v else if (k > index) activeSegmentByPage[k - 1] = v
                }
                pendingSnap.forEach { (k, v) ->
                    if (k < index) pendingStylesByPage[k] = v else if (k > index) pendingStylesByPage[k - 1] = v
                }
                fun remapSegmentKey(key: String): String? {
                    val parts = key.split(':')
                    if (parts.size != 2) return null
                    val p = parts[0].toIntOrNull() ?: return null
                    val s = parts[1].toIntOrNull() ?: return null
                    return when {
                        p < index -> key
                        p > index -> "${p - 1}:$s"
                        else -> null
                    }
                }
                selSnap.forEach { (k, v) -> remapSegmentKey(k)?.let { segmentSelections[it] = v } }
                compSnap.forEach { (k, v) -> remapSegmentKey(k)?.let { segmentCompositions[it] = v } }
                val clamped = nextTarget.coerceIn(0, pages.lastIndex)
                if (pagerState.currentPage != clamped) {
                    pagerState.scrollToPage(clamped)
                }
            } finally {
                isRemovingPage = false
            }
        }
    }

    fun updateTitleBounded(newTitle: String) {
        val currentBytesWithoutTitle = totalNoteBytes - title.toByteArray(Charsets.UTF_8).size.toLong()
        val budget = (NotePagesCodec.MAX_NOTE_BYTES - currentBytesWithoutTitle).coerceAtLeast(0L)
        val (fitted, truncated) = NotePagesCodec.fitWithinByteBudget(newTitle, budget)
        title = fitted
        if (truncated) triggerLimitWarning(isCountLimit = false)
    }

    fun updatePageTitleBounded(pageIndex: Int, newPageTitle: String) {
        if (pageIndex !in pages.indices) return
        while (pageTitles.size < pages.size) pageTitles.add("")
        val cleanNew = newPageTitle.replace("\n", " ").replace("\r", "")
        val currentRaw = NotePagesCodec.encodePageEntry(pageTitles[pageIndex], pages[pageIndex])
        val otherBytes = totalNoteBytes - currentRaw.toByteArray(Charsets.UTF_8).size.toLong()
        val safetyMargin = if (pageIndex > 0) 8L else 0L
        val candidateRaw = NotePagesCodec.encodePageEntry(cleanNew, pages[pageIndex])
        val budget = (NotePagesCodec.MAX_NOTE_BYTES - otherBytes - safetyMargin).coerceAtLeast(0L)
        if (candidateRaw.toByteArray(Charsets.UTF_8).size.toLong() <= budget) {
            pageTitles[pageIndex] = cleanNew
        } else {
            val existingBodyBytes = pages[pageIndex].toByteArray(Charsets.UTF_8).size.toLong() + 36L
            val titleBudget = (budget - existingBodyBytes).coerceAtLeast(0L)
            val (fitted, _) = NotePagesCodec.fitWithinByteBudget(cleanNew, titleBudget)
            pageTitles[pageIndex] = fitted
            triggerLimitWarning(isCountLimit = false)
        }
    }

    fun updatePageTextBounded(pageIndex: Int, newText: String): Boolean {
        if (pageIndex !in pages.indices) return false
        val pTitle = pageTitles.getOrElse(pageIndex) { "" }
        val currentRaw = NotePagesCodec.encodePageEntry(pTitle, pages[pageIndex])
        val otherBytes = totalNoteBytes - currentRaw.toByteArray(Charsets.UTF_8).size.toLong()
        val headerOverhead = if (pTitle.isBlank()) 0L else {
            (NotePagesCodec.encodePageEntry(pTitle, "").toByteArray(Charsets.UTF_8).size.toLong())
        }
        val safetyMargin = if (pageIndex > 0) 6L else 0L
        val budget = (NotePagesCodec.MAX_NOTE_BYTES - otherBytes - headerOverhead - safetyMargin).coerceAtLeast(0L)
        val (fitted, truncated) = NotePagesCodec.fitWithinByteBudget(newText, budget)
        pages[pageIndex] = fitted
        if (truncated) triggerLimitWarning(isCountLimit = false)
        return truncated
    }

    fun pageSegmentSelections(pageIndex: Int): Map<Int, TextRange> {
        val prefix = "$pageIndex:"
        val out = mutableMapOf<Int, TextRange>()
        segmentSelections.forEach { (k, v) ->
            if (k.startsWith(prefix)) {
                k.removePrefix(prefix).toIntOrNull()?.let { out[it] = v }
            }
        }
        return out
    }

    fun pageSegmentCompositions(pageIndex: Int): Map<Int, TextRange?> {
        val prefix = "$pageIndex:"
        val out = mutableMapOf<Int, TextRange?>()
        segmentCompositions.forEach { (k, v) ->
            if (k.startsWith(prefix)) {
                k.removePrefix(prefix).toIntOrNull()?.let { out[it] = v }
            }
        }
        return out
    }

    fun clearPageSegments(pageIndex: Int) {
        val prefix = "$pageIndex:"
        segmentSelections.keys.filter { it.startsWith(prefix) }.forEach { segmentSelections.remove(it) }
        segmentCompositions.keys.filter { it.startsWith(prefix) }.forEach { segmentCompositions.remove(it) }
    }

    fun applyFormatActionToPage(pageIndex: Int, action: RichFormatAction) {
        if (pageIndex !in pages.indices) return
        val result = applyActionToSegmentedPage(
            pageText = pages[pageIndex],
            preferredSegmentIndex = activeSegmentByPage[pageIndex] ?: 0,
            segmentSelections = pageSegmentSelections(pageIndex),
            segmentCompositions = pageSegmentCompositions(pageIndex),
            pendingStyles = pendingStylesByPage[pageIndex] ?: emptySet(),
            action = action
        )
        val truncated = updatePageTextBounded(pageIndex, result.updatedPageText)
        val latestSegments = RichTextCodec.splitPageIntoEditorSegments(pages[pageIndex])
        val (_, newSeg) = resolveActiveTextSegment(latestSegments, result.targetSegmentIndex)
        activeSegmentByPage[pageIndex] = result.targetSegmentIndex
        segmentSelections["$pageIndex:${result.targetSegmentIndex}"] = if (truncated) {
            TextRange(newSeg.text.length)
        } else {
            TextRange(
                result.updatedSelection.start.coerceIn(0, newSeg.text.length),
                result.updatedSelection.end.coerceIn(0, newSeg.text.length)
            )
        }
        segmentCompositions["$pageIndex:${result.targetSegmentIndex}"] = null
        pendingStylesByPage[pageIndex] = result.updatedPendingStyles
    }

    fun applyInsertFeatureToPage(pageIndex: Int, feature: InsertFeatureType) {
        if (pageIndex !in pages.indices) return
        val snippet = RichTextCodec.buildInsertSnippet(feature, isArabic)
        if (feature == InsertFeatureType.PROS_CONS_TABLE) {
            val updatedPageText = insertTableInSegmentedPage(
                pageText = pages[pageIndex],
                preferredSegmentIndex = activeSegmentByPage[pageIndex] ?: 0,
                segmentSelections = pageSegmentSelections(pageIndex),
                tableMarkdown = snippet.trimEnd('\n')
            )
            updatePageTextBounded(pageIndex, updatedPageText)
            val latestSegments = RichTextCodec.splitPageIntoEditorSegments(pages[pageIndex])
            clearPageSegments(pageIndex)
            val tailIdx = latestSegments.indexOfLast { it is EditorPageSegment.TextSegment }.coerceAtLeast(0)
            activeSegmentByPage[pageIndex] = tailIdx
            segmentSelections["$pageIndex:$tailIdx"] = TextRange(0)
            pendingStylesByPage.remove(pageIndex)
            return
        }
        val needNewLine = feature == InsertFeatureType.DAILY_HEADER ||
            feature == InsertFeatureType.PROGRESS_BAR ||
            feature == InsertFeatureType.MEETING_TEMPLATE
        val result = insertSnippetInSegmentedPage(
            pageText = pages[pageIndex],
            preferredSegmentIndex = activeSegmentByPage[pageIndex] ?: 0,
            segmentSelections = pageSegmentSelections(pageIndex),
            snippet = snippet,
            ensureNewLinePrefix = needNewLine
        )
        val truncated = updatePageTextBounded(pageIndex, result.updatedPageText)
        val latestSegments = RichTextCodec.splitPageIntoEditorSegments(pages[pageIndex])
        val (_, newSeg) = resolveActiveTextSegment(latestSegments, result.targetSegmentIndex)
        activeSegmentByPage[pageIndex] = result.targetSegmentIndex
        segmentSelections["$pageIndex:${result.targetSegmentIndex}"] = if (truncated) {
            TextRange(newSeg.text.length)
        } else {
            TextRange(
                result.updatedSelection.start.coerceIn(0, newSeg.text.length),
                result.updatedSelection.end.coerceIn(0, newSeg.text.length)
            )
        }
        segmentCompositions["$pageIndex:${result.targetSegmentIndex}"] = null
        pendingStylesByPage.remove(pageIndex)
    }

    fun triggerImmediateSaveIfViewMode() {
        if (!isEditMode) {
            onAutoSave?.invoke(
                title, page1Content, encodedExtraPages, category, selectedTint.id, backgroundId, customColorArgb,
                isPinned, isFavorite, checklistItems.toList(),
                audioDurationSec, audioFilePath, audioByteSize
            )
        }
    }

    fun extractOcrFromUrisAndInsert(uris: List<Uri>) {
        if (uris.isEmpty() || isExtractingOcr) return
        val targetPageIdx = pagerState.currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        coroutineScope.launch {
            isExtractingOcr = true
            try {
                val extractedChunks = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        val bitmap = decodeScaledBitmapFromUri(context, uri) ?: return@mapNotNull null
                        try {
                            OfflineOcrAndBarcodeEngine.extractTextOffline(bitmap, context).fullText.trim()
                        } finally {
                            bitmap.recycle()
                        }
                    }.filter { it.isNotBlank() }
                }
                val combined = extractedChunks.joinToString("\n\n")
                if (combined.isNotBlank()) {
                    val segResult = insertSnippetInSegmentedPage(
                        pageText = pages[targetPageIdx],
                        preferredSegmentIndex = activeSegmentByPage[targetPageIdx] ?: 0,
                        segmentSelections = pageSegmentSelections(targetPageIdx),
                        snippet = combined,
                        ensureNewLinePrefix = true
                    )
                    updatePageTextBounded(targetPageIdx, segResult.updatedPageText)
                    val latestSegments = RichTextCodec.splitPageIntoEditorSegments(pages[targetPageIdx])
                    val (_, newSeg) = resolveActiveTextSegment(latestSegments, segResult.targetSegmentIndex)
                    activeSegmentByPage[targetPageIdx] = segResult.targetSegmentIndex
                    segmentSelections["$targetPageIdx:${segResult.targetSegmentIndex}"] = TextRange(
                        segResult.updatedSelection.start.coerceIn(0, newSeg.text.length),
                        segResult.updatedSelection.end.coerceIn(0, newSeg.text.length)
                    )
                    segmentCompositions["$targetPageIdx:${segResult.targetSegmentIndex}"] = null
                    pendingStylesByPage.remove(targetPageIdx)
                    justCopiedPage = true
                    copyFeedbackMessage = if (isArabic) "تم استخراج النص وإدراجه في الصفحة ✓" else "Text extracted & inserted ✓"
                } else {
                    justCopiedPage = false
                    copyFeedbackMessage = if (isArabic) "لم يتم العثور على نص واضح في الصورة" else "No readable text found in image"
                }
            } finally {
                isExtractingOcr = false
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            extractOcrFromUrisAndInsert(listOf(uri))
        }
    }

    val mlKitScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val payload = MlKitDocumentScannerHelper.parseResult(result.data)
            if (payload != null && payload.pageImageUris.isNotEmpty()) {
                extractOcrFromUrisAndInsert(payload.pageImageUris)
            }
        }
    }

    fun buildCurrentEditorNote(): NoteEntity {
        return note.copy(
            title = title,
            content = page1Content,
            pagesJson = encodedExtraPages,
            category = category,
            colorPreset = selectedTint.id,
            backgroundId = backgroundId,
            customColorArgb = customColorArgb,
            isPinned = isPinned,
            isFavorite = isFavorite,
            checklistJson = ChecklistItem.listToJson(checklistItems.filter { it.text.isNotBlank() }),
            audioDurationSec = audioDurationSec,
            audioFilePath = audioFilePath,
            audioByteSize = AudioCloudPolicy.effectiveAudioByteSize(audioByteSize, audioFilePath)
        )
    }

    // Real-time automatic background save with 500ms debounce (active only in Writing/Edit mode)
    LaunchedEffect(
        isEditMode, title, page1Content, encodedExtraPages, category, selectedTint.id, backgroundId, customColorArgb,
        isPinned, isFavorite, checklistItems.toList(),
        audioDurationSec, audioFilePath, audioByteSize
    ) {
        if (!isEditMode) return@LaunchedEffect
        kotlinx.coroutines.delay(500)
        onAutoSave?.invoke(
            title, page1Content, encodedExtraPages, category, selectedTint.id, backgroundId, customColorArgb,
            isPinned, isFavorite, checklistItems.toList(),
            audioDurationSec, audioFilePath, audioByteSize
        )
    }

    // Handle Android system back gesture cleanly for both Edit & View modes
    BackHandler {
        playbackController.stop()
        if (isEditMode) {
            onSave(
                title, page1Content, encodedExtraPages, category, selectedTint.id, backgroundId, customColorArgb,
                isPinned, isFavorite, checklistItems.toList(),
                audioDurationSec, audioFilePath, audioByteSize
            )
        } else {
            if (isPinned != note.isPinned ||
                selectedTint.id != note.colorPreset ||
                backgroundId != note.backgroundId ||
                customColorArgb != note.customColorArgb ||
                category != note.category
            ) {
                onAutoSave?.invoke(
                    title, page1Content, encodedExtraPages, category, selectedTint.id, backgroundId, customColorArgb,
                    isPinned, isFavorite, checklistItems.toList(),
                    audioDurationSec, audioFilePath, audioByteSize
                )
            }
            onBack()
        }
    }

    val activeBgPreset = remember(backgroundId) { NoteBackgroundCatalog.findById(backgroundId) }
    val surfaceColors = remember(isDark, selectedTint, customColorArgb, backgroundId) {
        NoteBackgroundCatalog.resolveSurfaceColors(
            themeIsDark = isDark,
            tint = selectedTint,
            customColorArgb = customColorArgb,
            backgroundId = backgroundId
        )
    }
    val effectiveDark = surfaceColors.effectiveDark
    val textColor = surfaceColors.titleColor
    val bodyColor = surfaceColors.bodyColor
    val metaColor = surfaceColors.metaColor
    val accentColor = surfaceColors.accentColor
    val pillFill = surfaceColors.pillFill
    val hasCustomVisualStyle = activeBgPreset != null || customColorArgb != 0L || selectedTint != GlassTint.FROST

    FrostedBackdrop(isDark = isDark) {
        // Per-document background or custom document color layer
        if (activeBgPreset != null) {
            NoteBackgroundCanvas(
                preset = activeBgPreset,
                modifier = Modifier.fillMaxSize()
            )
        } else if (customColorArgb != 0L) {
            val baseDocColor = Color(customColorArgb.toInt())
            val deepShade = if (surfaceColors.effectiveDark) {
                Color(0xFF020617).copy(alpha = 0.28f)
            } else {
                Color.White.copy(alpha = 0.16f)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(baseDocColor, baseDocColor)
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, deepShade)
                        )
                    )
            )
        } else if (selectedTint != GlassTint.FROST) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        selectedTint.accentColor.copy(alpha = if (isDark) 0.14f else 0.12f)
                    )
            )
        }

        val density = LocalDensity.current
        val isImeVisible = WindowInsets.ime.getBottom(density) > WindowInsets.navigationBars.getBottom(density)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
        ) {
            // 1. Clean Top App Bar: Back on start; Pin + Save/Done (or Edit) + Single Overflow Menu (⋮) on end
            EditorTopAppBar(
                isEditMode = isEditMode,
                isPinned = isPinned,
                isFavorite = isFavorite,
                isArchived = note.isArchived,
                canArchiveOrDelete = note.id != 0L,
                isFocusMode = isFocusMode,
                isDark = effectiveDark,
                isArabic = isArabic,
                selectedTint = selectedTint,
                accentColor = accentColor,
                textColor = textColor,
                metaColor = metaColor,
                pillFill = pillFill,
                onBack = {
                    playbackController.stop()
                    if (isEditMode) {
                        onSave(
                            title, page1Content, encodedExtraPages, category, selectedTint.id, backgroundId, customColorArgb,
                            isPinned, isFavorite, checklistItems.toList(),
                            audioDurationSec, audioFilePath, audioByteSize
                        )
                    } else {
                        if (isPinned != note.isPinned ||
                            selectedTint.id != note.colorPreset ||
                            backgroundId != note.backgroundId ||
                            customColorArgb != note.customColorArgb ||
                            category != note.category
                        ) {
                            onAutoSave?.invoke(
                                title, page1Content, encodedExtraPages, category, selectedTint.id, backgroundId, customColorArgb,
                                isPinned, isFavorite, checklistItems.toList(),
                                audioDurationSec, audioFilePath, audioByteSize
                            )
                        }
                        onBack()
                    }
                },
                onTogglePin = {
                    isPinned = !isPinned
                    triggerImmediateSaveIfViewMode()
                },
                onSaveOrEdit = {
                    if (isEditMode) {
                        onSave(
                            title, page1Content, encodedExtraPages, category, selectedTint.id, backgroundId, customColorArgb,
                            isPinned, isFavorite, checklistItems.toList(),
                            audioDurationSec, audioFilePath, audioByteSize
                        )
                    } else {
                        isEditMode = true
                    }
                },
                onAddPage = { addPageAndNavigate() },
                onMoveToVault = {
                    playbackController.stop()
                    onMoveToVault(buildCurrentEditorNote())
                },
                onExportNote = {
                    shareNoteText(context, title, encodedPageEntries, checklistItems, isArabic)
                },
                onCopyPage = {
                    val currentReadPageIdx = pagerState.currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                    val rawPageEntry = encodedPageEntries.getOrNull(currentReadPageIdx).orEmpty()
                    val cleanPageText = NotePagesCodec.plainTextSnippet(rawPageEntry)
                    val fallbackChecklistText = if (currentReadPageIdx == 0 && checklistItems.isNotEmpty()) {
                        checklistItems
                            .filter { it.text.isNotBlank() }
                            .joinToString("\n") { item ->
                                "${if (item.isDone) "[✓]" else "[ ]"} ${item.text}"
                            }
                    } else {
                        ""
                    }
                    val textToCopy = cleanPageText.ifBlank { fallbackChecklistText }
                    if (textToCopy.isNotEmpty()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Fluffy Page Content", textToCopy))
                        justCopiedPage = true
                        copyFeedbackMessage = if (isArabic) {
                            "تم نسخ محتوى الصفحة ${currentReadPageIdx + 1} ✓"
                        } else {
                            "Page ${currentReadPageIdx + 1} copied ✓"
                        }
                    } else {
                        justCopiedPage = false
                        copyFeedbackMessage = if (isArabic) "الصفحة فارغة للنسخ" else "Current page is empty"
                    }
                },
                onToggleFocusMode = { isFocusMode = !isFocusMode },
                onToggleFavorite = {
                    isFavorite = !isFavorite
                    triggerImmediateSaveIfViewMode()
                },
                onToggleArchive = {
                    playbackController.stop()
                    onToggleArchive()
                },
                onDelete = {
                    playbackController.stop()
                    onDelete()
                }
            )

            // 2. Streamlined Note Title & Compact Single-Row Multi-Page Pill
            EditorNoteHeader(
                title = title,
                isEditMode = isEditMode,
                isFocusMode = isFocusMode,
                pages = pagesSnapshot,
                currentPage = pagerState.currentPage,
                canAppendAnotherPage = canAppendAnotherPage,
                totalNoteBytes = totalNoteBytes,
                isPremium = isPremium,
                maxAllowedPages = maxAllowedPages,
                pageChipListState = pageChipListState,
                copyFeedbackMessage = copyFeedbackMessage,
                justCopiedPage = justCopiedPage,
                limitWarningMessage = limitWarningMessage,
                isDark = effectiveDark,
                isArabic = isArabic,
                accentColor = accentColor,
                textColor = textColor,
                metaColor = metaColor,
                pillFill = pillFill,
                onTitleChange = { updateTitleBounded(it) },
                onOpenPagePicker = { showPagePickerDialog = true },
                onJumpToPage = { idx ->
                    coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                },
                onRemoveCurrentPage = { removePageAt(pagerState.currentPage) },
                onAddPage = { addPageAndNavigate() },
                onRequestPremiumUpgrade = onRequestPremiumUpgrade
            )

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = isEditMode || readZoomScale <= 1.02f,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("editor_pages_pager")
            ) { pageIndex ->
                if (pageIndex < pages.size) {
                    val pageScrollState = rememberScrollState()
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .onSizeChanged { readViewportSize = it }
                            .pointerInput(isEditMode, pageIndex) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                    var wasMultiTouch = false
                                    do {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        val pressedChanges = event.changes.filter { it.pressed }
                                        if (isEditMode) {
                                            if (pressedChanges.size >= 2) {
                                                wasMultiTouch = true
                                                val zoomChange = event.calculateZoom()
                                                if (zoomChange.isFinite() && zoomChange > 0f && abs(zoomChange - 1f) > 0.001f) {
                                                    writingFontSizeSp = (writingFontSizeSp * zoomChange).coerceIn(12f, 32f)
                                                }
                                                event.changes.forEach { it.consume() }
                                            } else if (wasMultiTouch && pressedChanges.isNotEmpty()) {
                                                event.changes.forEach { it.consume() }
                                            }
                                        } else {
                                            if (pressedChanges.size >= 2) {
                                                wasMultiTouch = true
                                                val zoomChange = event.calculateZoom()
                                                val panChange = event.calculatePan()
                                                val previousCentroid = event.calculateCentroid(useCurrent = false)
                                                val oldScale = readZoomScale
                                                val newScale = (oldScale * zoomChange).coerceIn(1f, 3.0f)
                                                if (newScale <= 1.01f) {
                                                    readZoomScale = 1f
                                                    readPanOffset = Offset.Zero
                                                } else {
                                                    val zoomFactor = newScale / oldScale
                                                    val stageW = size.width.toFloat().coerceAtLeast(1f)
                                                    val stageH = size.height.toFloat().coerceAtLeast(1f)
                                                    val center = Offset(stageW / 2f, stageH / 2f)
                                                    val focal = if (previousCentroid.isSpecified) previousCentroid - center else Offset.Zero
                                                    val candidate = (readPanOffset - focal) * zoomFactor + focal + panChange
                                                    val maxX = (stageW * (newScale - 1f)) / 2f
                                                    val maxY = (stageH * (newScale - 1f)) / 2f
                                                    readZoomScale = newScale
                                                    readPanOffset = Offset(
                                                        x = candidate.x.coerceIn(-maxX, maxX),
                                                        y = candidate.y.coerceIn(-maxY, maxY)
                                                    )
                                                }
                                                event.changes.forEach { it.consume() }
                                            } else if (pressedChanges.size == 1 && !wasMultiTouch && readZoomScale > 1.02f) {
                                                val change = pressedChanges.first()
                                                val delta = change.position - change.previousPosition
                                                if (delta.getDistanceSquared() > 2f) {
                                                    val stageW = size.width.toFloat().coerceAtLeast(1f)
                                                    val stageH = size.height.toFloat().coerceAtLeast(1f)
                                                    val maxX = (stageW * (readZoomScale - 1f)) / 2f
                                                    val maxY = (stageH * (readZoomScale - 1f)) / 2f
                                                    val candidate = readPanOffset + delta
                                                    readPanOffset = Offset(
                                                        x = candidate.x.coerceIn(-maxX, maxX),
                                                        y = candidate.y.coerceIn(-maxY, maxY)
                                                    )
                                                    change.consume()
                                                }
                                            } else if (wasMultiTouch && pressedChanges.isNotEmpty()) {
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                    if (!isEditMode && readZoomScale <= 1.03f) {
                                        readZoomScale = 1f
                                        readPanOffset = Offset.Zero
                                    }
                                }
                            }
                    ) {
                        val minCanvasHeight = maxHeight
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = minCanvasHeight)
                                .graphicsLayer {
                                    val activeScale = if (!isEditMode) readZoomScale else 1f
                                    scaleX = activeScale
                                    scaleY = activeScale
                                    translationX = if (!isEditMode) readPanOffset.x else 0f
                                    translationY = if (!isEditMode) readPanOffset.y else 0f
                                }
                                .verticalScroll(pageScrollState)
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            // Audio & Checklist live cleanly at the top of Page 1
                            if (pageIndex == 0) {
                                if (audioDurationSec > 0 || audioFilePath.isNotBlank()) {
                                    val stableAudioTrackId = remember(note.syncId) {
                                        "editor_${note.syncId.ifBlank { note.id.toString() }}"
                                    }
                                    AudioPlayerCard(
                                        trackId = stableAudioTrackId,
                                        title = title.ifBlank {
                                            if (isArabic) "تسجيل صوتي مرفق" else "Attached Voice Memo"
                                        },
                                        filePathOrUri = audioFilePath,
                                        durationSec = audioDurationSec,
                                        byteSize = audioByteSize,
                                        playbackController = playbackController,
                                        isDark = effectiveDark,
                                        isArabic = isArabic,
                                        canDelete = isEditMode,
                                        onDelete = {
                                            audioDurationSec = 0
                                            audioFilePath = ""
                                            audioByteSize = 0L
                                        },
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }

                                if (checklistItems.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    ) {
                                        checklistItems.forEachIndexed { index, item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (item.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                    contentDescription = if (isArabic) "تحديد المهمة" else "Toggle checklist item",
                                                    tint = if (item.isDone) selectedTint.accentColor else metaColor,
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clickable(enabled = isEditMode) {
                                                            checklistItems[index] = item.copy(isDone = !item.isDone)
                                                        }
                                                )

                                                Spacer(modifier = Modifier.width(8.dp))

                                                if (isEditMode) {
                                                    BasicTextField(
                                                        value = item.text,
                                                        onValueChange = { newText ->
                                                            val curItemBytes = item.text.toByteArray(Charsets.UTF_8).size.toLong()
                                                            val budget = (NotePagesCodec.MAX_NOTE_BYTES - (totalNoteBytes - curItemBytes) - 8L).coerceAtLeast(0L)
                                                            val (fitted, truncated) = NotePagesCodec.fitWithinByteBudget(newText, budget)
                                                            checklistItems[index] = item.copy(text = fitted)
                                                            if (truncated) triggerLimitWarning(isCountLimit = false)
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        textStyle = TextStyle(
                                                            fontSize = (writingFontSizeSp - 1f).coerceIn(11f, 30f).sp,
                                                            color = if (item.isDone) metaColor else textColor,
                                                            textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                                                        ),
                                                        cursorBrush = SolidColor(selectedTint.accentColor),
                                                        decorationBox = { inner ->
                                                            if (item.text.isEmpty()) {
                                                                Text(
                                                                    text = if (isArabic) "عنصر مهمة..." else "Checklist item...",
                                                                    fontSize = (writingFontSizeSp - 1f).coerceIn(11f, 30f).sp,
                                                                    color = metaColor.copy(alpha = 0.55f)
                                                                )
                                                            }
                                                            inner()
                                                        }
                                                    )

                                                    IconButton(
                                                        onClick = { checklistItems.removeAt(index) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = if (isArabic) "إزالة العنصر" else "Remove",
                                                            tint = metaColor,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = item.text,
                                                        modifier = Modifier.weight(1f),
                                                        style = TextStyle(
                                                            fontSize = (writingFontSizeSp - 1f).coerceIn(11f, 30f).sp,
                                                            color = if (item.isDone) metaColor else textColor,
                                                            textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        if (isEditMode) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        if (totalNoteBytes + 32L <= NotePagesCodec.MAX_NOTE_BYTES) {
                                                            checklistItems.add(ChecklistItem(text = "", isDone = false))
                                                        } else {
                                                            triggerLimitWarning(isCountLimit = false)
                                                        }
                                                    }
                                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = selectedTint.accentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isArabic) "عنصر جديد" else "List item",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = selectedTint.accentColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            // Sleek optional per-page title input
                            val currentPageTitle = pageTitles.getOrElse(pageIndex) { "" }
                            EditorPageTitleField(
                                pageIndex = pageIndex,
                                currentPageTitle = currentPageTitle,
                                isEditMode = isEditMode,
                                isFocusMode = isFocusMode,
                                isDark = effectiveDark,
                                isArabic = isArabic,
                                accentColor = accentColor,
                                textColor = textColor,
                                metaColor = metaColor,
                                onPageTitleChange = { updatePageTitleBounded(pageIndex, it) }
                            )

                            val pageText = pages[pageIndex]
                            val richVisualTransformation = remember(accentColor, metaColor, effectiveDark) {
                                RichTextCodec.createRichVisualTransformation(
                                    accentColor = accentColor,
                                    metaColor = metaColor,
                                    isDark = effectiveDark
                                )
                            }
                            val remainingMinHeight = (minCanvasHeight - 130.dp).coerceAtLeast(180.dp)
                            val fieldTestTag = if (pageIndex == 0) {
                                if (isEditMode) "editor_content_input" else "editor_content_readonly"
                            } else {
                                if (isEditMode) "editor_content_input_page_$pageIndex" else "editor_content_readonly_page_$pageIndex"
                            }

                            if (isEditMode) {
                                val activeSegIdx = activeSegmentByPage[pageIndex] ?: 0
                                val pageSegSels = pageSegmentSelections(pageIndex)
                                val pageSegComps = pageSegmentCompositions(pageIndex)
                                val pendingStyles = pendingStylesByPage[pageIndex] ?: emptySet()

                                SegmentedRichPageEditor(
                                    pageText = pageText,
                                    activeSegmentIndex = activeSegIdx,
                                    segmentSelections = pageSegSels,
                                    segmentCompositions = pageSegComps,
                                    pendingInlineStyles = pendingStyles,
                                    richVisualTransformation = richVisualTransformation,
                                    fontSizeSp = writingFontSizeSp,
                                    minHeight = remainingMinHeight,
                                    placeholderText = if (pageIndex == 0) {
                                        if (isArabic) "اكتب أفكارك وملاحظاتك هنا بحرية..." else "Write your thoughts and notes freely..."
                                    } else {
                                        if (isArabic) "اكتب محتوى الصفحة ${pageIndex + 1} هنا..." else "Write page ${pageIndex + 1} here..."
                                    },
                                    fieldTestTag = fieldTestTag,
                                    isDark = effectiveDark,
                                    isArabic = isArabic,
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    bodyColor = bodyColor,
                                    metaColor = metaColor,
                                    onSegmentTextChanged = { updatedPageText, segIdx, newSegSel, newSegComp, nextPending ->
                                        val truncated = updatePageTextBounded(pageIndex, updatedPageText)
                                        val latestSegments = RichTextCodec.splitPageIntoEditorSegments(pages[pageIndex])
                                        val activeSeg = latestSegments.getOrNull(segIdx) as? EditorPageSegment.TextSegment
                                        val maxSegLen = activeSeg?.text?.length ?: 0
                                        activeSegmentByPage[pageIndex] = segIdx
                                        segmentSelections["$pageIndex:$segIdx"] = if (truncated) {
                                            TextRange(maxSegLen)
                                        } else {
                                            TextRange(
                                                newSegSel.start.coerceIn(0, maxSegLen),
                                                newSegSel.end.coerceIn(0, maxSegLen)
                                            )
                                        }
                                        segmentCompositions["$pageIndex:$segIdx"] = if (truncated) null else newSegComp
                                        pendingStylesByPage[pageIndex] = nextPending
                                    },
                                    onSegmentFocused = { segIdx ->
                                        if (activeSegmentByPage[pageIndex] != segIdx) {
                                            activeSegmentByPage[pageIndex] = segIdx
                                            pendingStylesByPage.remove(pageIndex)
                                        }
                                    },
                                    onEditTable = { tbl ->
                                        editingTableTarget = tbl
                                        showTableDialog = true
                                    },
                                    onDeleteTable = { tbl ->
                                        val updated = RichTextCodec.deleteTableAtRange(
                                            pageText,
                                            tbl.startOffset,
                                            tbl.endOffset
                                        )
                                        updatePageTextBounded(pageIndex, updated)
                                        clearPageSegments(pageIndex)
                                        activeSegmentByPage[pageIndex] = 0
                                        pendingStylesByPage.remove(pageIndex)
                                    },
                                    textAlign = editorTextAlign
                                )
                            } else {
                                SelectionContainer {
                                    RichContentView(
                                        rawText = pageText,
                                        fontSizeSp = writingFontSizeSp,
                                        isDark = effectiveDark,
                                        isArabic = isArabic,
                                        accentColor = accentColor,
                                        textColor = textColor,
                                        bodyColor = bodyColor,
                                        metaColor = metaColor,
                                        onToggleChecklistLine = { lineOffset ->
                                            val toggled = RichTextCodec.toggleChecklistLineAtOffset(pageText, lineOffset)
                                            if (toggled != pageText) {
                                                updatePageTextBounded(pageIndex, toggled)
                                                triggerImmediateSaveIfViewMode()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = remainingMinHeight)
                                            .testTag(fieldTestTag),
                                        textAlign = editorTextAlign
                                    )
                                }
                            }
                        }

                        if (!isEditMode && readZoomScale > 1.02f && pageIndex == pagerState.currentPage) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("editor_read_zoom_reset")
                                    .liquidGlass(
                                        isDark = effectiveDark,
                                        tint = selectedTint,
                                        shape = CircleShape,
                                        borderWidth = 1.dp,
                                        customFill = accentColor.copy(alpha = if (effectiveDark) 0.28f else 0.18f)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true)
                                    ) {
                                        readZoomScale = 1f
                                        readPanOffset = Offset.Zero
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1fx", readZoomScale)} ↺",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        } else if (isEditMode && abs(writingFontSizeSp - 16f) > 0.4f && pageIndex == pagerState.currentPage) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("editor_font_size_pill")
                                    .liquidGlass(
                                        isDark = effectiveDark,
                                        tint = selectedTint,
                                        shape = CircleShape,
                                        borderWidth = 1.dp,
                                        customFill = accentColor.copy(alpha = if (effectiveDark) 0.28f else 0.18f)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true)
                                    ) {
                                        writingFontSizeSp = 16f
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${writingFontSizeSp.roundToInt()} sp ↺",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                } else {
                    // Trailing virtual swipe slot -> settles seamlessly into Page N+1
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = if (isArabic) "جاري فتح الصفحة ${pages.size + 1}..." else "Opening Page ${pages.size + 1}...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 3. Unified Single-Row Dark Liquid Glass Formatting & Insert Bar
            AnimatedVisibility(
                visible = isEditMode,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                val activePageIdx = pagerState.currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                val activePageText = pages.getOrElse(activePageIdx) { "" }
                val activePageSegments = remember(activePageText) {
                    RichTextCodec.splitPageIntoEditorSegments(activePageText)
                }
                val preferredSeg = activeSegmentByPage[activePageIdx] ?: 0
                val (resolvedSegIdx, resolvedSeg) = resolveActiveTextSegment(activePageSegments, preferredSeg)
                val activeSegSel = (segmentSelections["$activePageIdx:$resolvedSegIdx"] ?: TextRange(resolvedSeg.text.length)).let {
                    TextRange(it.start.coerceIn(0, resolvedSeg.text.length), it.end.coerceIn(0, resolvedSeg.text.length))
                }
                val pendingStyles = pendingStylesByPage[activePageIdx] ?: emptySet()
                val activeStyles = remember(resolvedSeg.text, activeSegSel, pendingStyles) {
                    RichTextCodec.inspectActiveStyles(resolvedSeg.text, activeSegSel) + pendingStyles
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isImeVisible) {
                                if (effectiveDark) Color(0xFF070D19).copy(alpha = 0.96f)
                                else Color(0xFFF8FAFC).copy(alpha = 0.96f)
                            } else {
                                Color.Transparent
                            }
                        )
                ) {
                    if (isImeVisible) {
                        HorizontalDivider(
                            thickness = 0.8.dp,
                            color = accentColor.copy(alpha = if (effectiveDark) 0.22f else 0.14f)
                        )
                    }
                    UnifiedWritingBottomBar(
                        activeStyles = activeStyles,
                        currentAlignment = editorTextAlign,
                        hasCustomStyle = hasCustomVisualStyle,
                        isDark = effectiveDark,
                        isArabic = isArabic,
                        accentColor = accentColor,
                        textColor = textColor,
                        metaColor = metaColor,
                        pillFill = pillFill,
                        onFormatAction = { action -> applyFormatActionToPage(activePageIdx, action) },
                        onOpenAddMenu = { showInsertSheet = true },
                        onCycleAlignment = {
                            editorTextAlign = when (editorTextAlign) {
                                TextAlign.Start -> TextAlign.Center
                                TextAlign.Center -> TextAlign.End
                                else -> TextAlign.Start
                            }
                        },
                        onOpenStyleSheet = {
                            styleSheetInitialTab = if (activeBgPreset != null) 1 else 0
                            showStyleSheet = true
                        }
                    )
                }
            }
        }

        // Quick All-Pages Navigator Dialog (supports up to 100 pages cleanly)
        if (showPagePickerDialog) {
            Dialog(onDismissRequest = { showPagePickerDialog = false }) {
                LiquidGlassCard(
                    isDark = effectiveDark,
                    tint = selectedTint,
                    shape = RoundedCornerShape(24.dp),
                    borderWidth = 1.4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "صفحات المسودة (${pages.size}/$maxAllowedPages)" else "Draft Pages (${pages.size}/$maxAllowedPages)",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textColor
                                )
                                Text(
                                    text = if (isArabic) {
                                        "الحجم: ${NotePagesCodec.formatSizeKb(totalNoteBytes, true)} من 900 ك.ب"
                                    } else {
                                        "Size: ${NotePagesCodec.formatSizeKb(totalNoteBytes, false)} / 900 KB"
                                    },
                                    fontSize = 12.sp,
                                    color = metaColor
                                )
                            }
                            if (isEditMode) {
                                val atFreeCap = !isPremium && pages.size >= maxAllowedPages
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (atFreeCap) Color(0xFFF59E0B).copy(alpha = 0.22f)
                                            else accentColor.copy(alpha = 0.2f)
                                        )
                                        .clickable {
                                            showPagePickerDialog = false
                                            if (atFreeCap) {
                                                onRequestPremiumUpgrade()
                                            } else {
                                                addPageAndNavigate()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = if (atFreeCap) {
                                            if (isArabic) "ترقية (100) 👑" else "Upgrade (100) 👑"
                                        } else {
                                            if (isArabic) "+ صفحة" else "+ Page"
                                        },
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (atFreeCap) Color(0xFFF59E0B) else accentColor
                                    )
                                }
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            gridItemsIndexed(pages) { index, pageText ->
                                val isSelected = index == pagerState.currentPage
                                val pTitle = pageTitles.getOrElse(index) { "" }
                                val previewSnippet = RichTextCodec.stripFormattingForPreview(pageText).trim()
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) accentColor.copy(alpha = if (effectiveDark) 0.28f else 0.18f)
                                            else pillFill.copy(alpha = 0.72f)
                                        )
                                        .border(
                                            width = if (isSelected) 1.4.dp else 0.8.dp,
                                            color = if (isSelected) accentColor else metaColor.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            showPagePickerDialog = false
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isArabic) "ص ${index + 1}" else "P${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) accentColor else textColor
                                        )
                                        if (isEditMode && pages.size > 1) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.18f))
                                                    .clickable {
                                                        removePageAt(index)
                                                    }
                                                    .testTag("picker_delete_page_$index"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = if (isArabic) "حذف الصفحة" else "Delete Page",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (pTitle.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = pTitle,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = previewSnippet.ifBlank { if (isArabic) "فارغة" else "Empty" },
                                        fontSize = 10.5.sp,
                                        color = metaColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 20-Tool Insert Bottom Sheet (+ Add button)
        if (showInsertSheet) {
            val activeIdx = pagerState.currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))

            InsertFeaturesBottomSheet(
                isDark = effectiveDark,
                isArabic = isArabic,
                accentColor = accentColor,
                textColor = textColor,
                metaColor = metaColor,
                onDismiss = { showInsertSheet = false },
                onQuickImageScan = {
                    showInsertSheet = false
                    showImageScanDialog = true
                },
                onSelectFeature = { feature ->
                    showInsertSheet = false
                    when (feature) {
                        InsertFeatureType.TABLE -> {
                            editingTableTarget = null
                            showTableDialog = true
                        }
                        InsertFeatureType.VOICE_MEMO -> showVoiceRecorderDialog = true
                        InsertFeatureType.NEW_PAGE -> addPageAndNavigate()
                        InsertFeatureType.CHECKLIST_CARD -> {
                            if (totalNoteBytes + 32L <= NotePagesCodec.MAX_NOTE_BYTES) {
                                checklistItems.add(ChecklistItem(text = "", isDone = false))
                                if (pagerState.currentPage != 0) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                }
                            } else {
                                triggerLimitWarning(isCountLimit = false)
                            }
                        }
                        InsertFeatureType.TODO_CHECKBOX -> applyFormatActionToPage(activeIdx, RichFormatAction.CHECKBOX)
                        InsertFeatureType.BULLET_LIST -> applyFormatActionToPage(activeIdx, RichFormatAction.BULLET_LIST)
                        InsertFeatureType.NUMBERED_LIST -> applyFormatActionToPage(activeIdx, RichFormatAction.NUMBERED_LIST)
                        InsertFeatureType.QUOTE -> applyFormatActionToPage(activeIdx, RichFormatAction.QUOTE)
                        InsertFeatureType.CALLOUT -> applyFormatActionToPage(activeIdx, RichFormatAction.CALLOUT)
                        InsertFeatureType.CODE_BLOCK -> applyFormatActionToPage(activeIdx, RichFormatAction.CODE_BLOCK)
                        InsertFeatureType.INLINE_CODE -> applyFormatActionToPage(activeIdx, RichFormatAction.CODE)
                        InsertFeatureType.DIVIDER -> applyFormatActionToPage(activeIdx, RichFormatAction.DIVIDER)
                        else -> applyInsertFeatureToPage(activeIdx, feature)
                    }
                }
            )
        }

        // Image / Document Scanner OCR Insert Chooser Dialog
        if (showImageScanDialog) {
            Dialog(onDismissRequest = { showImageScanDialog = false }) {
                LiquidGlassCard(
                    isDark = effectiveDark,
                    tint = selectedTint,
                    shape = RoundedCornerShape(24.dp),
                    borderWidth = 1.2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isArabic) "استخراج نص من صورة أو مستند" else "Scan or Pick Image (OCR)",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                        Text(
                            text = if (isArabic) {
                                "يتم تحليل النص وإدراجه مباشرة في الصفحة الحالية"
                            } else {
                                "Recognized text will be inserted directly into this page"
                            },
                            fontSize = 12.sp,
                            color = metaColor
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(accentColor.copy(alpha = if (effectiveDark) 0.18f else 0.11f))
                                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .clickable {
                                    showImageScanDialog = false
                                    MlKitDocumentScannerHelper.launch(
                                        context = context,
                                        launcher = mlKitScanLauncher,
                                        onFallbackToCameraX = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    )
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                                .testTag("ocr_scan_doc_option"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "مسح مستند بالكاميرا" else "Scan Document",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = if (isArabic) "تصوير ورقة واستخراج النص تلقائياً" else "Auto-crop page and extract text",
                                    fontSize = 11.5.sp,
                                    color = metaColor
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(pillFill.copy(alpha = 0.72f))
                                .border(0.9.dp, metaColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                .clickable {
                                    showImageScanDialog = false
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                                .testTag("ocr_pick_photo_option"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "اختيار صورة من المعرض" else "Pick Image from Gallery",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = if (isArabic) "قراءة النصوص العربية والإنجليزية" else "Extract Arabic & Latin text offline",
                                    fontSize = 11.5.sp,
                                    color = metaColor
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isExtractingOcr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassCard(
                    isDark = effectiveDark,
                    tint = selectedTint,
                    shape = RoundedCornerShape(20.dp),
                    borderWidth = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.4.dp,
                            color = accentColor
                        )
                        Text(
                            text = if (isArabic) "جاري استخراج النص..." else "Extracting text...",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        }

        // Interactive Notion-Style Table Builder / Cell Editor Dialog
        if (showTableDialog) {
            val activeIdx = pagerState.currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
            val curBody = pages.getOrElse(activeIdx) { "" }
            val editTarget = editingTableTarget
            TableEditorDialog(
                initialTable = editTarget,
                isDark = effectiveDark,
                isArabic = isArabic,
                accentColor = accentColor,
                textColor = textColor,
                metaColor = metaColor,
                onDismiss = {
                    showTableDialog = false
                    editingTableTarget = null
                },
                onConfirmTable = { rows ->
                    val md = RichTextCodec.serializeTable(rows)
                    if (editTarget != null && editTarget.startOffset >= 0) {
                        val updated = RichTextCodec.replaceTableAtRange(
                            text = curBody,
                            startOffset = editTarget.startOffset,
                            endOffset = editTarget.endOffset,
                            tableMarkdown = md
                        ).text
                        updatePageTextBounded(activeIdx, updated)
                        clearPageSegments(activeIdx)
                    } else {
                        val updated = insertTableInSegmentedPage(
                            pageText = curBody,
                            preferredSegmentIndex = activeSegmentByPage[activeIdx] ?: 0,
                            segmentSelections = pageSegmentSelections(activeIdx),
                            tableMarkdown = md
                        )
                        updatePageTextBounded(activeIdx, updated)
                        clearPageSegments(activeIdx)
                        val latestSegments = RichTextCodec.splitPageIntoEditorSegments(pages[activeIdx])
                        val tailIdx = latestSegments.indexOfLast { it is EditorPageSegment.TextSegment }.coerceAtLeast(0)
                        activeSegmentByPage[activeIdx] = tailIdx
                        segmentSelections["$activeIdx:$tailIdx"] = TextRange(0)
                    }
                    pendingStylesByPage.remove(activeIdx)
                    showTableDialog = false
                    editingTableTarget = null
                },
                onDeleteTable = if (editTarget != null && editTarget.startOffset >= 0) {
                    {
                        val updated = RichTextCodec.deleteTableAtRange(
                            curBody,
                            editTarget.startOffset,
                            editTarget.endOffset
                        )
                        updatePageTextBounded(activeIdx, updated)
                        clearPageSegments(activeIdx)
                        activeSegmentByPage[activeIdx] = 0
                        pendingStylesByPage.remove(activeIdx)
                        showTableDialog = false
                        editingTableTarget = null
                    }
                } else null
            )
        }

        // Document Colors & 40 Backgrounds (20 Free + 20 Premium) Modal Bottom Sheet
        if (showStyleSheet) {
            DocumentStyleBottomSheet(
                isDark = isDark,
                isArabic = isArabic,
                isPremium = isPremium,
                selectedTint = selectedTint,
                customColorArgb = customColorArgb,
                backgroundId = backgroundId,
                category = category,
                initialTab = styleSheetInitialTab,
                onSelectTint = { newTint ->
                    selectedTint = newTint
                    triggerImmediateSaveIfViewMode()
                },
                onSelectCustomColor = { argb ->
                    customColorArgb = argb
                    if (argb != 0L) {
                        backgroundId = NoteBackgroundCatalog.NONE_ID
                    }
                    triggerImmediateSaveIfViewMode()
                },
                onSelectBackground = { bgId ->
                    backgroundId = bgId
                    if (bgId != NoteBackgroundCatalog.NONE_ID) {
                        customColorArgb = 0L
                    }
                    triggerImmediateSaveIfViewMode()
                },
                onSelectCategory = { newCat ->
                    category = newCat
                    triggerImmediateSaveIfViewMode()
                },
                onResetStyle = {
                    selectedTint = GlassTint.FROST
                    customColorArgb = 0L
                    backgroundId = NoteBackgroundCatalog.NONE_ID
                    triggerImmediateSaveIfViewMode()
                },
                onRequestPremiumUpgrade = {
                    showStyleSheet = false
                    onRequestPremiumUpgrade()
                },
                onDismiss = { showStyleSheet = false }
            )
        }

        // Voice Recorder Dialog
        if (showVoiceRecorderDialog) {
            VoiceRecorderDialog(
                isDark = effectiveDark,
                isArabic = isArabic,
                onDismiss = { showVoiceRecorderDialog = false },
                onSaveVoiceNote = { duration, filePath, byteSize ->
                    audioDurationSec = duration
                    audioFilePath = filePath
                    audioByteSize = byteSize
                    showVoiceRecorderDialog = false
                    if (pagerState.currentPage != 0) {
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    }
                }
            )
        }
    }
}

private fun shareNoteText(
    context: Context,
    title: String,
    pages: List<String>,
    checklist: List<ChecklistItem>,
    isArabic: Boolean
) {
    val body = NotePagesCodec.buildShareableDraftText(title, pages, checklist, isArabic)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "Share note via"))
}

private fun decodeScaledBitmapFromUri(context: Context, uri: Uri, maxDim: Int = 2048): Bitmap? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val largest = maxOf(w, h)
                if (largest > maxDim && largest > 0) {
                    val ratio = maxDim.toFloat() / largest.toFloat()
                    decoder.setTargetSize(
                        (w * ratio).roundToInt().coerceAtLeast(1),
                        (h * ratio).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            var sample = 1
            while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        }
    }.getOrNull()
}
