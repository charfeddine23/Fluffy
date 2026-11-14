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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChecklistItem
import com.example.data.model.GlassTint
import com.example.data.model.NoteEntity
import com.example.ui.components.VoiceRecorderDialog
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
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
        isPinned: Boolean,
        isFavorite: Boolean,
        checklist: List<ChecklistItem>,
        audioDurationSec: Int
    ) -> Unit,
    onDelete: () -> Unit,
    onAutoSave: ((
        title: String,
        content: String,
        category: String,
        colorPreset: String,
        isPinned: Boolean,
        isFavorite: Boolean,
        checklist: List<ChecklistItem>,
        audioDurationSec: Int
    ) -> Unit)? = null
) {
    val context = LocalContext.current

    // New drafts open in Edit/Writing mode; saved notes reopen in View-Only mode
    var isEditMode by remember { mutableStateOf(isCreatingNew || note.id == 0L) }

    // Local mutable state for literal editing
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    var category by remember { mutableStateOf(note.category) }
    var selectedTint by remember { mutableStateOf(GlassTint.fromId(note.colorPreset)) }
    var isPinned by remember { mutableStateOf(note.isPinned) }
    var isFavorite by remember { mutableStateOf(note.isFavorite) }
    var audioDurationSec by remember { mutableIntStateOf(note.audioDurationSec) }

    var showVoiceRecorderDialog by remember { mutableStateOf(false) }
    var showStylePanel by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val checklistItems = remember {
        mutableStateListOf<ChecklistItem>().apply {
            addAll(ChecklistItem.jsonToList(note.checklistJson))
        }
    }

    // Real-time automatic background save with 500ms debounce (active only in Writing/Edit mode)
    LaunchedEffect(
        isEditMode, title, content, category, selectedTint.id,
        isPinned, isFavorite, checklistItems.toList(), audioDurationSec
    ) {
        if (!isEditMode) return@LaunchedEffect
        kotlinx.coroutines.delay(500)
        onAutoSave?.invoke(
            title, content, category, selectedTint.id,
            isPinned, isFavorite, checklistItems.toList(), audioDurationSec
        )
    }

    // Handle Android system back gesture cleanly for both Edit & View modes
    BackHandler {
        if (isEditMode) {
            onSave(
                title, content, category, selectedTint.id,
                isPinned, isFavorite, checklistItems.toList(), audioDurationSec
            )
        } else {
            if (isPinned != note.isPinned) {
                onAutoSave?.invoke(
                    title, content, category, selectedTint.id,
                    isPinned, isFavorite, checklistItems.toList(), audioDurationSec
                )
            }
            onBack()
        }
    }

    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val bodyColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val pillFill = if (isDark) Color(0xFF1E293B).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.72f)

    val wordCount = remember(content) {
        if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
    }
    val charCount = content.length

    FrostedBackdrop(isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
        ) {
            // 1. Minimal Top Bar: Back on start; Pin + (Save in Edit mode OR Pencil in View-Only mode)
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
                            if (isEditMode) {
                                onSave(
                                    title, content, category, selectedTint.id,
                                    isPinned, isFavorite, checklistItems.toList(), audioDurationSec
                                )
                            } else {
                                if (isPinned != note.isPinned) {
                                    onAutoSave?.invoke(
                                        title, content, category, selectedTint.id,
                                        isPinned, isFavorite, checklistItems.toList(), audioDurationSec
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
                    // Pin Button (always remains visible in both Edit & View-Only modes)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("editor_pin_toggle")
                            .liquidGlass(
                                isDark = isDark,
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
                                if (!isEditMode) {
                                    onAutoSave?.invoke(
                                        title, content, category, selectedTint.id,
                                        isPinned, isFavorite, checklistItems.toList(), audioDurationSec
                                    )
                                }
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
                                    isDark = isDark,
                                    tint = selectedTint,
                                    shape = RoundedCornerShape(14.dp),
                                    borderWidth = 1.2.dp,
                                    customFill = selectedTint.accentColor.copy(alpha = if (isDark) 0.28f else 0.18f)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    onSave(
                                        title, content, category, selectedTint.id,
                                        isPinned, isFavorite, checklistItems.toList(), audioDurationSec
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
                                    isDark = isDark,
                                    tint = selectedTint,
                                    shape = RoundedCornerShape(14.dp),
                                    borderWidth = 1.2.dp,
                                    customFill = selectedTint.accentColor.copy(alpha = if (isDark) 0.26f else 0.18f)
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

                    // Attached Voice Memo Card (only shown when a voice memo is attached)
                    if (audioDurationSec > 0) {
                        LiquidGlassCard(
                            isDark = isDark,
                            tint = GlassTint.ROSE,
                            shape = RoundedCornerShape(16.dp),
                            borderWidth = 1.2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = Color(0xFFEC4899),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isArabic) "تسجيل صوتي مرفق" else "Attached Voice Memo",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        Text(
                                            text = "${audioDurationSec / 60}:${String.format("%02d", audioDurationSec % 60)}",
                                            fontSize = 11.sp,
                                            color = metaColor
                                        )
                                    }
                                }

                                if (isEditMode) {
                                    IconButton(
                                        onClick = { audioDurationSec = 0 },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = if (isArabic) "حذف التسجيل" else "Remove Voice Memo",
                                            tint = metaColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
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

            // Expandable Style / Glass Tint + Category Panel (only in Edit mode)
            AnimatedVisibility(
                visible = isEditMode && showStylePanel,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                LiquidGlassCard(
                    isDark = isDark,
                    tint = selectedTint,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    borderWidth = 1.2.dp,
                    customFill = if (isDark) Color(0xFF0F172A).copy(alpha = 0.90f) else Color.White.copy(alpha = 0.92f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "لون الزجاج السائل" else "Glass Tint",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = metaColor
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassTint.entries.forEach { tint ->
                                    val isSelected = selectedTint == tint
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("tint_${tint.id}")
                                            .clip(CircleShape)
                                            .background(tint.accentColor)
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedTint = tint }
                                    )
                                }
                            }
                        }

                        val categories = listOf("أفكار", "مهام", "عمل", "شخصي", "دراسة", "عام")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = category.equals(cat, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) selectedTint.accentColor.copy(alpha = 0.35f)
                                            else if (isDark) Color(0x22FFFFFF) else Color(0x15000000)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) selectedTint.accentColor else Color(0x33FFFFFF),
                                            shape = CircleShape
                                        )
                                        .clickable { category = cat }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) textColor else metaColor
                                    )
                                }
                            }
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
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("editor_add_voice_button")
                                .liquidGlass(
                                    isDark = isDark,
                                    tint = if (audioDurationSec > 0) GlassTint.ROSE else GlassTint.FROST,
                                    shape = CircleShape,
                                    borderWidth = 1.dp,
                                    customFill = if (audioDurationSec > 0) Color(0xFFEC4899).copy(alpha = 0.25f) else pillFill
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
                                tint = if (audioDurationSec > 0) Color(0xFFEC4899) else textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Add Checklist button (now anchored at bottom)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("editor_add_checklist_button")
                                .liquidGlass(
                                    isDark = isDark,
                                    tint = if (checklistItems.isNotEmpty()) selectedTint else GlassTint.FROST,
                                    shape = CircleShape,
                                    borderWidth = 1.dp,
                                    customFill = if (checklistItems.isNotEmpty()) selectedTint.accentColor.copy(alpha = 0.24f) else pillFill
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
                                tint = if (checklistItems.isNotEmpty()) selectedTint.accentColor else textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Glass Tint & Category Palette button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("editor_palette_button")
                                .liquidGlass(
                                    isDark = isDark,
                                    tint = if (showStylePanel) selectedTint else GlassTint.FROST,
                                    shape = CircleShape,
                                    borderWidth = 1.dp,
                                    customFill = if (showStylePanel) selectedTint.accentColor.copy(alpha = 0.26f) else pillFill
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) { showStylePanel = !showStylePanel },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = if (isArabic) "لون الزجاج والقسم" else "Color & Category",
                                tint = if (showStylePanel) selectedTint.accentColor else textColor,
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
                                        isDark = isDark,
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
                                containerColor = if (isDark) Color(0xFF1E293B) else Color.White
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
                                                text = if (isArabic) "حذف الملاحظة" else "Delete Note",
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

        // Voice Recorder Dialog
        if (showVoiceRecorderDialog) {
            VoiceRecorderDialog(
                isDark = isDark,
                isArabic = isArabic,
                onDismiss = { showVoiceRecorderDialog = false },
                onSaveVoiceNote = { duration ->
                    audioDurationSec = duration
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
