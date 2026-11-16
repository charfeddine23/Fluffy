package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlassTint
import com.example.ui.glass.FrostedBackdrop
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.glass.liquidGlass
import com.example.ui.theme.AppThemeGroup
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.AppThemeRegistry
import com.example.ui.theme.LocalAppThemePreset

private enum class ThemeFilterTab {
    ALL,
    LIQUID_GLASS,
    CLASSIC
}

@Composable
fun ThemesScreen(
    selectedThemeId: String,
    isDark: Boolean,
    isArabic: Boolean,
    onSelectTheme: (AppThemePreset) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val activePreset = LocalAppThemePreset.current
    val textColor = activePreset.onBackground
    val metaColor = activePreset.onSurfaceVariant

    var searchQuery by remember { mutableStateOf("") }
    var filterTab by remember { mutableStateOf(ThemeFilterTab.ALL) }

    val filteredThemes = remember(searchQuery, filterTab) {
        val base = when (filterTab) {
            ThemeFilterTab.ALL -> AppThemeRegistry.allThemes
            ThemeFilterTab.LIQUID_GLASS -> AppThemeRegistry.allThemes.filter { it.group == AppThemeGroup.LIQUID_GLASS }
            ThemeFilterTab.CLASSIC -> AppThemeRegistry.allThemes.filter { it.group == AppThemeGroup.CLASSIC }
        }
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) base else base.filter { it.name.lowercase().contains(q) }
    }

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
                        .testTag("themes_back_button")
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Themes",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                    )
                    Text(
                        text = if (isArabic) {
                            "${AppThemeRegistry.allThemes.size} ثيم • الحالي: ${activePreset.name}"
                        } else {
                            "${AppThemeRegistry.allThemes.size} themes • Active: ${activePreset.name}"
                        },
                        fontSize = 11.5.sp,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .liquidGlass(
                            isDark = isDark,
                            tint = GlassTint.CYAN,
                            shape = RoundedCornerShape(14.dp),
                            borderWidth = 1.2.dp,
                            customFill = activePreset.primary.copy(alpha = 0.22f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = activePreset.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Search Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .liquidGlass(
                        isDark = isDark,
                        tint = GlassTint.FROST,
                        shape = RoundedCornerShape(18.dp),
                        borderWidth = 1.dp
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = metaColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (isArabic) "ابحث عن ثيم (مثل Dracula, Monokai, Glass...)" else "Search themes (Dracula, Monokai, Glass...)",
                                fontSize = 13.sp,
                                color = metaColor
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            cursorBrush = SolidColor(activePreset.primary),
                            textStyle = TextStyle(
                                color = textColor,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("themes_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = metaColor,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeTabPill(
                    label = if (isArabic) "الكل (${AppThemeRegistry.allThemes.size})" else "All (${AppThemeRegistry.allThemes.size})",
                    selected = filterTab == ThemeFilterTab.ALL,
                    accent = activePreset.primary,
                    isDark = isDark,
                    onClick = { filterTab = ThemeFilterTab.ALL }
                )
                ThemeTabPill(
                    label = if (isArabic) "تصميم المحررات (40)" else "Classic Editor (40)",
                    selected = filterTab == ThemeFilterTab.CLASSIC,
                    accent = activePreset.secondary,
                    isDark = isDark,
                    onClick = { filterTab = ThemeFilterTab.CLASSIC }
                )
                ThemeTabPill(
                    label = if (isArabic) "الزجاج السائل (20)" else "Liquid Glass (20)",
                    selected = filterTab == ThemeFilterTab.LIQUID_GLASS,
                    accent = activePreset.tertiary,
                    isDark = isDark,
                    onClick = { filterTab = ThemeFilterTab.LIQUID_GLASS }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Themes Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("themes_grid"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 28.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredThemes, key = { it.id }) { preset ->
                    val isSelected = preset.id == selectedThemeId
                    ThemePreviewCard(
                        preset = preset,
                        isSelected = isSelected,
                        isDarkApp = isDark,
                        isArabic = isArabic,
                        onClick = { onSelectTheme(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeTabPill(
    label: String,
    selected: Boolean,
    accent: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val activePreset = LocalAppThemePreset.current
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) accent.copy(alpha = 0.22f)
                else activePreset.surface.copy(alpha = if (isDark) 0.55f else 0.72f)
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else activePreset.onSurfaceVariant.copy(alpha = 0.25f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else activePreset.onSurface
        )
    }
}

@Composable
private fun ThemePreviewCard(
    preset: AppThemePreset,
    isSelected: Boolean,
    isDarkApp: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        isDark = isDarkApp,
        tint = GlassTint.FROST,
        shape = RoundedCornerShape(20.dp),
        borderWidth = if (isSelected) 2.4.dp else 1.1.dp,
        customFill = if (isSelected) preset.surface.copy(alpha = 0.96f) else null,
        onClick = onClick,
        testTag = "theme_card_${preset.id}",
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live Miniature Theme Canvas Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                preset.background,
                                preset.surface,
                                preset.background
                            )
                        )
                    )
                    .border(
                        width = if (preset.isGlassStyle) 1.2.dp else 0.8.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                preset.primary.copy(alpha = 0.7f),
                                preset.secondary.copy(alpha = 0.7f),
                                preset.tertiary.copy(alpha = 0.7f)
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(9.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(preset.secondary)
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(preset.tertiary)
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(preset.primary)
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = preset.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Simulated Note / Syntax Bars inside preview
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(preset.primary.copy(alpha = 0.9f))
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.38f)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(preset.secondary.copy(alpha = 0.85f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(preset.tertiary.copy(alpha = 0.8f))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.50f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(preset.onBackground.copy(alpha = 0.35f))
                        )
                    }
                }
            }

            // Theme Name & Polarity/Type Metadata
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = preset.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalAppThemePreset.current.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildString {
                            append(
                                if (preset.isGlassStyle) {
                                    if (isArabic) "زجاج سائل" else "Liquid Glass"
                                } else {
                                    if (isArabic) "كلاسيكي" else "Classic"
                                }
                            )
                            append(" • ")
                            append(
                                if (preset.isDark) {
                                    if (isArabic) "داكن" else "Dark"
                                } else {
                                    if (isArabic) "فاتح" else "Light"
                                }
                            )
                        },
                        fontSize = 10.sp,
                        color = LocalAppThemePreset.current.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf(preset.primary, preset.secondary, preset.tertiary).forEach { dot ->
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(dot)
                            )
                        }
                    }
                }
            }
        }
    }
}
