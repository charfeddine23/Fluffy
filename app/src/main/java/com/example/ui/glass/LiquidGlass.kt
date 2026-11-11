package com.example.ui.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.GlassTint
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated dynamic frosted backdrop with floating ambient aura orbs that pulse and move smoothly.
 * Creates the authentic "Frosted / Blurred Backdrop" through which liquid glass elements refract light.
 */
@Composable
fun FrostedBackdrop(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "backdrop_animation")

    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb1_phase"
    )

    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb2_phase"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Palette tokens for backdrop
    val baseBackground = if (isDark) Color(0xFF070A12) else Color(0xFFF6F8FC)
    val orbColor1 = if (isDark) Color(0xFF0EA5E9).copy(alpha = 0.32f) else Color(0xFF38BDF8).copy(alpha = 0.40f)
    val orbColor2 = if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.30f) else Color(0xFFC084FC).copy(alpha = 0.36f)
    val orbColor3 = if (isDark) Color(0xFFEC4899).copy(alpha = 0.26f) else Color(0xFFF472B6).copy(alpha = 0.32f)
    val orbColor4 = if (isDark) Color(0xFF312E81).copy(alpha = 0.45f) else Color(0xFFFBBF24).copy(alpha = 0.28f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Orb 1: Upper-left flowing cyan
            val x1 = width * 0.25f + cos(t1) * (width * 0.18f)
            val y1 = height * 0.20f + sin(t1) * (height * 0.12f)
            val r1 = (width * 0.55f) * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orbColor1, orbColor1.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(x1, y1),
                    radius = r1
                ),
                center = Offset(x1, y1),
                radius = r1
            )

            // Orb 2: Center-right glowing violet
            val x2 = width * 0.80f + sin(t2) * (width * 0.15f)
            val y2 = height * 0.45f + cos(t2) * (height * 0.16f)
            val r2 = (width * 0.60f) * (2f - pulse)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orbColor2, orbColor2.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(x2, y2),
                    radius = r2
                ),
                center = Offset(x2, y2),
                radius = r2
            )

            // Orb 3: Lower-left magenta
            val x3 = width * 0.30f - cos(t2) * (width * 0.16f)
            val y3 = height * 0.82f - sin(t2) * (height * 0.10f)
            val r3 = (width * 0.52f) * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orbColor3, orbColor3.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(x3, y3),
                    radius = r3
                ),
                center = Offset(x3, y3),
                radius = r3
            )

            // Orb 4: Top-right deep ambient amber / indigo
            val x4 = width * 0.65f + cos(t1 * 0.7f) * (width * 0.12f)
            val y4 = height * 0.10f + sin(t1 * 0.7f) * (height * 0.08f)
            val r4 = (width * 0.48f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orbColor4, orbColor4.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(x4, y4),
                    radius = r4
                ),
                center = Offset(x4, y4),
                radius = r4
            )

            // Subtle Frosted noise / sheen overlay to diffuse the lights
            val sheenBrush = Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(Color(0x18FFFFFF), Color(0x04000000), Color(0x10FFFFFF))
                } else {
                    listOf(Color(0x35FFFFFF), Color(0x10FFFFFF), Color(0x25FFFFFF))
                }
            )
            drawRect(brush = sheenBrush)
        }

        content()
    }
}

/**
 * Custom Modifier for Liquid Glass:
 * 1. Translucent frosted glass fill with diagonal specular gradient.
 * 2. Chromatic Dispersion Border: Refractive iridescent rim that separates light like an optical prism.
 * 3. Inner Specular Highlight & Inner Shadow: Light bevel along top/left, ambient occlusion along bottom/right.
 */
fun Modifier.liquidGlass(
    isDark: Boolean,
    tint: GlassTint = GlassTint.FROST,
    shape: Shape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.5.dp,
    customFill: Color? = null
): Modifier = this
    .clip(shape)
    .drawBehind {
        val width = size.width
        val height = size.height

        // 1. Frosted Liquid Fill
        val baseFill = customFill ?: if (isDark) tint.darkFill else tint.lightFill
        val specularSheen = if (customFill != null) {
            val a = customFill.alpha
            Brush.linearGradient(
                colors = listOf(
                    customFill.copy(alpha = (a * 1.08f).coerceIn(0.01f, 1f)),
                    customFill,
                    customFill.copy(alpha = (a * 0.92f).coerceIn(0.01f, 1f))
                ),
                start = Offset(0f, 0f),
                end = Offset(width, height)
            )
        } else if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    baseFill.copy(alpha = (baseFill.alpha * 1.25f).coerceAtMost(0.95f)),
                    baseFill,
                    baseFill.copy(alpha = (baseFill.alpha * 0.85f).coerceAtLeast(0.15f))
                ),
                start = Offset(0f, 0f),
                end = Offset(width, height)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xF0FFFFFF).copy(alpha = 0.88f),
                    baseFill,
                    Color(0xD0FFFFFF).copy(alpha = 0.72f)
                ),
                start = Offset(0f, 0f),
                end = Offset(width, height)
            )
        }
        drawRect(brush = specularSheen)

        // 2. Inner Specular Highlight (Top/Left inner rim)
        val highlightColor = if (isDark) Color(0x60FFFFFF) else Color(0x99FFFFFF)
        val shadowColor = if (isDark) Color(0x40000000) else Color(0x18000000)

        // Top-left bevel highlight
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(highlightColor, highlightColor.copy(alpha = 0.1f))
            ),
            start = Offset(12f, 2f),
            end = Offset(width - 12f, 2f),
            strokeWidth = 3f
        )
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(highlightColor, highlightColor.copy(alpha = 0.1f))
            ),
            start = Offset(2f, 12f),
            end = Offset(2f, height - 12f),
            strokeWidth = 3f
        )

        // Bottom-right inner ambient shadow
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(shadowColor.copy(alpha = 0.05f), shadowColor)
            ),
            start = Offset(12f, height - 2f),
            end = Offset(width - 12f, height - 2f),
            strokeWidth = 2.5f
        )
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(shadowColor.copy(alpha = 0.05f), shadowColor)
            ),
            start = Offset(width - 2f, 12f),
            end = Offset(width - 2f, height - 12f),
            strokeWidth = 2.5f
        )
    }
    // 3. Chromatic Dispersion Border (Prism Refraction Edge)
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                tint.chromaticLeading.copy(alpha = if (isDark) 0.85f else 0.75f),
                Color(0xFF38BDF8).copy(alpha = if (isDark) 0.75f else 0.65f),
                Color(0xFFC084FC).copy(alpha = if (isDark) 0.85f else 0.75f),
                Color(0xFFF472B6).copy(alpha = if (isDark) 0.80f else 0.70f),
                Color(0xFFFDE047).copy(alpha = if (isDark) 0.60f else 0.50f),
                tint.chromaticTrailing.copy(alpha = if (isDark) 0.75f else 0.65f)
            ),
            start = Offset(0f, 0f),
            end = Offset(1200f, 1200f)
        ),
        shape = shape
    )

/**
 * Liquid Glass Card wrapper for notes, dialogs, and panels.
 */
@Composable
fun LiquidGlassCard(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    tint: GlassTint = GlassTint.FROST,
    shape: Shape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.5.dp,
    customFill: Color? = null,
    onClick: (() -> Unit)? = null,
    testTag: String = "liquid_glass_card",
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = tint.accentColor),
                onClick = onClick
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .liquidGlass(
                isDark = isDark,
                tint = tint,
                shape = shape,
                borderWidth = borderWidth,
                customFill = customFill
            )
            .then(clickModifier),
        content = content
    )
}

/**
 * Liquid Glass Pill for tags, action buttons, and filters.
 */
@Composable
fun LiquidGlassPill(
    isDark: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: GlassTint = GlassTint.FROST,
    testTag: String = "liquid_glass_pill",
    content: @Composable BoxScope.() -> Unit
) {
    val activeTint = if (isSelected) tint else GlassTint.FROST
    val shape = CircleShape
    val activeBorderWidth = if (isSelected) 2.dp else 1.dp

    Box(
        modifier = modifier
            .testTag(testTag)
            .liquidGlass(
                isDark = isDark,
                tint = activeTint,
                shape = shape,
                borderWidth = activeBorderWidth,
                customFill = if (isSelected) {
                    if (isDark) tint.accentColor.copy(alpha = 0.35f) else tint.accentColor.copy(alpha = 0.22f)
                } else null
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = tint.accentColor),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
