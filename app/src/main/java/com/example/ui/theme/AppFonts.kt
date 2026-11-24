package com.example.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R

enum class FontCategory {
    SYSTEM,
    ARABIC_MODERN,
    EDITORIAL_SERIF,
    MONOSPACE,
    HANDWRITING
}

data class AppFontOption(
    val id: String,
    val displayName: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val category: FontCategory,
    val fontFamily: FontFamily,
    val hasNativeLatinItalic: Boolean,
    val hasNativeArabicItalic: Boolean = false
)

data class ObliqueTextRun(
    val text: String,
    val needsObliqueSynthesis: Boolean
)

object AppFontCatalog {
    const val DEFAULT_FONT_ID = "sans_serif"

    val defaultFont = AppFontOption(
        id = "sans_serif",
        displayName = "Sans Serif",
        subtitleAr = "خط سانس سيريف النظيف (الافتراضي)",
        subtitleEn = "Clean modern system sans-serif (Default)",
        category = FontCategory.SYSTEM,
        fontFamily = FontFamily.SansSerif,
        hasNativeLatinItalic = false,
        hasNativeArabicItalic = false
    )

    val allFonts: List<AppFontOption> = listOf(
        defaultFont,
        AppFontOption(
            id = "default_system",
            displayName = "System Default",
            subtitleAr = "خط النظام الأساسي المتوافق",
            subtitleEn = "Native Android system typeface",
            category = FontCategory.SYSTEM,
            fontFamily = FontFamily.Default,
            hasNativeLatinItalic = false,
            hasNativeArabicItalic = false
        ),
        AppFontOption(
            id = "serif",
            displayName = "Serif",
            subtitleAr = "خط سيريف الكلاسيكي للكتب والمقالات",
            subtitleEn = "Classic editorial system serif",
            category = FontCategory.SYSTEM,
            fontFamily = FontFamily.Serif,
            hasNativeLatinItalic = false,
            hasNativeArabicItalic = false
        ),
        AppFontOption(
            id = "monospace",
            displayName = "Monospace",
            subtitleAr = "خط أحادي العرض للملاحظات التقنية والأكواد",
            subtitleEn = "Fixed-width technical monospace",
            category = FontCategory.SYSTEM,
            fontFamily = FontFamily.Monospace,
            hasNativeLatinItalic = false,
            hasNativeArabicItalic = false
        ),
        AppFontOption(
            id = "cursive",
            displayName = "Cursive",
            subtitleAr = "خط يدوي انسيابي أنيق",
            subtitleEn = "Flowing handwritten cursive script",
            category = FontCategory.SYSTEM,
            fontFamily = FontFamily.Cursive,
            hasNativeLatinItalic = false,
            hasNativeArabicItalic = false
        ),
        AppFontOption(
            id = "cairo",
            displayName = "Cairo • القاهرة",
            subtitleAr = "خط عربي وإنجليزي عصري متوازن",
            subtitleEn = "Modern geometric Arabic & Latin sans",
            category = FontCategory.ARABIC_MODERN,
            fontFamily = FontFamily(Font(R.font.cairo, FontWeight.Normal)),
            hasNativeLatinItalic = false,
            hasNativeArabicItalic = false
        ),
        AppFontOption(
            id = "tajawal",
            displayName = "Tajawal • تجوال",
            subtitleAr = "خط هندسي ناعم وواضح للقراءة السريعة",
            subtitleEn = "Soft geometric Arabic & Latin sans",
            category = FontCategory.ARABIC_MODERN,
            fontFamily = FontFamily(Font(R.font.tajawal, FontWeight.Normal)),
            hasNativeLatinItalic = false,
            hasNativeArabicItalic = false
        ),
        AppFontOption(
            id = "amiri",
            displayName = "Amiri • أميري",
            subtitleAr = "خط نسخ عربي أصيل وأنيق للكتابات الأدبية",
            subtitleEn = "Classical Naskh & literary typeface",
            category = FontCategory.EDITORIAL_SERIF,
            fontFamily = FontFamily(Font(R.font.amiri, FontWeight.Normal)),
            hasNativeLatinItalic = false,
            hasNativeArabicItalic = false
        ),
        AppFontOption(
            id = "inter",
            displayName = "Inter",
            subtitleAr = "خط واجهات وتدوين فائق الوضوح",
            subtitleEn = "Crisp UI & note-taking neo-grotesque",
            category = FontCategory.ARABIC_MODERN,
            fontFamily = FontFamily(Font(R.font.inter, FontWeight.Normal)),
            hasNativeLatinItalic = false,
            hasNativeArabicItalic = false
        ),
        AppFontOption(
            id = "open_sans",
            displayName = "Open Sans",
            subtitleAr = "خط إنساني مريح للعين في القراءة الطويلة",
            subtitleEn = "Friendly humanist sans for long reading",
            category = FontCategory.ARABIC_MODERN,
            fontFamily = FontFamily(Font(R.font.open_sans, FontWeight.Normal)),
            hasNativeLatinItalic = false,
            hasNativeArabicItalic = false
        ),
        AppFontOption(
            id = "playfair_display",
            displayName = "Playfair Display",
            subtitleAr = "خط عناوين ومجلات كلاسيكي فاخر",
            subtitleEn = "High-contrast editorial display serif",
            category = FontCategory.EDITORIAL_SERIF,
            fontFamily = FontFamily(Font(R.font.playfair_display, FontWeight.Normal)),
            hasNativeLatinItalic = false,
            hasNativeArabicItalic = false
        ),
        AppFontOption(
            id = "roboto_mono",
            displayName = "Roboto Mono",
            subtitleAr = "خط برمجي حديث متساوي المسافات",
            subtitleEn = "Precision coding & markdown monospace",
            category = FontCategory.MONOSPACE,
            fontFamily = FontFamily(Font(R.font.roboto_mono, FontWeight.Normal)),
            hasNativeLatinItalic = false,
            hasNativeArabicItalic = false
        ),
        AppFontOption(
            id = "dancing_script",
            displayName = "Dancing Script",
            subtitleAr = "خط زخرفي حيوي يشبه الكتابة باليد",
            subtitleEn = "Lively handwritten calligraphy script",
            category = FontCategory.HANDWRITING,
            fontFamily = FontFamily(Font(R.font.dancing_script, FontWeight.Normal)),
            hasNativeLatinItalic = false,
            hasNativeArabicItalic = false
        )
    )

    fun resolve(id: String?): AppFontOption =
        allFonts.firstOrNull { it.id == id } ?: defaultFont

    fun isNonLatinNoItalicScript(ch: Char): Boolean {
        val code = ch.code
        return code in 0x0600..0x06FF || // Arabic
            code in 0x0750..0x077F || // Arabic Supplement
            code in 0x08A0..0x08FF || // Arabic Extended-A
            code in 0xFB50..0xFDFF || // Arabic Presentation Forms-A
            code in 0xFE70..0xFEFF || // Arabic Presentation Forms-B
            code in 0x0590..0x05FF || // Hebrew
            code in 0x0700..0x074F || // Syriac
            code in 0x0780..0x07BF || // Thaana
            code in 0x0900..0x0DFF || // Indic scripts
            code in 0x2D30..0x2D7F || // Tifinagh
            code in 0x3040..0x30FF || // Japanese Hiragana/Katakana
            code in 0x4E00..0x9FFF || // CJK Unified Ideographs
            code in 0xAC00..0xD7AF    // Hangul
    }

    private fun isLatinLetter(ch: Char): Boolean =
        ch in 'A'..'Z' || ch in 'a'..'z' || ch.code in 0x00C0..0x024F

    /**
     * Splits [text] inside an Italic span into cohesive script runs:
     * - For bundled single-weight fonts or Mono/Cursive where native italic `.ttf` isn't guaranteed,
     *   synthesizes oblique across the entire run.
     * - For system SansSerif/Default/Serif where Latin uses native Roboto-Italic / NotoSerif-Italic,
     *   keeps pure Latin runs native and applies oblique synthesis cleanly to Arabic / non-Latin runs
     *   without breaking Arabic contextual ligatures.
     */
    fun splitObliqueRuns(text: String, fontOption: AppFontOption): List<ObliqueTextRun> {
        if (text.isEmpty()) return emptyList()
        if (!fontOption.hasNativeLatinItalic) {
            return listOf(ObliqueTextRun(text, needsObliqueSynthesis = true))
        }

        val hasNonLatin = !fontOption.hasNativeArabicItalic && text.any(::isNonLatinNoItalicScript)
        if (!hasNonLatin) {
            return listOf(ObliqueTextRun(text, needsObliqueSynthesis = false))
        }

        val hasLatin = text.any(::isLatinLetter)
        if (!hasLatin) {
            return listOf(ObliqueTextRun(text, needsObliqueSynthesis = true))
        }

        val runs = mutableListOf<ObliqueTextRun>()
        var currentNeedsOblique = isNonLatinNoItalicScript(text.first { isNonLatinNoItalicScript(it) || isLatinLetter(it) })
        val sb = StringBuilder()

        for (ch in text) {
            val chMode = when {
                isNonLatinNoItalicScript(ch) -> true
                isLatinLetter(ch) -> false
                else -> currentNeedsOblique
            }
            if (sb.isNotEmpty() && chMode != currentNeedsOblique) {
                runs.add(ObliqueTextRun(sb.toString(), currentNeedsOblique))
                sb.setLength(0)
            }
            currentNeedsOblique = chMode
            sb.append(ch)
        }
        if (sb.isNotEmpty()) {
            runs.add(ObliqueTextRun(sb.toString(), currentNeedsOblique))
        }
        return runs
    }
}
