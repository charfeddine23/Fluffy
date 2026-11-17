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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

object ScannerGlassPalette {
    val DeepObsidian = Color(0xFF050814)
    val MidnightNavy = Color(0xFF0B1226)
    val FrostedSurface = Color(0xCC0F172A)
    val FrostedCardDark = Color(0xB80E1629)
    val FrostedElevated = Color(0xD915203B)

    val CyanGlow = Color(0xFF22D3EE)
    val VioletGlow = Color(0xFFA855F7)
    val EdgeOverlayCyan = Color(0xFF00F5D4)
    val EdgeOverlayPurple = Color(0xFF9B5DE5)
    val EmeraldGlow = Color(0xFF34D399)
    val RoseGlow = Color(0xFFF43F5E)
    val AmberGlow = Color(0xFFFBBF24)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val SubtleDivider = Color(0x26FFFFFF)

    val NeonBorderBrush = Brush.linearGradient(
        colors = listOf(
            CyanGlow.copy(alpha = 0.58f),
            Color.White.copy(alpha = 0.16f),
            VioletGlow.copy(alpha = 0.52f)
        )
    )

    val SubtleGlassBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.06f),
            CyanGlow.copy(alpha = 0.12f)
        )
    )
}

@Composable
fun ScannerDarkLiquidBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ScannerGlassPalette.DeepObsidian,
                        ScannerGlassPalette.MidnightNavy,
                        Color(0xFF070C1C)
                    )
                )
            )
    ) {
        // Subtle ambient cyan & violet liquid glow orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ScannerGlassPalette.CyanGlow.copy(alpha = 0.09f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.10f),
                    radius = size.width * 0.60f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ScannerGlassPalette.VioletGlow.copy(alpha = 0.09f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.80f),
                    radius = size.width * 0.65f
                )
            )
        }
        content()
    }
}

fun Modifier.scannerGlassSurface(
    shape: Shape = RoundedCornerShape(22.dp),
    borderWidth: Dp = 0.9.dp,
    glowBorder: Boolean = false,
    fillAlpha: Float = 0.72f
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF111A30).copy(alpha = fillAlpha),
                Color(0xFF080E1C).copy(alpha = (fillAlpha + 0.10f).coerceAtMost(0.96f))
            )
        ),
        shape = shape
    )
    .border(
        width = borderWidth,
        brush = if (glowBorder) ScannerGlassPalette.NeonBorderBrush else ScannerGlassPalette.SubtleGlassBorder,
        shape = shape
    )

/**
 * iOS 26/27-inspired Dark Liquid Glass modifier with crisp specular top highlight,
 * refractive dual-tone stroke, and deep translucent frosted depth.
 */
fun Modifier.iosLiquidGlass(
    shape: Shape = CircleShape,
    glowAccent: Boolean = false,
    fillAlpha: Float = 0.68f
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1D2948).copy(alpha = (fillAlpha + 0.06f).coerceAtMost(0.95f)),
                Color(0xFF0B1224).copy(alpha = fillAlpha),
                Color(0xFF060A16).copy(alpha = (fillAlpha + 0.14f).coerceAtMost(0.98f))
            )
        ),
        shape = shape
    )
    .border(
        width = if (glowAccent) 1.15.dp else 0.85.dp,
        brush = Brush.linearGradient(
            colors = if (glowAccent) {
                listOf(
                    Color.White.copy(alpha = 0.42f),
                    ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.65f),
                    ScannerGlassPalette.EdgeOverlayPurple.copy(alpha = 0.50f),
                    Color.White.copy(alpha = 0.18f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.36f),
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.05f),
                    ScannerGlassPalette.CyanGlow.copy(alpha = 0.16f)
                )
            }
        ),
        shape = shape
    )

/**
 * Custom Vector / Canvas Document Scanner Icon with Glowing Laser Line.
 */
@Composable
fun ScannerLaserIcon(
    modifier: Modifier = Modifier,
    frameColor: Color = ScannerGlassPalette.CyanGlow,
    laserColor: Color = ScannerGlassPalette.VioletGlow
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pad = w * 0.12f
        val cornerLen = w * 0.24f
        val stroke = w * 0.09f

        // 4 glowing corner brackets
        val bracketPaint = Stroke(width = stroke, cap = StrokeCap.Round)
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
            color = Color.White.copy(alpha = 0.22f),
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
