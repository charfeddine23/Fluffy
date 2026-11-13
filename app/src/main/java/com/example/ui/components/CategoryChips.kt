package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlassTint
import com.example.ui.glass.LiquidGlassPill

data class CategoryOption(
    val id: String,
    val labelAr: String,
    val labelEn: String,
    val icon: ImageVector,
    val tint: GlassTint
)

val defaultCategories = listOf(
    CategoryOption("ALL", "الكل", "All", Icons.Default.WorkspacePremium, GlassTint.FROST),
    CategoryOption("PINNED", "المثبتة", "Pinned", Icons.Default.PushPin, GlassTint.AMBER),
    CategoryOption("FAVORITES", "المفضلة", "Favorites", Icons.Default.Favorite, GlassTint.ROSE),
    CategoryOption("TODO", "المهام", "Checklists", Icons.Default.CheckCircle, GlassTint.CYAN),
    CategoryOption("أفكار", "أفكار", "Ideas", Icons.Default.Lightbulb, GlassTint.AMETHYST),
    CategoryOption("عمل", "عمل", "Work", Icons.Default.Work, GlassTint.CYAN),
    CategoryOption("شخصي", "شخصي", "Personal", Icons.Default.Person, GlassTint.EMERALD)
)

@Composable
fun CategoryChips(
    isDark: Boolean,
    isArabic: Boolean,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    // Very small, completely static fog zone at both left and right edges (never moves or resizes)
    val edgeFogDp = 18.dp

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Static micro edge fade mask so scrolling items vanish gently at the 18.dp left & right edges
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val width = size.width
                    if (width > 0f && size.height > 0f) {
                        val edgeFrac = (edgeFogDp.toPx() / width).coerceIn(0.01f, 0.15f)
                        val rightStart = 1f - edgeFrac

                        drawRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    (edgeFrac * 0.35f) to Color.Black.copy(alpha = 0.25f),
                                    (edgeFrac * 0.72f) to Color.Black.copy(alpha = 0.72f),
                                    edgeFrac to Color.Black,
                                    rightStart to Color.Black,
                                    (rightStart + edgeFrac * 0.28f) to Color.Black.copy(alpha = 0.72f),
                                    (rightStart + edgeFrac * 0.65f) to Color.Black.copy(alpha = 0.25f),
                                    1.0f to Color.Transparent
                                )
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
        ) {
            CategoryChipsRow(
                isDark = isDark,
                isArabic = isArabic,
                selectedCategory = selectedCategory,
                onSelectCategory = onSelectCategory,
                includeTestTags = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            )
        }

        // Tiny, static frosted edge mist anchored at the far left and right edges
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val radius = edgeFogDp.toPx()
            val mistCoreColor = if (isDark) Color(0x1E94A3B8) else Color(0x2EFFFFFF)
            val mistMidColor = if (isDark) Color(0x0E38BDF8) else Color(0x18E0F2FE)

            if (radius > 1f) {
                // Static left edge mist
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(mistCoreColor, mistMidColor, Color.Transparent),
                        center = Offset(0f, size.height / 2f),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(0f, size.height / 2f)
                )

                // Static right edge mist
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(mistCoreColor, mistMidColor, Color.Transparent),
                        center = Offset(size.width, size.height / 2f),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(size.width, size.height / 2f)
                )
            }
        }
    }
}

@Composable
private fun CategoryChipsRow(
    isDark: Boolean,
    isArabic: Boolean,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    includeTestTags: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        defaultCategories.forEach { option ->
            val isSelected = selectedCategory.equals(option.id, ignoreCase = true)
            val label = if (isArabic) option.labelAr else option.labelEn
            val textColor = if (isSelected) {
                if (isDark) Color.White else Color(0xFF0F172A)
            } else {
                if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
            }

            LiquidGlassPill(
                isDark = isDark,
                isSelected = isSelected,
                tint = option.tint,
                onClick = { onSelectCategory(option.id) },
                testTag = if (includeTestTags) "category_chip_${option.id}" else "category_chip_blur_${option.id}",
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = if (includeTestTags) label else null,
                        tint = if (isSelected) option.tint.accentColor else textColor,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}
