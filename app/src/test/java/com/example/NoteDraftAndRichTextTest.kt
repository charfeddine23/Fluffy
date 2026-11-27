package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.model.InsertFeatureType
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

    @Test
    fun `writing mode formatting toggles strikethrough highlight H3 and 19 insert features cleanly`() {
        assertEquals(19, InsertFeatureType.entries.size)

        val strike = RichTextCodec.applyRichAction(
            TextFieldValue("تشطيب", TextRange(0, 5)),
            RichFormatAction.STRIKETHROUGH
        )
        assertEquals("~~تشطيب~~", strike.text)
        assertTrue(RichFormatAction.STRIKETHROUGH in RichTextCodec.inspectActiveStyles(strike.text, strike.selection))

        val highlight = RichTextCodec.applyRichAction(
            TextFieldValue("مهم", TextRange(0, 3)),
            RichFormatAction.HIGHLIGHT
        )
        assertEquals("==مهم==", highlight.text)
        assertTrue(RichFormatAction.HIGHLIGHT in RichTextCodec.inspectActiveStyles(highlight.text, highlight.selection))

        val h3 = RichTextCodec.applyRichAction(TextFieldValue("فرعي", TextRange(4)), RichFormatAction.H3)
        assertEquals("### فرعي", h3.text)
        assertTrue(RichFormatAction.H3 in RichTextCodec.inspectActiveStyles(h3.text, h3.selection))

        val progress = RichTextCodec.buildProgressBarSnippet(80, isArabic = true, label = "الإنجاز")
        assertTrue(progress.contains("80%"))
        assertTrue(progress.contains("الإنجاز"))
        val badge = RichTextCodec.buildStatusBadgeSnippet("✅", "مكتمل")
        assertEquals("【✅ مكتمل】 ", badge)
        assertTrue(RichTextCodec.buildInsertSnippet(InsertFeatureType.DATE_STAMP, isArabic = true, nowMillis = 0L).startsWith("📅"))
        assertTrue(RichTextCodec.buildInsertSnippet(InsertFeatureType.MEETING_TEMPLATE, isArabic = false).contains("Meeting Notes"))
    }

    @Test
    fun `pending italic and bold+italic typing wraps seamlessly without empty marker bugs`() {
        // Toggle Italic on empty page -> zero dangling markers injected
        val startState = RichTextCodec.toggleFormatForEditor(
            value = TextFieldValue("", TextRange(0)),
            action = RichFormatAction.ITALIC,
            pendingStyles = emptySet()
        )
        assertEquals("", startState.fieldValue.text)
        assertEquals(setOf(RichFormatAction.ITALIC), startState.pendingStyles)

        // Type Arabic word character-by-character -> merges into single *كتاب* span
        var curVal = startState.fieldValue
        var pending = startState.pendingStyles
        for (ch in listOf('ك', 'ت', 'ا', 'ب')) {
            val typed = curVal.text.substring(0, curVal.selection.start) + ch + curVal.text.substring(curVal.selection.end)
            val outcome = RichTextCodec.applyPendingStylesOnEdit(
                prevText = curVal.text,
                nextVal = TextFieldValue(typed, TextRange(curVal.selection.start + 1)),
                pendingStyles = pending
            )
            curVal = outcome.fieldValue
            pending = outcome.pendingStyles
        }
        assertEquals("*كتاب*", curVal.text)

        // Toggle Italic OFF at end of *كتاب* -> plain text follows cleanly
        val turnOff = RichTextCodec.toggleFormatForEditor(curVal, RichFormatAction.ITALIC, pending)
        assertTrue(turnOff.pendingStyles.isEmpty())
        val afterPlain = RichTextCodec.applyPendingStylesOnEdit(
            prevText = turnOff.fieldValue.text,
            nextVal = TextFieldValue(turnOff.fieldValue.text + " جديد", TextRange(turnOff.fieldValue.text.length + 5)),
            pendingStyles = turnOff.pendingStyles
        )
        assertEquals("*كتاب* جديد", afterPlain.fieldValue.text)

        // Combined Bold + Italic pending typing -> ***hi***
        val biStart = RichTextCodec.toggleFormatForEditor(
            TextFieldValue("", TextRange(0)),
            RichFormatAction.BOLD,
            setOf(RichFormatAction.ITALIC)
        )
        assertEquals(setOf(RichFormatAction.ITALIC, RichFormatAction.BOLD), biStart.pendingStyles)
        var biVal = biStart.fieldValue
        var biPending = biStart.pendingStyles
        for (ch in listOf('h', 'i')) {
            val typed = biVal.text + ch
            val res = RichTextCodec.applyPendingStylesOnEdit(
                prevText = biVal.text,
                nextVal = TextFieldValue(typed, TextRange(typed.length)),
                pendingStyles = biPending
            )
            biVal = res.fieldValue
            biPending = res.pendingStyles
        }
        assertEquals("***hi***", biVal.text)

        // Adding Bold inside an existing Italic word preserves both *ital**x**ic* cleanly
        val nestedRes = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "*italic*",
            nextVal = TextFieldValue("*italxic*", TextRange(6)),
            pendingStyles = setOf(RichFormatAction.BOLD)
        )
        assertEquals("*ital**x**ic*", nestedRes.fieldValue.text)
    }

    @Test
    fun `splitPageIntoEditorSegments isolates pipe tables and divider visual transform stays monotonic`() {
        val rawPage = "فقرة أولى\n\n| العمود 1 | العمود 2 |\n| --- | --- |\n| أ | ب |\nفقرة ثانية"
        val segments = RichTextCodec.splitPageIntoEditorSegments(rawPage)
        val textSegments = segments.filterIsInstance<com.example.data.model.EditorPageSegment.TextSegment>()
        val tableSegments = segments.filterIsInstance<com.example.data.model.EditorPageSegment.TableSegment>()

        assertEquals(1, tableSegments.size)
        assertEquals(2, textSegments.size)
        assertFalse(textSegments.any { it.text.contains("|") })
        assertTrue(textSegments[0].text.contains("فقرة أولى"))
        assertEquals("فقرة ثانية", textSegments[1].text)

        // Updating tail segment preserves table intact
        val tail = textSegments[1]
        val updatedPage = RichTextCodec.replaceSegmentInPage(
            rawText = rawPage,
            segStart = tail.startOffset,
            segEnd = tail.endOffset,
            newSegmentText = "فقرة محدثة",
            hasTableBefore = true,
            hasTableAfter = false
        )
        assertEquals(1, RichTextCodec.parseAllTables(updatedPage).size)
        assertTrue(updatedPage.endsWith("فقرة محدثة"))

        // Divider + code fence transformForEditor stays strictly monotonic and hides raw tokens
        val specialRaw = "أعلى\n---\n```\ncode()\n```\nأسفل"
        val transformed = RichTextCodec.transformForEditor(
            raw = specialRaw,
            accentColor = Color(0xFF8B5CF6),
            metaColor = Color(0xFF94A3B8),
            isDark = true
        )
        assertFalse(transformed.text.text.contains("---"))
        assertFalse(transformed.text.text.contains("```"))
        assertTrue(transformed.text.text.contains("────────────────────"))
        val mapping = transformed.offsetMapping
        var prevT = 0
        for (o in 0..specialRaw.length) {
            val t = mapping.originalToTransformed(o)
            assertTrue(t >= prevT && t <= transformed.text.length)
            prevT = t
        }
    }

    @Test
    fun `italic and bold italic emit FontSynthesis All and backspace leaves zero raw symbols`() {
        val raw = "***عربي مائل وعريض*** و *مائل*"
        val transformed = RichTextCodec.transformForEditor(
            raw = raw,
            accentColor = Color(0xFF8B5CF6),
            metaColor = Color(0xFF94A3B8),
            isDark = true
        )
        val rendered = transformed.text
        assertFalse(rendered.text.contains("*"))
        val obliqueAnnotations = rendered.getStringAnnotations(RichTextCodec.OBLIQUE_SPAN_TAG, 0, rendered.length)
        assertTrue(obliqueAnnotations.any { rendered.text.substring(it.start, it.end) == "عربي مائل وعريض" })
        assertTrue(obliqueAnnotations.any { rendered.text.substring(it.start, it.end) == "مائل" })
        assertTrue(rendered.spanStyles.any {
            (it.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold ||
                it.item.fontWeight == androidx.compose.ui.text.font.FontWeight.ExtraBold) &&
                it.item.fontSynthesis == androidx.compose.ui.text.font.FontSynthesis.All
        })

        // Simulate Backspace removing last character of <u>abc</u> -> <u>abc</u -> cleaned cleanly
        val afterUnderlineBackspace = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "<u>abc</u>",
            nextVal = TextFieldValue("<u>abc</u", TextRange(9)),
            pendingStyles = emptySet()
        )
        assertFalse(afterUnderlineBackspace.fieldValue.text.contains("<u>"))
        assertFalse(afterUnderlineBackspace.fieldValue.text.contains("</u>"))
        assertFalse(afterUnderlineBackspace.fieldValue.text.contains("</u"))

        // Simulate Backspace inside **bold** leaving dangling stars
        val afterBoldBackspace = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "**abc**",
            nextVal = TextFieldValue("**abc*", TextRange(6)),
            pendingStyles = emptySet()
        )
        assertFalse(afterBoldBackspace.fieldValue.text.contains("*"))

        // Simulate Backspace on damaged divider `--` -> removed cleanly
        val afterDividerBackspace = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "نص\n---\nآخر",
            nextVal = TextFieldValue("نص\n--\nآخر", TextRange(5)),
            pendingStyles = emptySet()
        )
        assertFalse(afterDividerBackspace.fieldValue.text.contains("--"))

        // Simulate selection deletion cutting across ~~strike~~ and ==highlight== -> zero stray ~ or =
        val afterStrikeCut = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "قبل ~~شطب~~ بعد",
            nextVal = TextFieldValue("قبل ~~شطب~ بعد", TextRange(10)),
            pendingStyles = emptySet()
        )
        assertFalse(afterStrikeCut.fieldValue.text.contains("~"))

        val afterHighlightCut = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "==مهم==",
            nextVal = TextFieldValue("==مهم=", TextRange(6)),
            pendingStyles = emptySet()
        )
        assertFalse(afterHighlightCut.fieldValue.text.contains("="))

        // Simulate multi-line deletion clipping part of `- [ ] ` prefix -> zero stray ` ] `
        val afterMultilinePrefixCut = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "أول\n- [ ] مهمة",
            nextVal = TextFieldValue("أول ] مهمة", TextRange(3)),
            pendingStyles = emptySet()
        )
        assertFalse(afterMultilinePrefixCut.fieldValue.text.contains("]"))
    }

    @Test
    fun `italic selection normalization arabic diacritics IME recovery and EditorFontFamily spans work cleanly`() {
        // 1. Visual selection spanning past closing `*` into plain word (`hello* world` -> `*hello world*`)
        val extendPlain = RichTextCodec.applyRichAction(
            TextFieldValue("*hello* world", TextRange(1, 13)),
            RichFormatAction.ITALIC
        )
        assertEquals("*hello world*", extendPlain.text)

        // 2. All non-whitespace words already italic (`*hello* *world*`) -> unwraps cleanly
        val unwrapMulti = RichTextCodec.applyRichAction(
            TextFieldValue("*hello* *world*", TextRange(1, 15)),
            RichFormatAction.ITALIC
        )
        assertEquals("hello world", unwrapMulti.text)

        // 3. Collapsed cursor inside Arabic word containing Tashkeel toggles entire word without splitting
        val arabicWord = "كِتَابٌ جديد"
        val toggleArabicTashkeel = RichTextCodec.applyRichAction(
            TextFieldValue(arabicWord, TextRange(3)),
            RichFormatAction.ITALIC
        )
        assertEquals("*كِتَابٌ* جديد", toggleArabicTashkeel.text)

        // 4. Android IME composing replacement dropping closing `*` recovers seamlessly while pending
        val imePendingItalic = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "*ك*",
            nextVal = TextFieldValue("*كتاب", TextRange(5)),
            pendingStyles = setOf(RichFormatAction.ITALIC)
        )
        assertEquals("*كتاب*", imePendingItalic.fieldValue.text)

        // 5. Android Gboard autocorrect replacing word + closing `*` without pending styles preserves italic
        val imeAutocorrectPlain = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "*helo* ok",
            nextVal = TextFieldValue("*hello ok", TextRange(6)),
            pendingStyles = emptySet()
        )
        assertEquals("*hello* ok", imeAutocorrectPlain.fieldValue.text)

        // 6. Oblique-synthesis annotations + EditorFontFamily + FontSynthesis.All emitted for both Latin and Arabic
        val transformed = RichTextCodec.transformForEditor(
            raw = "*italic* و *مائل*",
            accentColor = Color(0xFF8B5CF6),
            metaColor = Color(0xFF94A3B8),
            isDark = true
        )
        val obliqueAnns = transformed.text.getStringAnnotations(
            RichTextCodec.OBLIQUE_SPAN_TAG,
            0,
            transformed.text.length
        )
        assertEquals(2, obliqueAnns.size)
        assertEquals("italic", transformed.text.text.substring(obliqueAnns[0].start, obliqueAnns[0].end))
        assertEquals("مائل", transformed.text.text.substring(obliqueAnns[1].start, obliqueAnns[1].end))
        assertTrue(obliqueAnns.all { ann ->
            transformed.text.spanStyles.any {
                it.start == ann.start &&
                    it.end == ann.end &&
                    it.item.fontFamily == com.example.ui.theme.EditorFontFamily &&
                    it.item.fontSynthesis == androidx.compose.ui.text.font.FontSynthesis.All
            }
        })

        // 7. Partial bold + text italic wrapping avoids `***` collision
        val mixedBoldItalic = RichTextCodec.applyRichAction(
            TextFieldValue("**bold** text", TextRange(0, 13)),
            RichFormatAction.ITALIC
        )
        assertEquals("***bold*** *text*", mixedBoldItalic.text)

        // 8. Replacing table preserves clean newline separation with adjacent text
        val safeReplaced = RichTextCodec.replaceTableAtRange(
            text = "top| A | B |\n| --- | --- |\n| 1 | 2 |bottom",
            startOffset = 3,
            endOffset = 36,
            tableMarkdown = "| X | Y |\n| --- | --- |\n| 9 | 8 |"
        )
        assertTrue(safeReplaced.text.startsWith("top\n| X | Y |"))
        assertTrue(safeReplaced.text.endsWith("| 9 | 8 |\nbottom"))
    }

    @Test
    fun `todo bullet and numbered lists auto-continue on Enter and exit with single Backspace`() {
        // 1. Todo box: pressing Enter on a todo item adds "- [ ] " on the next line
        val todoEnter = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "- [ ] Buy milk",
            nextVal = TextFieldValue("- [ ] Buy milk\n", TextRange(15)),
            pendingStyles = emptySet()
        )
        assertEquals("- [ ] Buy milk\n- [ ] ", todoEnter.fieldValue.text)
        assertEquals(21, todoEnter.fieldValue.selection.start)

        // Pressing Enter immediately after inserting a single empty Todo Box also adds the 2nd box
        val emptyTodoEnter = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "- [ ] ",
            nextVal = TextFieldValue("- [ ] \n", TextRange(7)),
            pendingStyles = emptySet()
        )
        assertEquals("- [ ] \n- [ ] ", emptyTodoEnter.fieldValue.text)
        assertEquals(13, emptyTodoEnter.fieldValue.selection.start)

        // Single Backspace on the newly created "- [ ] " removes the whole box prefix in one tap
        val todoSingleBackspace = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "- [ ] Buy milk\n- [ ] ",
            nextVal = TextFieldValue("- [ ] Buy milk\n- [ ]", TextRange(20)),
            pendingStyles = emptySet()
        )
        assertEquals("- [ ] Buy milk\n", todoSingleBackspace.fieldValue.text)
        assertEquals(15, todoSingleBackspace.fieldValue.selection.start)

        // Pressing Enter again from that cleared plain line does NOT add another todo box
        val plainEnterAfterDelete = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "- [ ] Buy milk\n",
            nextVal = TextFieldValue("- [ ] Buy milk\n\n", TextRange(16)),
            pendingStyles = emptySet()
        )
        assertEquals("- [ ] Buy milk\n\n", plainEnterAfterDelete.fieldValue.text)
        assertEquals(16, plainEnterAfterDelete.fieldValue.selection.start)

        // 2. Bullet list: pressing Enter adds "- " and single Backspace removes it
        val bulletEnter = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "- First point",
            nextVal = TextFieldValue("- First point\n", TextRange(14)),
            pendingStyles = emptySet()
        )
        assertEquals("- First point\n- ", bulletEnter.fieldValue.text)
        assertEquals(16, bulletEnter.fieldValue.selection.start)

        val bulletBackspace = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "- First point\n- ",
            nextVal = TextFieldValue("- First point\n-", TextRange(15)),
            pendingStyles = emptySet()
        )
        assertEquals("- First point\n", bulletBackspace.fieldValue.text)

        // 3. Numbered list: pressing Enter increments the number ("1. " -> "2. "), single Backspace clears it
        val numEnter = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "1. Step one",
            nextVal = TextFieldValue("1. Step one\n", TextRange(12)),
            pendingStyles = emptySet()
        )
        assertEquals("1. Step one\n2. ", numEnter.fieldValue.text)
        assertEquals(15, numEnter.fieldValue.selection.start)

        val numBackspace = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "1. Step one\n2. ",
            nextVal = TextFieldValue("1. Step one\n2.", TextRange(14)),
            pendingStyles = emptySet()
        )
        assertEquals("1. Step one\n", numBackspace.fieldValue.text)
    }

    @Test
    fun `buildStarRatingSnippet formats chosen star counts cleanly`() {
        assertEquals("★☆☆☆☆ (1/5) ", RichTextCodec.buildStarRatingSnippet(1))
        assertEquals("★★★☆☆ (3/5) ", RichTextCodec.buildStarRatingSnippet(3))
        assertEquals("★★★★★ (5/5) ", RichTextCodec.buildStarRatingSnippet(5))
    }

    @Test
    fun `deleting line 1 leaving leading newline and cursor at zero never crashes`() {
        val prev = "كلمة أولى\nالسطر الثاني"
        val next = TextFieldValue("\nالسطر الثاني", TextRange(0))

        // Must never throw StringIndexOutOfBoundsException in applyPendingStylesOnEdit or inspectActiveStyles
        val editResult = RichTextCodec.applyPendingStylesOnEdit(
            prevText = prev,
            nextVal = next,
            pendingStyles = emptySet()
        )
        assertEquals("\nالسطر الثاني", editResult.fieldValue.text)
        assertEquals(TextRange(0), editResult.fieldValue.selection)

        val stylesAtZero = RichTextCodec.inspectActiveStyles(
            text = editResult.fieldValue.text,
            selection = TextRange(0)
        )
        assertTrue(stylesAtZero.isEmpty())

        // Deleting everything to empty string must also succeed cleanly
        val cleared = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "\nالسطر الثاني",
            nextVal = TextFieldValue("", TextRange(0)),
            pendingStyles = emptySet()
        )
        assertEquals("", cleared.fieldValue.text)
        assertEquals(TextRange(0), cleared.fieldValue.selection)
        assertTrue(RichTextCodec.inspectActiveStyles("", TextRange(0)).isEmpty())
    }

    @Test
    fun `deleting words with active IME composition preserves composition verbatim when no markers changed`() {
        val nextWithComp = TextFieldValue(
            text = "مرحبا بالعالم\nسطر ثاني",
            selection = TextRange(5),
            composition = TextRange(0, 5)
        )
        val outcome = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "مرحبا بالعالم الجميل\nسطر ثاني",
            nextVal = nextWithComp,
            pendingStyles = emptySet()
        )
        assertEquals(nextWithComp.text, outcome.fieldValue.text)
        assertEquals(nextWithComp.selection, outcome.fieldValue.selection)
        assertEquals(nextWithComp.composition, outcome.fieldValue.composition)
    }

    @Test
    fun `selecting and deleting entire prefixed line does not resurrect deleted text`() {
        val todoResult = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "- [ ] مهمة للحذف\nالسطر التالي",
            nextVal = TextFieldValue("\nالسطر التالي", TextRange(0)),
            pendingStyles = emptySet()
        )
        assertEquals("\nالسطر التالي", todoResult.fieldValue.text)
        assertFalse(todoResult.fieldValue.text.contains("مهمة للحذف"))

        val bulletResult = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "- نقطة أولى\nنقطة ثانية",
            nextVal = TextFieldValue("\nنقطة ثانية", TextRange(0)),
            pendingStyles = emptySet()
        )
        assertEquals("\nنقطة ثانية", bulletResult.fieldValue.text)
        assertFalse(bulletResult.fieldValue.text.contains("نقطة أولى"))

        val numberedResult = RichTextCodec.applyPendingStylesOnEdit(
            prevText = "1. البند الأول\n2. البند الثاني",
            nextVal = TextFieldValue("\n2. البند الثاني", TextRange(0)),
            pendingStyles = emptySet()
        )
        assertEquals("\n2. البند الثاني", numberedResult.fieldValue.text)
        assertFalse(numberedResult.fieldValue.text.contains("البند الأول"))
    }

    @Test
    fun `transformForEditor remains monotonic on leading newline arabic emoji and damaged markers`() {
        val edgeSamples = listOf(
            "\nالسطر الثاني",
            "\n\n",
            "👍🎉\n**عريض**",
            "- [ ] مهمة\n"
        )
        for (raw in edgeSamples) {
            val transformed = RichTextCodec.transformForEditor(
                raw = raw,
                accentColor = Color(0xFF8B5CF6),
                metaColor = Color(0xFF94A3B8),
                isDark = true
            )
            val transLen = transformed.text.length
            val mapping = transformed.offsetMapping
            var prevT = 0
            for (o in 0..raw.length) {
                val t = mapping.originalToTransformed(o)
                assertTrue(t in prevT..transLen)
                prevT = t
            }
            var prevO = 0
            for (t in 0..transLen) {
                val o = mapping.transformedToOriginal(t)
                assertTrue(o in prevO..raw.length)
                prevO = o
            }
        }
    }
}

