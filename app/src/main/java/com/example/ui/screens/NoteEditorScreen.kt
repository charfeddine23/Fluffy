package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.rememberAudioPlaybackController
import com.example.data.model.ChecklistItem
import com.example.data.model.GlassTint
import com.example.data.model.NoteBackgroundCanvas
import com.example.data.model.NoteBackgroundCatalog
import com.example.data.model.NoteEntity
import com.example.data.storage.AudioCloudPolicy
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.DocumentStyleBottomSheet
import com.example.ui.components.VoiceRecorderDialog
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.liquidGlass

@Composable
fun NoteEditorScreen(
    note: NoteEntity,
    isCreatingNew: Boolean = note.id == 0L,
    isDark: Boolean,
    isArabic: Boolean,
    onBack: () -> Unit,
    onSave: (
        title: String,
        content: String,
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
    onAutoSave: ((
        title: String,
        content: String,
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

    // New drafts open in Edit/Writing mode; saved notes reopen in View-Only mode
    var isEditMode by remember { mutableStateOf(isCreatingNew || note.id == 0L) }

    // Local mutable state for literal editing
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
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

    val checklistItems = remember {
        mutableStateListOf<ChecklistItem>().apply {
            addAll(ChecklistItem.jsonToList(note.checklistJson))
        }
    }

    fun triggerImmediateSaveIfViewMode() {
        if (!isEditMode) {
            onAutoSave?.invoke(
                title, content, category, selectedTint.id, backgroundId, customColorArgb,
                isPinned, isFavorite, checklistItems.toList(),
                audioDurationSec, audioFilePath, audioByteSize
            )
        }
    }

    // Real-time automatic background save with 500ms debounce (active only in Writing/Edit mode)
    LaunchedEffect(
        isEditMode, title, content, category, selectedTint.id, backgroundId, customColorArgb,
        isPinned, isFavorite, checklistItems.toList(),
        audioDurationSec, audioFilePath, audioByteSize
    ) {
        if (!isEditMode) return@LaunchedEffect
        kotlinx.coroutines.delay(500)
        onAutoSave?.invoke(
            title, content, category, selectedTint.id, backgroundId, customColorArgb,
            isPinned, isFavorite, checklistItems.toList(),
            audioDurationSec, audioFilePath, audioByteSize
        )
    }

    // Handle Android system back gesture cleanly for both Edit & View modes
    BackHandler {
        playbackController.stop()
        if (isEditMode) {
            onSave(
                title, content, category, selectedTint.id, backgroundId, customColorArgb,
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
                    title, content, category, selectedTint.id, backgroundId, customColorArgb,
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

    val wordCount = remember(content) {
        if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
    }
    val charCount = content.length

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
                    .padding(horizontal = 14.dp, vertical = 8.dp),
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
                                    title, content, category, selectedTint.id, backgroundId, customColorArgb,
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
                                        title, content, category, selectedTint.id, backgroundId, customColorArgb,
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
                    // Style & 20 Backgrounds quick button (accessible in BOTH View & Edit modes)
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
                        Icon(
                            imageVector = if (activeBgPreset != null) Icons.Default.Wallpaper else Icons.Default.Palette,
                            contentDescription = if (isArabic) "ألوان وخلفيات المستند" else "Document Colors & Backgrounds",
                            tint = if (hasCustomVisual) accentColor else textColor,
                            modifier = Modifier.size(20.dp)
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
                                        title, content, category, selectedTint.id, backgroundId, customColorArgb,
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
                                modifier = Modifier.size(21.dp)
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
                                    customFill = accentColor.copy(alpha = if (effectiveDark) 0.26f else 0.18f)
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
                    }
                }
            }

            // 2. Full-Height Writing Area (Title right at top + Writing Canvas stretching edge-to-edge down to bottom toolbar)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val minCanvasHeight = maxHeight
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minCanvasHeight)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    // Title (Editable in Edit mode; Read-Only selectable text in View-Only mode)
                    if (isEditMode) {
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
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
                        Spacer(modifier = Modifier.height(14.dp))
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
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Interactive Audio Player Card (only shown when a voice memo / audio file is attached)
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

                    // Checklist Items (only shown if checklist items exist)
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
                                                checklistItems[index] = item.copy(text = newText)
                                            },
                                            modifier = Modifier.weight(1f),
                                            textStyle = TextStyle(
                                                fontSize = 15.sp,
                                                color = if (item.isDone) metaColor else textColor,
                                                textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                                            ),
                                            cursorBrush = SolidColor(selectedTint.accentColor),
                                            decorationBox = { inner ->
                                                if (item.text.isEmpty()) {
                                                    Text(
                                                        text = if (isArabic) "عنصر مهمة..." else "Checklist item...",
                                                        fontSize = 15.sp,
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
                                                fontSize = 15.sp,
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
                                            checklistItems.add(ChecklistItem(text = "", isDone = false))
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

                    // Main Full-Screen Note Content: Editable in Edit mode, Read-Only selectable in View-Only mode
                    val remainingMinHeight = (minCanvasHeight - 64.dp).coerceAtLeast(220.dp)
                    if (isEditMode) {
                        BasicTextField(
                            value = content,
                            onValueChange = { content = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = remainingMinHeight)
                                .testTag("editor_content_input"),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = bodyColor,
                                lineHeight = 24.sp
                            ),
                            cursorBrush = SolidColor(selectedTint.accentColor),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (content.isEmpty()) {
                                        Text(
                                            text = if (isArabic) "اكتب أفكارك وملاحظاتك هنا بحرية..." else "Note",
                                            style = TextStyle(
                                                fontSize = 16.sp,
                                                color = metaColor.copy(alpha = 0.6f),
                                                lineHeight = 24.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = content,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = remainingMinHeight)
                                    .testTag("editor_content_readonly"),
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = bodyColor,
                                    lineHeight = 24.sp
                                )
                            )
                        }
                    }
                }
            }

            // 3. Bottom Action Toolbar (Hidden completely in View-Only mode; visible only in Edit/Writing mode)
            AnimatedVisibility(
                visible = isEditMode,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Add Voice Memo button (now anchored at bottom)
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
                                    customFill = if (hasAttachedAudio) Color(0xFFEC4899).copy(alpha = 0.25f) else pillFill
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

                        // Add Checklist button (now anchored at bottom)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("editor_add_checklist_button")
                                .liquidGlass(
                                    isDark = effectiveDark,
                                    tint = if (checklistItems.isNotEmpty()) selectedTint else GlassTint.FROST,
                                    shape = CircleShape,
                                    borderWidth = 1.dp,
                                    customFill = if (checklistItems.isNotEmpty()) accentColor.copy(alpha = 0.24f) else pillFill
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    checklistItems.add(ChecklistItem(text = "", isDone = false))
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
                                    tint = if (customColorArgb != 0L || selectedTint != GlassTint.FROST) selectedTint else GlassTint.FROST,
                                    shape = CircleShape,
                                    borderWidth = 1.dp,
                                    customFill = if (customColorArgb != 0L) accentColor.copy(alpha = 0.26f) else pillFill
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
                                tint = if (customColorArgb != 0L) accentColor else textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 20 Document Backgrounds button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("editor_backgrounds_button")
                                .liquidGlass(
                                    isDark = effectiveDark,
                                    tint = if (activeBgPreset != null) selectedTint else GlassTint.FROST,
                                    shape = CircleShape,
                                    borderWidth = 1.dp,
                                    customFill = if (activeBgPreset != null) accentColor.copy(alpha = 0.26f) else pillFill
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
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = if (isArabic) "خلفيات المستند (20)" else "Document Backgrounds (20)",
                                tint = if (activeBgPreset != null) accentColor else textColor,
                                modifier = Modifier.size(20.dp)
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
                                        shareNoteText(context, title, content, checklistItems)
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

        // Document Colors & 20 Backgrounds Modal Bottom Sheet
        if (showStyleSheet) {
            DocumentStyleBottomSheet(
                isDark = isDark,
                isArabic = isArabic,
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
                }
            )
        }
    }
}

private fun shareNoteText(
    context: Context,
    title: String,
    content: String,
    checklist: List<ChecklistItem>
) {
    val sb = StringBuilder()
    if (title.isNotBlank()) sb.append("$title\n\n")
    if (content.isNotBlank()) sb.append("$content\n\n")
    if (checklist.isNotEmpty()) {
        checklist.forEach {
            val mark = if (it.isDone) "[✓]" else "[ ]"
            sb.append("$mark ${it.text}\n")
        }
    }
    sb.append("\n— Shared from Fluffy Liquid Glass Notes ✨")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Share note via"))
}
