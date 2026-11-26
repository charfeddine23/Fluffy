package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.data.model.VaultItemEntity
import com.example.data.storage.AudioCloudPolicy
import com.example.data.storage.AudioStorageManager
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.DocumentStyleBottomSheet
import com.example.ui.components.InsertFeaturesBottomSheet
import com.example.ui.components.SegmentedRichPageEditor
import com.example.ui.components.TableEditorDialog
import com.example.ui.components.VoiceRecorderDialog
import com.example.ui.components.WritingFormattingBar
import com.example.ui.components.applyActionToSegmentedPage
import com.example.ui.components.insertSnippetInSegmentedPage
import com.example.ui.components.insertTableInSegmentedPage
import com.example.ui.components.resolveActiveTextSegment
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.theme.LocalAppThemePreset
import com.example.ui.theme.tr

@Composable
fun VaultNoteEditorSheet(
    existingItem: VaultItemEntity?,
    existingMeta: NoteEntity?,
    isDark: Boolean,
    isArabic: Boolean,
    isPremium: Boolean,
    initialPageIndex: Int = 0,
    initialStartVoiceRecorder: Boolean = false,
    initialFontSizeSp: Float = 15.5f,
    onRequestPremiumUpgrade: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        content: String,
        pagesJson: String,
        category: String,
        colorPreset: String,
        backgroundId: String,
        customColorArgb: Long,
        isPinned: Boolean,
        checklist: List<ChecklistItem>,
        audioDurationSec: Int,
        audioFilePath: String,
        audioByteSize: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val activePreset = LocalAppThemePreset.current
    val effectiveDark = activePreset.isDark || isDark
    val playbackController = rememberAudioPlaybackController()

    DisposableEffect(Unit) {
        onDispose { playbackController.stop() }
    }

    val initialAudioPath = remember(existingItem?.id, existingMeta?.audioFilePath) {
        val metaPath = existingMeta?.audioFilePath.orEmpty()
        val itemPath = existingItem?.localFilePath.orEmpty()
        val isItemAudio = existingItem != null &&
            (AudioCloudPolicy.isAudioFile(existingItem.mimeType, existingItem.fileName) ||
                (existingMeta?.audioDurationSec ?: 0) > 0)
        when {
            metaPath.isNotBlank() -> metaPath
            isItemAudio && itemPath.isNotBlank() -> itemPath
            else -> ""
        }
    }

    var title by remember(existingItem?.id) {
        mutableStateOf(
            existingMeta?.title?.ifBlank { existingItem?.title.orEmpty() }
                ?: existingItem?.title.orEmpty()
        )
    }

    val initialDraftPages = remember(existingItem?.id, existingMeta?.syncId) {
        val decoded = if (existingMeta != null) {
            NotePagesCodec.decodeDraftPages(existingMeta.content, existingMeta.pagesJson)
        } else {
            val rawSingle = existingItem?.literalContent.orEmpty()
            listOf(
                NotePagesCodec.DraftPage(
                    title = NotePagesCodec.extractPageTitle(rawSingle),
                    body = NotePagesCodec.extractPageBody(rawSingle)
                )
            )
        }
        decoded.ifEmpty { listOf(NotePagesCodec.DraftPage()) }
    }
    val pages = remember(existingItem?.id, existingMeta?.syncId) {
        mutableStateListOf<String>().apply {
            addAll(initialDraftPages.map { it.body })
        }
    }
    val pageTitles = remember(existingItem?.id, existingMeta?.syncId) {
        mutableStateListOf<String>().apply {
            addAll(initialDraftPages.map { it.title })
        }
    }
    var activePageIndex by remember(existingItem?.id, initialPageIndex) {
        mutableIntStateOf(initialPageIndex.coerceIn(0, (initialDraftPages.size - 1).coerceAtLeast(0)))
    }
    val activeSegmentByPage = remember(existingItem?.id) { mutableStateMapOf<Int, Int>() }
    val segmentSelections = remember(existingItem?.id) { mutableStateMapOf<String, TextRange>() }
    val segmentCompositions = remember(existingItem?.id) { mutableStateMapOf<String, TextRange?>() }
    val pendingStylesByPage = remember(existingItem?.id) { mutableStateMapOf<Int, Set<RichFormatAction>>() }
    var writingFontSizeSp by remember(existingItem?.id, initialFontSizeSp) {
        mutableFloatStateOf(initialFontSizeSp.coerceIn(12f, 32f))
    }
    var showInsertSheet by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var editingTableTarget by remember { mutableStateOf<MarkdownTableData?>(null) }

    val checklist = remember(existingItem?.id) {
        mutableStateListOf<ChecklistItem>().apply {
            addAll(existingMeta?.let { ChecklistItem.jsonToList(it.checklistJson) }.orEmpty())
        }
    }
    var newCheckText by remember { mutableStateOf("") }

    var category by remember(existingItem?.id) {
        mutableStateOf(existingMeta?.category?.ifBlank { if (isArabic) "شخصي" else "Personal" }
            ?: if (isArabic) "شخصي" else "Personal")
    }
    var selectedTint by remember(existingItem?.id) {
        mutableStateOf(
            existingMeta?.let { GlassTint.fromId(it.colorPreset) }
                ?: if (initialAudioPath.isNotBlank() || initialStartVoiceRecorder) GlassTint.ROSE else GlassTint.AMETHYST
        )
    }
    var backgroundId by remember(existingItem?.id) {
        mutableStateOf(existingMeta?.backgroundId ?: NoteBackgroundCatalog.NONE_ID)
    }
    var customColorArgb by remember(existingItem?.id) {
        mutableLongStateOf(existingMeta?.customColorArgb ?: 0L)
    }
    var isPinned by remember(existingItem?.id) {
        mutableStateOf(existingMeta?.isPinned ?: false)
    }

    var audioFilePath by remember(existingItem?.id) { mutableStateOf(initialAudioPath) }
    var audioDurationSec by remember(existingItem?.id) {
        mutableIntStateOf(existingMeta?.audioDurationSec ?: 0)
    }
    var audioByteSize by remember(existingItem?.id) {
        mutableLongStateOf(
            AudioCloudPolicy.effectiveAudioByteSize(
                existingMeta?.audioByteSize ?: existingItem?.byteSize ?: 0L,
                initialAudioPath
            )
        )
    }

    var showStyleSheet by remember { mutableStateOf(false) }
    var showVoiceRecorder by remember(existingItem?.id) { mutableStateOf(initialStartVoiceRecorder) }

    val audioPickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            playbackController.stop()
            val path = uri.toString()
            audioFilePath = path
            audioDurationSec = AudioStorageManager.computeDurationSec(context, path)
            audioByteSize = AudioCloudPolicy.effectiveAudioByteSize(0L, path)
        }
    }

    val bgPreset = remember(backgroundId) { NoteBackgroundCatalog.findById(backgroundId) }
    val surfaceColors = remember(effectiveDark, selectedTint, customColorArgb, backgroundId) {
        NoteBackgroundCatalog.resolveSurfaceColors(
            themeIsDark = effectiveDark,
            tint = selectedTint,
            customColorArgb = customColorArgb,
            backgroundId = backgroundId
        )
    }
    val textColor = surfaceColors.titleColor
    val metaColor = surfaceColors.metaColor
    val accentColor = surfaceColors.accentColor

    val maxAllowedPages = NotePagesCodec.maxPagesForTier(isPremium)
    val safePageIdx = activePageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))

    fun pageSegmentSelections(pageIdx: Int): Map<Int, TextRange> {
        val prefix = "$pageIdx:"
        val out = mutableMapOf<Int, TextRange>()
        segmentSelections.forEach { (k, v) ->
            if (k.startsWith(prefix)) {
                k.removePrefix(prefix).toIntOrNull()?.let { out[it] = v }
            }
        }
        return out
    }

    fun pageSegmentCompositions(pageIdx: Int): Map<Int, TextRange?> {
        val prefix = "$pageIdx:"
        val out = mutableMapOf<Int, TextRange?>()
        segmentCompositions.forEach { (k, v) ->
            if (k.startsWith(prefix)) {
                k.removePrefix(prefix).toIntOrNull()?.let { out[it] = v }
            }
        }
        return out
    }

    fun clearPageSegments(pageIdx: Int) {
        val prefix = "$pageIdx:"
        segmentSelections.keys.filter { it.startsWith(prefix) }.forEach { segmentSelections.remove(it) }
        segmentCompositions.keys.filter { it.startsWith(prefix) }.forEach { segmentCompositions.remove(it) }
    }

    fun applyFormatActionToPage(pageIdx: Int, action: RichFormatAction) {
        if (pageIdx !in pages.indices) return
        val result = applyActionToSegmentedPage(
            pageText = pages[pageIdx],
            preferredSegmentIndex = activeSegmentByPage[pageIdx] ?: 0,
            segmentSelections = pageSegmentSelections(pageIdx),
            segmentCompositions = pageSegmentCompositions(pageIdx),
            pendingStyles = pendingStylesByPage[pageIdx] ?: emptySet(),
            action = action
        )
        pages[pageIdx] = result.updatedPageText
        val latestSegments = RichTextCodec.splitPageIntoEditorSegments(pages[pageIdx])
        val (_, newSeg) = resolveActiveTextSegment(latestSegments, result.targetSegmentIndex)
        activeSegmentByPage[pageIdx] = result.targetSegmentIndex
        segmentSelections["$pageIdx:${result.targetSegmentIndex}"] = TextRange(
            result.updatedSelection.start.coerceIn(0, newSeg.text.length),
            result.updatedSelection.end.coerceIn(0, newSeg.text.length)
        )
        segmentCompositions["$pageIdx:${result.targetSegmentIndex}"] = null
        pendingStylesByPage[pageIdx] = result.updatedPendingStyles
    }

    fun applyInsertFeatureToPage(pageIdx: Int, feature: InsertFeatureType) {
        if (pageIdx !in pages.indices) return
        val snippet = RichTextCodec.buildInsertSnippet(feature, isArabic)
        if (feature == InsertFeatureType.PROS_CONS_TABLE) {
            val updatedPageText = insertTableInSegmentedPage(
                pageText = pages[pageIdx],
                preferredSegmentIndex = activeSegmentByPage[pageIdx] ?: 0,
                segmentSelections = pageSegmentSelections(pageIdx),
                tableMarkdown = snippet.trimEnd('\n')
            )
            pages[pageIdx] = updatedPageText
            val latestSegments = RichTextCodec.splitPageIntoEditorSegments(pages[pageIdx])
            clearPageSegments(pageIdx)
            val tailIdx = latestSegments.indexOfLast { it is EditorPageSegment.TextSegment }.coerceAtLeast(0)
            activeSegmentByPage[pageIdx] = tailIdx
            segmentSelections["$pageIdx:$tailIdx"] = TextRange(0)
            pendingStylesByPage.remove(pageIdx)
            return
        }
        val needNewLine = feature == InsertFeatureType.DAILY_HEADER ||
            feature == InsertFeatureType.PROGRESS_BAR ||
            feature == InsertFeatureType.MEETING_TEMPLATE
        val result = insertSnippetInSegmentedPage(
            pageText = pages[pageIdx],
            preferredSegmentIndex = activeSegmentByPage[pageIdx] ?: 0,
            segmentSelections = pageSegmentSelections(pageIdx),
            snippet = snippet,
            ensureNewLinePrefix = needNewLine
        )
        pages[pageIdx] = result.updatedPageText
        val latestSegments = RichTextCodec.splitPageIntoEditorSegments(pages[pageIdx])
        val (_, newSeg) = resolveActiveTextSegment(latestSegments, result.targetSegmentIndex)
        activeSegmentByPage[pageIdx] = result.targetSegmentIndex
        segmentSelections["$pageIdx:${result.targetSegmentIndex}"] = TextRange(
            result.updatedSelection.start.coerceIn(0, newSeg.text.length),
            result.updatedSelection.end.coerceIn(0, newSeg.text.length)
        )
        segmentCompositions["$pageIdx:${result.targetSegmentIndex}"] = null
        pendingStylesByPage.remove(pageIdx)
    }

    fun removePageAt(index: Int) {
        if (pages.size <= 1 || index !in pages.indices) return
        val activeSnap = activeSegmentByPage.toMap()
        val pendingSnap = pendingStylesByPage.toMap()
        val selSnap = segmentSelections.toMap()
        val compSnap = segmentCompositions.toMap()
        val (_, nextIdx) = NotePagesCodec.removePageSafely(pages, index, safePageIdx)
        pages.removeAt(index)
        if (index in pageTitles.indices) pageTitles.removeAt(index)
        while (pageTitles.size < pages.size) pageTitles.add("")
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
        fun remapKey(key: String): String? {
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
        selSnap.forEach { (k, v) -> remapKey(k)?.let { segmentSelections[it] = v } }
        compSnap.forEach { (k, v) -> remapKey(k)?.let { segmentCompositions[it] = v } }
        activePageIndex = nextIdx
    }

    Dialog(
        onDismissRequest = {
            playbackController.stop()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val density = LocalDensity.current
        val isImeVisible = WindowInsets.ime.getBottom(density) > WindowInsets.navigationBars.getBottom(density)
        FrostedBackdrop(isDark = effectiveDark) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (bgPreset != null) {
                    NoteBackgroundCanvas(
                        preset = bgPreset,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (customColorArgb != 0L) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(customColorArgb.toInt()).copy(alpha = 0.25f))
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                        .testTag("vault_note_editor_sheet")
                ) {
                    // Header Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    playbackController.stop()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(surfaceColors.pillFill)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = tr("رجوع", "Retour", "Back", "Uɣal"),
                                    tint = textColor
                                )
                            }

                            Text(
                                text = if (existingItem == null) {
                                    tr("إنشاء في Vault", "Nouveau dans Vault", "New in Vault", "Amaynut deg Vault")
                                } else {
                                    tr("تعديل داخل Vault", "Modifier dans Vault", "Edit in Vault", "Ẓreg deg Vault")
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { isPinned = !isPinned },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isPinned) accentColor.copy(alpha = 0.22f) else surfaceColors.pillFill)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = tr("تثبيت", "Épingler", "Pin", "Senteḍ"),
                                    tint = if (isPinned) accentColor else metaColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { showStyleSheet = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(surfaceColors.pillFill)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = tr("المظهر واللون", "Style", "Style", "Udem"),
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    playbackController.stop()
                                    val draftPages = pages.mapIndexed { idx, body ->
                                        NotePagesCodec.DraftPage(
                                            title = pageTitles.getOrElse(idx) { "" },
                                            body = body
                                        )
                                    }
                                    val (mainContent, extraPagesJson) = NotePagesCodec.encodeDraftPages(draftPages)
                                    onSave(
                                        title.trim(),
                                        mainContent,
                                        extraPagesJson,
                                        category,
                                        selectedTint.id,
                                        backgroundId,
                                        customColorArgb,
                                        isPinned,
                                        checklist.toList(),
                                        audioDurationSec,
                                        audioFilePath,
                                        audioByteSize
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor,
                                    contentColor = Color.White
                                ),
                                shape = CircleShape,
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("vault_editor_save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr("حفظ", "Enregistrer", "Save", "Sekles"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Scrollable Body: Title, Audio Player / Recorder Controls, Page Selector & Content, Checklist
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(tr("العنوان", "Titre", "Title", "Azwel")) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = metaColor.copy(alpha = 0.35f),
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = metaColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_editor_title_input")
                        )

                        // Voice Note Card + Record / Replace / Import Audio Controls
                        LiquidGlassCard(
                            isDark = effectiveDark,
                            tint = if (audioFilePath.isNotBlank()) GlassTint.ROSE else GlassTint.AMETHYST,
                            shape = RoundedCornerShape(20.dp),
                            borderWidth = 1.1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (audioFilePath.isNotBlank()) {
                                    AudioPlayerCard(
                                        trackId = "vault_edit_${existingItem?.id ?: "new"}",
                                        title = title.ifBlank { tr("تسجيل صوتي", "Note vocale", "Voice Note", "Amesli") },
                                        filePathOrUri = audioFilePath,
                                        durationSec = audioDurationSec,
                                        byteSize = audioByteSize,
                                        playbackController = playbackController,
                                        isDark = effectiveDark,
                                        isArabic = isArabic
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            playbackController.stop()
                                            showVoiceRecorder = true
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                            .testTag("vault_editor_record_audio_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = Color(0xFFF43F5E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (audioFilePath.isNotBlank()) {
                                                tr("إعادة تسجيل / استبدال", "Réenregistrer", "Re-record", "Ales asekles")
                                            } else {
                                                tr("إضافة تسجيل صوتي", "Ajouter audio", "Record Voice", "Rnu amesli")
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { audioPickLauncher.launch(arrayOf("audio/*")) },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Text(
                                            text = tr("ملف صوتي", "Fichier", "Audio File", "Afaylu"),
                                            fontSize = 12.sp,
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (audioFilePath.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                playbackController.stop()
                                                audioFilePath = ""
                                                audioDurationSec = 0
                                                audioByteSize = 0L
                                            },
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = tr("حذف الصوت", "Supprimer l'audio", "Remove Audio", "Kkes amesli"),
                                                tint = Color(0xFFF43F5E)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Multi-Page Tabs + Add Page (with per-page title preview & safe removal)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            pages.forEachIndexed { idx, _ ->
                                val selected = idx == safePageIdx
                                val tabTitle = pageTitles.getOrElse(idx) { "" }.trim()
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) accentColor.copy(alpha = 0.22f)
                                            else surfaceColors.pillFill
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) accentColor else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { activePageIndex = idx }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (tabTitle.isNotEmpty()) {
                                            if (isArabic) "ص${idx + 1}: $tabTitle" else "P${idx + 1}: $tabTitle"
                                        } else {
                                            if (isArabic) "صفحة ${idx + 1}" else "Page ${idx + 1}"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) accentColor else textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (pages.size > 1) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444).copy(alpha = if (selected) 0.22f else 0.14f))
                                                .clickable { removePageAt(idx) }
                                                .testTag("vault_editor_remove_page_$idx"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = tr("حذف الصفحة", "Supprimer page", "Remove Page", "Kkes asebter"),
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (pages.size < maxAllowedPages) {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(surfaceColors.pillFill)
                                        .clickable {
                                            pages.add("")
                                            pageTitles.add("")
                                            activePageIndex = pages.lastIndex
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("vault_editor_add_page_button"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = tr("صفحة جديدة", "Nouvelle page", "Add Page", "Rnu asebter"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor
                                    )
                                }
                            }
                        }

                        // Per-Page Title Input
                        val curPageTitle = pageTitles.getOrElse(safePageIdx) { "" }
                        OutlinedTextField(
                            value = curPageTitle,
                            onValueChange = { nextTitle ->
                                while (pageTitles.size < pages.size) pageTitles.add("")
                                if (safePageIdx in pageTitles.indices) {
                                    pageTitles[safePageIdx] = nextTitle
                                }
                            },
                            label = {
                                Text(
                                    if (isArabic) "عنوان الصفحة ${safePageIdx + 1} (اختياري)"
                                    else "Page ${safePageIdx + 1} Title (optional)"
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = metaColor.copy(alpha = 0.28f),
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = metaColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_editor_page_title_input_$safePageIdx")
                        )

                        // Rich Notion-Style Page Body Editor with Clean Interactive Tables
                        val activeFontOption = com.example.ui.theme.LocalAppFontOption.current
                        val activePageText = pages.getOrElse(safePageIdx) { "" }
                        val richVisualTransformation = remember(accentColor, metaColor, effectiveDark, activeFontOption) {
                            RichTextCodec.createRichVisualTransformation(
                                accentColor = accentColor,
                                metaColor = metaColor,
                                isDark = effectiveDark,
                                fontOption = activeFontOption
                            )
                        }
                        val preferredSegIdx = activeSegmentByPage[safePageIdx] ?: 0

                        val pendingVaultStyles = pendingStylesByPage[safePageIdx] ?: emptySet()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (effectiveDark) Color.White.copy(alpha = 0.04f)
                                    else Color.Black.copy(alpha = 0.025f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = metaColor.copy(alpha = 0.30f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(14.dp)
                        ) {
                            SegmentedRichPageEditor(
                                pageText = activePageText,
                                activeSegmentIndex = preferredSegIdx,
                                segmentSelections = pageSegmentSelections(safePageIdx),
                                segmentCompositions = pageSegmentCompositions(safePageIdx),
                                pendingInlineStyles = pendingVaultStyles,
                                richVisualTransformation = richVisualTransformation,
                                fontSizeSp = writingFontSizeSp,
                                minHeight = 160.dp,
                                placeholderText = tr(
                                    "اكتب أو عدّل محتوى المدونة هنا...",
                                    "Écrivez ou modifiez ici...",
                                    "Write or edit here...",
                                    "Aru da..."
                                ),
                                fieldTestTag = "vault_editor_content_input",
                                isDark = effectiveDark,
                                isArabic = isArabic,
                                accentColor = accentColor,
                                textColor = textColor,
                                bodyColor = textColor,
                                metaColor = metaColor,
                                onSegmentTextChanged = { updatedPageText, segIdx, newSegSel, newSegComp, nextPending ->
                                    if (safePageIdx in pages.indices) {
                                        pages[safePageIdx] = updatedPageText
                                        val latestSegments = RichTextCodec.splitPageIntoEditorSegments(updatedPageText)
                                        val activeSeg = latestSegments.getOrNull(segIdx) as? EditorPageSegment.TextSegment
                                        val maxSegLen = activeSeg?.text?.length ?: 0
                                        activeSegmentByPage[safePageIdx] = segIdx
                                        segmentSelections["$safePageIdx:$segIdx"] = TextRange(
                                            newSegSel.start.coerceIn(0, maxSegLen),
                                            newSegSel.end.coerceIn(0, maxSegLen)
                                        )
                                        segmentCompositions["$safePageIdx:$segIdx"] = newSegComp
                                        pendingStylesByPage[safePageIdx] = nextPending
                                    }
                                },
                                onSegmentFocused = { segIdx ->
                                    if (activeSegmentByPage[safePageIdx] != segIdx) {
                                        activeSegmentByPage[safePageIdx] = segIdx
                                        pendingStylesByPage.remove(safePageIdx)
                                    }
                                },
                                onEditTable = { tbl ->
                                    editingTableTarget = tbl
                                    showTableDialog = true
                                },
                                onDeleteTable = { tbl ->
                                    if (safePageIdx in pages.indices) {
                                        pages[safePageIdx] = RichTextCodec.deleteTableAtRange(
                                            activePageText,
                                            tbl.startOffset,
                                            tbl.endOffset
                                        )
                                        clearPageSegments(safePageIdx)
                                        activeSegmentByPage[safePageIdx] = 0
                                        pendingStylesByPage.remove(safePageIdx)
                                    }
                                }
                            )
                        }

                        // Checklist Section
                        LiquidGlassCard(
                            isDark = effectiveDark,
                            tint = selectedTint,
                            shape = RoundedCornerShape(18.dp),
                            borderWidth = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = tr("قائمة المهام", "Liste de tâches", "Checklist", "Tabdart n twuriwin"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )

                                checklist.forEachIndexed { idx, chk ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { checklist[idx] = chk.copy(isDone = !chk.isDone) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (chk.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                contentDescription = null,
                                                tint = accentColor
                                            )
                                        }
                                        Text(
                                            text = chk.text,
                                            fontSize = 13.5.sp,
                                            color = if (chk.isDone) metaColor else textColor,
                                            textDecoration = if (chk.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { checklist.removeAt(idx) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = tr("حذف", "Supprimer", "Remove", "Kkes"),
                                                tint = metaColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newCheckText,
                                        onValueChange = { newCheckText = it },
                                        placeholder = {
                                            Text(
                                                tr("أضف مهمة...", "Ajouter tâche...", "Add task...", "Rnu tawuri..."),
                                                color = metaColor.copy(alpha = 0.7f),
                                                fontSize = 12.5.sp
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = textColor,
                                            unfocusedTextColor = textColor,
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = metaColor.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = {
                                            val clean = newCheckText.trim()
                                            if (clean.isNotEmpty()) {
                                                checklist.add(ChecklistItem(text = clean))
                                                newCheckText = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.2f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = tr("إضافة", "Ajouter", "Add", "Rnu"),
                                            tint = accentColor
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Bottom Formatting Bar pinned flush above keyboard
                    val activePageText = pages.getOrElse(safePageIdx) { "" }
                    val activePageSegments = remember(activePageText) {
                        RichTextCodec.splitPageIntoEditorSegments(activePageText)
                    }
                    val preferredSegIdx = activeSegmentByPage[safePageIdx] ?: 0
                    val (resolvedSegIdx, resolvedSeg) = resolveActiveTextSegment(activePageSegments, preferredSegIdx)
                    val activeSegSel = (segmentSelections["$safePageIdx:$resolvedSegIdx"] ?: TextRange(resolvedSeg.text.length)).let {
                        TextRange(it.start.coerceIn(0, resolvedSeg.text.length), it.end.coerceIn(0, resolvedSeg.text.length))
                    }
                    val pendingVaultStyles = pendingStylesByPage[safePageIdx] ?: emptySet()
                    val activeVaultStyles = remember(resolvedSeg.text, activeSegSel, pendingVaultStyles) {
                        RichTextCodec.inspectActiveStyles(resolvedSeg.text, activeSegSel) + pendingVaultStyles
                    }
                    val dockBg = if (isImeVisible) {
                        if (effectiveDark) Color(0xFF0B1120).copy(alpha = 0.94f) else Color(0xFFF8FAFC).copy(alpha = 0.96f)
                    } else {
                        Color.Transparent
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(dockBg)
                            .padding(bottom = if (isImeVisible) 2.dp else 6.dp)
                    ) {
                        if (isImeVisible) {
                            HorizontalDivider(
                                thickness = 0.8.dp,
                                color = accentColor.copy(alpha = if (effectiveDark) 0.24f else 0.16f)
                            )
                        }
                        WritingFormattingBar(
                            activeStyles = activeVaultStyles,
                            isDark = effectiveDark,
                            isArabic = isArabic,
                            accentColor = accentColor,
                            textColor = textColor,
                            metaColor = metaColor,
                            pillFill = surfaceColors.pillFill,
                            onFormatAction = { action -> applyFormatActionToPage(safePageIdx, action) },
                            onOpenAddMenu = { showInsertSheet = true },
                            outerVerticalPadding = if (isImeVisible) 3.dp else 4.dp
                        )
                    }
                }
            }
        }
    }

    if (showInsertSheet) {
        InsertFeaturesBottomSheet(
            isDark = effectiveDark,
            isArabic = isArabic,
            accentColor = accentColor,
            textColor = textColor,
            metaColor = metaColor,
            onDismiss = { showInsertSheet = false },
            onSelectFeature = { feature ->
                showInsertSheet = false
                when (feature) {
                    InsertFeatureType.TABLE -> {
                        editingTableTarget = null
                        showTableDialog = true
                    }
                    InsertFeatureType.VOICE_MEMO -> {
                        playbackController.stop()
                        showVoiceRecorder = true
                    }
                    InsertFeatureType.NEW_PAGE -> {
                        if (pages.size < maxAllowedPages) {
                            pages.add("")
                            pageTitles.add("")
                            activePageIndex = pages.lastIndex
                        } else {
                            onRequestPremiumUpgrade()
                        }
                    }
                    InsertFeatureType.CHECKLIST_CARD -> {
                        checklist.add(ChecklistItem(text = if (isArabic) "مهمة جديدة" else "New task", isDone = false))
                    }
                    InsertFeatureType.TODO_CHECKBOX -> applyFormatActionToPage(safePageIdx, RichFormatAction.CHECKBOX)
                    InsertFeatureType.BULLET_LIST -> applyFormatActionToPage(safePageIdx, RichFormatAction.BULLET_LIST)
                    InsertFeatureType.NUMBERED_LIST -> applyFormatActionToPage(safePageIdx, RichFormatAction.NUMBERED_LIST)
                    InsertFeatureType.QUOTE -> applyFormatActionToPage(safePageIdx, RichFormatAction.QUOTE)
                    InsertFeatureType.CALLOUT -> applyFormatActionToPage(safePageIdx, RichFormatAction.CALLOUT)
                    InsertFeatureType.CODE_BLOCK -> applyFormatActionToPage(safePageIdx, RichFormatAction.CODE_BLOCK)
                    InsertFeatureType.INLINE_CODE -> applyFormatActionToPage(safePageIdx, RichFormatAction.CODE)
                    InsertFeatureType.DIVIDER -> applyFormatActionToPage(safePageIdx, RichFormatAction.DIVIDER)
                    else -> applyInsertFeatureToPage(safePageIdx, feature)
                }
            }
        )
    }

    if (showTableDialog) {
        val curBody = pages.getOrElse(safePageIdx) { "" }
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
                if (safePageIdx in pages.indices) {
                    if (editTarget != null && editTarget.startOffset >= 0) {
                        pages[safePageIdx] = RichTextCodec.replaceTableAtRange(
                            text = curBody,
                            startOffset = editTarget.startOffset,
                            endOffset = editTarget.endOffset,
                            tableMarkdown = md
                        ).text
                        clearPageSegments(safePageIdx)
                    } else {
                        val updated = insertTableInSegmentedPage(
                            pageText = curBody,
                            preferredSegmentIndex = activeSegmentByPage[safePageIdx] ?: 0,
                            segmentSelections = pageSegmentSelections(safePageIdx),
                            tableMarkdown = md
                        )
                        pages[safePageIdx] = updated
                        clearPageSegments(safePageIdx)
                        val latestSegments = RichTextCodec.splitPageIntoEditorSegments(updated)
                        val tailIdx = latestSegments.indexOfLast { it is EditorPageSegment.TextSegment }.coerceAtLeast(0)
                        activeSegmentByPage[safePageIdx] = tailIdx
                        segmentSelections["$safePageIdx:$tailIdx"] = TextRange(0)
                    }
                    pendingStylesByPage.remove(safePageIdx)
                }
                showTableDialog = false
                editingTableTarget = null
            },
            onDeleteTable = if (editTarget != null && editTarget.startOffset >= 0) {
                {
                    if (safePageIdx in pages.indices) {
                        pages[safePageIdx] = RichTextCodec.deleteTableAtRange(
                            curBody,
                            editTarget.startOffset,
                            editTarget.endOffset
                        )
                        clearPageSegments(safePageIdx)
                        activeSegmentByPage[safePageIdx] = 0
                        pendingStylesByPage.remove(safePageIdx)
                    }
                    showTableDialog = false
                    editingTableTarget = null
                }
            } else null
        )
    }

    if (showStyleSheet) {
        DocumentStyleBottomSheet(
            isDark = effectiveDark,
            isArabic = isArabic,
            isPremium = isPremium,
            selectedTint = selectedTint,
            customColorArgb = customColorArgb,
            backgroundId = backgroundId,
            category = category,
            onSelectTint = { selectedTint = it },
            onSelectCustomColor = { customColorArgb = it },
            onSelectBackground = { backgroundId = it },
            onSelectCategory = { category = it },
            onResetStyle = {
                selectedTint = GlassTint.AMETHYST
                backgroundId = NoteBackgroundCatalog.NONE_ID
                customColorArgb = 0L
            },
            onRequestPremiumUpgrade = onRequestPremiumUpgrade,
            onDismiss = { showStyleSheet = false }
        )
    }

    if (showVoiceRecorder) {
        VoiceRecorderDialog(
            isDark = effectiveDark,
            isArabic = isArabic,
            onDismiss = { showVoiceRecorder = false },
            onSaveVoiceNote = { durSec, path, size ->
                audioDurationSec = durSec
                audioFilePath = path
                audioByteSize = size
                showVoiceRecorder = false
            }
        )
    }
}
