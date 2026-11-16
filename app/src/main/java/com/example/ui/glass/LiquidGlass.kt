package com.example.ui.glass

import android.util.TypedValue
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.GlassTint
import com.example.ui.theme.LocalAppThemePreset
import kotlin.math.cos
import kotlin.math.sin

val LocalColoredContainerBorders = staticCompositionLocalOf { true }

/**
 * Resolves the exact 2mm physical screen dimension into Dp using DisplayMetrics,
 * with a canonical 160dpi-density fallback (~12.6.dp) if xdpi is irregular.
 */
@Composable
fun rememberTwoMillimetersDp(): Dp {
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(context, density) {
        val metrics = context.resources.displayMetrics
        val mmPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 2f, metrics)
        val xdpiValid = metrics.xdpi in (metrics.densityDpi * 0.5f)..(metrics.densityDpi * 2.0f)
        val resolvedPx = if (xdpiValid && mmPx.isFinite() && mmPx > 0f) {
            mmPx
        } else {
            2f * metrics.densityDpi / 25.4f
        }
        with(density) { resolvedPx.coerceAtLeast(1f).toDp() }
    }
}

/**
 * Feathers content alpha gently across the 2mm left and right edges so items reaching
 * the screen borders dissolve smoothly into the blurry frosted edges.
 */
fun Modifier.blurryEdgeMask(edgeWidthDp: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val width = size.width
        val height = size.height
        val edgePx = edgeWidthDp.toPx()
        if (width > edgePx * 2f && height > 0f && edgePx > 0f) {
            val frac = (edgePx / width).coerceIn(0.005f, 0.18f)
            val rightStart = 1f - frac
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        (frac * 0.30f) to Color.Black.copy(alpha = 0.25f),
                        (frac * 0.65f) to Color.Black.copy(alpha = 0.72f),
                        frac to Color.Black,
                        rightStart to Color.Black,
                        (rightStart + frac * 0.35f) to Color.Black.copy(alpha = 0.72f),
                        (rightStart + frac * 0.70f) to Color.Black.copy(alpha = 0.25f),
                        1.0f to Color.Transparent
                    )
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

/**
 * Symmetric 2mm blurry frosted edges anchored on both physical Left and Right sides of the screen.
 * Purely visual (non-touch-intercepting) so edge swipes and scrolling pass through unaffected.
 */
@Composable
fun BlurryScreenEdges(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    edgeWidthDp: Dp = rememberTwoMillimetersDp(),
    edgeColorOverride: Color? = null
) {
    val activePreset = LocalAppThemePreset.current
    val coloredBorders = LocalColoredContainerBorders.current
    val effectiveDark = if (edgeColorOverride != null) isDark else (activePreset.isDark || isDark)
    val edgeBase = edgeColorOverride ?: activePreset.background
    val edgeSurface = edgeColorOverride ?: activePreset.surface
    val blurGlowTop = if (effectiveDark) activePreset.orb1.copy(alpha = 0.42f) else activePreset.orb1.copy(alpha = 0.28f)
    val blurGlowMid = if (effectiveDark) edgeSurface.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f)
    val blurGlowBot = if (effectiveDark) activePreset.orb2.copy(alpha = 0.38f) else activePreset.orb2.copy(alpha = 0.24f)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = modifier.fillMaxSize()) {
            // 1. Native Gaussian-blurred frosted edge strips (API 31+ RenderEffect; soft on older APIs)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(edgeWidthDp * 0.52f)
                    .blur(
                        radiusX = edgeWidthDp * 0.48f,
                        radiusY = edgeWidthDp * 0.48f,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(blurGlowTop, blurGlowMid, blurGlowBot)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(edgeWidthDp * 0.52f)
                    .blur(
                        radiusX = edgeWidthDp * 0.48f,
                        radiusY = edgeWidthDp * 0.48f,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(blurGlowBot, blurGlowMid, blurGlowTop)
                        )
                    )
            )

            // 2. Smooth multi-stop 2mm optical mist & frosted edge refraction veil on both sides
            Canvas(modifier = Modifier.fillMaxSize()) {
                val edgePx = edgeWidthDp.toPx().coerceAtLeast(1f)
                val w = size.width
                val h = size.height
                if (w <= edgePx * 2f || h <= 0f) return@Canvas

                val coreAlpha = if (effectiveDark) 0.78f else 0.80f
                val midAlpha = if (effectiveDark) 0.44f else 0.46f
                val tailAlpha = if (effectiveDark) 0.14f else 0.16f
                val hairlineColor = when {
                    !coloredBorders && effectiveDark -> Color.White.copy(alpha = 0.12f)
                    !coloredBorders -> Color(0xFF0F172A).copy(alpha = 0.10f)
                    effectiveDark -> activePreset.primary.copy(alpha = 0.16f)
                    else -> Color.White.copy(alpha = 0.42f)
                }

                // Left 2mm blurry edge
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to edgeBase.copy(alpha = coreAlpha),
                            0.32f to edgeSurface.copy(alpha = midAlpha),
                            0.68f to edgeBase.copy(alpha = tailAlpha),
                            1.00f to Color.Transparent
                        ),
                        startX = 0f,
                        endX = edgePx
                    ),
                    topLeft = Offset.Zero,
                    size = Size(edgePx, h)
                )

                // Right 2mm blurry edge
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.32f to edgeBase.copy(alpha = tailAlpha),
                            0.68f to edgeSurface.copy(alpha = midAlpha),
                            1.00f to edgeBase.copy(alpha = coreAlpha)
                        ),
                        startX = w - edgePx,
                        endX = w
                    ),
                    topLeft = Offset(w - edgePx, 0f),
                    size = Size(edgePx, h)
                )

                // Subtle 1dp glass edge specular rim on both sides
                val rimStroke = 1.dp.toPx()
                drawLine(
                    color = hairlineColor,
                    start = Offset(rimStroke * 0.5f, 0f),
                    end = Offset(rimStroke * 0.5f, h),
                    strokeWidth = rimStroke
                )
                drawLine(
                    color = hairlineColor,
                    start = Offset(w - rimStroke * 0.5f, 0f),
                    end = Offset(w - rimStroke * 0.5f, h),
                    strokeWidth = rimStroke
                )
            }
        }
    }
}

/**
 * Animated dynamic frosted backdrop with floating ambient aura orbs that pulse and move smoothly.
 * Creates the authentic "Frosted / Blurred Backdrop" through which liquid glass elements refract light.
 */
@Composable
fun FrostedBackdrop(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    showBlurryEdges: Boolean = true,
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

    val activePreset = LocalAppThemePreset.current
    val effectiveDark = activePreset.isDark

    // Dynamic palette tokens derived from the active theme preset
    val baseBackground = activePreset.background
    val orbColor1 = activePreset.orb1
    val orbColor2 = activePreset.orb2
    val orbColor3 = activePreset.orb3
    val orbColor4 = if (effectiveDark) {
        activePreset.secondary.copy(alpha = if (activePreset.isGlassStyle) 0.28f else 0.16f)
    } else {
        activePreset.tertiary.copy(alpha = if (activePreset.isGlassStyle) 0.22f else 0.12f)
    }

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

        if (showBlurryEdges) {
            BlurryScreenEdges(isDark = effectiveDark)
        }
    }
}

/**
 * Custom Modifier for Liquid Glass:
 * 1. Translucent frosted glass fill with diagonal specular gradient.
 * 2. Chromatic Dispersion Border: Refractive iridescent rim that separates light like an optical prism.
 * 3. Inner Specular Highlight & Inner Shadow: Light bevel along top/left, ambient occlusion along bottom/right.
 */
@Composable
fun Modifier.liquidGlass(
    isDark: Boolean,
    tint: GlassTint = GlassTint.FROST,
    shape: Shape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.5.dp,
    customFill: Color? = null
): Modifier {
    val activePreset = LocalAppThemePreset.current
    val coloredBorders = LocalColoredContainerBorders.current
    val effectiveDark = activePreset.isDark
    val presetFill = when {
        customFill != null -> customFill
        tint == GlassTint.FROST -> activePreset.surface.copy(
            alpha = when {
                activePreset.isGlassStyle && effectiveDark -> 0.56f
                activePreset.isGlassStyle && !effectiveDark -> 0.80f
                !activePreset.isGlassStyle && effectiveDark -> 0.88f
                else -> 0.94f
            }
        )
        else -> if (effectiveDark) tint.darkFill else tint.lightFill
    }

    val borderColors = when {
        !coloredBorders -> {
            if (effectiveDark) {
                listOf(
                    Color.White.copy(alpha = 0.24f),
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.12f)
                )
            } else {
                listOf(
                    Color(0xFF0F172A).copy(alpha = 0.14f),
                    Color(0xFF64748B).copy(alpha = 0.08f),
                    Color(0xFF0F172A).copy(alpha = 0.12f),
                    Color(0xFF64748B).copy(alpha = 0.09f)
                )
            }
        }
        tint == GlassTint.FROST -> {
            listOf(
                activePreset.primary.copy(alpha = if (effectiveDark) 0.82f else 0.70f),
                activePreset.secondary.copy(alpha = if (effectiveDark) 0.78f else 0.65f),
                activePreset.tertiary.copy(alpha = if (effectiveDark) 0.75f else 0.62f),
                activePreset.primary.copy(alpha = if (effectiveDark) 0.72f else 0.60f)
            )
        }
        else -> {
            listOf(
                tint.chromaticLeading.copy(alpha = if (effectiveDark) 0.85f else 0.75f),
                Color(0xFF38BDF8).copy(alpha = if (effectiveDark) 0.75f else 0.65f),
                Color(0xFFC084FC).copy(alpha = if (effectiveDark) 0.85f else 0.75f),
                Color(0xFFF472B6).copy(alpha = if (effectiveDark) 0.80f else 0.70f),
                Color(0xFFFDE047).copy(alpha = if (effectiveDark) 0.60f else 0.50f),
                tint.chromaticTrailing.copy(alpha = if (effectiveDark) 0.75f else 0.65f)
            )
        }
    }

    return this
        .clip(shape)
        .drawBehind {
            val width = size.width
            val height = size.height

            // 1. Frosted / Tinted Fill
            val specularSheen = if (customFill != null || tint == GlassTint.FROST) {
                val a = presetFill.alpha
                Brush.linearGradient(
                    colors = listOf(
                        presetFill.copy(alpha = (a * 1.08f).coerceIn(0.01f, 1f)),
                        presetFill,
                        presetFill.copy(alpha = (a * 0.92f).coerceIn(0.01f, 1f))
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(width, height)
                )
            } else if (effectiveDark) {
                Brush.linearGradient(
                    colors = listOf(
                        presetFill.copy(alpha = (presetFill.alpha * 1.25f).coerceAtMost(0.95f)),
                        presetFill,
                        presetFill.copy(alpha = (presetFill.alpha * 0.85f).coerceAtLeast(0.15f))
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(width, height)
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xF0FFFFFF).copy(alpha = 0.88f),
                        presetFill,
                        Color(0xD0FFFFFF).copy(alpha = 0.72f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(width, height)
                )
            }
            drawRect(brush = specularSheen)

            // 2. Inner Specular Highlight (Top/Left inner rim)
            val highlightColor = if (effectiveDark) Color(0x55FFFFFF) else Color(0x99FFFFFF)
            val shadowColor = if (effectiveDark) Color(0x40000000) else Color(0x18000000)

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
        // 3. Chromatic Dispersion Border
        .border(
            width = borderWidth,
            brush = Brush.linearGradient(
                colors = borderColors,
                start = Offset(0f, 0f),
                end = Offset(1200f, 1200f)
            ),
            shape = shape
        )
}

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
