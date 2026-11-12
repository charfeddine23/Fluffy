package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
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

    // Local mutable state for editing
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    var category by remember { mutableStateOf(note.category) }
    var selectedTint by remember { mutableStateOf(GlassTint.fromId(note.colorPreset)) }
    var isPinned by remember { mutableStateOf(note.isPinned) }
    var isFavorite by remember { mutableStateOf(note.isFavorite) }
    var audioDurationSec by remember { mutableIntStateOf(note.audioDurationSec) }
    var showVoiceRecorderDialog by remember { mutableStateOf(false) }

    val checklistItems = remember {
        mutableStateListOf<ChecklistItem>().apply {
            addAll(ChecklistItem.jsonToList(note.checklistJson))
        }
    }

    // Real-time automatic background save with 500ms debounce
    androidx.compose.runtime.LaunchedEffect(
        title, content, category, selectedTint.id,
        isPinned, isFavorite, checklistItems.toList(), audioDurationSec
    ) {
        kotlinx.coroutines.delay(500)
        onAutoSave?.invoke(
            title, content, category, selectedTint.id,
            isPinned, isFavorite, checklistItems.toList(), audioDurationSec
        )
    }

    // Handle Android system back gesture to save or pop
    BackHandler {
        onSave(
            title, content, category, selectedTint.id,
            isPinned, isFavorite, checklistItems.toList(), audioDurationSec
        )
    }

    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val bodyColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    FrostedBackdrop(isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
        ) {
            // Top Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("editor_back_button")
                        .liquidGlass(
                            isDark = isDark,
                            tint = GlassTint.FROST,
                            shape = CircleShape,
                            borderWidth = 1.2.dp
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Actions: Pin, Favorite, Share, Delete, Done
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pin Toggle
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("editor_pin_toggle")
                            .clip(CircleShape)
                            .background(if (isPinned) Color(0xFFFBBF24).copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { isPinned = !isPinned },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (isPinned) Color(0xFFFBBF24) else metaColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Favorite Toggle
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("editor_fav_toggle")
                            .clip(CircleShape)
                            .background(if (isFavorite) Color(0xFFF43F5E).copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { isFavorite = !isFavorite },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFF43F5E) else metaColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Share Note
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable {
                                shareNoteText(context, title, content, checklistItems)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = metaColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Delete Note (if not new)
                    if (note.id != 0L) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable { onDelete() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Save / Done Button
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("editor_save_button")
                            .liquidGlass(
                                isDark = isDark,
                                tint = selectedTint,
                                shape = RoundedCornerShape(19.dp),
                                borderWidth = 1.6.dp,
                                customFill = if (isDark) selectedTint.accentColor.copy(alpha = 0.35f) else selectedTint.accentColor.copy(alpha = 0.2f)
                            )
                            .clickable {
                                onSave(
                                    title, content, category, selectedTint.id,
                                    isPinned, isFavorite, checklistItems.toList(), audioDurationSec
                                )
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isArabic) "حفظ" else "Save",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
            }

            // Scrollable Content Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Glass Tint Selector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isArabic) "لون الزجاج السائل:" else "Glass Tint:",
                        fontSize = 12.sp,
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

                // Category Chips Selector
                val categories = listOf("أفكار", "مهام", "عمل", "شخصي", "دراسة", "عام")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
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
                                .padding(horizontal = 14.dp, vertical = 6.dp)
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

                Spacer(modifier = Modifier.height(14.dp))

                // Title Input
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_title_input"),
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    ),
                    cursorBrush = SolidColor(selectedTint.accentColor),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = if (isArabic) "عنوان الملاحظة..." else "Note Title...",
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = metaColor.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Note Card (if attached or add button)
                if (audioDurationSec > 0) {
                    LiquidGlassCard(
                        isDark = isDark,
                        tint = GlassTint.ROSE,
                        shape = RoundedCornerShape(18.dp),
                        borderWidth = 1.2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = Color(0xFFEC4899),
                                    modifier = Modifier.size(24.dp)
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

                            IconButton(onClick = { audioDurationSec = 0 }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Voice Memo",
                                    tint = metaColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x1AFFFFFF) else Color(0x10000000))
                            .clickable { showVoiceRecorderDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color(0xFFEC4899),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "+ إضافة تسجيل صوتي" else "+ Add Voice Memo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Checklist Section
                if (checklistItems.isNotEmpty()) {
                    Text(
                        text = if (isArabic) "قائمة المهام:" else "Checklist:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = selectedTint.accentColor,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    checklistItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (item.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = "Toggle",
                                tint = if (item.isDone) selectedTint.accentColor else metaColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        checklistItems[index] = item.copy(isDone = !item.isDone)
                                    }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            BasicTextField(
                                value = item.text,
                                onValueChange = { newText ->
                                    checklistItems[index] = item.copy(text = newText)
                                },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    color = if (item.isDone) metaColor else textColor,
                                    textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                cursorBrush = SolidColor(selectedTint.accentColor),
                                decorationBox = { inner ->
                                    if (item.text.isEmpty()) {
                                        Text(
                                            text = if (isArabic) "عنصر مهمة..." else "Checklist item...",
                                            fontSize = 14.sp,
                                            color = metaColor.copy(alpha = 0.5f)
                                        )
                                    }
                                    inner()
                                }
                            )

                            IconButton(
                                onClick = { checklistItems.removeAt(index) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = metaColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Add new item button
                    Row(
                        modifier = Modifier
                            .clickable {
                                checklistItems.add(ChecklistItem(text = "", isDone = false))
                            }
                            .padding(vertical = 8.dp),
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
                            text = if (isArabic) "إضافة عنصر جديد للقائمة" else "Add new item",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = selectedTint.accentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    // Option to convert/add checklist
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x1AFFFFFF) else Color(0x10000000))
                            .clickable {
                                checklistItems.add(ChecklistItem(text = "", isDone = false))
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = selectedTint.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "+ إضافة قائمة مهام" else "+ Add Checklist",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Main Note Content Input
                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .testTag("editor_content_input"),
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = bodyColor,
                        lineHeight = 22.sp
                    ),
                    cursorBrush = SolidColor(selectedTint.accentColor),
                    decorationBox = { innerTextField ->
                        if (content.isEmpty()) {
                            Text(
                                text = if (isArabic) "اكتب أفكارك وملاحظاتك هنا بحرية..." else "Write your thoughts and ideas here...",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    color = metaColor.copy(alpha = 0.5f),
                                    lineHeight = 22.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Word count & Character count indicator
                val wordCount = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
                val charCount = content.length
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = if (isArabic) "$wordCount كلمة  •  $charCount حرف" else "$wordCount words • $charCount chars",
                        fontSize = 11.sp,
                        color = metaColor
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
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
