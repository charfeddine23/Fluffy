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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Unarchive
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChecklistItem
import com.example.data.model.GlassTint
import com.example.data.model.NoteBackgroundCanvas
import com.example.data.model.NoteBackgroundCatalog
import com.example.data.model.NoteEntity
import com.example.data.model.NotePagesCodec
import com.example.data.storage.AudioCloudPolicy
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.theme.tr
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
    onArchive: () -> Unit = {},
    onMoveToVault: () -> Unit = {},
    onDelete: () -> Unit,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val glassTint = GlassTint.fromId(note.colorPreset)
    val bgPreset = remember(note.backgroundId) { NoteBackgroundCatalog.findById(note.backgroundId) }
    val customFillColor = remember(note.customColorArgb, isDark) {
        if (note.customColorArgb != 0L) {
            Color(note.customColorArgb.toInt()).copy(alpha = if (isDark) 0.90f else 0.96f)
        } else {
            null
        }
    }
    val surfaceColors = remember(isDark, glassTint, note.customColorArgb, note.backgroundId) {
        NoteBackgroundCatalog.resolveSurfaceColors(
            themeIsDark = isDark,
            tint = glassTint,
            customColorArgb = note.customColorArgb,
            backgroundId = note.backgroundId
        )
    }

    val checklistItems = remember(note.checklistJson) {
        ChecklistItem.jsonToList(note.checklistJson)
    }

    val titleColor = surfaceColors.titleColor
    val bodyColor = surfaceColors.bodyColor
    val metaColor = surfaceColors.metaColor
    val cardAccent = surfaceColors.accentColor

    LiquidGlassCard(
        isDark = surfaceColors.effectiveDark,
        tint = glassTint,
        shape = RoundedCornerShape(22.dp),
        borderWidth = when {
            isDragging -> 2.8.dp
            note.isPinned -> 2.2.dp
            else -> 1.4.dp
        },
        customFill = if (bgPreset != null) Color.Transparent else customFillColor,
        onClick = onClick,
        testTag = "note_card_${note.id}",
        modifier = modifier.fillMaxWidth()
    ) {
        if (bgPreset != null) {
            NoteBackgroundCanvas(
                preset = bgPreset,
                isCompactCard = true,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (bgPreset.isDarkSurface) Color(0xFF020617).copy(alpha = 0.24f)
                        else Color.White.copy(alpha = 0.22f)
                    )
            )
        }

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
                            if (surfaceColors.effectiveDark) cardAccent.copy(alpha = 0.26f)
                            else cardAccent.copy(alpha = 0.16f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = note.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardAccent
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val totalPages = remember(note.content, note.pagesJson) {
                        NotePagesCodec.pageCount(note)
                    }
                    if (totalPages > 1) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (surfaceColors.effectiveDark) cardAccent.copy(alpha = 0.22f)
                                    else cardAccent.copy(alpha = 0.14f)
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = if (isArabic) "$totalPages صفحات" else "$totalPages pages",
                                    tint = cardAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isArabic) "$totalPages ص" else "${totalPages}p",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = cardAccent
                                )
                            }
                        }
                    }

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
                    if (note.audioDurationSec > 0 || note.audioFilePath.isNotBlank()) {
                        val effectiveSize = AudioCloudPolicy.effectiveAudioByteSize(note.audioByteSize, note.audioFilePath)
                        val isLocalOnlyAudio = AudioCloudPolicy.isAudioOverCloudLimit(effectiveSize)
                        val badgeColor = if (isLocalOnlyAudio) Color(0xFFF59E0B) else Color(0xFFEC4899)
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isLocalOnlyAudio) Icons.Default.CloudOff else Icons.Default.GraphicEq,
                                    contentDescription = if (isLocalOnlyAudio) {
                                        if (isArabic) "محلي فقط (> 900 م.ب)" else "Local only (> 900 MB)"
                                    } else {
                                        "Voice Memo"
                                    },
                                    tint = badgeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isLocalOnlyAudio) {
                                        if (isArabic) "${formatDuration(note.audioDurationSec)} • محلي" else "${formatDuration(note.audioDurationSec)} • Local"
                                    } else {
                                        formatDuration(note.audioDurationSec)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
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
                    color = cardAccent,
                    trackColor = if (surfaceColors.effectiveDark) Color(0x22FFFFFF) else Color(0x18000000)
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
                            tint = if (item.isDone) cardAccent else metaColor,
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
                        color = cardAccent,
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
                    color = metaColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Pin Button
                    Box(
                        modifier = Modifier
                            .size(26.dp)
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
                            contentDescription = tr("تثبيت", "Épingler", "Toggle Pin", "Senteḍ"),
                            tint = if (note.isPinned) Color(0xFFFBBF24) else metaColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Favorite Button
                    Box(
                        modifier = Modifier
                            .size(26.dp)
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
                            contentDescription = tr("مفضلة", "Favori", "Toggle Favorite", "Anurif"),
                            tint = if (note.isFavorite) Color(0xFFF43F5E) else metaColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Move to Vault Button
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("note_move_to_vault_${note.id}")
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onMoveToVault
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = tr("نقل إلى الـ Vault", "Déplacer vers Vault", "Move to Vault", "Smutti ɣer Vault"),
                            tint = Color(0xFFC084FC).copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Archive Button
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onArchive
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (note.isArchived) "Unarchive" else "Archive",
                            tint = if (note.isArchived) Color(0xFF38BDF8) else metaColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Delete Button
                    Box(
                        modifier = Modifier
                            .size(26.dp)
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
                            contentDescription = tr("حذف", "Supprimer", "Delete", "Kkes"),
                            tint = Color(0xFFEF4444).copy(alpha = 0.78f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Gallery View card: uniform curated showcase tile highlighting document art, tint, page count,
 * bold title, concise preview, and quick footer actions.
 */
@Composable
fun GalleryNoteCard(
    note: NoteEntity,
    isDark: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArchive: () -> Unit = {},
    onMoveToVault: () -> Unit = {},
    onDelete: () -> Unit,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val glassTint = GlassTint.fromId(note.colorPreset)
    val bgPreset = remember(note.backgroundId) { NoteBackgroundCatalog.findById(note.backgroundId) }
    val customFillColor = remember(note.customColorArgb, isDark) {
        if (note.customColorArgb != 0L) {
            Color(note.customColorArgb.toInt()).copy(alpha = if (isDark) 0.90f else 0.96f)
        } else null
    }
    val surfaceColors = remember(isDark, glassTint, note.customColorArgb, note.backgroundId) {
        NoteBackgroundCatalog.resolveSurfaceColors(
            themeIsDark = isDark,
            tint = glassTint,
            customColorArgb = note.customColorArgb,
            backgroundId = note.backgroundId
        )
    }
    val checklistItems = remember(note.checklistJson) { ChecklistItem.jsonToList(note.checklistJson) }
    val totalPages = remember(note.content, note.pagesJson) { NotePagesCodec.pageCount(note) }
    val cardAccent = surfaceColors.accentColor
    val titleColor = surfaceColors.titleColor
    val bodyColor = surfaceColors.bodyColor
    val metaColor = surfaceColors.metaColor

    LiquidGlassCard(
        isDark = surfaceColors.effectiveDark,
        tint = glassTint,
        shape = RoundedCornerShape(24.dp),
        borderWidth = when {
            isDragging -> 2.8.dp
            note.isPinned -> 2.2.dp
            else -> 1.4.dp
        },
        customFill = if (bgPreset != null) Color.Transparent else customFillColor,
        onClick = onClick,
        testTag = "gallery_note_card_${note.id}",
        modifier = modifier
            .fillMaxWidth()
            .height(216.dp)
    ) {
        // Artwork / Gradient Header Backdrop
        if (bgPreset != null) {
            NoteBackgroundCanvas(
                preset = bgPreset,
                isCompactCard = true,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                cardAccent.copy(alpha = if (surfaceColors.effectiveDark) 0.24f else 0.16f),
                                Color.Transparent,
                                cardAccent.copy(alpha = if (surfaceColors.effectiveDark) 0.10f else 0.06f)
                            )
                        )
                    )
            )
        }

        // Bottom readability scrim
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (surfaceColors.effectiveDark) {
                            listOf(
                                Color(0xFF020617).copy(alpha = 0.14f),
                                Color(0xFF020617).copy(alpha = 0.56f),
                                Color(0xFF020617).copy(alpha = 0.78f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.52f),
                                Color.White.copy(alpha = 0.78f)
                            )
                        }
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Showcase Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (surfaceColors.effectiveDark) cardAccent.copy(alpha = 0.28f)
                            else cardAccent.copy(alpha = 0.18f)
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = note.category,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (totalPages > 1) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(cardAccent.copy(alpha = 0.24f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isArabic) "$totalPages ص" else "${totalPages}p",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = cardAccent
                            )
                        }
                    }
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    if (note.audioDurationSec > 0 || note.audioFilePath.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Audio",
                            tint = Color(0xFFEC4899),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Center-Bottom Showcase Typography
            Column(modifier = Modifier.fillMaxWidth()) {
                val displayTitle = note.title.ifBlank {
                    if (checklistItems.isNotEmpty()) {
                        checklistItems.firstOrNull()?.text.orEmpty()
                    } else {
                        note.content.lineSequence().firstOrNull().orEmpty()
                    }
                }.ifBlank { if (isArabic) "مسودة بدون عنوان" else "Untitled Draft" }

                Text(
                    text = displayTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleColor,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val snippet = remember(note.content, note.pagesJson, checklistItems) {
                    when {
                        note.content.isNotBlank() -> note.content.trim()
                        checklistItems.isNotEmpty() -> checklistItems.joinToString(" • ") { it.text }
                        else -> ""
                    }
                }
                if (snippet.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = snippet,
                        fontSize = 12.sp,
                        color = bodyColor,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFriendlyDate(note.updatedAt, isArabic),
                        fontSize = 10.5.sp,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        CompactCardIcon(
                            icon = if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (note.isFavorite) Color(0xFFF43F5E) else metaColor.copy(alpha = 0.72f),
                            onClick = onToggleFavorite
                        )
                        CompactCardIcon(
                            icon = Icons.Default.Shield,
                            contentDescription = tr("نقل إلى الـ Vault", "Déplacer vers Vault", "Move to Vault", "Smutti ɣer Vault"),
                            tint = Color(0xFFC084FC).copy(alpha = 0.88f),
                            onClick = onMoveToVault
                        )
                        CompactCardIcon(
                            icon = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (note.isArchived) "Unarchive" else "Archive",
                            tint = if (note.isArchived) Color(0xFF38BDF8) else metaColor.copy(alpha = 0.72f),
                            onClick = onArchive
                        )
                        CompactCardIcon(
                            icon = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                            onClick = onDelete
                        )
                    }
                }
            }
        }
    }
}

/**
 * Magazine View card: editorial publication aesthetic.
 * [isLeadFeature] renders a wide cover story banner at top; subsequent items render split editorial cards.
 */
@Composable
fun MagazineNoteCard(
    note: NoteEntity,
    issueIndex: Int,
    isLeadFeature: Boolean,
    isDark: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArchive: () -> Unit = {},
    onMoveToVault: () -> Unit = {},
    onDelete: () -> Unit,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val glassTint = GlassTint.fromId(note.colorPreset)
    val bgPreset = remember(note.backgroundId) { NoteBackgroundCatalog.findById(note.backgroundId) }
    val customFillColor = remember(note.customColorArgb, isDark) {
        if (note.customColorArgb != 0L) {
            Color(note.customColorArgb.toInt()).copy(alpha = if (isDark) 0.90f else 0.96f)
        } else null
    }
    val surfaceColors = remember(isDark, glassTint, note.customColorArgb, note.backgroundId) {
        NoteBackgroundCatalog.resolveSurfaceColors(
            themeIsDark = isDark,
            tint = glassTint,
            customColorArgb = note.customColorArgb,
            backgroundId = note.backgroundId
        )
    }
    val checklistItems = remember(note.checklistJson) { ChecklistItem.jsonToList(note.checklistJson) }
    val pages = remember(note.content, note.pagesJson) { NotePagesCodec.decodePages(note.content, note.pagesJson) }
    val totalPages = pages.size
    val totalWords = remember(pages) {
        pages.sumOf { p -> if (p.isBlank()) 0 else p.trim().split("\\s+".toRegex()).size }
    }
    val cardAccent = surfaceColors.accentColor
    val titleColor = surfaceColors.titleColor
    val bodyColor = surfaceColors.bodyColor
    val metaColor = surfaceColors.metaColor

    LiquidGlassCard(
        isDark = surfaceColors.effectiveDark,
        tint = glassTint,
        shape = RoundedCornerShape(24.dp),
        borderWidth = when {
            isDragging -> 2.8.dp
            note.isPinned -> 2.2.dp
            else -> 1.4.dp
        },
        customFill = if (bgPreset != null) Color.Transparent else customFillColor,
        onClick = onClick,
        testTag = "magazine_note_card_${note.id}",
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLeadFeature) {
            // Featured Hero Issue Layout
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                ) {
                    if (bgPreset != null) {
                        NoteBackgroundCanvas(
                            preset = bgPreset,
                            isCompactCard = true,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            cardAccent.copy(alpha = if (surfaceColors.effectiveDark) 0.42f else 0.28f),
                                            cardAccent.copy(alpha = if (surfaceColors.effectiveDark) 0.14f else 0.10f)
                                        )
                                    )
                                )
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.34f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isArabic) "عدد مميز • ${note.category}" else "FEATURED • ${note.category.uppercase()}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 0.6.sp
                            )
                        }
                        if (totalPages > 1) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.34f))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "$totalPages صفحات" else "$totalPages PAGES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = note.title.ifBlank { if (isArabic) "مقال رئيسي بدون عنوان" else "Untitled Editorial" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = titleColor,
                        lineHeight = 26.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (note.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = note.content,
                            fontSize = 14.sp,
                            color = bodyColor,
                            lineHeight = 20.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildString {
                                append(formatFriendlyDate(note.updatedAt, isArabic))
                                if (totalWords > 0) {
                                    append(" • ")
                                    append(if (isArabic) "$totalWords كلمة" else "$totalWords words")
                                }
                            },
                            fontSize = 11.5.sp,
                            color = metaColor,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            CompactCardIcon(
                                icon = Icons.Default.PushPin,
                                contentDescription = "Pin",
                                tint = if (note.isPinned) Color(0xFFFBBF24) else metaColor.copy(alpha = 0.65f),
                                onClick = onTogglePin
                            )
                            CompactCardIcon(
                                icon = if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (note.isFavorite) Color(0xFFF43F5E) else metaColor.copy(alpha = 0.65f),
                                onClick = onToggleFavorite
                            )
                            CompactCardIcon(
                                icon = Icons.Default.Shield,
                                contentDescription = tr("نقل إلى الـ Vault", "Déplacer vers Vault", "Move to Vault", "Smutti ɣer Vault"),
                                tint = Color(0xFFC084FC).copy(alpha = 0.88f),
                                onClick = onMoveToVault
                            )
                            CompactCardIcon(
                                icon = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (note.isArchived) "Unarchive" else "Archive",
                                tint = if (note.isArchived) Color(0xFF38BDF8) else metaColor.copy(alpha = 0.65f),
                                onClick = onArchive
                            )
                            CompactCardIcon(
                                icon = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444).copy(alpha = 0.82f),
                                onClick = onDelete
                            )
                        }
                    }
                }
            }
        } else {
            // Split Editorial Row Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 124.dp)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Editorial Visual Cover Thumbnail
                Box(
                    modifier = Modifier
                        .width(86.dp)
                        .height(112.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    if (bgPreset != null) {
                        NoteBackgroundCanvas(
                            preset = bgPreset,
                            isCompactCard = true,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            cardAccent.copy(alpha = if (surfaceColors.effectiveDark) 0.45f else 0.30f),
                                            cardAccent.copy(alpha = if (surfaceColors.effectiveDark) 0.18f else 0.12f)
                                        )
                                    )
                                )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = String.format(Locale.US, "%02d", issueIndex + 1),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (bgPreset != null) Color.White else cardAccent
                        )
                        if (totalPages > 1) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.32f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "$totalPages ص" else "${totalPages}p",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Editorial Text Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = note.category.uppercase(),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = cardAccent,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (note.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = note.title.ifBlank { if (isArabic) "بدون عنوان" else "Untitled" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        lineHeight = 21.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val excerpt = remember(note.content, checklistItems) {
                        when {
                            note.content.isNotBlank() -> note.content
                            checklistItems.isNotEmpty() -> checklistItems.joinToString(" • ") { it.text }
                            else -> ""
                        }
                    }
                    if (excerpt.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = excerpt,
                            fontSize = 12.5.sp,
                            color = bodyColor,
                            lineHeight = 17.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildString {
                                append(formatFriendlyDate(note.updatedAt, isArabic))
                                if (totalWords > 0) {
                                    append(" • ")
                                    append(if (isArabic) "$totalWords ك" else "${totalWords}w")
                                }
                            },
                            fontSize = 11.sp,
                            color = metaColor
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            CompactCardIcon(
                                icon = if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (note.isFavorite) Color(0xFFF43F5E) else metaColor.copy(alpha = 0.65f),
                                onClick = onToggleFavorite
                            )
                            CompactCardIcon(
                                icon = Icons.Default.Shield,
                                contentDescription = tr("نقل إلى الـ Vault", "Déplacer vers Vault", "Move to Vault", "Smutti ɣer Vault"),
                                tint = Color(0xFFC084FC).copy(alpha = 0.88f),
                                onClick = onMoveToVault
                            )
                            CompactCardIcon(
                                icon = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (note.isArchived) "Unarchive" else "Archive",
                                tint = if (note.isArchived) Color(0xFF38BDF8) else metaColor.copy(alpha = 0.65f),
                                onClick = onArchive
                            )
                            CompactCardIcon(
                                icon = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444).copy(alpha = 0.82f),
                                onClick = onDelete
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCardIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
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
