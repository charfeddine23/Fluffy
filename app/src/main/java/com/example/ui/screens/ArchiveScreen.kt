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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlassTint
import com.example.data.model.NoteBackgroundCanvas
import com.example.data.model.NoteBackgroundCatalog
import com.example.data.model.NoteEntity
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.darkLiquidGlass
import com.example.ui.glass.liquidGlass
import com.example.ui.theme.LocalAppThemePreset
import com.example.ui.theme.tr

@Composable
fun ArchiveScreen(
    archivedNotes: List<NoteEntity>,
    isDark: Boolean,
    isArabic: Boolean,
    onOpenNote: (NoteEntity) -> Unit,
    onUnarchiveNote: (NoteEntity) -> Unit,
    onMoveToTrash: (NoteEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val activePreset = LocalAppThemePreset.current
    val textColor = activePreset.onBackground
    val metaColor = activePreset.onSurfaceVariant

    FrostedBackdrop(isDark = isDark, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("archive_back_button")
                        .darkLiquidGlass(
                            shape = CircleShape,
                            elevation = 10.dp,
                            rimWidth = 1.25.dp,
                            baseAlpha = 0.50f
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("رجوع", "Retour", "Back", "Uɣal"),
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("الأرشيف", "Archives", "Archive", "Iɣbaren"),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                    )
                    Text(
                        text = tr(
                            "${archivedNotes.size} ملاحظة مؤرشفة",
                            "${archivedNotes.size} notes archivées",
                            "${archivedNotes.size} archived notes",
                            "${archivedNotes.size} n tira yettwaɣebren"
                        ),
                        fontSize = 11.5.sp,
                        color = metaColor
                    )
                }
            }

            if (archivedNotes.isEmpty()) {
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
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = metaColor.copy(alpha = 0.55f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = tr("الأرشيف فارغ", "Les archives sont vides", "Archive is empty", "Iɣbaren d ilem"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = tr(
                                "يمكنك أرشفة أي ملاحظة لإخفائها عن الرئيسية مع الاحتفاظ بها هنا.",
                                "Archivez des notes pour les masquer de l'accueil tout en les gardant en sécurité ici.",
                                "Archive notes to tuck them away from Home while keeping them safe here.",
                                "Tzemreḍ ad tɣebreḍ tira i wakken ad tent-teffreḍ seg usebter agejdan."
                            ),
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
                        .testTag("archive_notes_list"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(archivedNotes, key = { it.id }) { note ->
                        val cardTint = GlassTint.fromId(note.colorPreset)
                        val cardBg = NoteBackgroundCatalog.findById(note.backgroundId)
                        val cardFill = if (note.customColorArgb != 0L) {
                            Color(note.customColorArgb.toInt()).copy(alpha = if (isDark) 0.90f else 0.95f)
                        } else null
                        val cardSurface = NoteBackgroundCatalog.resolveSurfaceColors(
                            themeIsDark = isDark,
                            tint = cardTint,
                            customColorArgb = note.customColorArgb,
                            backgroundId = note.backgroundId
                        )
                        LiquidGlassCard(
                            isDark = cardSurface.effectiveDark,
                            tint = cardTint,
                            shape = RoundedCornerShape(20.dp),
                            borderWidth = 1.2.dp,
                            customFill = if (cardBg != null) Color.Transparent else cardFill,
                            onClick = { onOpenNote(note) },
                            testTag = "archive_note_card_${note.id}",
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (cardBg != null) {
                                NoteBackgroundCanvas(
                                    preset = cardBg,
                                    isCompactCard = true,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = note.category,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = cardSurface.accentColor
                                )

                                if (note.title.isNotBlank()) {
                                    Text(
                                        text = note.title,
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cardSurface.titleColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (note.content.isNotBlank()) {
                                    Text(
                                        text = note.content,
                                        fontSize = 13.sp,
                                        color = cardSurface.metaColor,
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
                                            .clickable { onUnarchiveNote(note) }
                                            .padding(vertical = 9.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Unarchive,
                                            contentDescription = null,
                                            tint = activePreset.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isArabic) "إلغاء الأرشفة" else "Unarchive",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activePreset.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFEF4444).copy(alpha = 0.14f))
                                            .clickable { onMoveToTrash(note) }
                                            .padding(vertical = 9.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isArabic) "حذف" else "Trash",
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
    }
}
