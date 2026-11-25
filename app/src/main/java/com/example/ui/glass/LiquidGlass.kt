package com.example.ui.glass

import android.util.TypedValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.GlassTint
import com.example.ui.theme.LocalAppThemePreset

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
 * Preserved lightweight API for scrolling containers.
 * Visual edge misting is handled cleanly by [BlurryScreenEdges] without allocating
 * full-viewport offscreen FBO render targets every scroll frame.
 */
fun Modifier.blurryEdgeMask(@Suppress("UNUSED_PARAMETER") edgeWidthDp: Dp): Modifier = this

/**
 * Symmetric 2mm optical mist frosted edges anchored on both physical Left and Right sides.
 * Uses [drawWithCache] so zero Brush objects are reallocated during scroll or animation frames.
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
    val blurGlow = if (effectiveDark) activePreset.orb1.copy(alpha = 0.22f) else activePreset.orb1.copy(alpha = 0.16f)
    val hairlineColor = when {
        !coloredBorders && effectiveDark -> Color.White.copy(alpha = 0.12f)
        !coloredBorders -> Color(0xFF0F172A).copy(alpha = 0.10f)
        effectiveDark -> activePreset.primary.copy(alpha = 0.16f)
        else -> Color.White.copy(alpha = 0.42f)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawWithCache {
                    val edgePx = edgeWidthDp.toPx().coerceAtLeast(1f)
                    val w = size.width
                    val h = size.height
                    val coreAlpha = if (effectiveDark) 0.78f else 0.80f
                    val midAlpha = if (effectiveDark) 0.44f else 0.46f
                    val tailAlpha = if (effectiveDark) 0.14f else 0.16f
                    val rimStroke = 1.dp.toPx()

                    val leftGlowBrush = Brush.horizontalGradient(
                        colors = listOf(blurGlow, Color.Transparent),
                        startX = 0f,
                        endX = (edgePx * 1.15f).coerceAtLeast(1f)
                    )
                    val rightGlowBrush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, blurGlow),
                        startX = (w - edgePx * 1.15f).coerceAtLeast(0f),
                        endX = w.coerceAtLeast(1f)
                    )
                    val leftEdgeBrush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to edgeBase.copy(alpha = coreAlpha),
                            0.32f to edgeSurface.copy(alpha = midAlpha),
                            0.68f to edgeBase.copy(alpha = tailAlpha),
                            1.00f to Color.Transparent
                        ),
                        startX = 0f,
                        endX = edgePx
                    )
                    val rightEdgeBrush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.32f to edgeBase.copy(alpha = tailAlpha),
                            0.68f to edgeSurface.copy(alpha = midAlpha),
                            1.00f to edgeBase.copy(alpha = coreAlpha)
                        ),
                        startX = (w - edgePx).coerceAtLeast(0f),
                        endX = w.coerceAtLeast(1f)
                    )

                    onDrawBehind {
                        if (w <= edgePx * 2f || h <= 0f) return@onDrawBehind
                        drawRect(brush = leftGlowBrush, topLeft = Offset.Zero, size = Size(edgePx * 1.15f, h))
                        drawRect(brush = rightGlowBrush, topLeft = Offset(w - edgePx * 1.15f, 0f), size = Size(edgePx * 1.15f, h))
                        drawRect(brush = leftEdgeBrush, topLeft = Offset.Zero, size = Size(edgePx, h))
                        drawRect(brush = rightEdgeBrush, topLeft = Offset(w - edgePx, 0f), size = Size(edgePx, h))

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
        )
    }
}

/**
 * Rich multi-orb frosted ambient backdrop.
 * Uses cached GPU gradients ([drawWithCache]) instead of continuous 60-120fps per-frame redraws
 * so the UI & RenderThread stay completely responsive and idle cleanly between user interactions.
 */
@Composable
fun FrostedBackdrop(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    showBlurryEdges: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val activePreset = LocalAppThemePreset.current
    val effectiveDark = activePreset.isDark

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
            .drawWithCache {
                val width = size.width.coerceAtLeast(1f)
                val height = size.height.coerceAtLeast(1f)

                val c1 = Offset(width * 0.28f, height * 0.18f)
                val r1 = (width * 0.58f).coerceAtLeast(1f)
                val b1 = Brush.radialGradient(
                    colors = listOf(orbColor1, orbColor1.copy(alpha = 0.08f), Color.Transparent),
                    center = c1,
                    radius = r1
                )

                val c2 = Offset(width * 0.82f, height * 0.44f)
                val r2 = (width * 0.62f).coerceAtLeast(1f)
                val b2 = Brush.radialGradient(
                    colors = listOf(orbColor2, orbColor2.copy(alpha = 0.08f), Color.Transparent),
                    center = c2,
                    radius = r2
                )

                val c3 = Offset(width * 0.26f, height * 0.82f)
                val r3 = (width * 0.54f).coerceAtLeast(1f)
                val b3 = Brush.radialGradient(
                    colors = listOf(orbColor3, orbColor3.copy(alpha = 0.08f), Color.Transparent),
                    center = c3,
                    radius = r3
                )

                val c4 = Offset(width * 0.68f, height * 0.11f)
                val r4 = (width * 0.48f).coerceAtLeast(1f)
                val b4 = Brush.radialGradient(
                    colors = listOf(orbColor4, orbColor4.copy(alpha = 0.06f), Color.Transparent),
                    center = c4,
                    radius = r4
                )

                val sheenBrush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0x18FFFFFF), Color(0x04000000), Color(0x10FFFFFF))
                    } else {
                        listOf(Color(0x35FFFFFF), Color(0x10FFFFFF), Color(0x25FFFFFF))
                    }
                )

                onDrawBehind {
                    drawCircle(brush = b1, center = c1, radius = r1)
                    drawCircle(brush = b2, center = c2, radius = r2)
                    drawCircle(brush = b3, center = c3, radius = r3)
                    drawCircle(brush = b4, center = c4, radius = r4)
                    drawRect(brush = sheenBrush)
                }
            }
    ) {
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

/**
 * Authentic Dark Liquid Glass (Glassmorphism الداكن) modifier for floating circular & pill buttons:
 * - Translucent smoked obsidian glass fill so underlying card colors refract softly through
 * - Convex top-left lens dome highlight + bottom-right internal caustic bounce
 * - Crisp curved diagonal white specular rim (top-left bright crescent + bottom-right rim glow)
 * - Soft floating drop shadow for elevated spatial depth
 */
@Composable
fun Modifier.darkLiquidGlass(
    shape: Shape = CircleShape,
    tintColor: Color? = null,
    elevation: Dp = 14.dp,
    rimWidth: Dp = 1.35.dp,
    baseAlpha: Float = 0.50f,
    isPressed: Boolean = false
): Modifier {
    val safeAlpha = baseAlpha.coerceIn(0.22f, 0.85f)
    return this
        .shadow(
            elevation = if (isPressed) (elevation * 0.65f) else elevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.55f),
            spotColor = Color.Black.copy(alpha = 0.72f)
        )
        .clip(shape)
        .drawBehind {
            val w = size.width
            val h = size.height
            val maxDim = maxOf(w, h).coerceAtLeast(1f)

            // 1. Translucent smoked dark glass base allowing underlying colors to shine through
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF232936).copy(alpha = (safeAlpha * 0.84f).coerceIn(0.18f, 0.82f)),
                        Color(0xFF10141D).copy(alpha = safeAlpha),
                        Color(0xFF05070B).copy(alpha = (safeAlpha * 1.18f).coerceIn(0.26f, 0.88f))
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            // Optional delicate colored glass refraction when a button represents an active state
            if (tintColor != null) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tintColor.copy(alpha = 0.28f),
                            tintColor.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = maxDim * 0.75f
                    )
                )
            }

            // 2. Top-left curved dome reflection (3D liquid glass lens gleam)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.26f else 0.19f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.24f, h * 0.18f),
                    radius = maxDim * 0.68f
                )
            )

            // 3. Bottom-right secondary internal caustic bounce
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.11f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.78f, h * 0.84f),
                    radius = maxDim * 0.52f
                )
            )

            // 4. Subtle vertical spherical shading for optical depth
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.07f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.16f)
                    )
                )
            )
        }
        .drawWithContent {
            drawContent()
            val w = size.width
            val h = size.height
            val outline = shape.createOutline(size, layoutDirection, this)
            val strokePx = rimWidth.toPx()

            // Crisp outer occlusion edge
            drawOutline(
                outline = outline,
                color = Color.Black.copy(alpha = 0.42f),
                style = Stroke(width = (strokePx * 1.35f))
            )

            // Diagonal specular glass rim matching Apple/BitChord Dark Liquid Glass
            drawOutline(
                outline = outline,
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = if (isPressed) 0.94f else 0.82f),
                        0.24f to Color.White.copy(alpha = 0.28f),
                        0.50f to Color.White.copy(alpha = 0.08f),
                        0.76f to Color.White.copy(alpha = 0.20f),
                        1.00f to Color.White.copy(alpha = 0.48f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                style = Stroke(width = strokePx)
            )
        }
}

