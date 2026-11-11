package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
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
    val canScrollLeft by remember { derivedStateOf { scrollState.value > 6 } }
    val canScrollRight by remember { derivedStateOf { scrollState.value < (scrollState.maxValue - 6) } }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val width = size.width
                    val height = size.height
                    if (width > 0f && height > 0f) {
                        val leftFadePx = if (canScrollLeft) 28.dp.toPx() else 0.01f
                        val rightFadePx = if (canScrollRight) 52.dp.toPx() else 0.01f
                        val leftRatio = (leftFadePx / width).coerceIn(0f, 0.4f)
                        val rightRatio = (1f - (rightFadePx / width)).coerceIn(0.6f, 1f)

                        val colorStops = if (canScrollLeft && canScrollRight) {
                            arrayOf(
                                0.0f to Color.Transparent,
                                leftRatio to Color.Black,
                                rightRatio to Color.Black,
                                1.0f to Color.Transparent
                            )
                        } else if (canScrollRight) {
                            arrayOf(
                                0.0f to Color.Black,
                                rightRatio to Color.Black,
                                1.0f to Color.Transparent
                            )
                        } else if (canScrollLeft) {
                            arrayOf(
                                0.0f to Color.Transparent,
                                leftRatio to Color.Black,
                                1.0f to Color.Black
                            )
                        } else {
                            arrayOf(
                                0.0f to Color.Black,
                                1.0f to Color.Black
                            )
                        }

                        drawRect(
                            brush = Brush.horizontalGradient(colorStops = colorStops),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp),
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
                    testTag = "category_chip_${option.id}",
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = label,
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

        // Frosted mist overlay on the right edge
        if (canScrollRight) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(44.dp)
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                if (isDark) Color(0x400F172A) else Color(0x40FFFFFF),
                                if (isDark) Color(0xCC0F172A) else Color(0xD9FFFFFF)
                            )
                        )
                    )
            )
        }

        // Frosted mist overlay on the left edge when scrolled
        if (canScrollLeft) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(28.dp)
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                if (isDark) Color(0xCC0F172A) else Color(0xD9FFFFFF),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}
