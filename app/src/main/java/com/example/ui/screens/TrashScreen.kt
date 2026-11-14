package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlassTint
import com.example.data.model.NoteEntity
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import com.example.ui.theme.LocalAppThemePreset
import com.example.viewmodel.FluffyViewModel
import kotlin.math.ceil

@Composable
fun TrashScreen(
    trashedNotes: List<NoteEntity>,
    isDark: Boolean,
    isArabic: Boolean,
    onRestoreNote: (NoteEntity) -> Unit,
    onDeletePermanently: (NoteEntity) -> Unit,
    onEmptyTrash: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val activePreset = LocalAppThemePreset.current
    val textColor = activePreset.onBackground
    val metaColor = activePreset.onSurfaceVariant
    var showEmptyConfirm by remember { mutableStateOf(false) }

    FrostedBackdrop(isDark = isDark, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Top Bar with Back, Title, and Empty Trash action button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("trash_back_button")
                            .liquidGlass(
                                isDark = isDark,
                                tint = GlassTint.FROST,
                                shape = CircleShape,
                                borderWidth = 1.2.dp
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onBack
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back",
                            tint = textColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isArabic) "سلة المهملات" else "Trash",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor
                            )
                        )
                        Text(
                            text = if (isArabic) "${trashedNotes.size} عنصر" else "${trashedNotes.size} items",
                            fontSize = 11.5.sp,
                            color = metaColor
                        )
                    }
                }

                if (trashedNotes.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .testTag("empty_trash_button")
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.20f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true)
                            ) { showEmptyConfirm = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = if (isArabic) "تفريغ السلة" else "Empty Trash",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isArabic) "تفريغ السلة" else "Empty Trash",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }

            // 30-day automatic deletion policy banner
            LiquidGlassCard(
                isDark = isDark,
                tint = GlassTint.FROST,
                shape = RoundedCornerShape(16.dp),
                borderWidth = 1.dp,
                customFill = activePreset.surface.copy(alpha = if (isDark) 0.65f else 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoDelete,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isArabic)
                            "تُحذف الملاحظات المحذوفة تلقائياً ونهائياً بعد مرور 30 يوماً إذا لم يتم تفريغ السلة يدوياً."
                        else
                            "Items in Trash are automatically permanently deleted after 30 days.",
                        fontSize = 11.5.sp,
                        color = metaColor,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (trashedNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = metaColor.copy(alpha = 0.55f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (isArabic) "سلة المهملات فارغة" else "Trash is empty",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = if (isArabic)
                                "أي ملاحظة تقوم بحذفها ستظهر هنا لمدة 30 يوماً قبل المسح النهائي."
                            else
                                "Deleted notes stay here for 30 days before permanent removal.",
                            fontSize = 12.5.sp,
                            color = metaColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("trash_notes_list"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trashedNotes, key = { it.id }) { note ->
                        val now = System.currentTimeMillis()
                        val remainingMs = ((note.deletedAt + FluffyViewModel.TRASH_RETENTION_MILLIS) - now)
                            .coerceAtLeast(0L)
                        val daysLeft = ceil(remainingMs.toDouble() / (24.0 * 60.0 * 60.0 * 1000.0))
                            .toInt()
                            .coerceIn(0, 30)

                        LiquidGlassCard(
                            isDark = isDark,
                            tint = GlassTint.fromId(note.colorPreset),
                            shape = RoundedCornerShape(20.dp),
                            borderWidth = 1.2.dp,
                            testTag = "trash_note_card_${note.id}",
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note.category,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activePreset.primary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFFF59E0B).copy(alpha = 0.18f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (isArabic) "متبقي $daysLeft يوم" else "${daysLeft}d left",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF59E0B)
                                        )
                                    }
                                }

                                if (note.title.isNotBlank()) {
                                    Text(
                                        text = note.title,
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (note.content.isNotBlank()) {
                                    Text(
                                        text = note.content,
                                        fontSize = 13.sp,
                                        color = metaColor,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(activePreset.primary.copy(alpha = 0.16f))
                                            .clickable { onRestoreNote(note) }
                                            .padding(vertical = 9.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RestoreFromTrash,
                                            contentDescription = null,
                                            tint = activePreset.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isArabic) "استعادة" else "Restore",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activePreset.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                            .clickable { onDeletePermanently(note) }
                                            .padding(vertical = 9.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isArabic) "حذف نهائي" else "Delete",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showEmptyConfirm) {
            AlertDialog(
                onDismissRequest = { showEmptyConfirm = false },
                containerColor = activePreset.surface,
                title = {
                    Text(
                        text = if (isArabic) "تفريغ سلة المهملات؟" else "Empty Trash?",
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                },
                text = {
                    Text(
                        text = if (isArabic)
                            "سيتم حذف جميع الملاحظات في سلة المهملات (${trashedNotes.size}) نهائياً ولا يمكن التراجع عن ذلك."
                        else
                            "All ${trashedNotes.size} notes in Trash will be permanently deleted.",
                        color = metaColor
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showEmptyConfirm = false
                            onEmptyTrash()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Text(
                            text = if (isArabic) "تفريغ نهائي" else "Empty Trash",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyConfirm = false }) {
                        Text(
                            text = if (isArabic) "إلغاء" else "Cancel",
                            color = activePreset.primary
                        )
                    }
                }
            )
        }
    }
}
