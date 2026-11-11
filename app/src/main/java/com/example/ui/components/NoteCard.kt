package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChecklistItem
import com.example.data.model.GlassTint
import com.example.data.model.NoteEntity
import com.example.ui.glass.LiquidGlassCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteCard(
    note: NoteEntity,
    isDark: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleChecklistItem: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassTint = GlassTint.fromId(note.colorPreset)
    val checklistItems = remember(note.checklistJson) {
        ChecklistItem.jsonToList(note.checklistJson)
    }

    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val bodyColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    LiquidGlassCard(
        isDark = isDark,
        tint = glassTint,
        shape = RoundedCornerShape(22.dp),
        borderWidth = if (note.isPinned) 2.2.dp else 1.4.dp,
        onClick = onClick,
        testTag = "note_card_${note.id}",
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Category Badge + Pin Indicator + Favorite Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isDark) glassTint.accentColor.copy(alpha = 0.25f)
                            else glassTint.accentColor.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = note.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = glassTint.accentColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Pinned icon indicator
                    if (note.isPinned) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFBBF24).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Voice Note badge
                    if (note.audioDurationSec > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFEC4899).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Voice Memo",
                                    tint = Color(0xFFEC4899),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = formatDuration(note.audioDurationSec),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEC4899)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Note Title
            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = titleColor,
                        lineHeight = 22.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Note Content
            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        color = bodyColor,
                        lineHeight = 19.sp
                    ),
                    maxLines = if (checklistItems.isNotEmpty()) 2 else 5,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Interactive Checklist Preview (if any)
            if (checklistItems.isNotEmpty()) {
                val doneCount = checklistItems.count { it.isDone }
                val progress = doneCount.toFloat() / checklistItems.size.toFloat()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "$doneCount من ${checklistItems.size} مكتمل" else "$doneCount of ${checklistItems.size} done",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = metaColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = glassTint.accentColor,
                    trackColor = if (isDark) Color(0x22FFFFFF) else Color(0x18000000)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Show up to 3 checklist items with interactive checkboxes
                checklistItems.take(3).forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true)
                            ) { onToggleChecklistItem(index) }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = if (item.isDone) "Done" else "Pending",
                            tint = if (item.isDone) glassTint.accentColor else metaColor,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.text,
                            fontSize = 12.5.sp,
                            color = if (item.isDone) metaColor else bodyColor,
                            textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (checklistItems.size > 3) {
                    Text(
                        text = if (isArabic) "+ ${checklistItems.size - 3} مهام إضافية..." else "+ ${checklistItems.size - 3} more items...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = glassTint.accentColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Card Footer: Timestamp + Quick Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatFriendlyDate(note.updatedAt, isArabic),
                    fontSize = 11.sp,
                    color = metaColor
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Pin Button
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onTogglePin
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Toggle Pin",
                            tint = if (note.isPinned) Color(0xFFFBBF24) else metaColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Favorite Button
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onToggleFavorite
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (note.isFavorite) Color(0xFFF43F5E) else metaColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete Button
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onDelete
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Note",
                            tint = metaColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatFriendlyDate(timestamp: Long, isArabic: Boolean): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60000 -> if (isArabic) "الآن" else "Just now"
        diff < 3600000 -> {
            val mins = diff / 60000
            if (isArabic) "منذ $mins د" else "${mins}m ago"
        }
        diff < 86400000 -> {
            val hours = diff / 3600000
            if (isArabic) "منذ $hours س" else "${hours}h ago"
        }
        else -> {
            val sdf = SimpleDateFormat("dd MMM", if (isArabic) Locale("ar") else Locale.ENGLISH)
            sdf.format(Date(timestamp))
        }
    }
}

private fun formatDuration(sec: Int): String {
    val mins = sec / 60
    val remSec = sec % 60
    return String.format(Locale.US, "%d:%02d", mins, remSec)
}
