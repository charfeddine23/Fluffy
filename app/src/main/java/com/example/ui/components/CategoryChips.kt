package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
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
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl || isArabic

    // Physical overflow detection respecting RTL (Arabic) vs LTR (English)
    val canScrollPhysicalLeft by remember(isRtl) {
        derivedStateOf {
            if (isRtl) scrollState.value < (scrollState.maxValue - 2) else scrollState.value > 2
        }
    }
    val canScrollPhysicalRight by remember(isRtl) {
        derivedStateOf {
            if (isRtl) scrollState.value > 2 else scrollState.value < (scrollState.maxValue - 2)
        }
    }

    val leftFogStrength by animateFloatAsState(
        targetValue = if (canScrollPhysicalLeft) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "left_fog_strength"
    )
    val rightFogStrength by animateFloatAsState(
        targetValue = if (canScrollPhysicalRight) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "right_fog_strength"
    )

    // Primary fog edge is Right in English (LTR) and Left in Arabic (RTL)
    val leftMaxFogDp = if (isRtl) 96.dp else 44.dp
    val rightMaxFogDp = if (isRtl) 44.dp else 96.dp

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Layer 1 (API 31+): Progressive optical frosted blur layer inside the fog transition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (leftFogStrength > 0.01f || rightFogStrength > 0.01f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val width = size.width
                        if (width > 0f && size.height > 0f) {
                            val leftFrac = ((leftMaxFogDp.toPx() * leftFogStrength) / width).coerceIn(0f, 0.45f)
                            val rightFrac = ((rightMaxFogDp.toPx() * rightFogStrength) / width).coerceIn(0f, 0.45f)
                            val rightStart = (1f - rightFrac).coerceAtLeast(leftFrac + 0.01f)

                            val stops = buildList {
                                if (leftFrac > 0.005f) {
                                    add(0.0f to Color.Transparent)
                                    add((leftFrac * 0.16f) to Color.Black.copy(alpha = 0.14f))
                                    add((leftFrac * 0.42f) to Color.Black.copy(alpha = 0.65f))
                                    add((leftFrac * 0.68f) to Color.Black.copy(alpha = 0.88f))
                                    add((leftFrac * 0.92f) to Color.Black.copy(alpha = 0.25f))
                                    add(leftFrac to Color.Transparent)
                                } else {
                                    add(0.0f to Color.Transparent)
                                }

                                if (rightFrac > 0.005f) {
                                    add(rightStart to Color.Transparent)
                                    add((rightStart + rightFrac * 0.08f) to Color.Black.copy(alpha = 0.25f))
                                    add((rightStart + rightFrac * 0.32f) to Color.Black.copy(alpha = 0.88f))
                                    add((rightStart + rightFrac * 0.58f) to Color.Black.copy(alpha = 0.65f))
                                    add((rightStart + rightFrac * 0.84f) to Color.Black.copy(alpha = 0.14f))
                                    add(1.0f to Color.Transparent)
                                } else {
                                    add(1.0f to Color.Transparent)
                                }
                            }.toTypedArray()

                            drawRect(
                                brush = Brush.horizontalGradient(colorStops = stops),
                                blendMode = BlendMode.DstIn
                            )
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .blur(
                            radiusX = 12.dp,
                            radiusY = 8.dp,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                        )
                ) {
                    CategoryChipsRow(
                        isDark = isDark,
                        isArabic = isArabic,
                        selectedCategory = selectedCategory,
                        onSelectCategory = {},
                        includeTestTags = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    constraints.copy(maxWidth = Constraints.Infinity)
                                )
                                val viewportWidth = constraints.maxWidth
                                layout(viewportWidth, placeable.height) {
                                    placeable.placeRelative(-scrollState.value, 0)
                                }
                            }
                    )
                }
            }
        }

        // Layer 2: Interactive sharp chips with smooth multi-stop cubic fade into total transparency
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val width = size.width
                    if (width > 0f && size.height > 0f) {
                        val leftFrac = ((leftMaxFogDp.toPx() * leftFogStrength) / width).coerceIn(0f, 0.45f)
                        val rightFrac = ((rightMaxFogDp.toPx() * rightFogStrength) / width).coerceIn(0f, 0.45f)
                        val rightStart = (1f - rightFrac).coerceAtLeast(leftFrac + 0.01f)

                        val stops = buildList {
                            if (leftFrac > 0.005f) {
                                add(0.0f to Color.Transparent)
                                add((leftFrac * 0.22f) to Color.Black.copy(alpha = 0.05f))
                                add((leftFrac * 0.46f) to Color.Black.copy(alpha = 0.25f))
                                add((leftFrac * 0.72f) to Color.Black.copy(alpha = 0.65f))
                                add(leftFrac to Color.Black)
                            } else {
                                add(0.0f to Color.Black)
                            }

                            if (rightFrac > 0.005f) {
                                add(rightStart to Color.Black)
                                add((rightStart + rightFrac * 0.28f) to Color.Black.copy(alpha = 0.65f))
                                add((rightStart + rightFrac * 0.54f) to Color.Black.copy(alpha = 0.25f))
                                add((rightStart + rightFrac * 0.78f) to Color.Black.copy(alpha = 0.05f))
                                add(1.0f to Color.Transparent)
                            } else {
                                add(1.0f to Color.Black)
                            }
                        }.toTypedArray()

                        drawRect(
                            brush = Brush.horizontalGradient(colorStops = stops),
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

        // Layer 3: Subtle airy radial frosted mist aura at the primary fog edge (no hard edges)
        if (leftFogStrength > 0.01f || rightFogStrength > 0.01f) {
            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                val mistCoreColor = if (isDark) Color(0x2894A3B8) else Color(0x45FFFFFF)
                val mistMidColor = if (isDark) Color(0x1438BDF8) else Color(0x22E0F2FE)

                if (rightFogStrength > 0.01f) {
                    val radius = rightMaxFogDp.toPx() * rightFogStrength
                    if (radius > 1f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    mistCoreColor.copy(alpha = mistCoreColor.alpha * rightFogStrength),
                                    mistMidColor.copy(alpha = mistMidColor.alpha * rightFogStrength),
                                    Color.Transparent
                                ),
                                center = Offset(size.width, size.height / 2f),
                                radius = radius
                            ),
                            radius = radius,
                            center = Offset(size.width, size.height / 2f)
                        )
                    }
                }

                if (leftFogStrength > 0.01f) {
                    val radius = leftMaxFogDp.toPx() * leftFogStrength
                    if (radius > 1f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    mistCoreColor.copy(alpha = mistCoreColor.alpha * leftFogStrength),
                                    mistMidColor.copy(alpha = mistMidColor.alpha * leftFogStrength),
                                    Color.Transparent
                                ),
                                center = Offset(0f, size.height / 2f),
                                radius = radius
                            ),
                            radius = radius,
                            center = Offset(0f, size.height / 2f)
                        )
                    }
                }
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
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp),
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
