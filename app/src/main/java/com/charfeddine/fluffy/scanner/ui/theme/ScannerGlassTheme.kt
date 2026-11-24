package com.charfeddine.fluffy.scanner.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.glass.LocalColoredContainerBorders
import com.example.ui.theme.LocalAppThemePreset
import java.util.Locale

/**
 * Adaptive Liquid Glass palette for the Scanner & Document Editor modules.
 * Dynamically resolves every token from [LocalAppThemePreset] and [LocalColoredContainerBorders]
 * so both dark and light themes across Fluffy harmonize seamlessly.
 */
object ScannerGlassPalette {
    val DeepObsidian: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppThemePreset.current.background

    val MidnightNavy: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val preset = LocalAppThemePreset.current
            return lerp(preset.background, preset.surface, if (preset.isDark) 0.55f else 0.72f)
        }

    val FrostedSurface: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val preset = LocalAppThemePreset.current
            val alpha = when {
                preset.isGlassStyle && preset.isDark -> 0.80f
                preset.isGlassStyle && !preset.isDark -> 0.88f
                else -> 0.95f
            }
            return preset.surface.copy(alpha = alpha)
        }

    val FrostedCardDark: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val preset = LocalAppThemePreset.current
            val base = lerp(preset.surface, preset.background, 0.22f)
            val alpha = when {
                preset.isGlassStyle && preset.isDark -> 0.78f
                preset.isGlassStyle && !preset.isDark -> 0.88f
                else -> 0.95f
            }
            return base.copy(alpha = alpha)
        }

    val FrostedElevated: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val preset = LocalAppThemePreset.current
            val alpha = when {
                preset.isGlassStyle && preset.isDark -> 0.90f
                preset.isGlassStyle && !preset.isDark -> 0.94f
                else -> 0.98f
            }
            return preset.surfaceVariant.copy(alpha = alpha)
        }

    val CyanGlow: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppThemePreset.current.primary

    val VioletGlow: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppThemePreset.current.secondary

    val EdgeOverlayCyan: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppThemePreset.current.primary

    val EdgeOverlayPurple: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppThemePreset.current.tertiary

    val EmeraldGlow: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LocalAppThemePreset.current.isDark) Color(0xFF34D399) else Color(0xFF059669)

    val RoseGlow: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LocalAppThemePreset.current.isDark) Color(0xFFF43F5E) else Color(0xFFE11D48)

    val AmberGlow: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LocalAppThemePreset.current.isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    val TextPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppThemePreset.current.onSurface

    val TextSecondary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppThemePreset.current.onSurfaceVariant

    val SubtleDivider: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val preset = LocalAppThemePreset.current
            return preset.onSurface.copy(alpha = if (preset.isDark) 0.14f else 0.11f)
        }

    val SubtleStroke: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val preset = LocalAppThemePreset.current
            val coloredBorders = LocalColoredContainerBorders.current
            return if (coloredBorders) {
                preset.primary.copy(alpha = if (preset.isDark) 0.28f else 0.24f)
            } else {
                preset.onSurface.copy(alpha = if (preset.isDark) 0.16f else 0.14f)
            }
        }

    val SubtleChipBg: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val preset = LocalAppThemePreset.current
            return preset.surfaceVariant.copy(alpha = if (preset.isDark) 0.72f else 0.85f)
        }

    val ThumbnailFrame: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val preset = LocalAppThemePreset.current
            return lerp(preset.background, preset.surfaceVariant, 0.50f)
        }

    val StageSurface: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val preset = LocalAppThemePreset.current
            return if (preset.isDark) {
                lerp(preset.background, Color.Black, 0.35f)
            } else {
                lerp(preset.background, preset.surfaceVariant, 0.45f)
            }
        }

    val InputContainer: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val preset = LocalAppThemePreset.current
            return preset.surfaceVariant.copy(alpha = if (preset.isDark) 0.68f else 0.86f)
        }

    val OnAccent: Color
        @Composable
        @ReadOnlyComposable
        get() = contrastOn(LocalAppThemePreset.current.primary)

    val NeonBorderBrush: Brush
        @Composable
        @ReadOnlyComposable
        get() {
            val preset = LocalAppThemePreset.current
            val coloredBorders = LocalColoredContainerBorders.current
            return if (coloredBorders) {
                Brush.linearGradient(
                    colors = listOf(
                        preset.primary.copy(alpha = if (preset.isDark) 0.68f else 0.58f),
                        preset.onSurface.copy(alpha = if (preset.isDark) 0.16f else 0.12f),
                        preset.secondary.copy(alpha = if (preset.isDark) 0.58f else 0.50f)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        preset.onSurface.copy(alpha = if (preset.isDark) 0.24f else 0.18f),
                        preset.onSurface.copy(alpha = if (preset.isDark) 0.10f else 0.08f),
                        preset.onSurface.copy(alpha = if (preset.isDark) 0.20f else 0.14f)
                    )
                )
            }
        }

    val SubtleGlassBorder: Brush
        @Composable
        @ReadOnlyComposable
        get() {
            val preset = LocalAppThemePreset.current
            val coloredBorders = LocalColoredContainerBorders.current
            return if (coloredBorders) {
                Brush.linearGradient(
                    colors = listOf(
                        preset.onSurface.copy(alpha = if (preset.isDark) 0.18f else 0.12f),
                        preset.onSurface.copy(alpha = if (preset.isDark) 0.06f else 0.05f),
                        preset.primary.copy(alpha = if (preset.isDark) 0.24f else 0.18f)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        preset.onSurface.copy(alpha = if (preset.isDark) 0.15f else 0.12f),
                        preset.onSurface.copy(alpha = if (preset.isDark) 0.06f else 0.05f),
                        preset.onSurface.copy(alpha = if (preset.isDark) 0.12f else 0.10f)
                    )
                )
            }
        }

    fun contrastOn(color: Color): Color {
        return if (color.luminance() > 0.48f) Color(0xFF0B1120) else Color.White
    }
}

@Composable
fun ScannerDarkLiquidBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val preset = LocalAppThemePreset.current
    val midStop = lerp(preset.background, preset.surface, if (preset.isDark) 0.45f else 0.65f)
    val deepStop = if (preset.isDark) {
        lerp(preset.background, Color.Black, 0.22f)
    } else {
        lerp(preset.background, preset.surfaceVariant, 0.30f)
    }
    val topOrb = preset.orb1.copy(alpha = if (preset.isDark) 0.22f else 0.18f)
    val botOrb = preset.orb2.copy(alpha = if (preset.isDark) 0.20f else 0.16f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        preset.background,
                        midStop,
                        deepStop
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        topOrb,
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.10f),
                    radius = size.width * 0.62f
                ),
                center = Offset(size.width * 0.15f, size.height * 0.10f),
                radius = size.width * 0.62f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        botOrb,
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.80f),
                    radius = size.width * 0.66f
                ),
                center = Offset(size.width * 0.88f, size.height * 0.80f),
                radius = size.width * 0.66f
            )
        }
        content()
    }
}

@Composable
fun Modifier.scannerGlassSurface(
    shape: Shape = RoundedCornerShape(22.dp),
    borderWidth: Dp = 0.9.dp,
    glowBorder: Boolean = false,
    fillAlpha: Float = 0.72f
): Modifier {
    val preset = LocalAppThemePreset.current
    val effectiveAlpha = if (preset.isDark) {
        fillAlpha.coerceIn(0.25f, 0.96f)
    } else {
        (fillAlpha + 0.14f).coerceIn(0.65f, 0.96f)
    }
    val topFill = preset.surface.copy(alpha = effectiveAlpha)
    val bottomFill = lerp(preset.surface, preset.background, 0.26f).copy(
        alpha = (effectiveAlpha + 0.08f).coerceAtMost(0.98f)
    )
    val borderBrush = if (glowBorder) {
        ScannerGlassPalette.NeonBorderBrush
    } else {
        ScannerGlassPalette.SubtleGlassBorder
    }

    return this
        .clip(shape)
        .background(
            brush = Brush.linearGradient(colors = listOf(topFill, bottomFill)),
            shape = shape
        )
        .border(
            width = borderWidth,
            brush = borderBrush,
            shape = shape
        )
}

/**
 * Adaptive Liquid Glass modifier with crisp specular top highlight,
 * refractive dual-tone stroke, and translucent frosted depth synced with active theme.
 */
@Composable
fun Modifier.iosLiquidGlass(
    shape: Shape = CircleShape,
    glowAccent: Boolean = false,
    fillAlpha: Float = 0.68f
): Modifier {
    val preset = LocalAppThemePreset.current
    val coloredBorders = LocalColoredContainerBorders.current
    val baseAlpha = if (preset.isDark) {
        fillAlpha.coerceIn(0.25f, 0.95f)
    } else {
        (fillAlpha + 0.16f).coerceIn(0.70f, 0.96f)
    }

    val topFill = preset.surfaceVariant.copy(alpha = (baseAlpha + 0.06f).coerceAtMost(0.96f))
    val midFill = preset.surface.copy(alpha = baseAlpha)
    val botFill = lerp(preset.surface, preset.background, 0.30f).copy(
        alpha = (baseAlpha + 0.12f).coerceAtMost(0.98f)
    )

    val highlightRim = if (preset.isDark) {
        Color.White.copy(alpha = 0.36f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }

    val strokeColors = when {
        glowAccent && coloredBorders -> listOf(
            highlightRim,
            preset.primary.copy(alpha = 0.72f),
            preset.tertiary.copy(alpha = 0.56f),
            preset.onSurface.copy(alpha = 0.18f)
        )
        glowAccent -> listOf(
            highlightRim,
            preset.onSurface.copy(alpha = 0.32f),
            preset.onSurface.copy(alpha = 0.18f),
            preset.onSurface.copy(alpha = 0.26f)
        )
        coloredBorders -> listOf(
            highlightRim,
            preset.onSurface.copy(alpha = if (preset.isDark) 0.12f else 0.10f),
            preset.onSurface.copy(alpha = 0.06f),
            preset.primary.copy(alpha = if (preset.isDark) 0.22f else 0.18f)
        )
        else -> listOf(
            highlightRim,
            preset.onSurface.copy(alpha = if (preset.isDark) 0.14f else 0.12f),
            preset.onSurface.copy(alpha = 0.06f),
            preset.onSurface.copy(alpha = if (preset.isDark) 0.12f else 0.10f)
        )
    }

    return this
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(colors = listOf(topFill, midFill, botFill)),
            shape = shape
        )
        .border(
            width = if (glowAccent) 1.15.dp else 0.85.dp,
            brush = Brush.linearGradient(colors = strokeColors),
            shape = shape
        )
}

/**
 * Custom Vector / Canvas Document Scanner Icon with Glowing Laser Line.
 */
@Composable
fun ScannerLaserIcon(
    modifier: Modifier = Modifier,
    frameColor: Color = ScannerGlassPalette.CyanGlow,
    laserColor: Color = ScannerGlassPalette.VioletGlow
) {
    val sheetStrokeColor = ScannerGlassPalette.TextSecondary.copy(alpha = 0.38f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pad = w * 0.12f
        val cornerLen = w * 0.24f
        val stroke = w * 0.09f

        // Top-Left
        drawLine(frameColor, Offset(pad, pad + cornerLen), Offset(pad, pad), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(frameColor, Offset(pad, pad), Offset(pad + cornerLen, pad), strokeWidth = stroke, cap = StrokeCap.Round)
        // Top-Right
        drawLine(frameColor, Offset(w - pad - cornerLen, pad), Offset(w - pad, pad), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(frameColor, Offset(w - pad, pad), Offset(w - pad, pad + cornerLen), strokeWidth = stroke, cap = StrokeCap.Round)
        // Bottom-Left
        drawLine(frameColor, Offset(pad, h - pad - cornerLen), Offset(pad, h - pad), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(frameColor, Offset(pad, h - pad), Offset(pad + cornerLen, h - pad), strokeWidth = stroke, cap = StrokeCap.Round)
        // Bottom-Right
        drawLine(frameColor, Offset(w - pad - cornerLen, h - pad), Offset(w - pad, h - pad), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(frameColor, Offset(w - pad, h - pad - cornerLen), Offset(w - pad, h - pad), strokeWidth = stroke, cap = StrokeCap.Round)

        // Inner miniature document sheet
        drawRoundRect(
            color = sheetStrokeColor,
            topLeft = Offset(w * 0.27f, h * 0.24f),
            size = Size(w * 0.46f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
            style = Stroke(width = stroke * 0.65f)
        )

        // Glowing Laser Scan Sweep Line across document
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, frameColor, laserColor, Color.Transparent)
            ),
            start = Offset(pad * 0.6f, h * 0.52f),
            end = Offset(w - pad * 0.6f, h * 0.52f),
            strokeWidth = stroke * 0.95f,
            cap = StrokeCap.Round
        )
    }
}

fun formatScannerBytes(bytes: Long, isArabic: Boolean): String {
    return when {
        bytes < 1024L -> if (isArabic) "$bytes بايت" else "$bytes B"
        bytes < 1024L * 1024L -> {
            val kb = bytes / 1024f
            String.format(Locale.US, "%.1f %s", kb, if (isArabic) "ك.ب" else "KB")
        }
        else -> {
            val mb = bytes / (1024f * 1024f)
            String.format(Locale.US, "%.2f %s", mb, if (isArabic) "م.ب" else "MB")
        }
    }
}
