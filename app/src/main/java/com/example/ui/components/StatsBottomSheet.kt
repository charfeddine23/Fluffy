package com.example.ui.components

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChecklistItem
import com.example.data.model.GlassTint
import com.example.data.model.NoteEntity
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsBottomSheet(
    notes: List<NoteEntity>,
    isDark: Boolean,
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val totalNotes = notes.size
    val pinnedCount = notes.count { it.isPinned }
    val favCount = notes.count { it.isFavorite }
    val voiceCount = notes.count { it.audioDurationSec > 0 }

    var totalTasks = 0
    var doneTasks = 0
    notes.forEach { note ->
        val items = ChecklistItem.jsonToList(note.checklistJson)
        totalTasks += items.size
        doneTasks += items.count { it.isDone }
    }

    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF0A0F1D) else Color(0xFFFFFFFF),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isArabic) "إحصائيات Fluffy 📊" else "Fluffy Insights 📊",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                    Text(
                        text = if (isArabic) "نظرة سريعة على ملاحظاتك وإنتاجيتك" else "Overview of your notes and productivity",
                        style = MaterialTheme.typography.bodySmall.copy(color = metaColor)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = metaColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2x2 Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = if (isArabic) "إجمالي الملاحظات" else "Total Notes",
                    value = "$totalNotes",
                    icon = Icons.Default.Notes,
                    tint = GlassTint.FROST,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = if (isArabic) "الملاحظات المثبتة" else "Pinned",
                    value = "$pinnedCount",
                    icon = Icons.Default.PushPin,
                    tint = GlassTint.AMBER,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = if (isArabic) "المفضلة" else "Favorites",
                    value = "$favCount",
                    icon = Icons.Default.Favorite,
                    tint = GlassTint.ROSE,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = if (isArabic) "المهام المنجزة" else "Done Tasks",
                    value = "$doneTasks / $totalTasks",
                    icon = Icons.Default.CheckCircle,
                    tint = GlassTint.CYAN,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Voice Notes Banner
            LiquidGlassCard(
                isDark = isDark,
                tint = GlassTint.EMERALD,
                shape = RoundedCornerShape(20.dp),
                borderWidth = 1.2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GlassTint.EMERALD.accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = GlassTint.EMERALD.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isArabic) "تسجيلات صوتية مدمجة" else "Voice Memos",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = if (isArabic) "$voiceCount تسجيل صوتي محفوظ" else "$voiceCount recorded memos",
                                fontSize = 12.sp,
                                color = metaColor
                            )
                        }
                    }

                    Text(
                        text = "$voiceCount",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GlassTint.EMERALD.accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: GlassTint,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val metaColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    LiquidGlassCard(
        isDark = isDark,
        tint = tint,
        shape = RoundedCornerShape(20.dp),
        borderWidth = 1.2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint.accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            Text(
                text = title,
                fontSize = 11.5.sp,
                color = metaColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
