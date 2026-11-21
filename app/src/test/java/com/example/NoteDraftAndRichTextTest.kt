package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.model.NoteEntity
import com.example.data.model.NotePagesCodec
import com.example.data.model.RichFormatAction
import com.example.data.model.RichTextCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoteDraftAndRichTextTest {

    @Test
    fun `removePageSafely deletes page 2 of 2 cleanly and clamps active index`() {
        val pages = listOf("Page 1 body", "Page 2 body")
        val (remaining, nextIdx) = NotePagesCodec.removePageSafely(pages, index = 1, currentPage = 1)
        assertEquals(1, remaining.size)
        assertEquals("Page 1 body", remaining.first())
        assertEquals(0, nextIdx)

        // Cannot remove the last remaining page
        val (stillOne, stillIdx) = NotePagesCodec.removePageSafely(remaining, index = 0, currentPage = 0)
        assertEquals(1, stillOne.size)
        assertEquals(0, stillIdx)
    }

    @Test
    fun `removePageSafely deletes first page and shifts active index safely`() {
        val pages = listOf("One", "Two", "Three")
        val (afterRemove, nextIdx) = NotePagesCodec.removePageSafely(pages, index = 0, currentPage = 2)
        assertEquals(listOf("Two", "Three"), afterRemove)
        assertEquals(1, nextIdx)
    }

    @Test
    fun `per-page titles encode and decode cleanly with special characters and snippets`() {
        val pages = listOf(
            NotePagesCodec.DraftPage(title = "مقدمة \\ [هام]]", body = "**مرحبا** بكم"),
            NotePagesCodec.DraftPage(title = "صفحة ثانية", body = "| الاسم | الحالة |\n| --- | --- |\n| أحمد | جاهز |"),
            NotePagesCodec.DraftPage(title = "", body = "<u>تسطير</u> فقط")
        )
        val (content, pagesJson) = NotePagesCodec.encodeDraftPages(pages)
        val decoded = NotePagesCodec.decodeDraftPages(content, pagesJson)

        assertEquals(3, decoded.size)
        assertEquals("مقدمة \\ [هام]]", decoded[0].title)
        assertEquals("**مرحبا** بكم", decoded[0].body)
        assertEquals("صفحة ثانية", decoded[1].title)
        assertTrue(decoded[1].body.contains("| الاسم | الحالة |"))
        assertEquals("", decoded[2].title)
        assertEquals("<u>تسطير</u> فقط", decoded[2].body)

        val snippet = NotePagesCodec.plainTextSnippet(content)
        assertFalse(snippet.contains("FLUFFY_PAGE_TITLE"))
        assertFalse(snippet.contains("**"))
        assertTrue(snippet.contains("مرحبا بكم"))

        val note = NoteEntity(title = "مسودة", content = content, pagesJson = pagesJson)
        val searchable = NotePagesCodec.searchableBody(note)
        assertTrue(searchable.contains("صفحة ثانية"))
        assertTrue(searchable.contains("تسطير فقط"))
        assertFalse(searchable.contains("FLUFFY_PAGE_TITLE"))
    }

    @Test
    fun `rich actions toggle bold italic underline and block types cleanly`() {
        val initial = TextFieldValue("hello world", TextRange(0, 5))

        // Bold
        val bolded = RichTextCodec.applyRichAction(initial, RichFormatAction.BOLD)
        assertEquals("**hello** world", bolded.text)
        assertTrue(RichFormatAction.BOLD in RichTextCodec.inspectActiveStyles(bolded.text, bolded.selection))

        // Italic on top of Bold -> ***hello***
        val boldItalic = RichTextCodec.applyRichAction(bolded, RichFormatAction.ITALIC)
        assertEquals("***hello*** world", boldItalic.text)

        // Toggle Italic off without breaking Bold -> **hello**
        val onlyBold = RichTextCodec.applyRichAction(boldItalic, RichFormatAction.ITALIC)
        assertEquals("**hello** world", onlyBold.text)

        // Underline
        val underlined = RichTextCodec.applyRichAction(
            TextFieldValue("كلمة", TextRange(0, 4)),
            RichFormatAction.UNDERLINE
        )
        assertEquals("<u>كلمة</u>", underlined.text)
        assertTrue(RichFormatAction.UNDERLINE in RichTextCodec.inspectActiveStyles(underlined.text, underlined.selection))

        // Toggle Underline off
        val unUnderlined = RichTextCodec.applyRichAction(underlined, RichFormatAction.UNDERLINE)
        assertEquals("كلمة", unUnderlined.text)

        // Headings, Checklist, Callout, Divider
        val h1 = RichTextCodec.applyRichAction(TextFieldValue("Title", TextRange(5)), RichFormatAction.H1)
        assertEquals("# Title", h1.text)
        val h2 = RichTextCodec.applyRichAction(h1, RichFormatAction.H2)
        assertEquals("## Title", h2.text)
        val todo = RichTextCodec.applyRichAction(h2, RichFormatAction.CHECKBOX)
        assertEquals("- [ ] Title", todo.text)
        val callout = RichTextCodec.applyRichAction(todo, RichFormatAction.CALLOUT)
        assertEquals("> 💡 Title", callout.text)
        val cleared = RichTextCodec.applyRichAction(callout, RichFormatAction.CLEAR_FORMATTING)
        assertEquals("Title", cleared.text)
    }

    @Test
    fun `markdown tables serialize parse replace and delete accurately`() {
        val md = RichTextCodec.serializeTable(
            listOf(
                listOf("البند", "القيمة"),
                listOf("النفقات", "1200"),
                listOf("الادخار", "800")
            )
        )
        val inserted = RichTextCodec.insertTableAtSelection("ملاحظة أولى", TextRange(11), md)
        val tables = RichTextCodec.parseAllTables(inserted.text)
        assertEquals(1, tables.size)
        assertEquals(3, tables[0].rowCount)
        assertEquals(2, tables[0].colCount)
        assertEquals("النفقات", tables[0].rows[1][0])

        val updatedMd = RichTextCodec.serializeTable(
            listOf(
                listOf("Item", "Cost"),
                listOf("Book", "20")
            )
        )
        val replaced = RichTextCodec.replaceTableAtRange(
            text = inserted.text,
            startOffset = tables[0].startOffset,
            endOffset = tables[0].endOffset,
            tableMarkdown = updatedMd
        )
        val reparsed = RichTextCodec.parseAllTables(replaced.text)
        assertEquals(1, reparsed.size)
        assertEquals(2, reparsed[0].rowCount)
        assertEquals("Book", reparsed[0].rows[1][0])

        val afterDelete = RichTextCodec.deleteTableAtRange(
            replaced.text,
            reparsed[0].startOffset,
            reparsed[0].endOffset
        )
        assertTrue(RichTextCodec.parseAllTables(afterDelete).isEmpty())
        assertTrue(afterDelete.contains("ملاحظة أولى"))
    }

    @Test
    fun `transformForEditor maintains valid monotonic bidirectional offsets on mixed arabic and rich syntax`() {
        val samples = listOf(
            "# عنوان **مهم** جداً\n*مائل* و <u>تسطير</u> و ~~مشطوب~~ و ==مميز==\n- [ ] مهمة\n- [x] مكتملة\n> 💡 تنبيه\n> اقتباس\n```\ncode()\n```",
            "**<u>*متداخل*</u>** 👍🎉",
            "**** `` <u></u>",
            "1. أولاً **خط عريض**\n2. ثانياً *خط مائل*"
        )

        for (raw in samples) {
            val transformed = RichTextCodec.transformForEditor(
                raw = raw,
                accentColor = Color(0xFF8B5CF6),
                metaColor = Color(0xFF94A3B8),
                isDark = true
            )
            val transLen = transformed.text.length
            val mapping = transformed.offsetMapping

            var prevTrans = 0
            for (origIdx in 0..raw.length) {
                val t = mapping.originalToTransformed(origIdx)
                assertTrue("origToTrans($origIdx)=$t out of 0..$transLen for: $raw", t in 0..transLen)
                assertTrue("origToTrans not monotonic at $origIdx: $t < $prevTrans", t >= prevTrans)
                prevTrans = t
            }

            var prevOrig = 0
            for (transIdx in 0..transLen) {
                val o = mapping.transformedToOriginal(transIdx)
                assertTrue("transToOrig($transIdx)=$o out of 0..${raw.length} for: $raw", o in 0..raw.length)
                assertTrue("transToOrig not monotonic at $transIdx: $o < $prevOrig", o >= prevOrig)
                prevOrig = o
            }
        }
    }
}
