package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.model.NoteBackgroundPreset
import com.example.data.model.NoteEntity
import com.example.data.model.NotePagesCodec
import com.example.data.model.NoteSurfaceColors
import com.example.data.model.VaultItemEntity
import com.example.data.storage.AudioCloudPolicy
import com.example.data.storage.LocalDeviceStorageManager
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.theme.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ResolvedVaultVisuals(
    val glassTint: GlassTint,
    val bgPreset: NoteBackgroundPreset?,
    val customFillColor: Color?,
    val surfaceColors: NoteSurfaceColors,
    val checklistItems: List<ChecklistItem>,
    val totalPages: Int,
    val totalWords: Int,
    val isAudio: Boolean,
    val audioDurationSec: Int,
    val isLocalOnlyAudio: Boolean,
    val isIntegrityValid: Boolean,
    val categoryLabel: String,
    val displayTitle: String,
    val displayBody: String,
    val isPinned: Boolean
)

@Composable
private fun rememberVaultVisuals(
    item: VaultItemEntity,
    metaNote: NoteEntity?,
    isDark: Boolean,
    isArabic: Boolean
): ResolvedVaultVisuals {
    val isAudio = remember(item.mimeType, item.fileName, metaNote?.audioDurationSec) {
        AudioCloudPolicy.isAudioFile(item.mimeType, item.fileName) ||
            ((metaNote?.audioDurationSec ?: 0) > 0)
    }
    val glassTint = remember(metaNote?.colorPreset, isAudio, item.fileName) {
        when {
            metaNote != null -> GlassTint.fromId(metaNote.colorPreset)
            isAudio -> GlassTint.ROSE
            item.fileName.isNotEmpty() -> GlassTint.CYAN
            else -> GlassTint.AMETHYST
        }
    }
    val bgPreset = remember(metaNote?.backgroundId) {
        metaNote?.backgroundId?.let { NoteBackgroundCatalog.findById(it) }
    }
    val customArgb = metaNote?.customColorArgb ?: 0L
    val customFillColor = remember(customArgb, isDark) {
        if (customArgb != 0L) {
            Color(customArgb.toInt()).copy(alpha = if (isDark) 0.90f else 0.96f)
        } else null
    }
    val surfaceColors = remember(isDark, glassTint, customArgb, bgPreset?.id) {
        NoteBackgroundCatalog.resolveSurfaceColors(
            themeIsDark = isDark,
            tint = glassTint,
            customColorArgb = customArgb,
            backgroundId = bgPreset?.id ?: NoteBackgroundCatalog.NONE_ID
        )
    }
    val checklistItems = remember(metaNote?.checklistJson) {
        metaNote?.let { ChecklistItem.jsonToList(it.checklistJson) }.orEmpty()
    }
    val pages = remember(metaNote?.content, metaNote?.pagesJson, item.literalContent) {
        if (metaNote != null) {
            NotePagesCodec.decodePages(metaNote.content, metaNote.pagesJson)
        } else {
            listOf(item.literalContent)
        }
    }
    val totalPages = pages.size
    val totalWords = remember(pages) {
        NotePagesCodec.countWords(pages)
    }
    val effectiveAudioBytes = remember(item.byteSize, item.localFilePath) {
        AudioCloudPolicy.effectiveAudioByteSize(item.byteSize, item.localFilePath)
    }
    val isLocalOnlyAudio = isAudio && AudioCloudPolicy.isAudioOverCloudLimit(effectiveAudioBytes)
    val isIntegrityValid = remember(item.id, item.sha256, item.localFilePath) {
        LocalDeviceStorageManager.verifyVaultItemIntegrity(item)
    }
    val categoryLabel = metaNote?.category?.takeIf { it.isNotBlank() } ?: when {
        isAudio -> if (isArabic) "تسجيل صوتي" else "Audio"
        item.fileName.isNotEmpty() -> if (isArabic) "ملف محمي" else "File"
        else -> "Vault"
    }
    val displayTitle = item.title.ifBlank {
        item.fileName.ifBlank {
            if (isArabic) "عنصر Vault" else "Vault Note"
        }
    }
    val rawDisplayBody = metaNote?.content?.takeIf { it.isNotBlank() } ?: item.literalContent
    val displayBody = remember(rawDisplayBody) {
        NotePagesCodec.plainTextSnippet(rawDisplayBody)
    }

    return ResolvedVaultVisuals(
        glassTint = glassTint,
        bgPreset = bgPreset,
        customFillColor = customFillColor,
        surfaceColors = surfaceColors,
        checklistItems = checklistItems,
        totalPages = totalPages,
        totalWords = totalWords,
        isAudio = isAudio,
        audioDurationSec = metaNote?.audioDurationSec ?: 0,
        isLocalOnlyAudio = isLocalOnlyAudio,
        isIntegrityValid = isIntegrityValid,
        categoryLabel = categoryLabel,
        displayTitle = displayTitle,
        displayBody = displayBody,
        isPinned = metaNote?.isPinned == true
    )
}

@Composable
fun VaultNoteStyleCard(
    item: VaultItemEntity,
    metaNote: NoteEntity?,
    isDark: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onMoveToNotesClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = rememberVaultVisuals(item, metaNote, isDark, isArabic)
    val surfaceColors = visuals.surfaceColors
    val titleColor = surfaceColors.titleColor
    val bodyColor = surfaceColors.bodyColor
    val metaColor = surfaceColors.metaColor
    val cardAccent = surfaceColors.accentColor

    LiquidGlassCard(
        isDark = surfaceColors.effectiveDark,
        tint = visuals.glassTint,
        shape = RoundedCornerShape(22.dp),
        borderWidth = if (visuals.isPinned) 2.2.dp else 1.4.dp,
        customFill = if (visuals.bgPreset != null) Color.Transparent else visuals.customFillColor,
        onClick = onClick,
        testTag = "vault_card_${item.id}",
        modifier = modifier.fillMaxWidth()
    ) {
        if (visuals.bgPreset != null) {
            NoteBackgroundCanvas(
                preset = visuals.bgPreset,
                isCompactCard = true,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (visuals.bgPreset.isDarkSurface) Color(0xFF020617).copy(alpha = 0.24f)
                        else Color.White.copy(alpha = 0.22f)
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (surfaceColors.effectiveDark) cardAccent.copy(alpha = 0.26f)
                            else cardAccent.copy(alpha = 0.16f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = cardAccent,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = visuals.categoryLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (visuals.totalPages > 1) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (surfaceColors.effectiveDark) cardAccent.copy(alpha = 0.22f)
                                    else cardAccent.copy(alpha = 0.14f)
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = cardAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isArabic) "${visuals.totalPages} ص" else "${visuals.totalPages}p",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = cardAccent
                                )
                            }
                        }
                    }

                    if (visuals.isPinned) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFBBF24).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    if (visuals.isAudio) {
                        val badgeColor = if (visuals.isLocalOnlyAudio) Color(0xFFF59E0B) else Color(0xFFEC4899)
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (visuals.isLocalOnlyAudio) Icons.Default.CloudOff else Icons.Default.GraphicEq,
                                    contentDescription = "Audio",
                                    tint = badgeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                if (visuals.audioDurationSec > 0) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = formatVaultDuration(visuals.audioDurationSec),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = visuals.displayTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.5.sp,
                    color = titleColor,
                    lineHeight = 22.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (visuals.displayBody.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = visuals.displayBody,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        color = bodyColor,
                        lineHeight = 19.sp
                    ),
                    maxLines = if (visuals.checklistItems.isNotEmpty()) 2 else 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (visuals.checklistItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val doneCount = visuals.checklistItems.count { it.isDone }
                val progress = doneCount.toFloat() / visuals.checklistItems.size.toFloat()
                Text(
                    text = if (isArabic) "$doneCount من ${visuals.checklistItems.size} مكتمل" else "$doneCount of ${visuals.checklistItems.size} done",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = metaColor
                )
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
                Spacer(modifier = Modifier.height(6.dp))
                visuals.checklistItems.take(3).forEach { check ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (check.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (check.isDone) cardAccent else metaColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = check.text,
                            fontSize = 12.sp,
                            color = if (check.isDone) metaColor else bodyColor,
                            textDecoration = if (check.isDone) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (item.fileName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF38BDF8).copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (visuals.isAudio) Icons.Default.GraphicEq else Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = if (visuals.isAudio) Color(0xFFEC4899) else Color(0xFF38BDF8),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${item.fileName} • ${formatVaultBytes(item.byteSize)}",
                        fontSize = 11.sp,
                        color = if (visuals.isAudio) Color(0xFFEC4899) else Color(0xFF38BDF8),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Date + Integrity + Move Back / Export / Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = formatVaultDate(item.createdAt, isArabic),
                        fontSize = 10.5.sp,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (visuals.isIntegrityValid) {
                            if (isArabic) "محفوظ حرفياً ✓" else "Immutable ✓"
                        } else {
                            if (isArabic) "نسخة محلية" else "Local Copy"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                VaultFooterActions(
                    itemId = item.id,
                    isAudio = visuals.isAudio,
                    onEditClick = onEditClick,
                    onMoveToNotesClick = onMoveToNotesClick,
                    onExportClick = onExportClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    }
}

@Composable
fun VaultGalleryCard(
    item: VaultItemEntity,
    metaNote: NoteEntity?,
    isDark: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onMoveToNotesClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = rememberVaultVisuals(item, metaNote, isDark, isArabic)
    val surfaceColors = visuals.surfaceColors
    val cardAccent = surfaceColors.accentColor
    val titleColor = surfaceColors.titleColor
    val bodyColor = surfaceColors.bodyColor
    val metaColor = surfaceColors.metaColor

    LiquidGlassCard(
        isDark = surfaceColors.effectiveDark,
        tint = visuals.glassTint,
        shape = RoundedCornerShape(24.dp),
        borderWidth = if (visuals.isPinned) 2.2.dp else 1.4.dp,
        customFill = if (visuals.bgPreset != null) Color.Transparent else visuals.customFillColor,
        onClick = onClick,
        testTag = "vault_gallery_card_${item.id}",
        modifier = modifier
            .fillMaxWidth()
            .height(216.dp)
    ) {
        if (visuals.bgPreset != null) {
            NoteBackgroundCanvas(
                preset = visuals.bgPreset,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (surfaceColors.effectiveDark) cardAccent.copy(alpha = 0.28f)
                            else cardAccent.copy(alpha = 0.18f)
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = cardAccent,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = visuals.categoryLabel,
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
                    if (visuals.totalPages > 1) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(cardAccent.copy(alpha = 0.24f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isArabic) "${visuals.totalPages} ص" else "${visuals.totalPages}p",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = cardAccent
                            )
                        }
                    }
                    if (visuals.isAudio) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Audio",
                            tint = Color(0xFFEC4899),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = visuals.displayTitle,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleColor,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val snippet = remember(visuals.displayBody, visuals.checklistItems, item.fileName) {
                    when {
                        visuals.displayBody.isNotBlank() -> visuals.displayBody.trim()
                        visuals.checklistItems.isNotEmpty() -> visuals.checklistItems.joinToString(" • ") { it.text }
                        item.fileName.isNotBlank() -> "${item.fileName} • ${formatVaultBytes(item.byteSize)}"
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
                        text = formatVaultDate(item.createdAt, isArabic),
                        fontSize = 10.sp,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    )

                    VaultFooterActions(
                        itemId = item.id,
                        isAudio = visuals.isAudio,
                        onEditClick = onEditClick,
                        onMoveToNotesClick = onMoveToNotesClick,
                        onExportClick = onExportClick,
                        onDeleteClick = onDeleteClick
                    )
                }
            }
        }
    }
}

@Composable
fun VaultMagazineCard(
    item: VaultItemEntity,
    metaNote: NoteEntity?,
    issueIndex: Int,
    isLeadFeature: Boolean,
    isDark: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onMoveToNotesClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = rememberVaultVisuals(item, metaNote, isDark, isArabic)
    val surfaceColors = visuals.surfaceColors
    val cardAccent = surfaceColors.accentColor
    val titleColor = surfaceColors.titleColor
    val bodyColor = surfaceColors.bodyColor
    val metaColor = surfaceColors.metaColor

    LiquidGlassCard(
        isDark = surfaceColors.effectiveDark,
        tint = visuals.glassTint,
        shape = RoundedCornerShape(24.dp),
        borderWidth = if (visuals.isPinned) 2.2.dp else 1.4.dp,
        customFill = if (visuals.bgPreset != null) Color.Transparent else visuals.customFillColor,
        onClick = onClick,
        testTag = "vault_magazine_card_${item.id}",
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLeadFeature) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                ) {
                    if (visuals.bgPreset != null) {
                        NoteBackgroundCanvas(
                            preset = visuals.bgPreset,
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
                                text = if (isArabic) "محفوظة • ${visuals.categoryLabel}" else "VAULT • ${visuals.categoryLabel.uppercase()}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 0.6.sp
                            )
                        }

                        if (visuals.totalPages > 1) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.34f))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "${visuals.totalPages} صفحات" else "${visuals.totalPages} PAGES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else if (visuals.isAudio) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFEC4899).copy(alpha = 0.85f))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "تسجيل صوتي" else "AUDIO",
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
                        text = visuals.displayTitle,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = titleColor,
                        lineHeight = 25.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val bodyText = visuals.displayBody.ifBlank {
                        if (item.fileName.isNotEmpty()) "${item.fileName} • ${formatVaultBytes(item.byteSize)}" else ""
                    }
                    if (bodyText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = bodyText,
                            fontSize = 13.5.sp,
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
                                append(formatVaultDate(item.createdAt, isArabic))
                                if (visuals.totalWords > 0) {
                                    append(" • ")
                                    append(if (isArabic) "${visuals.totalWords} كلمة" else "${visuals.totalWords} words")
                                }
                            },
                            fontSize = 11.sp,
                            color = metaColor,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        )

                        VaultFooterActions(
                            itemId = item.id,
                            isAudio = visuals.isAudio,
                            onEditClick = onEditClick,
                            onMoveToNotesClick = onMoveToNotesClick,
                            onExportClick = onExportClick,
                            onDeleteClick = onDeleteClick
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 124.dp)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(86.dp)
                        .height(112.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    if (visuals.bgPreset != null) {
                        NoteBackgroundCanvas(
                            preset = visuals.bgPreset,
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
                            color = if (visuals.bgPreset != null) Color.White else cardAccent
                        )
                        if (visuals.totalPages > 1) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.32f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "${visuals.totalPages} ص" else "${visuals.totalPages}p",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else if (visuals.isAudio) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Audio",
                                tint = Color(0xFFEC4899),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

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
                            text = visuals.categoryLabel.uppercase(),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = cardAccent,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (visuals.isPinned) {
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
                        text = visuals.displayTitle,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        lineHeight = 20.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val excerpt = remember(visuals.displayBody, visuals.checklistItems, item.fileName) {
                        when {
                            visuals.displayBody.isNotBlank() -> visuals.displayBody
                            visuals.checklistItems.isNotEmpty() -> visuals.checklistItems.joinToString(" • ") { it.text }
                            item.fileName.isNotEmpty() -> "${item.fileName} • ${formatVaultBytes(item.byteSize)}"
                            else -> ""
                        }
                    }
                    if (excerpt.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = excerpt,
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
                            text = formatVaultDate(item.createdAt, isArabic),
                            fontSize = 10.5.sp,
                            color = metaColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        )

                        VaultFooterActions(
                            itemId = item.id,
                            isAudio = visuals.isAudio,
                            onEditClick = onEditClick,
                            onMoveToNotesClick = onMoveToNotesClick,
                            onExportClick = onExportClick,
                            onDeleteClick = onDeleteClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultFooterActions(
    itemId: String,
    isAudio: Boolean,
    onEditClick: (() -> Unit)? = null,
    onMoveToNotesClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (onEditClick != null) {
            CompactVaultActionIcon(
                icon = Icons.Default.Edit,
                contentDescription = tr("تعديل في الـ Vault", "Modifier", "Edit in Vault", "Ẓreg"),
                tint = Color(0xFFC084FC),
                testTag = "vault_edit_item_$itemId",
                onClick = onEditClick
            )
        }
        CompactVaultActionIcon(
            icon = Icons.Default.LockOpen,
            contentDescription = if (isAudio) {
                tr("إخراج إلى التسجيلات", "Déplacer vers Enregistrements", "Move to Recordings", "Smutti ɣer ikelsen")
            } else {
                tr("إخراج إلى المسودات", "Déplacer vers Notes", "Move to Notes", "Smutti ɣer tazmilt")
            },
            tint = Color(0xFF10B981),
            testTag = "vault_move_to_notes_$itemId",
            onClick = onMoveToNotesClick
        )
        CompactVaultActionIcon(
            icon = Icons.Default.Download,
            contentDescription = tr("تصدير إلى الهاتف", "Exporter", "Export", "Sifeḍ"),
            tint = Color(0xFF38BDF8),
            testTag = "vault_export_item_$itemId",
            onClick = onExportClick
        )
        CompactVaultActionIcon(
            icon = Icons.Default.DeleteOutline,
            contentDescription = tr("حذف من الـ Vault", "Supprimer du Vault", "Delete from Vault", "Kkes seg Vault"),
            tint = Color(0xFFF43F5E),
            testTag = "vault_delete_item_$itemId",
            onClick = onDeleteClick
        )
    }
}

@Composable
private fun CompactVaultActionIcon(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .testTag(testTag)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.13f))
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

private fun formatVaultDate(timestamp: Long, isArabic: Boolean): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> if (isArabic) "الآن" else "Just now"
        diff < 3_600_000 -> {
            val mins = diff / 60_000
            if (isArabic) "منذ $mins د" else "${mins}m ago"
        }
        diff < 86_400_000 -> {
            val hours = diff / 3_600_000
            if (isArabic) "منذ $hours س" else "${hours}h ago"
        }
        else -> SimpleDateFormat("dd MMM", if (isArabic) Locale("ar") else Locale.ENGLISH).format(Date(timestamp))
    }
}

private fun formatVaultBytes(byteSize: Long): String {
    val sizeKb = byteSize / 1024f
    return if (sizeKb >= 1024f) {
        String.format(Locale.US, "%.2f MB", sizeKb / 1024f)
    } else {
        String.format(Locale.US, "%.1f KB", sizeKb.coerceAtLeast(0.1f))
    }
}

private fun formatVaultDuration(sec: Int): String {
    val mins = sec / 60
    val rem = sec % 60
    return String.format(Locale.US, "%d:%02d", mins, rem)
}
