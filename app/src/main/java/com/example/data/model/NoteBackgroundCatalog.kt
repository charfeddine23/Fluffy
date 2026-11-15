package com.example.data.model

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class BackgroundPattern {
    AURORA_RIBBONS,
    STARFIELD,
    PETALS,
    LIQUID_WAVES,
    GOLDEN_DUNES,
    BOTANICAL_MIST,
    CYBER_GRID,
    SUNSET_GLOW,
    MARBLE_VEINS,
    RULED_PAPER,
    CRYSTAL_FACETS,
    VIOLET_MESH,
    RADIAL_BLOOM,
    SOFT_BOKEH,
    CONSTELLATION,
    CHERRY_BLOOM,
    CARBON_WEAVE,
    EMBER_SPARKS,
    ZEN_RINGS,
    PRISM_SHEEN
}

data class NoteBackgroundPreset(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val colors: List<Color>,
    val accentColor: Color,
    val pattern: BackgroundPattern,
    val isDarkSurface: Boolean
)

data class NoteDocumentColorOption(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val argb: Long,
    val accentColor: Color
)

data class NoteSurfaceColors(
    val effectiveDark: Boolean,
    val titleColor: Color,
    val bodyColor: Color,
    val metaColor: Color,
    val accentColor: Color,
    val pillFill: Color
)

object NoteBackgroundCatalog {
    const val NONE_ID = "none"

    /**
     * Exactly 20 curated artistic backgrounds selectable per document/note.
     */
    val all: List<NoteBackgroundPreset> = listOf(
        NoteBackgroundPreset(
            id = "aurora_borealis",
            nameAr = "الشفق القطبي",
            nameEn = "Aurora",
            colors = listOf(Color(0xFF041C2C), Color(0xFF0B3B49), Color(0xFF0F172A)),
            accentColor = Color(0xFF34D399),
            pattern = BackgroundPattern.AURORA_RIBBONS,
            isDarkSurface = true
        ),
        NoteBackgroundPreset(
            id = "midnight_cosmos",
            nameAr = "فضاء منتصف الليل",
            nameEn = "Cosmos",
            colors = listOf(Color(0xFF050816), Color(0xFF141036), Color(0xFF090D24)),
            accentColor = Color(0xFF818CF8),
            pattern = BackgroundPattern.STARFIELD,
            isDarkSurface = true
        ),
        NoteBackgroundPreset(
            id = "sakura_mist",
            nameAr = "ضباب الساكورا",
            nameEn = "Sakura Mist",
            colors = listOf(Color(0xFFFFF1F5), Color(0xFFFCE7F3), Color(0xFFFFE4E6)),
            accentColor = Color(0xFFDB2777),
            pattern = BackgroundPattern.PETALS,
            isDarkSurface = false
        ),
        NoteBackgroundPreset(
            id = "ocean_waves",
            nameAr = "أمواج المحيط",
            nameEn = "Ocean Waves",
            colors = listOf(Color(0xFF06283D), Color(0xFF0E4D71), Color(0xFF082F49)),
            accentColor = Color(0xFF38BDF8),
            pattern = BackgroundPattern.LIQUID_WAVES,
            isDarkSurface = true
        ),
        NoteBackgroundPreset(
            id = "golden_dune",
            nameAr = "كثبان ذهبية",
            nameEn = "Golden Dune",
            colors = listOf(Color(0xFFFFF8E7), Color(0xFFFEF3C7), Color(0xFFFDE68A)),
            accentColor = Color(0xFFD97706),
            pattern = BackgroundPattern.GOLDEN_DUNES,
            isDarkSurface = false
        ),
        NoteBackgroundPreset(
            id = "emerald_forest",
            nameAr = "غابة الزمرد",
            nameEn = "Emerald Forest",
            colors = listOf(Color(0xFF022C22), Color(0xFF064E3B), Color(0xFF042F2E)),
            accentColor = Color(0xFF34D399),
            pattern = BackgroundPattern.BOTANICAL_MIST,
            isDarkSurface = true
        ),
        NoteBackgroundPreset(
            id = "cyber_grid",
            nameAr = "شبكة سايبر",
            nameEn = "Cyber Grid",
            colors = listOf(Color(0xFF090A1A), Color(0xFF1A103D), Color(0xFF0B132B)),
            accentColor = Color(0xFF22D3EE),
            pattern = BackgroundPattern.CYBER_GRID,
            isDarkSurface = true
        ),
        NoteBackgroundPreset(
            id = "sunset_horizon",
            nameAr = "أفق الغروب",
            nameEn = "Sunset Glow",
            colors = listOf(Color(0xFF3B0A2A), Color(0xFF7C2D12), Color(0xFF2E1065)),
            accentColor = Color(0xFFFB923C),
            pattern = BackgroundPattern.SUNSET_GLOW,
            isDarkSurface = true
        ),
        NoteBackgroundPreset(
            id = "marble_silk",
            nameAr = "رخام حريري",
            nameEn = "Marble Silk",
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0), Color(0xFFF1F5F9)),
            accentColor = Color(0xFF475569),
            pattern = BackgroundPattern.MARBLE_VEINS,
            isDarkSurface = false
        ),
        NoteBackgroundPreset(
            id = "linen_paper",
            nameAr = "دفتر مسطّر",
            nameEn = "Ruled Paper",
            colors = listOf(Color(0xFFFFFBEB), Color(0xFFFEF9C3), Color(0xFFFFF7ED)),
            accentColor = Color(0xFFB45309),
            pattern = BackgroundPattern.RULED_PAPER,
            isDarkSurface = false
        ),
        NoteBackgroundPreset(
            id = "arctic_frost",
            nameAr = "صقيع قطبي",
            nameEn = "Arctic Frost",
            colors = listOf(Color(0xFFE0F2FE), Color(0xFFECFEFF), Color(0xFFDBEAFE)),
            accentColor = Color(0xFF0284C7),
            pattern = BackgroundPattern.CRYSTAL_FACETS,
            isDarkSurface = false
        ),
        NoteBackgroundPreset(
            id = "lavender_haze",
            nameAr = "ضباب اللافندر",
            nameEn = "Lavender Haze",
            colors = listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE), Color(0xFFFAE8FF)),
            accentColor = Color(0xFF7C3AED),
            pattern = BackgroundPattern.VIOLET_MESH,
            isDarkSurface = false
        ),
        NoteBackgroundPreset(
            id = "velvet_rose",
            nameAr = "ورد مخملي",
            nameEn = "Velvet Rose",
            colors = listOf(Color(0xFF3B0722), Color(0xFF6B0F3D), Color(0xFF280519)),
            accentColor = Color(0xFFFB7185),
            pattern = BackgroundPattern.RADIAL_BLOOM,
            isDarkSurface = true
        ),
        NoteBackgroundPreset(
            id = "mint_breeze",
            nameAr = "نسيم النعناع",
            nameEn = "Mint Breeze",
            colors = listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5), Color(0xFFE0F2FE)),
            accentColor = Color(0xFF059669),
            pattern = BackgroundPattern.SOFT_BOKEH,
            isDarkSurface = false
        ),
        NoteBackgroundPreset(
            id = "starry_indigo",
            nameAr = "أبراج النيلي",
            nameEn = "Constellation",
            colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF172554)),
            accentColor = Color(0xFF60A5FA),
            pattern = BackgroundPattern.CONSTELLATION,
            isDarkSurface = true
        ),
        NoteBackgroundPreset(
            id = "peach_blossom",
            nameAr = "زهر الخوخ",
            nameEn = "Peach Bloom",
            colors = listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFFFFE4E6)),
            accentColor = Color(0xFFEA580C),
            pattern = BackgroundPattern.CHERRY_BLOOM,
            isDarkSurface = false
        ),
        NoteBackgroundPreset(
            id = "obsidian_carbon",
            nameAr = "كاربون داكن",
            nameEn = "Obsidian Carbon",
            colors = listOf(Color(0xFF09090B), Color(0xFF18181B), Color(0xFF111827)),
            accentColor = Color(0xFFA1A1AA),
            pattern = BackgroundPattern.CARBON_WEAVE,
            isDarkSurface = true
        ),
        NoteBackgroundPreset(
            id = "cherry_ember",
            nameAr = "جمر الكرز",
            nameEn = "Cherry Ember",
            colors = listOf(Color(0xFF28090D), Color(0xFF5C1118), Color(0xFF1F070A)),
            accentColor = Color(0xFFF87171),
            pattern = BackgroundPattern.EMBER_SPARKS,
            isDarkSurface = true
        ),
        NoteBackgroundPreset(
            id = "matcha_zen",
            nameAr = "هدوء الماتشا",
            nameEn = "Matcha Zen",
            colors = listOf(Color(0xFFF4F7EE), Color(0xFFE4ECD8), Color(0xFFECF3E8)),
            accentColor = Color(0xFF4D7C0F),
            pattern = BackgroundPattern.ZEN_RINGS,
            isDarkSurface = false
        ),
        NoteBackgroundPreset(
            id = "holographic_prism",
            nameAr = "منشور هولوغرافي",
            nameEn = "Holo Prism",
            colors = listOf(Color(0xFFE0F2FE), Color(0xFFF3E8FF), Color(0xFFFCE7F3)),
            accentColor = Color(0xFF8B5CF6),
            pattern = BackgroundPattern.PRISM_SHEEN,
            isDarkSurface = false
        )
    )

    /**
     * 16 curated solid/glass individual document colors (soft light papers + deep rich tones).
     */
    val documentColors: List<NoteDocumentColorOption> = listOf(
        NoteDocumentColorOption("ivory_paper", "ورق عاجي", "Ivory Paper", 0xFFFFFBF2, Color(0xFFB45309)),
        NoteDocumentColorOption("butter_cream", "أصفر دافئ", "Butter", 0xFFFEF3C7, Color(0xFFD97706)),
        NoteDocumentColorOption("peach_soft", "خوخي ناعم", "Peach", 0xFFFFE4D6, Color(0xFFEA580C)),
        NoteDocumentColorOption("blush_rose", "وردي حالم", "Blush", 0xFFFFE4EC, Color(0xFFDB2777)),
        NoteDocumentColorOption("lavender_soft", "بنفسجي هادئ", "Lavender", 0xFFEDE9FE, Color(0xFF7C3AED)),
        NoteDocumentColorOption("sky_pastel", "سماوي ناعم", "Sky", 0xFFE0F2FE, Color(0xFF0284C7)),
        NoteDocumentColorOption("mint_pastel", "نعناع فاتح", "Mint", 0xFFD1FAE5, Color(0xFF059669)),
        NoteDocumentColorOption("sage_soft", "زيتوني ناعم", "Sage", 0xFFE7F0DC, Color(0xFF4D7C0F)),
        NoteDocumentColorOption("deep_navy", "كحلي عميق", "Navy", 0xFF0F254A, Color(0xFF60A5FA)),
        NoteDocumentColorOption("royal_indigo", "نيلي ملكي", "Indigo", 0xFF1E1B4B, Color(0xFF818CF8)),
        NoteDocumentColorOption("plum_velvet", "أرجواني داكن", "Plum", 0xFF3B0764, Color(0xFFC084FC)),
        NoteDocumentColorOption("crimson_wine", "عنابي فاخر", "Crimson", 0xFF4C0519, Color(0xFFFB7185)),
        NoteDocumentColorOption("forest_pine", "أخضر غابات", "Forest", 0xFF064E3B, Color(0xFF34D399)),
        NoteDocumentColorOption("teal_abyss", "تركواز عميق", "Deep Teal", 0xFF134E4A, Color(0xFF2DD4BF)),
        NoteDocumentColorOption("amber_espresso", "بني كهرماني", "Espresso", 0xFF451A03, Color(0xFFFBBF24)),
        NoteDocumentColorOption("charcoal_ink", "فحمي أنيق", "Charcoal", 0xFF18181B, Color(0xFF38BDF8))
    )

    fun findById(id: String?): NoteBackgroundPreset? {
        if (id.isNullOrBlank() || id == NONE_ID) return null
        return all.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }

    fun findColorOption(argb: Long): NoteDocumentColorOption? {
        if (argb == 0L) return null
        return documentColors.firstOrNull { it.argb == argb }
    }

    fun resolveSurfaceColors(
        themeIsDark: Boolean,
        tint: GlassTint,
        customColorArgb: Long,
        backgroundId: String
    ): NoteSurfaceColors {
        val bg = findById(backgroundId)
        val customColor = if (customColorArgb != 0L) Color(customColorArgb.toInt()) else null
        val customOption = findColorOption(customColorArgb)

        val effectiveDark = when {
            bg != null -> bg.isDarkSurface
            customColor != null -> customColor.luminance() < 0.45f
            else -> themeIsDark
        }

        val accent = when {
            bg != null -> bg.accentColor
            customOption != null -> customOption.accentColor
            customColor != null -> if (effectiveDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
            else -> tint.accentColor
        }

        return if (effectiveDark) {
            NoteSurfaceColors(
                effectiveDark = true,
                titleColor = Color(0xFFF8FAFC),
                bodyColor = Color(0xFFE2E8F0),
                metaColor = Color(0xFFCBD5E1).copy(alpha = 0.78f),
                accentColor = accent,
                pillFill = Color(0xFF0F172A).copy(alpha = 0.62f)
            )
        } else {
            NoteSurfaceColors(
                effectiveDark = false,
                titleColor = Color(0xFF0F172A),
                bodyColor = Color(0xFF1E293B),
                metaColor = Color(0xFF475569),
                accentColor = accent,
                pillFill = Color.White.copy(alpha = 0.76f)
            )
        }
    }
}

@Composable
fun NoteBackgroundCanvas(
    preset: NoteBackgroundPreset,
    modifier: Modifier = Modifier,
    isCompactCard: Boolean = false
) {
    Canvas(modifier = modifier) {
        drawNoteBackgroundPreset(preset = preset, isCompactCard = isCompactCard)
    }
}

fun DrawScope.drawNoteBackgroundPreset(
    preset: NoteBackgroundPreset,
    isCompactCard: Boolean = false
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    drawRect(
        brush = Brush.linearGradient(
            colors = preset.colors,
            start = Offset.Zero,
            end = Offset(w, h)
        )
    )

    val alphaScale = if (isCompactCard) 0.75f else 1.0f
    val accent = preset.accentColor

    when (preset.pattern) {
        BackgroundPattern.AURORA_RIBBONS -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.36f * alphaScale), Color.Transparent),
                    center = Offset(w * 0.2f, h * 0.18f),
                    radius = w * 0.75f
                ),
                center = Offset(w * 0.2f, h * 0.18f),
                radius = w * 0.75f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.28f * alphaScale), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.55f),
                    radius = w * 0.7f
                ),
                center = Offset(w * 0.85f, h * 0.55f),
                radius = w * 0.7f
            )
            val ribbon = Path().apply {
                moveTo(0f, h * 0.42f)
                cubicTo(w * 0.3f, h * 0.24f, w * 0.65f, h * 0.62f, w, h * 0.32f)
            }
            drawPath(
                path = ribbon,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF2DD4BF).copy(alpha = 0.35f), Color(0xFFA855F7).copy(alpha = 0.25f))
                ),
                style = Stroke(width = (w * 0.08f).coerceAtLeast(8f), cap = StrokeCap.Round)
            )
        }

        BackgroundPattern.STARFIELD -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.24f * alphaScale), Color.Transparent),
                    center = Offset(w * 0.72f, h * 0.25f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.72f, h * 0.25f),
                radius = w * 0.65f
            )
            val starCount = if (isCompactCard) 18 else 42
            for (i in 0 until starCount) {
                val sx = ((i * 73 + 19) % 100) / 100f * w
                val sy = ((i * 41 + 37) % 100) / 100f * h
                val sr = if (i % 5 == 0) 2.6f else if (i % 2 == 0) 1.6f else 1.0f
                drawCircle(
                    color = Color.White.copy(alpha = (0.35f + (i % 4) * 0.14f) * alphaScale),
                    radius = sr,
                    center = Offset(sx, sy)
                )
            }
        }

        BackgroundPattern.PETALS, BackgroundPattern.CHERRY_BLOOM -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.18f * alphaScale), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.15f),
                    radius = w * 0.6f
                ),
                center = Offset(w * 0.85f, h * 0.15f),
                radius = w * 0.6f
            )
            val count = if (isCompactCard) 7 else 16
            for (i in 0 until count) {
                val cx = ((i * 61 + 13) % 92 + 4) / 100f * w
                val cy = ((i * 37 + 23) % 90 + 5) / 100f * h
                val r = (w * 0.045f).coerceIn(6f, 20f)
                drawOval(
                    color = accent.copy(alpha = (0.14f + (i % 3) * 0.05f) * alphaScale),
                    topLeft = Offset(cx - r, cy - r * 0.6f),
                    size = Size(r * 2f, r * 1.2f)
                )
            }
        }

        BackgroundPattern.LIQUID_WAVES -> {
            for (layer in 0..3) {
                val baseY = h * (0.35f + layer * 0.16f)
                val path = Path().apply {
                    moveTo(0f, baseY)
                    cubicTo(
                        w * 0.3f, baseY - h * 0.09f,
                        w * 0.7f, baseY + h * 0.09f,
                        w, baseY - h * 0.03f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = path,
                    color = accent.copy(alpha = (0.10f + layer * 0.04f) * alphaScale)
                )
            }
        }

        BackgroundPattern.GOLDEN_DUNES -> {
            for (layer in 0..2) {
                val baseY = h * (0.45f + layer * 0.17f)
                val path = Path().apply {
                    moveTo(0f, baseY)
                    cubicTo(
                        w * 0.35f, baseY - h * 0.11f,
                        w * 0.65f, baseY + h * 0.06f,
                        w, baseY - h * 0.05f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = path,
                    color = accent.copy(alpha = (0.11f + layer * 0.05f) * alphaScale)
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(w * 0.8f, h * 0.18f),
                    radius = w * 0.42f
                ),
                center = Offset(w * 0.8f, h * 0.18f),
                radius = w * 0.42f
            )
        }

        BackgroundPattern.BOTANICAL_MIST -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.25f * alphaScale), Color.Transparent),
                    center = Offset(w * 0.18f, h * 0.25f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.18f, h * 0.25f),
                radius = w * 0.65f
            )
            for (i in 0..5) {
                val cx = w * (0.15f + i * 0.16f)
                val cy = h * (0.78f - (i % 3) * 0.14f)
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = 0.11f * alphaScale),
                    radius = (w * 0.11f),
                    center = Offset(cx, cy)
                )
            }
        }

        BackgroundPattern.CYBER_GRID -> {
            val step = (w / if (isCompactCard) 7f else 11f).coerceAtLeast(18f)
            val lineColor = accent.copy(alpha = 0.16f * alphaScale)
            var x = 0f
            while (x <= w) {
                drawLine(lineColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y <= h) {
                drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                y += step
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFC084FC).copy(alpha = 0.26f), Color.Transparent),
                    center = Offset(w * 0.8f, h * 0.2f),
                    radius = w * 0.6f
                ),
                center = Offset(w * 0.8f, h * 0.2f),
                radius = w * 0.6f
            )
        }

        BackgroundPattern.SUNSET_GLOW -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFBBF24).copy(alpha = 0.35f * alphaScale),
                        Color(0xFFF43F5E).copy(alpha = 0.18f * alphaScale),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.38f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.5f, h * 0.38f),
                radius = w * 0.65f
            )
            drawLine(
                color = Color(0xFFFDE047).copy(alpha = 0.22f),
                start = Offset(0f, h * 0.64f),
                end = Offset(w, h * 0.64f),
                strokeWidth = 2f
            )
        }

        BackgroundPattern.MARBLE_VEINS -> {
            for (i in 0..4) {
                val offset = i * h * 0.2f
                val path = Path().apply {
                    moveTo(0f, offset)
                    cubicTo(w * 0.3f, offset + h * 0.08f, w * 0.6f, offset - h * 0.07f, w, offset + h * 0.12f)
                }
                drawPath(
                    path = path,
                    color = accent.copy(alpha = (0.12f - i * 0.015f).coerceAtLeast(0.04f)),
                    style = Stroke(width = if (i % 2 == 0) 2.5f else 1.2f)
                )
            }
        }

        BackgroundPattern.RULED_PAPER -> {
            val spacing = if (isCompactCard) 20f else 34.dp.toPx()
            val lineColor = Color(0xFF94A3B8).copy(alpha = 0.28f * alphaScale)
            var y = spacing * 1.4f
            while (y < h) {
                drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.2f)
                y += spacing
            }
            if (!isCompactCard) {
                drawLine(
                    color = Color(0xFFF43F5E).copy(alpha = 0.24f),
                    start = Offset(w * 0.12f, 0f),
                    end = Offset(w * 0.12f, h),
                    strokeWidth = 1.8f
                )
            }
        }

        BackgroundPattern.CRYSTAL_FACETS -> {
            val p1 = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.65f, 0f)
                lineTo(w * 0.3f, h * 0.55f)
                close()
            }
            val p2 = Path().apply {
                moveTo(w, h)
                lineTo(w * 0.35f, h)
                lineTo(w * 0.78f, h * 0.42f)
                close()
            }
            drawPath(p1, Color.White.copy(alpha = 0.42f * alphaScale))
            drawPath(p2, accent.copy(alpha = 0.14f * alphaScale))
        }

        BackgroundPattern.VIOLET_MESH -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFC084FC).copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(w * 0.2f, h * 0.22f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.2f, h * 0.22f),
                radius = w * 0.65f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF472B6).copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.78f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.85f, h * 0.78f),
                radius = w * 0.65f
            )
        }

        BackgroundPattern.RADIAL_BLOOM -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.34f * alphaScale), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.35f),
                    radius = w * 0.75f
                ),
                center = Offset(w * 0.5f, h * 0.35f),
                radius = w * 0.75f
            )
        }

        BackgroundPattern.SOFT_BOKEH -> {
            val circles = listOf(
                Triple(0.22f, 0.24f, 0.22f),
                Triple(0.78f, 0.30f, 0.18f),
                Triple(0.42f, 0.65f, 0.25f),
                Triple(0.82f, 0.80f, 0.15f)
            )
            circles.forEachIndexed { idx, (px, py, pr) ->
                drawCircle(
                    color = (if (idx % 2 == 0) Color.White else accent).copy(alpha = 0.20f * alphaScale),
                    radius = w * pr,
                    center = Offset(w * px, h * py)
                )
            }
        }

        BackgroundPattern.CONSTELLATION -> {
            val pts = listOf(
                Offset(w * 0.18f, h * 0.22f),
                Offset(w * 0.44f, h * 0.16f),
                Offset(w * 0.68f, h * 0.32f),
                Offset(w * 0.84f, h * 0.18f),
                Offset(w * 0.30f, h * 0.56f),
                Offset(w * 0.58f, h * 0.66f),
                Offset(w * 0.80f, h * 0.82f)
            )
            for (i in 0 until pts.size - 1) {
                drawLine(
                    color = accent.copy(alpha = 0.24f * alphaScale),
                    start = pts[i],
                    end = pts[i + 1],
                    strokeWidth = 1.4f
                )
            }
            pts.forEach { pt ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.65f * alphaScale),
                    radius = if (isCompactCard) 2.2f else 3.4f,
                    center = pt
                )
            }
        }

        BackgroundPattern.CARBON_WEAVE -> {
            val step = if (isCompactCard) 12f else 18f
            val dotColor = Color.White.copy(alpha = 0.07f * alphaScale)
            var y = 0f
            var row = 0
            while (y < h) {
                var x = if (row % 2 == 0) 0f else step * 0.5f
                while (x < w) {
                    drawRoundRect(
                        color = dotColor,
                        topLeft = Offset(x, y),
                        size = Size(step * 0.45f, step * 0.45f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    x += step
                }
                y += step
                row++
            }
        }

        BackgroundPattern.EMBER_SPARKS -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFEF4444).copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.85f),
                    radius = w * 0.7f
                ),
                center = Offset(w * 0.5f, h * 0.85f),
                radius = w * 0.7f
            )
            val sparks = if (isCompactCard) 10 else 24
            for (i in 0 until sparks) {
                val sx = ((i * 53 + 17) % 94 + 3) / 100f * w
                val sy = ((i * 67 + 11) % 92 + 4) / 100f * h
                drawCircle(
                    color = Color(0xFFFBBF24).copy(alpha = 0.45f * alphaScale),
                    radius = if (i % 3 == 0) 2.6f else 1.5f,
                    center = Offset(sx, sy)
                )
            }
        }

        BackgroundPattern.ZEN_RINGS -> {
            val center = Offset(w * 0.78f, h * 0.25f)
            for (ring in 1..5) {
                drawCircle(
                    color = accent.copy(alpha = (0.14f - ring * 0.02f).coerceAtLeast(0.04f)),
                    radius = w * (0.12f * ring),
                    center = center,
                    style = Stroke(width = 1.6f)
                )
            }
        }

        BackgroundPattern.PRISM_SHEEN -> {
            val prism = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF38BDF8).copy(alpha = 0.22f * alphaScale),
                    Color(0xFFC084FC).copy(alpha = 0.22f * alphaScale),
                    Color(0xFFF472B6).copy(alpha = 0.20f * alphaScale),
                    Color(0xFFFDE047).copy(alpha = 0.14f * alphaScale)
                ),
                start = Offset(0f, h),
                end = Offset(w, 0f)
            )
            drawRect(brush = prism)
            val angle = -PI / 5.0
            val dx = (cos(angle) * w).toFloat()
            val dy = (sin(angle) * w).toFloat()
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(w * 0.1f, h * 0.6f),
                end = Offset(w * 0.1f + dx, h * 0.6f + dy),
                strokeWidth = (w * 0.08f).coerceAtLeast(10f)
            )
        }
    }
}
