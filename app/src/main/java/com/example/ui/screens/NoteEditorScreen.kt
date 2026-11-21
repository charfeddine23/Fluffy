package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.rememberAudioPlaybackController
import com.example.data.model.ChecklistItem
import com.example.data.model.GlassTint
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
import com.example.ui.components.InlineTablesPreviewSection
import com.example.ui.components.NotionFormattingToolbar
import com.example.ui.components.RichContentView
import com.example.ui.components.TableEditorDialog
import com.example.ui.components.VoiceRecorderDialog
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
    val pageSelections = remember(note.id, note.syncId) { mutableStateMapOf<Int, TextRange>() }
    val pageCompositions = remember(note.id, note.syncId) { mutableStateMapOf<Int, TextRange?>() }
    val undoStacks = remember(note.id, note.syncId) { mutableStateMapOf<Int, MutableList<TextFieldValue>>() }
    val redoStacks = remember(note.id, note.syncId) { mutableStateMapOf<Int, MutableList<TextFieldValue>>() }

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
    var showMoreMenu by remember { mutableStateOf(false) }
    var showViewMoreMenu by remember { mutableStateOf(false) }
    var showPagePickerDialog by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
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
                val selSnap = pageSelections.toMap()
                val compSnap = pageCompositions.toMap()
                val undoSnap = undoStacks.toMap()
                val redoSnap = redoStacks.toMap()
                pageSelections.clear()
                pageCompositions.clear()
                undoStacks.clear()
                redoStacks.clear()
                selSnap.forEach { (k, v) ->
                    if (k < index) pageSelections[k] = v else if (k > index) pageSelections[k - 1] = v
                }
                compSnap.forEach { (k, v) ->
                    if (k < index) pageCompositions[k] = v else if (k > index) pageCompositions[k - 1] = v
                }
                undoSnap.forEach { (k, v) ->
                    if (k < index) undoStacks[k] = v else if (k > index) undoStacks[k - 1] = v
                }
                redoSnap.forEach { (k, v) ->
                    if (k < index) redoStacks[k] = v else if (k > index) redoStacks[k - 1] = v
                }
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

    fun pushUndoSnapshot(pageIndex: Int, currentVal: TextFieldValue) {
        val stack = undoStacks.getOrPut(pageIndex) { mutableListOf() }
        if (stack.lastOrNull()?.text != currentVal.text) {
            stack.add(currentVal)
            if (stack.size > 40) stack.removeAt(0)
            redoStacks[pageIndex]?.clear()
        }
    }

    fun applyRichFieldValue(pageIndex: Int, nextVal: TextFieldValue, recordUndo: Boolean = true) {
        if (pageIndex !in pages.indices) return
        val curText = pages[pageIndex]
        val curSel = (pageSelections[pageIndex] ?: TextRange(curText.length)).let {
            TextRange(it.start.coerceIn(0, curText.length), it.end.coerceIn(0, curText.length))
        }
        if (recordUndo && nextVal.text != curText) {
            pushUndoSnapshot(pageIndex, TextFieldValue(curText, curSel, pageCompositions[pageIndex]))
        }
        val wasTruncated = updatePageTextBounded(pageIndex, nextVal.text)
        val appliedLen = pages[pageIndex].length
        pageSelections[pageIndex] = if (wasTruncated) {
            TextRange(appliedLen)
        } else {
            TextRange(nextVal.selection.start.coerceIn(0, appliedLen), nextVal.selection.end.coerceIn(0, appliedLen))
        }
        pageCompositions[pageIndex] = if (nextVal.text == curText) nextVal.composition else null
    }

    fun undoOnPage(pageIndex: Int) {
        val stack = undoStacks[pageIndex] ?: return
        if (stack.isEmpty() || pageIndex !in pages.indices) return
        val curText = pages[pageIndex]
        val curSel = (pageSelections[pageIndex] ?: TextRange(curText.length)).let {
            TextRange(it.start.coerceIn(0, curText.length), it.end.coerceIn(0, curText.length))
        }
        redoStacks.getOrPut(pageIndex) { mutableListOf() }.add(TextFieldValue(curText, curSel))
        val prev = stack.removeAt(stack.lastIndex)
        updatePageTextBounded(pageIndex, prev.text)
        val len = pages[pageIndex].length
        pageSelections[pageIndex] = TextRange(prev.selection.start.coerceIn(0, len), prev.selection.end.coerceIn(0, len))
        pageCompositions[pageIndex] = null
    }

    fun redoOnPage(pageIndex: Int) {
        val stack = redoStacks[pageIndex] ?: return
        if (stack.isEmpty() || pageIndex !in pages.indices) return
        val curText = pages[pageIndex]
        val curSel = (pageSelections[pageIndex] ?: TextRange(curText.length)).let {
            TextRange(it.start.coerceIn(0, curText.length), it.end.coerceIn(0, curText.length))
        }
        undoStacks.getOrPut(pageIndex) { mutableListOf() }.add(TextFieldValue(curText, curSel))
        val next = stack.removeAt(stack.lastIndex)
        updatePageTextBounded(pageIndex, next.text)
        val len = pages[pageIndex].length
        pageSelections[pageIndex] = TextRange(next.selection.start.coerceIn(0, len), next.selection.end.coerceIn(0, len))
        pageCompositions[pageIndex] = null
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

    val wordCount = remember(encodedPageEntries) {
        NotePagesCodec.countWords(encodedPageEntries)
    }
    val charCount = remember(pagesSnapshot, pageTitlesSnapshot) {
        pagesSnapshot.sumOf { RichTextCodec.stripFormattingForPreview(it).length } +
            pageTitlesSnapshot.sumOf { it.length }
    }

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
        ) {
            // 1. Minimal Top Bar: Back on start; Style/Backgrounds + Pin + (Save in Edit mode OR Pencil in View-Only mode)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("editor_back_button")
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true)
                        ) {
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
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isArabic) "رجوع" else "Back",
                        tint = textColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // One-Tap Copy Current Page Button in Read / View-Only Mode
                    if (!isEditMode) {
                        val currentReadPageIdx = pagerState.currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                        val copyAccent = Color(0xFF10B981)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("editor_copy_page_button")
                                .liquidGlass(
                                    isDark = effectiveDark,
                                    tint = if (justCopiedPage) GlassTint.EMERALD else GlassTint.FROST,
                                    shape = RoundedCornerShape(14.dp),
                                    borderWidth = 1.dp,
                                    customFill = if (justCopiedPage) copyAccent.copy(alpha = 0.26f) else pillFill
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
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
                                        copyFeedbackMessage = if (isArabic) {
                                            "الصفحة فارغة للنسخ"
                                        } else {
                                            "Current page is empty"
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (justCopiedPage) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = if (isArabic) "نسخ محتوى الصفحة" else "Copy page content",
                                tint = if (justCopiedPage) copyAccent else textColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Style & Backgrounds button (original pill glass + landscape-art icon)
                    val hasCustomVisual = activeBgPreset != null || customColorArgb != 0L || selectedTint != GlassTint.FROST
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("editor_style_top_button")
                            .liquidGlass(
                                isDark = effectiveDark,
                                tint = selectedTint,
                                shape = RoundedCornerShape(14.dp),
                                borderWidth = 1.dp,
                                customFill = if (hasCustomVisual) accentColor.copy(alpha = 0.22f) else pillFill
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true)
                            ) {
                                styleSheetInitialTab = if (activeBgPreset != null) 1 else 0
                                showStyleSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BackgroundArtworkIcon(
                            tint = if (hasCustomVisual) accentColor else textColor,
                            secondaryTint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Pin Button (always remains visible in both Edit & View-Only modes)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("editor_pin_toggle")
                            .liquidGlass(
                                isDark = effectiveDark,
                                tint = if (isPinned) GlassTint.AMBER else GlassTint.FROST,
                                shape = RoundedCornerShape(14.dp),
                                borderWidth = 1.dp,
                                customFill = if (isPinned) Color(0xFFFBBF24).copy(alpha = 0.25f) else pillFill
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true)
                            ) {
                                isPinned = !isPinned
                                triggerImmediateSaveIfViewMode()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (isArabic) "تثبيت" else "Pin",
                            tint = if (isPinned) Color(0xFFFBBF24) else textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Edit Pencil button in View-Only mode OR Save Check button in Writing/Edit mode
                    if (isEditMode) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("editor_save_button")
                                .liquidGlass(
                                    isDark = effectiveDark,
                                    tint = selectedTint,
                                    shape = RoundedCornerShape(14.dp),
                                    borderWidth = 1.2.dp,
                                    customFill = accentColor.copy(alpha = if (effectiveDark) 0.28f else 0.18f)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    onSave(
                                        title, page1Content, encodedExtraPages, category, selectedTint.id, backgroundId, customColorArgb,
                                        isPinned, isFavorite, checklistItems.toList(),
                                        audioDurationSec, audioFilePath, audioByteSize
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = if (isArabic) "حفظ" else "Save",
                                tint = textColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("editor_edit_button")
                                .liquidGlass(
                                    isDark = effectiveDark,
                                    tint = selectedTint,
                                    shape = RoundedCornerShape(14.dp),
                                    borderWidth = 1.2.dp,
                                    customFill = accentColor.copy(alpha = if (effectiveDark) 0.26f else 0.16f)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    isEditMode = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = if (isArabic) "تعديل" else "Edit",
                                tint = textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // View-Only More Menu (Move to Vault, Share, Archive, Delete)
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("editor_view_more_button")
                                    .liquidGlass(
                                        isDark = effectiveDark,
                                        tint = GlassTint.FROST,
                                        shape = RoundedCornerShape(14.dp),
                                        borderWidth = 1.dp,
                                        customFill = pillFill
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true)
                                    ) { showViewMoreMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = if (isArabic) "خيارات الملاحظة" else "Note options",
                                    tint = textColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showViewMoreMenu,
                                onDismissRequest = { showViewMoreMenu = false },
                                containerColor = if (effectiveDark) Color(0xFF1E293B) else Color.White
                            ) {
                                DropdownMenuItem(
                                    modifier = Modifier.testTag("editor_move_to_vault_view_item"),
                                    text = {
                                        Text(
                                            text = if (isArabic) "نقل إلى الـ Vault" else "Move to Vault",
                                            color = textColor
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = Color(0xFFC084FC)
                                        )
                                    },
                                    onClick = {
                                        showViewMoreMenu = false
                                        playbackController.stop()
                                        onMoveToVault(buildCurrentEditorNote())
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isArabic) "مشاركة الملاحظة" else "Share Note",
                                            color = textColor
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8)
                                        )
                                    },
                                    onClick = {
                                        showViewMoreMenu = false
                                        shareNoteText(context, title, encodedPageEntries, checklistItems, isArabic)
                                    }
                                )

                                if (note.id != 0L) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (note.isArchived) {
                                                    if (isArabic) "إلغاء الأرشفة" else "Unarchive Note"
                                                } else {
                                                    if (isArabic) "أرشفة الملاحظة" else "Archive Note"
                                                },
                                                color = textColor
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8)
                                            )
                                        },
                                        onClick = {
                                            showViewMoreMenu = false
                                            playbackController.stop()
                                            onToggleArchive()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isArabic) "نقل إلى سلة المهملات" else "Move to Trash",
                                                color = Color(0xFFEF4444)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444)
                                            )
                                        },
                                        onClick = {
                                            showViewMoreMenu = false
                                            playbackController.stop()
                                            onDelete()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Title + Multi-Page Navigator Bar + Swipeable HorizontalPager Writing Canvas
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                if (isEditMode) {
                    BasicTextField(
                        value = title,
                        onValueChange = { updateTitleBounded(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_title_input"),
                        textStyle = TextStyle(
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        ),
                        cursorBrush = SolidColor(selectedTint.accentColor),
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(
                                    text = if (isArabic) "العنوان" else "Title",
                                    style = TextStyle(
                                        fontSize = 23.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = metaColor.copy(alpha = 0.6f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                } else if (title.isNotEmpty()) {
                    SelectionContainer {
                        Text(
                            text = title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_title_readonly"),
                            style = TextStyle(
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Multi-Page Strip (Page indicator, quick page pills, + add page up to 100, size cap 900 KB)
                val activePageDisplayIdx = pagerState.currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                val sizeRatio = totalNoteBytes.toFloat() / NotePagesCodec.MAX_NOTE_BYTES.toFloat()
                val sizeIndicatorColor = when {
                    sizeRatio >= 0.96f -> Color(0xFFEF4444)
                    sizeRatio >= 0.80f -> Color(0xFFF59E0B)
                    else -> metaColor
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Page Counter Pill (tappable to open All Pages Navigator Dialog)
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = if (effectiveDark) 0.20f else 0.12f))
                            .border(
                                width = 0.9.dp,
                                color = accentColor.copy(alpha = 0.42f),
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true)
                            ) { showPagePickerDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("editor_page_counter_pill"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = if (isArabic) "صفحات المسودة" else "Draft Pages",
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isArabic) {
                                "صفحة ${activePageDisplayIdx + 1}/${pages.size}"
                            } else {
                                "Page ${activePageDisplayIdx + 1}/${pages.size}"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    // Scrollable Quick-Jump Page Chips
                    if (pages.size > 1) {
                        LazyRow(
                            state = pageChipListState,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(pages) { idx, _ ->
                                val isSelectedPage = idx == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 28.dp, minHeight = 28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelectedPage) accentColor
                                            else pillFill.copy(alpha = 0.72f)
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true)
                                        ) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(idx)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("editor_page_chip_$idx"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelectedPage) Color.White else metaColor
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = if (isEditMode) {
                                if (isArabic) "اسحب لليمين لصفحة جديدة" else "Swipe for Page 2"
                            } else "",
                            fontSize = 11.sp,
                            color = metaColor.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Note size indicator (against 900 KB limit)
                    if (pages.size > 1 || totalNoteBytes >= 1024L) {
                        Text(
                            text = "${NotePagesCodec.formatSizeKb(totalNoteBytes, isArabic)} / ${if (isArabic) "900 ك.ب" else "900 KB"}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = sizeIndicatorColor,
                            modifier = Modifier.testTag("editor_note_size_label")
                        )
                    }

                    if (isEditMode) {
                        if (pages.size > 1 && pagerState.currentPage < pages.size) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.16f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true)
                                    ) { removePageAt(pagerState.currentPage) }
                                    .testTag("editor_remove_page_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = if (isArabic) "حذف الصفحة الحالية" else "Delete current page",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canAppendAnotherPage) accentColor.copy(alpha = if (effectiveDark) 0.25f else 0.16f)
                                    else metaColor.copy(alpha = 0.14f)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) { addPageAndNavigate() }
                                .testTag("editor_add_page_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = if (isArabic) "إضافة صفحة جديدة" else "Add New Page",
                                tint = if (canAppendAnotherPage) accentColor else metaColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = copyFeedbackMessage != null) {
                    val isCopySuccess = justCopiedPage
                    val toastAccent = if (isCopySuccess) Color(0xFF10B981) else accentColor
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(toastAccent.copy(alpha = if (effectiveDark) 0.20f else 0.12f))
                            .border(1.dp, toastAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = copyFeedbackMessage.orEmpty(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (effectiveDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                        )
                    }
                }

                AnimatedVisibility(visible = limitWarningMessage != null) {
                    val isFreePageCapWarning = !isPremium && pages.size >= maxAllowedPages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEF4444).copy(alpha = if (effectiveDark) 0.22f else 0.12f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .clickable(enabled = isFreePageCapWarning) {
                                onRequestPremiumUpgrade()
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = limitWarningMessage.orEmpty(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (effectiveDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                            modifier = Modifier.weight(1f)
                        )
                        if (isFreePageCapWarning) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "ترقية" else "Upgrade",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1F1200)
                                )
                            }
                        }
                    }
                }
            }

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
                            // Per-Page Title Section (Every page supports its own optional title)
                            val currentPageTitle = pageTitles.getOrElse(pageIndex) { "" }
                            if (isEditMode) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (effectiveDark) Color.White.copy(alpha = 0.06f)
                                            else Color.Black.copy(alpha = 0.04f)
                                        )
                                        .border(
                                            width = 0.9.dp,
                                            color = if (currentPageTitle.isNotBlank()) accentColor.copy(alpha = 0.45f) else metaColor.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = if (effectiveDark) 0.24f else 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (isArabic) "ص ${pageIndex + 1}" else "P${pageIndex + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = accentColor
                                        )
                                    }

                                    BasicTextField(
                                        value = currentPageTitle,
                                        onValueChange = { updatePageTitleBounded(pageIndex, it) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("editor_page_title_input_$pageIndex"),
                                        textStyle = TextStyle(
                                            fontSize = 15.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        ),
                                        cursorBrush = SolidColor(accentColor),
                                        decorationBox = { innerTitleField ->
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                if (currentPageTitle.isEmpty()) {
                                                    Text(
                                                        text = if (isArabic) {
                                                            "عنوان الصفحة ${pageIndex + 1} (اختياري)..."
                                                        } else {
                                                            "Page ${pageIndex + 1} title (optional)..."
                                                        },
                                                        fontSize = 14.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = metaColor.copy(alpha = 0.58f)
                                                    )
                                                }
                                                innerTitleField()
                                            }
                                        }
                                    )
                                }
                            } else if (currentPageTitle.isNotBlank() || pageIndex > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = if (effectiveDark) 0.22f else 0.14f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (isArabic) "صفحة ${pageIndex + 1}" else "Page ${pageIndex + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                    }
                                    if (currentPageTitle.isNotBlank()) {
                                        SelectionContainer {
                                            Text(
                                                text = currentPageTitle,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = textColor,
                                                modifier = Modifier.testTag("editor_page_title_readonly_$pageIndex")
                                            )
                                        }
                                    }
                                }
                            }

                            val pageText = pages[pageIndex]
                            val pageTables = remember(pageText) { RichTextCodec.parseAllTables(pageText) }
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
                                val curSelection = (pageSelections[pageIndex] ?: TextRange(pageText.length)).let {
                                    TextRange(
                                        it.start.coerceIn(0, pageText.length),
                                        it.end.coerceIn(0, pageText.length)
                                    )
                                }
                                val curComposition = pageCompositions[pageIndex]?.let { comp ->
                                    if (comp.start in 0..pageText.length && comp.end in 0..pageText.length) comp else null
                                }
                                val fieldValue = TextFieldValue(
                                    text = pageText,
                                    selection = curSelection,
                                    composition = curComposition
                                )

                                BasicTextField(
                                    value = fieldValue,
                                    onValueChange = { updatedVal ->
                                        if (updatedVal.text != pageText) {
                                            pushUndoSnapshot(pageIndex, fieldValue)
                                        }
                                        val wasTruncated = updatePageTextBounded(pageIndex, updatedVal.text)
                                        val clampedLen = pages[pageIndex].length
                                        pageSelections[pageIndex] = if (wasTruncated) {
                                            TextRange(clampedLen)
                                        } else {
                                            TextRange(
                                                updatedVal.selection.start.coerceIn(0, clampedLen),
                                                updatedVal.selection.end.coerceIn(0, clampedLen)
                                            )
                                        }
                                        pageCompositions[pageIndex] = updatedVal.composition?.let { comp ->
                                            if (comp.start in 0..clampedLen && comp.end in 0..clampedLen) comp else null
                                        }
                                    },
                                    visualTransformation = richVisualTransformation,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = if (pageTables.isEmpty()) remainingMinHeight else 120.dp)
                                        .testTag(fieldTestTag),
                                    textStyle = TextStyle(
                                        fontSize = writingFontSizeSp.sp,
                                        color = bodyColor,
                                        lineHeight = (writingFontSizeSp * 1.52f).sp
                                    ),
                                    cursorBrush = SolidColor(selectedTint.accentColor),
                                    decorationBox = { innerTextField ->
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            if (pageText.isEmpty()) {
                                                Text(
                                                    text = if (pageIndex == 0) {
                                                        if (isArabic) "اكتب أفكارك وملاحظاتك هنا بحرية... أو نسّق النص من الشريط بالأسفل" else "Write freely... use the Notion bar below to style or add tables"
                                                    } else {
                                                        if (isArabic) "اكتب محتوى الصفحة ${pageIndex + 1} هنا..." else "Write page ${pageIndex + 1} here..."
                                                    },
                                                    style = TextStyle(
                                                        fontSize = writingFontSizeSp.sp,
                                                        color = metaColor.copy(alpha = 0.6f),
                                                        lineHeight = (writingFontSizeSp * 1.52f).sp
                                                    )
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )

                                // Rendered Interactive Table Cards in Writing Mode (1-tap edit cells/rows/cols)
                                InlineTablesPreviewSection(
                                    tables = pageTables,
                                    isDark = effectiveDark,
                                    isArabic = isArabic,
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    metaColor = metaColor,
                                    onEditTable = { tbl ->
                                        editingTableTarget = tbl
                                        showTableDialog = true
                                    }
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
                                            .testTag(fieldTestTag)
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

            // 3. Notion-Style Rich Formatting Dock + Bottom Action Toolbar (visible only in Writing Mode)
            AnimatedVisibility(
                visible = isEditMode,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                val activePageIdx = pagerState.currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                val activePageBody = pages.getOrElse(activePageIdx) { "" }
                val activeSel = (pageSelections[activePageIdx] ?: TextRange(activePageBody.length)).let {
                    TextRange(it.start.coerceIn(0, activePageBody.length), it.end.coerceIn(0, activePageBody.length))
                }
                val activeStyles = remember(activePageBody, activeSel) {
                    RichTextCodec.inspectActiveStyles(activePageBody, activeSel)
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    NotionFormattingToolbar(
                        activeStyles = activeStyles,
                        canUndo = undoStacks[activePageIdx]?.isNotEmpty() == true,
                        canRedo = redoStacks[activePageIdx]?.isNotEmpty() == true,
                        isDark = effectiveDark,
                        isArabic = isArabic,
                        accentColor = accentColor,
                        textColor = textColor,
                        metaColor = metaColor,
                        pillFill = pillFill,
                        onAction = { action ->
                            val curVal = TextFieldValue(
                                text = activePageBody,
                                selection = activeSel,
                                composition = pageCompositions[activePageIdx]
                            )
                            val transformed = RichTextCodec.applyRichAction(curVal, action)
                            applyRichFieldValue(activePageIdx, transformed, recordUndo = true)
                        },
                        onUndo = { undoOnPage(activePageIdx) },
                        onRedo = { redoOnPage(activePageIdx) },
                        onInsertTableClick = {
                            editingTableTarget = null
                            showTableDialog = true
                        },
                        onDecreaseFontSize = {
                            writingFontSizeSp = (writingFontSizeSp - 1.5f).coerceIn(12f, 32f)
                        },
                        onIncreaseFontSize = {
                            writingFontSizeSp = (writingFontSizeSp + 1.5f).coerceIn(12f, 32f)
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Add Voice Memo button
                        val hasAttachedAudio = audioDurationSec > 0 || audioFilePath.isNotBlank()
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("editor_add_voice_button")
                                .liquidGlass(
                                    isDark = effectiveDark,
                                    tint = if (hasAttachedAudio) GlassTint.ROSE else GlassTint.FROST,
                                    shape = CircleShape,
                                    borderWidth = 1.dp,
                                    customFill = if (hasAttachedAudio) Color(0xFFEC4899).copy(alpha = 0.24f) else pillFill
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) { showVoiceRecorderDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = if (isArabic) "إضافة تسجيل صوتي" else "Add Voice Memo",
                                tint = if (hasAttachedAudio) Color(0xFFEC4899) else textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Add Checklist button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("editor_add_checklist_button")
                                .liquidGlass(
                                    isDark = effectiveDark,
                                    tint = if (checklistItems.isNotEmpty()) selectedTint else GlassTint.FROST,
                                    shape = CircleShape,
                                    borderWidth = 1.dp,
                                    customFill = if (checklistItems.isNotEmpty()) accentColor.copy(alpha = 0.22f) else pillFill
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    if (totalNoteBytes + 32L <= NotePagesCodec.MAX_NOTE_BYTES) {
                                        checklistItems.add(ChecklistItem(text = "", isDone = false))
                                        if (pagerState.currentPage != 0) {
                                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                        }
                                    } else {
                                        triggerLimitWarning(isCountLimit = false)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckBox,
                                contentDescription = if (isArabic) "إضافة قائمة مهام" else "Add Checklist",
                                tint = if (checklistItems.isNotEmpty()) accentColor else textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Document Colors & Category button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("editor_palette_button")
                                .liquidGlass(
                                    isDark = effectiveDark,
                                    tint = selectedTint,
                                    shape = CircleShape,
                                    borderWidth = 1.dp,
                                    customFill = if (customColorArgb != 0L || selectedTint != GlassTint.FROST) accentColor.copy(alpha = 0.24f) else pillFill
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    styleSheetInitialTab = 0
                                    showStyleSheet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = if (isArabic) "تلوين المستند" else "Document Color",
                                tint = if (customColorArgb != 0L || selectedTint != GlassTint.FROST) accentColor else textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Document Backgrounds button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("editor_backgrounds_button")
                                .liquidGlass(
                                    isDark = effectiveDark,
                                    tint = selectedTint,
                                    shape = CircleShape,
                                    borderWidth = 1.dp,
                                    customFill = if (activeBgPreset != null) accentColor.copy(alpha = 0.24f) else pillFill
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    styleSheetInitialTab = 1
                                    showStyleSheet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            BackgroundArtworkIcon(
                                tint = if (activeBgPreset != null) accentColor else textColor,
                                secondaryTint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Right/End section: subtle word count + More Menu (Favorite, Share, Delete)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (charCount > 0) {
                            Text(
                                text = if (isArabic) "$wordCount ك • $charCount ح" else "$wordCount w • $charCount c",
                                fontSize = 11.sp,
                                color = metaColor
                            )
                        }

                        Box {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("editor_more_button")
                                    .liquidGlass(
                                        isDark = effectiveDark,
                                        tint = GlassTint.FROST,
                                        shape = CircleShape,
                                        borderWidth = 1.dp,
                                        customFill = pillFill
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true)
                                    ) { showMoreMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = if (isArabic) "خيارات إضافية" else "More options",
                                    tint = textColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                containerColor = if (effectiveDark) Color(0xFF1E293B) else Color.White
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isFavorite) {
                                                if (isArabic) "إزالة من المفضلة" else "Remove Favorite"
                                            } else {
                                                if (isArabic) "إضافة للمفضلة" else "Add to Favorites"
                                            },
                                            color = textColor
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            tint = if (isFavorite) Color(0xFFF43F5E) else metaColor
                                        )
                                    },
                                    onClick = {
                                        isFavorite = !isFavorite
                                        showMoreMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    modifier = Modifier.testTag("editor_move_to_vault_edit_item"),
                                    text = {
                                        Text(
                                            text = if (isArabic) "نقل إلى الـ Vault" else "Move to Vault",
                                            color = textColor
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = Color(0xFFC084FC)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        playbackController.stop()
                                        onMoveToVault(buildCurrentEditorNote())
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isArabic) "مشاركة الملاحظة" else "Share Note",
                                            color = textColor
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        shareNoteText(context, title, encodedPageEntries, checklistItems, isArabic)
                                    }
                                )

                                if (note.id != 0L) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (note.isArchived) {
                                                    if (isArabic) "إلغاء الأرشفة" else "Unarchive Note"
                                                } else {
                                                    if (isArabic) "أرشفة الملاحظة" else "Archive Note"
                                                },
                                                color = textColor
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8)
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            playbackController.stop()
                                            onToggleArchive()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isArabic) "نقل إلى سلة المهملات" else "Move to Trash",
                                                color = Color(0xFFEF4444)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444)
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            playbackController.stop()
                                            onDelete()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
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

        // Interactive Notion-Style Table Builder / Cell Editor Dialog
        if (showTableDialog) {
            val activeIdx = pagerState.currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
            val curBody = pages.getOrElse(activeIdx) { "" }
            val curSel = (pageSelections[activeIdx] ?: TextRange(curBody.length)).let {
                TextRange(it.start.coerceIn(0, curBody.length), it.end.coerceIn(0, curBody.length))
            }
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
                    val nextVal = if (editTarget != null && editTarget.startOffset >= 0) {
                        RichTextCodec.replaceTableAtRange(
                            text = curBody,
                            startOffset = editTarget.startOffset,
                            endOffset = editTarget.endOffset,
                            tableMarkdown = md
                        )
                    } else {
                        RichTextCodec.insertTableAtSelection(
                            text = curBody,
                            selection = curSel,
                            tableMarkdown = md
                        )
                    }
                    applyRichFieldValue(activeIdx, nextVal, recordUndo = true)
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
                        applyRichFieldValue(activeIdx, TextFieldValue(updated, TextRange(updated.length)), recordUndo = true)
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

/**
 * Sleek custom-drawn layered canvas & scenic horizon illustration icon for Document Backgrounds.
 */
@Composable
private fun BackgroundArtworkIcon(
    tint: Color,
    secondaryTint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Subtle back layered card offset at top-right
        val backLeft = w * 0.22f
        val backTop = h * 0.08f
        val backW = w * 0.70f
        val backH = h * 0.66f
        drawRoundRect(
            color = tint.copy(alpha = 0.36f),
            topLeft = Offset(backLeft, backTop),
            size = Size(backW, backH),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
            style = Stroke(width = w * 0.065f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Main foreground artwork frame
        val frameLeft = w * 0.08f
        val frameTop = h * 0.24f
        val frameW = w * 0.72f
        val frameH = h * 0.68f
        drawRoundRect(
            color = tint.copy(alpha = 0.12f),
            topLeft = Offset(frameLeft, frameTop),
            size = Size(frameW, frameH),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(frameLeft, frameTop),
            size = Size(frameW, frameH),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
            style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Smooth rolling horizon wave inside the frame
        val wavePath = Path().apply {
            val bottom = frameTop + frameH - w * 0.04f
            moveTo(frameLeft + w * 0.07f, bottom)
            lineTo(frameLeft + w * 0.07f, frameTop + frameH * 0.64f)
            cubicTo(
                frameLeft + frameW * 0.32f, frameTop + frameH * 0.38f,
                frameLeft + frameW * 0.54f, frameTop + frameH * 0.72f,
                frameLeft + frameW - w * 0.07f, frameTop + frameH * 0.46f
            )
            lineTo(frameLeft + frameW - w * 0.07f, bottom)
            close()
        }
        drawPath(
            path = wavePath,
            color = tint.copy(alpha = 0.78f)
        )

        // Radiant sun/orb jewel in upper-left of the canvas
        drawCircle(
            color = secondaryTint,
            radius = w * 0.095f,
            center = Offset(frameLeft + frameW * 0.30f, frameTop + frameH * 0.30f)
        )
    }
}
