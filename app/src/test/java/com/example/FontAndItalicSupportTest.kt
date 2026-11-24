package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import com.example.data.model.RichBlock
import com.example.data.model.RichTextCodec
import com.example.ui.theme.AppFontCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FontAndItalicSupportTest {

    @Test
    fun `font catalog resolves sans_serif system families and bundled studio fonts`() {
        val ids = AppFontCatalog.allFonts.map { it.id }
        assertTrue("sans_serif" in ids)
        assertTrue("default_system" in ids)
        assertTrue("serif" in ids)
        assertTrue("monospace" in ids)
        assertTrue("cursive" in ids)
        assertTrue("cairo" in ids)
        assertTrue("tajawal" in ids)
        assertTrue("amiri" in ids)
        assertTrue("inter" in ids)
        assertTrue("open_sans" in ids)
        assertTrue("playfair_display" in ids)
        assertTrue("roboto_mono" in ids)
        assertTrue("dancing_script" in ids)

        assertEquals(FontFamily.SansSerif, AppFontCatalog.resolve("sans_serif").fontFamily)
        assertEquals(FontFamily.Serif, AppFontCatalog.resolve("serif").fontFamily)
        assertEquals(FontFamily.Monospace, AppFontCatalog.resolve("monospace").fontFamily)
        assertEquals(FontFamily.Cursive, AppFontCatalog.resolve("cursive").fontFamily)
        assertEquals("sans_serif", AppFontCatalog.resolve("unknown_font_id").id)
    }

    @Test
    fun `splitObliqueRuns synthesizes slant for Arabic under SansSerif and full text under Cairo and Monospace`() {
        val sansSerif = AppFontCatalog.resolve("sans_serif")
        val arabicRuns = AppFontCatalog.splitObliqueRuns("مرحبا بكم", sansSerif)
        assertEquals(1, arabicRuns.size)
        assertTrue(arabicRuns.first().needsObliqueSynthesis)

        val latinRuns = AppFontCatalog.splitObliqueRuns("Hello World", sansSerif)
        assertEquals(1, latinRuns.size)
        assertFalse(latinRuns.first().needsObliqueSynthesis)

        val mixedRuns = AppFontCatalog.splitObliqueRuns("Hello مرحبا بكم", sansSerif)
        assertEquals(2, mixedRuns.size)
        assertFalse(mixedRuns[0].needsObliqueSynthesis)
        assertTrue(mixedRuns[1].needsObliqueSynthesis)

        val cairo = AppFontCatalog.resolve("cairo")
        val cairoLatinRuns = AppFontCatalog.splitObliqueRuns("Hello World", cairo)
        assertEquals(1, cairoLatinRuns.size)
        assertTrue(cairoLatinRuns.first().needsObliqueSynthesis)

        val mono = AppFontCatalog.resolve("monospace")
        val monoRuns = AppFontCatalog.splitObliqueRuns("Code Sample", mono)
        assertEquals(1, monoRuns.size)
        assertTrue(monoRuns.first().needsObliqueSynthesis)
    }

    @Test
    fun `transformForEditor and parseRichBlocks emit oblique annotations for Arabic italic and quotes with intact OffsetMapping`() {
        val sansSerif = AppFontCatalog.resolve("sans_serif")
        val raw = "نص *مائل عربي* و *English*\n> اقتباس مائل"
        val transformed = RichTextCodec.transformForEditor(
            raw = raw,
            accentColor = Color.Cyan,
            metaColor = Color.Gray,
            isDark = true,
            fontOption = sansSerif
        )

        val annotated = transformed.text
        val obliqueAnns = annotated.getStringAnnotations(RichTextCodec.OBLIQUE_SPAN_TAG, 0, annotated.length)
        assertTrue(obliqueAnns.any { annotated.text.substring(it.start, it.end).contains("مائل عربي") })
        assertTrue(obliqueAnns.any { annotated.text.substring(it.start, it.end).contains("اقتباس مائل") })
        assertFalse(obliqueAnns.any { annotated.text.substring(it.start, it.end).contains("English") })

        val englishStart = annotated.text.indexOf("English")
        val englishEnd = englishStart + "English".length
        assertTrue(
            annotated.spanStyles.any {
                it.start <= englishStart && it.end >= englishEnd && it.item.fontStyle == FontStyle.Italic
            }
        )

        for (i in 0..raw.length) {
            val transIdx = transformed.offsetMapping.originalToTransformed(i)
            assertTrue(transIdx in 0..annotated.length)
            val origIdx = transformed.offsetMapping.transformedToOriginal(transIdx)
            assertTrue(origIdx in 0..raw.length)
        }

        val blocks = RichTextCodec.parseRichBlocks(
            rawText = raw,
            accentColor = Color.Cyan,
            isDark = true,
            fontOption = AppFontCatalog.resolve("cairo")
        )
        assertEquals(2, blocks.size)
        val p = blocks[0] as RichBlock.Paragraph
        val pOblique = p.text.getStringAnnotations(RichTextCodec.OBLIQUE_SPAN_TAG, 0, p.text.length)
        assertTrue(pOblique.any { p.text.text.substring(it.start, it.end).contains("مائل عربي") })
        assertTrue(pOblique.any { p.text.text.substring(it.start, it.end).contains("English") })

        val q = blocks[1] as RichBlock.Quote
        val qOblique = q.text.getStringAnnotations(RichTextCodec.OBLIQUE_SPAN_TAG, 0, q.text.length)
        assertTrue(qOblique.any { q.text.text.substring(it.start, it.end).contains("اقتباس مائل") })
    }
}
