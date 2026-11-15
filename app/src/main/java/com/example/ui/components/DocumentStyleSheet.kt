package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlassTint
import com.example.data.model.NoteBackgroundCanvas
import com.example.data.model.NoteBackgroundCatalog
import com.example.data.model.NoteBackgroundPreset
import com.example.ui.theme.LocalAppThemePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentStyleBottomSheet(
    isDark: Boolean,
    isArabic: Boolean,
    selectedTint: GlassTint,
    customColorArgb: Long,
    backgroundId: String,
    category: String,
    initialTab: Int = 0,
    onSelectTint: (GlassTint) -> Unit,
    onSelectCustomColor: (Long) -> Unit,
    onSelectBackground: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onResetStyle: () -> Unit,
    onDismiss: () -> Unit
) {
    val activePreset = LocalAppThemePreset.current
    val effectiveDark = activePreset.isDark || isDark
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    val sheetSurface = if (effectiveDark) activePreset.surface else Color(0xFFF8FAFC)
    val textColor = if (effectiveDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val metaColor = if (effectiveDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (effectiveDark) Color(0x22FFFFFF) else Color(0x0D0F172A)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetSurface,
        contentColor = textColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(activePreset.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (activeTab == 1) Icons.Default.Wallpaper else Icons.Default.Palette,
                            contentDescription = null,
                            tint = activePreset.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isArabic) "تلوين وخلفيات المستند" else "Document Colors & Backgrounds",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = if (isArabic) "لون خاص أو اختر من 20 خلفية" else "Custom color or 20 backgrounds",
                            fontSize = 11.5.sp,
                            color = metaColor
                        )
                    }
                }

                if (customColorArgb != 0L || backgroundId != NoteBackgroundCatalog.NONE_ID || selectedTint != GlassTint.FROST) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(cardBg)
                            .clickable { onResetStyle() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isArabic) "إعادة للافتراضي" else "Reset",
                            tint = activePreset.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isArabic) "افتراضي" else "Reset",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = activePreset.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2-Tab Switcher: Colors | 20 Backgrounds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StyleTabPill(
                    label = if (isArabic) "ألوان المستند" else "Document Colors",
                    selected = activeTab == 0,
                    accent = activePreset.primary,
                    textColor = textColor,
                    metaColor = metaColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("style_tab_colors"),
                    onClick = { activeTab = 0 }
                )
                StyleTabPill(
                    label = if (isArabic) "الخلفيات (20)" else "Backgrounds (20)",
                    selected = activeTab == 1,
                    accent = activePreset.primary,
                    textColor = textColor,
                    metaColor = metaColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("style_tab_backgrounds"),
                    onClick = { activeTab = 1 }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (activeTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section 1: Individual Solid/Pastel/Deep Document Color (16 swatches + Default)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isArabic) "لون صفحة المستند" else "Page Color",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        val rows = NoteBackgroundCatalog.documentColors.chunked(4)
                        // Default pill button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isDefaultColor = customColorArgb == 0L && backgroundId == NoteBackgroundCatalog.NONE_ID
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isDefaultColor) activePreset.primary.copy(alpha = 0.22f) else cardBg
                                    )
                                    .border(
                                        width = if (isDefaultColor) 1.5.dp else 0.75.dp,
                                        color = if (isDefaultColor) activePreset.primary else metaColor.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectCustomColor(0L) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = if (isDefaultColor) activePreset.primary else metaColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isArabic) "بدون لون (ثيم التطبيق)" else "Theme Default",
                                    fontSize = 12.sp,
                                    fontWeight = if (isDefaultColor) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isDefaultColor) textColor else metaColor
                                )
                            }
                        }

                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { colorOption ->
                                    val swatchColor = Color(colorOption.argb.toInt())
                                    val isSelected = customColorArgb == colorOption.argb
                                    val checkTint = if (swatchColor.luminance() < 0.45f) Color.White else Color(0xFF0F172A)

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onSelectCustomColor(colorOption.argb) }
                                            .padding(vertical = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .testTag("doc_color_${colorOption.id}")
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(swatchColor)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) activePreset.primary else metaColor.copy(alpha = 0.28f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = checkTint,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isArabic) colorOption.nameAr else colorOption.nameEn,
                                            fontSize = 10.sp,
                                            color = if (isSelected) activePreset.primary else metaColor,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Glass Accent Tint
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "توهج الإطار" else "Glass Accent",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassTint.entries.forEach { tint ->
                                val isSelected = selectedTint == tint
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("tint_${tint.id}")
                                        .clip(CircleShape)
                                        .background(tint.accentColor)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) textColor else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { onSelectTint(tint) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: Category Chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isArabic) "التصنيف" else "Category",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        val categories = if (isArabic) {
                            listOf("أفكار", "مهام", "عمل", "شخصي", "دراسة", "عام")
                        } else {
                            listOf("Ideas", "Todo", "Work", "Personal", "Study", "General")
                        }
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
                                            if (isSelected) activePreset.primary.copy(alpha = 0.25f) else cardBg
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.75.dp,
                                            color = if (isSelected) activePreset.primary else metaColor.copy(alpha = 0.25f),
                                            shape = CircleShape
                                        )
                                        .clickable { onSelectCategory(cat) }
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
            } else {
                // Tab 2: 20 Artistic Backgrounds Grid (+ None option)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp, max = 440.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "none_bg") {
                        val isSelected = backgroundId == NoteBackgroundCatalog.NONE_ID || backgroundId.isBlank()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectBackground(NoteBackgroundCatalog.NONE_ID) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.85f)
                                    .testTag("doc_bg_none")
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(cardBg)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) activePreset.primary else metaColor.copy(alpha = 0.28f),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = if (isArabic) "بدون خلفية" else "No background",
                                    tint = if (isSelected) activePreset.primary else metaColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = if (isArabic) "بدون خلفية" else "None",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) activePreset.primary else metaColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    items(NoteBackgroundCatalog.all, key = { it.id }) { preset ->
                        BackgroundGridTile(
                            preset = preset,
                            isSelected = backgroundId == preset.id,
                            isArabic = isArabic,
                            activeBorderColor = activePreset.primary,
                            metaColor = metaColor,
                            onClick = { onSelectBackground(preset.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleTabPill(
    label: String,
    selected: Boolean,
    accent: Color,
    textColor: Color,
    metaColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) accent.copy(alpha = 0.6f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) textColor else metaColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BackgroundGridTile(
    preset: NoteBackgroundPreset,
    isSelected: Boolean,
    isArabic: Boolean,
    activeBorderColor: Color,
    metaColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f)
                .testTag("doc_bg_${preset.id}")
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) activeBorderColor else metaColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            NoteBackgroundCanvas(
                preset = preset,
                isCompactCard = true,
                modifier = Modifier.fillMaxSize()
            )

            // Sample miniature note lines to preview readability
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val sampleLineColor = if (preset.isDarkSurface) Color.White else Color(0xFF0F172A)
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(sampleLineColor.copy(alpha = 0.72f))
                )
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(sampleLineColor.copy(alpha = 0.45f))
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(activeBorderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Text(
            text = if (isArabic) preset.nameAr else preset.nameEn,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeBorderColor else metaColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
