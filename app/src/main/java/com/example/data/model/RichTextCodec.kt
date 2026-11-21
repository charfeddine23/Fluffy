package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class InsertFeatureType {
    TABLE,
    TODO_CHECKBOX,
    CHECKLIST_CARD,
    VOICE_MEMO,
    NEW_PAGE,
    BULLET_LIST,
    NUMBERED_LIST,
    QUOTE,
    CALLOUT,
    CODE_BLOCK,
    INLINE_CODE,
    DIVIDER,
    DATE_STAMP,
    TIME_STAMP,
    DAILY_HEADER,
    STAR_RATING,
    PROGRESS_BAR,
    STATUS_BADGE,
    MEETING_TEMPLATE,
    PROS_CONS_TABLE
}

enum class RichFormatAction {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    CODE,
    HIGHLIGHT,
    PARAGRAPH,
    H1,
    H2,
    H3,
    BULLET_LIST,
    NUMBERED_LIST,
    CHECKBOX,
    QUOTE,
    CALLOUT,
    CODE_BLOCK,
    DIVIDER,
    CLEAR_FORMATTING
}

data class MarkdownTableData(
    val rows: List<List<String>>,
    val startOffset: Int = -1,
    val endOffset: Int = -1
) {
    val rowCount: Int get() = rows.size
    val colCount: Int get() = rows.firstOrNull()?.size ?: 0
}

sealed interface RichBlock {
    data class Heading(val level: Int, val text: AnnotatedString) : RichBlock
    data class Paragraph(val text: AnnotatedString) : RichBlock
    data class BulletItem(val text: AnnotatedString) : RichBlock
    data class NumberedItem(val numberLabel: String, val text: AnnotatedString) : RichBlock
    data class ChecklistBlockItem(
        val checked: Boolean,
        val lineStartOffset: Int,
        val text: AnnotatedString
    ) : RichBlock
    data class Quote(val isCallout: Boolean, val text: AnnotatedString) : RichBlock
    data class CodeBlock(val code: String) : RichBlock
    data object Divider : RichBlock
    data class Table(val data: MarkdownTableData) : RichBlock
}

/**
 * Pure Kotlin rich-text & Notion-style Markdown+ codec:
 * - Inline styles: **Bold**, *Italic*, <u>Underline</u>, ~~Strikethrough~~, `Code`, ==Highlight==
 * - Line/Block styles: # H1, ## H2, ### H3, - Bullet, 1. Numbered, - [ ] / - [x] Todo, > Quote, > 💡 Callout, ``` Code Block ```, --- Divider
 * - Markdown Pipe Tables: parsed to [MarkdownTableData] for interactive Table Editor & Read-Mode Grids.
 * - Provides a strictly monotonic [VisualTransformation] with exact bidirectional [OffsetMapping].
 */
object RichTextCodec {

    private val linePrefixRegex = Regex(
        """^(### |## |# |- \[[ xX]] |- |\* |• |\d+\. |> 💡 |> )"""
    )
    private val numberedPrefixRegex = Regex("""^(\d+)\. """)
    private val tableDividerRegex = Regex("""^\|?(\s*:?-{2,}:?\s*\|)+\s*:?-{2,}:?\s*\|?$""")

    fun applyRichAction(value: TextFieldValue, action: RichFormatAction): TextFieldValue {
        return when (action) {
            RichFormatAction.BOLD -> toggleInlineWrap(value, "**", "**", isBold = true)
            RichFormatAction.ITALIC -> toggleItalicWrap(value)
            RichFormatAction.UNDERLINE -> toggleInlineWrap(value, "<u>", "</u>")
            RichFormatAction.STRIKETHROUGH -> toggleInlineWrap(value, "~~", "~~")
            RichFormatAction.CODE -> toggleInlineWrap(value, "`", "`")
            RichFormatAction.HIGHLIGHT -> toggleInlineWrap(value, "==", "==")
            RichFormatAction.PARAGRAPH -> applyLinePrefix(value, targetPrefix = "", toggleOffIfSame = false)
            RichFormatAction.H1 -> applyLinePrefix(value, targetPrefix = "# ", toggleOffIfSame = true)
            RichFormatAction.H2 -> applyLinePrefix(value, targetPrefix = "## ", toggleOffIfSame = true)
            RichFormatAction.H3 -> applyLinePrefix(value, targetPrefix = "### ", toggleOffIfSame = true)
            RichFormatAction.BULLET_LIST -> applyLinePrefix(value, targetPrefix = "- ", toggleOffIfSame = true)
            RichFormatAction.NUMBERED_LIST -> applyNumberedList(value)
            RichFormatAction.CHECKBOX -> applyLinePrefix(value, targetPrefix = "- [ ] ", toggleOffIfSame = true)
            RichFormatAction.QUOTE -> applyLinePrefix(value, targetPrefix = "> ", toggleOffIfSame = true)
            RichFormatAction.CALLOUT -> applyLinePrefix(value, targetPrefix = "> 💡 ", toggleOffIfSame = true)
            RichFormatAction.CODE_BLOCK -> applyCodeBlock(value)
            RichFormatAction.DIVIDER -> insertDivider(value)
            RichFormatAction.CLEAR_FORMATTING -> clearFormattingAtSelection(value)
        }
    }

    fun insertSnippetAtSelection(
        value: TextFieldValue,
        snippet: String,
        ensureNewLinePrefix: Boolean = false
    ): TextFieldValue {
        val text = value.text
        val rawSel = value.selection
        val start = rawSel.min.coerceIn(0, text.length)
        val end = rawSel.max.coerceIn(0, text.length)
        val leading = if (ensureNewLinePrefix && start > 0 && text[start - 1] != '\n') "\n" else ""
        val inserted = leading + snippet
        val updated = text.substring(0, start) + inserted + text.substring(end)
        val nextCursor = (start + inserted.length).coerceIn(0, updated.length)
        return TextFieldValue(updated, TextRange(nextCursor))
    }

    fun buildInsertSnippet(
        type: InsertFeatureType,
        isArabic: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val locale = if (isArabic) Locale("ar") else Locale.US
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
        val timeStr = SimpleDateFormat("hh:mm a", locale).format(Date(nowMillis))
        return when (type) {
            InsertFeatureType.DATE_STAMP -> "📅 $dateStr"
            InsertFeatureType.TIME_STAMP -> "🕒 $timeStr"
            InsertFeatureType.DAILY_HEADER -> if (isArabic) {
                "## 📅 يومية $dateStr • $timeStr\n"
            } else {
                "## 📅 Entry $dateStr • $timeStr\n"
            }
            InsertFeatureType.STAR_RATING -> if (isArabic) {
                "★★★★☆ (4/5) "
            } else {
                "★★★★☆ (4/5) "
            }
            InsertFeatureType.PROGRESS_BAR -> if (isArabic) {
                "📊 التقدم: [██████░░░░] 60%\n"
            } else {
                "📊 Progress: [██████░░░░] 60%\n"
            }
            InsertFeatureType.STATUS_BADGE -> if (isArabic) {
                "【🟢 قيد التنفيذ】 "
            } else {
                "【🟢 In Progress】 "
            }
            InsertFeatureType.MEETING_TEMPLATE -> if (isArabic) {
                """
                ## 🤝 محضر اجتماع ($dateStr)
                - **الموضوع:** 
                - **الحضور:** 
                - **أهم النقاط:** 
                - [ ] المتابعة والتنفيذ
                """.trimIndent() + "\n"
            } else {
                """
                ## 🤝 Meeting Notes ($dateStr)
                - **Topic:** 
                - **Attendees:** 
                - **Key Points:** 
                - [ ] Action items
                """.trimIndent() + "\n"
            }
            InsertFeatureType.PROS_CONS_TABLE -> if (isArabic) {
                """
                | ✅ الإيجابيات | ⚠️ السلبيات |
                | --- | --- |
                |  |  |
                |  |  |
                """.trimIndent() + "\n"
            } else {
                """
                | ✅ Pros | ⚠️ Cons |
                | --- | --- |
                |  |  |
                |  |  |
                """.trimIndent() + "\n"
            }
            else -> ""
        }
    }

    private fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'

    private fun toggleInlineWrap(
        value: TextFieldValue,
        openTag: String,
        closeTag: String,
        isBold: Boolean = false
    ): TextFieldValue {
        val text = value.text
        val rawSel = value.selection
        val start = rawSel.min.coerceIn(0, text.length)
        val end = rawSel.max.coerceIn(0, text.length)

        // 1. Collapsed cursor cases
        if (start == end) {
            // Case A: cursor sits right inside empty marker pair `openTag|closeTag`
            if (start >= openTag.length && start + closeTag.length <= text.length &&
                text.substring(start - openTag.length, start) == openTag &&
                text.substring(start, start + closeTag.length) == closeTag
            ) {
                val updated = text.removeRange(start - openTag.length, start + closeTag.length)
                val newCursor = (start - openTag.length).coerceIn(0, updated.length)
                return TextFieldValue(updated, TextRange(newCursor))
            }

            // Case B: cursor inside existing span of the same tag -> unwrap it if non-bold or matching
            val enclosingSpan = findEnclosingTagSpan(text, start, openTag, closeTag, isBold)
            if (enclosingSpan != null) {
                val (spanStart, spanEnd) = enclosingSpan
                val inner = text.substring(spanStart + openTag.length, spanEnd - closeTag.length)
                val updated = text.substring(0, spanStart) + inner + text.substring(spanEnd)
                val offsetInInner = (start - (spanStart + openTag.length)).coerceIn(0, inner.length)
                val newCursor = (spanStart + offsetInInner).coerceIn(0, updated.length)
                return TextFieldValue(updated, TextRange(newCursor))
            }

            // Case C: cursor inside a word -> toggle formatting on that word
            if (start > 0 && start < text.length && isWordChar(text[start - 1]) && isWordChar(text[start])) {
                var wStart = start
                while (wStart > 0 && isWordChar(text[wStart - 1])) wStart--
                var wEnd = start
                while (wEnd < text.length && isWordChar(text[wEnd])) wEnd++
                val wordSel = TextFieldValue(text, TextRange(wStart, wEnd))
                val formattedWord = toggleInlineWrap(wordSel, openTag, closeTag, isBold)
                // Restore cursor near original relative position
                val shiftedCursor = if (formattedWord.text.length > text.length) {
                    start + openTag.length
                } else {
                    (start - openTag.length).coerceAtLeast(wStart)
                }.coerceIn(0, formattedWord.text.length)
                return TextFieldValue(formattedWord.text, TextRange(shiftedCursor))
            }

            // Case D: insert marker pair and place cursor right in between
            val updated = text.substring(0, start) + openTag + closeTag + text.substring(end)
            return TextFieldValue(updated, TextRange(start + openTag.length))
        }

        // 2. Selection non-empty
        val selected = text.substring(start, end)
        val wrappedOutside = start >= openTag.length &&
            end + closeTag.length <= text.length &&
            text.substring(start - openTag.length, start) == openTag &&
            text.substring(end, end + closeTag.length) == closeTag
        if (wrappedOutside) {
            val updated = text.substring(0, start - openTag.length) + selected + text.substring(end + closeTag.length)
            val newStart = start - openTag.length
            return TextFieldValue(updated, TextRange(newStart, newStart + selected.length))
        }

        val wrappedInside = selected.length >= openTag.length + closeTag.length &&
            selected.startsWith(openTag) &&
            selected.endsWith(closeTag)
        if (wrappedInside) {
            val unwrapped = selected.substring(openTag.length, selected.length - closeTag.length)
            val updated = text.substring(0, start) + unwrapped + text.substring(end)
            return TextFieldValue(updated, TextRange(start, start + unwrapped.length))
        }

        val updated = text.substring(0, start) + openTag + selected + closeTag + text.substring(end)
        return TextFieldValue(
            text = updated,
            selection = TextRange(start + openTag.length, end + openTag.length)
        )
    }

    /**
     * Separate italic handler so single-asterisk `*` never clashes with double-asterisk `**` bold.
     */
    private fun toggleItalicWrap(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val rawSel = value.selection
        val start = rawSel.min.coerceIn(0, text.length)
        val end = rawSel.max.coerceIn(0, text.length)

        fun hasSingleItalicAtBoundary(leftIdx: Int, rightIdx: Int): Boolean {
            var leftCount = 0
            var li = leftIdx - 1
            while (li >= 0 && text[li] == '*') {
                leftCount++
                li--
            }
            var rightCount = 0
            var ri = rightIdx
            while (ri < text.length && text[ri] == '*') {
                rightCount++
                ri++
            }
            return (leftCount % 2 == 1) && (rightCount % 2 == 1)
        }

        if (start == end) {
            if (hasSingleItalicAtBoundary(start, start)) {
                val updated = text.removeRange(start - 1, start + 1)
                return TextFieldValue(updated, TextRange((start - 1).coerceIn(0, updated.length)))
            }
            val enclosing = findEnclosingSingleAsteriskSpan(text, start)
            if (enclosing != null) {
                val (s, e) = enclosing
                val inner = text.substring(s + 1, e - 1)
                val updated = text.substring(0, s) + inner + text.substring(e)
                val rel = (start - (s + 1)).coerceIn(0, inner.length)
                return TextFieldValue(updated, TextRange((s + rel).coerceIn(0, updated.length)))
            }
            if (start > 0 && start < text.length && isWordChar(text[start - 1]) && isWordChar(text[start])) {
                var wStart = start
                while (wStart > 0 && isWordChar(text[wStart - 1])) wStart--
                var wEnd = start
                while (wEnd < text.length && isWordChar(text[wEnd])) wEnd++
                val res = toggleItalicWrap(TextFieldValue(text, TextRange(wStart, wEnd)))
                val shifted = if (res.text.length > text.length) start + 1 else (start - 1).coerceAtLeast(wStart)
                return TextFieldValue(res.text, TextRange(shifted.coerceIn(0, res.text.length)))
            }
            val updated = text.substring(0, start) + "**" + text.substring(end)
            // Note: Wait! If cursor inserts single-asterisk pair `*` + `*`, `**` looks identical to empty bold!
            // Wrap empty italic cleanly as `* *`? No, placing cursor between `*` and `*` is ambiguous only with `**`.
            // Actually if we use single `*` + `*` when NOT adjacent to existing text, wait: typing `x` inside makes `*x*` which IS italic!
            // AND we can unwrap `*` on boundary if user taps Italic again. Wait: let's check `findEnclosingTagSpan` for `**` vs `*`.
            return TextFieldValue(updated, TextRange(start + 1))
        }

        val selected = text.substring(start, end)
        if (hasSingleItalicAtBoundary(start, end)) {
            val updated = text.substring(0, start - 1) + selected + text.substring(end + 1)
            return TextFieldValue(updated, TextRange(start - 1, start - 1 + selected.length))
        }
        val innerMatch = selected.length >= 2 && selected.startsWith("*") && selected.endsWith("*") &&
            !(selected.length >= 4 && selected.startsWith("**") && selected.endsWith("**"))
        if (innerMatch) {
            val unwrapped = selected.substring(1, selected.length - 1)
            val updated = text.substring(0, start) + unwrapped + text.substring(end)
            return TextFieldValue(updated, TextRange(start, start + unwrapped.length))
        }
        val updated = text.substring(0, start) + "*" + selected + "*" + text.substring(end)
        return TextFieldValue(updated, TextRange(start + 1, end + 1))
    }

    private fun findEnclosingTagSpan(
        text: String,
        cursor: Int,
        openTag: String,
        closeTag: String,
        isBold: Boolean
    ): Pair<Int, Int>? {
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it < 0) text.length else it }
        if (cursor < lineStart || cursor > lineEnd) return null
        val spans = parseInlineSpans(text.substring(lineStart, lineEnd))
        for (span in spans) {
            val absInnerStart = lineStart + span.innerStart
            val absInnerEnd = lineStart + span.innerEnd
            if (cursor in absInnerStart..absInnerEnd && span.openTag == openTag && span.closeTag == closeTag) {
                if (!isBold || openTag == "**") {
                    return (lineStart + span.rawStart) to (lineStart + span.rawEnd)
                }
            }
        }
        return null
    }

    private fun findEnclosingSingleAsteriskSpan(text: String, cursor: Int): Pair<Int, Int>? {
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it < 0) text.length else it }
        if (cursor < lineStart || cursor > lineEnd) return null
        val spans = parseInlineSpans(text.substring(lineStart, lineEnd))
        for (span in spans) {
            if (span.kind == InlineKind.ITALIC) {
                val absInnerStart = lineStart + span.innerStart
                val absInnerEnd = lineStart + span.innerEnd
                if (cursor in absInnerStart..absInnerEnd) {
                    return (lineStart + span.rawStart) to (lineStart + span.rawEnd)
                }
            }
        }
        return null
    }

    private fun applyLinePrefix(
        value: TextFieldValue,
        targetPrefix: String,
        toggleOffIfSame: Boolean
    ): TextFieldValue {
        val text = value.text
        val rawSel = value.selection
        val selMin = rawSel.min.coerceIn(0, text.length)
        val selMax = rawSel.max.coerceIn(0, text.length)

        val blockStart = if (selMin == 0) 0 else text.lastIndexOf('\n', selMin - 1).let { if (it < 0) 0 else it + 1 }
        val blockEnd = text.indexOf('\n', selMax).let { if (it < 0) text.length else it }

        val slice = text.substring(blockStart, blockEnd)
        val lines = slice.split('\n')
        val allAlreadyHavePrefix = targetPrefix.isNotEmpty() && lines.all { it.startsWith(targetPrefix) }

        val updatedLines = lines.map { line ->
            val clean = stripLinePrefix(line)
            if (toggleOffIfSame && allAlreadyHavePrefix) {
                clean
            } else if (targetPrefix.isEmpty()) {
                clean
            } else {
                targetPrefix + clean
            }
        }
        val replacedSlice = updatedLines.joinToString("\n")
        val updatedText = text.substring(0, blockStart) + replacedSlice + text.substring(blockEnd)

        val delta = replacedSlice.length - slice.length
        val newCursor = if (selMin == selMax) {
            (selMax + delta).coerceIn(blockStart, (blockStart + replacedSlice.length).coerceAtMost(updatedText.length))
        } else {
            (blockStart + replacedSlice.length).coerceIn(0, updatedText.length)
        }
        val newStart = if (selMin == selMax) newCursor else blockStart
        return TextFieldValue(updatedText, TextRange(newStart, newCursor))
    }

    private fun applyNumberedList(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val rawSel = value.selection
        val selMin = rawSel.min.coerceIn(0, text.length)
        val selMax = rawSel.max.coerceIn(0, text.length)

        val blockStart = if (selMin == 0) 0 else text.lastIndexOf('\n', selMin - 1).let { if (it < 0) 0 else it + 1 }
        val blockEnd = text.indexOf('\n', selMax).let { if (it < 0) text.length else it }

        val slice = text.substring(blockStart, blockEnd)
        val lines = slice.split('\n')
        val allNumbered = lines.all { numberedPrefixRegex.containsMatchIn(it) }

        // Detect previous line number when adding a single line
        var startNum = 1
        if (!allNumbered && blockStart > 1) {
            val prevLineStart = text.lastIndexOf('\n', blockStart - 2).let { if (it < 0) 0 else it + 1 }
            val prevLine = text.substring(prevLineStart, blockStart - 1)
            val match = numberedPrefixRegex.find(prevLine)
            if (match != null) {
                startNum = (match.groupValues[1].toIntOrNull() ?: 0) + 1
            }
        }

        val updatedLines = lines.mapIndexed { idx, line ->
            val clean = stripLinePrefix(line)
            if (allNumbered) clean else "${startNum + idx}. $clean"
        }
        val replacedSlice = updatedLines.joinToString("\n")
        val updatedText = text.substring(0, blockStart) + replacedSlice + text.substring(blockEnd)
        val delta = replacedSlice.length - slice.length
        val newCursor = if (selMin == selMax) {
            (selMax + delta).coerceIn(blockStart, (blockStart + replacedSlice.length).coerceAtMost(updatedText.length))
        } else {
            blockStart + replacedSlice.length
        }
        val newStart = if (selMin == selMax) newCursor else blockStart
        return TextFieldValue(updatedText, TextRange(newStart, newCursor))
    }

    private fun applyCodeBlock(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val rawSel = value.selection
        val start = rawSel.min.coerceIn(0, text.length)
        val end = rawSel.max.coerceIn(0, text.length)
        val selected = text.substring(start, end)
        return if (selected.isEmpty()) {
            val prefix = if (start > 0 && text[start - 1] != '\n') "\n" else ""
            val insert = "$prefix```\n\n```\n"
            val updated = text.substring(0, start) + insert + text.substring(end)
            val cursorPos = start + prefix.length + 4
            TextFieldValue(updated, TextRange(cursorPos))
        } else if (selected.startsWith("```\n") && selected.endsWith("\n```")) {
            val unwrapped = selected.removePrefix("```\n").removeSuffix("\n```")
            val updated = text.substring(0, start) + unwrapped + text.substring(end)
            TextFieldValue(updated, TextRange(start, start + unwrapped.length))
        } else {
            val wrap = "```\n$selected\n```"
            val updated = text.substring(0, start) + wrap + text.substring(end)
            TextFieldValue(updated, TextRange(start + 4, start + 4 + selected.length))
        }
    }

    private fun insertDivider(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursor = value.selection.max.coerceIn(0, text.length)
        val needLeadingNewline = cursor > 0 && text[cursor - 1] != '\n'
        val insert = buildString {
            if (needLeadingNewline) append('\n')
            append("---\n")
        }
        val updated = text.substring(0, cursor) + insert + text.substring(cursor)
        return TextFieldValue(updated, TextRange((cursor + insert.length).coerceIn(0, updated.length)))
    }

    private fun clearFormattingAtSelection(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val rawSel = value.selection
        val start = rawSel.min.coerceIn(0, text.length)
        val end = rawSel.max.coerceIn(0, text.length)
        if (start == end) {
            val lineStart = if (start == 0) 0 else text.lastIndexOf('\n', start - 1).let { if (it < 0) 0 else it + 1 }
            val lineEnd = text.indexOf('\n', start).let { if (it < 0) text.length else it }
            val line = text.substring(lineStart, lineEnd)
            val cleaned = stripInlineTokens(stripLinePrefix(line))
            val updated = text.substring(0, lineStart) + cleaned + text.substring(lineEnd)
            val newCursor = (lineStart + cleaned.length).coerceIn(0, updated.length)
            return TextFieldValue(updated, TextRange(newCursor))
        } else {
            val selected = text.substring(start, end)
            val cleaned = selected.split('\n').joinToString("\n") { stripInlineTokens(stripLinePrefix(it)) }
            val updated = text.substring(0, start) + cleaned + text.substring(end)
            return TextFieldValue(updated, TextRange(start, start + cleaned.length))
        }
    }

    fun stripLinePrefix(line: String): String {
        val m = linePrefixRegex.find(line)
        return if (m != null && m.range.first == 0) line.substring(m.range.last + 1) else line
    }

    fun stripInlineTokens(text: String): String {
        return text
            .replace("<u>", "")
            .replace("</u>", "")
            .replace("**", "")
            .replace("~~", "")
            .replace("==", "")
            .replace(Regex("""(?<!\*)\*(?!\*)"""), "")
            .replace("`", "")
    }

    /**
     * Strips Notion/Markdown formatting syntax and converts tables into compact readable summaries
     * for home cards, search, and share exports.
     */
    fun stripFormattingForPreview(raw: String): String {
        if (raw.isBlank()) return ""
        val lines = raw.split('\n')
        val out = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val trimmed = lines[i].trim()
            if (trimmed.startsWith("```")) {
                i++
                continue
            }
            if (trimmed == "---" || trimmed == "***") {
                i++
                continue
            }
            // Compact table preview lines
            if (trimmed.startsWith("|") && i + 1 < lines.size && tableDividerRegex.matches(lines[i + 1].trim())) {
                val tableRows = mutableListOf<String>()
                var j = i
                while (j < lines.size && lines[j].trim().startsWith("|")) {
                    val rowStr = lines[j].trim()
                    if (!tableDividerRegex.matches(rowStr)) {
                        val cells = parsePipeRow(rowStr).filter { it.isNotBlank() }
                        if (cells.isNotEmpty()) {
                            tableRows.add(cells.joinToString(" | "))
                        }
                    }
                    j++
                }
                if (tableRows.isNotEmpty()) {
                    out.add(tableRows.joinToString(" • "))
                }
                i = j
                continue
            }
            val cleanedLine = when {
                trimmed.startsWith("- [x] ", ignoreCase = true) -> "☑ " + stripInlineTokens(trimmed.drop(6))
                trimmed.startsWith("- [ ] ") -> "☐ " + stripInlineTokens(trimmed.drop(6))
                trimmed.startsWith("> 💡 ") -> "💡 " + stripInlineTokens(trimmed.drop(5))
                trimmed.startsWith("> ") -> stripInlineTokens(trimmed.drop(2))
                trimmed.startsWith("### ") -> stripInlineTokens(trimmed.drop(4))
                trimmed.startsWith("## ") -> stripInlineTokens(trimmed.drop(3))
                trimmed.startsWith("# ") -> stripInlineTokens(trimmed.drop(2))
                trimmed.startsWith("- ") -> "• " + stripInlineTokens(trimmed.drop(2))
                else -> stripInlineTokens(lines[i])
            }
            out.add(cleanedLine)
            i++
        }
        return out.joinToString("\n").trim()
    }

    /**
     * Inspects active styles at the current cursor/selection so Notion Toolbar buttons light up accurately.
     */
    fun inspectActiveStyles(text: String, selection: TextRange): Set<RichFormatAction> {
        val active = mutableSetOf<RichFormatAction>()
        if (text.isEmpty()) return active
        val cursor = selection.max.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it < 0) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        when {
            currentLine.startsWith("### ") -> active.add(RichFormatAction.H3)
            currentLine.startsWith("## ") -> active.add(RichFormatAction.H2)
            currentLine.startsWith("# ") -> active.add(RichFormatAction.H1)
            currentLine.startsWith("- [ ] ") || currentLine.startsWith("- [x] ") || currentLine.startsWith("- [X] ") -> active.add(RichFormatAction.CHECKBOX)
            currentLine.startsWith("- ") || currentLine.startsWith("• ") -> active.add(RichFormatAction.BULLET_LIST)
            numberedPrefixRegex.containsMatchIn(currentLine) -> active.add(RichFormatAction.NUMBERED_LIST)
            currentLine.startsWith("> 💡 ") -> active.add(RichFormatAction.CALLOUT)
            currentLine.startsWith("> ") -> active.add(RichFormatAction.QUOTE)
        }

        val lineCursor = (cursor - lineStart).coerceIn(0, currentLine.length)
        // Check empty pairs at cursor too
        if (cursor >= 2 && cursor + 2 <= text.length && text.substring(cursor - 2, cursor + 2) == "****") {
            active.add(RichFormatAction.BOLD)
        } else if (cursor >= 1 && cursor + 1 <= text.length &&
            text[cursor - 1] == '*' && text[cursor] == '*' &&
            !(cursor >= 2 && text[cursor - 2] == '*') &&
            !(cursor + 1 < text.length && text[cursor + 1] == '*')
        ) {
            active.add(RichFormatAction.ITALIC)
        }
        if (cursor >= 3 && cursor + 4 <= text.length && text.substring(cursor - 3, cursor + 4) == "<u></u>") {
            active.add(RichFormatAction.UNDERLINE)
        }
        if (cursor >= 2 && cursor + 2 <= text.length && text.substring(cursor - 2, cursor + 2) == "~~~~") {
            active.add(RichFormatAction.STRIKETHROUGH)
        }
        if (cursor >= 2 && cursor + 2 <= text.length && text.substring(cursor - 2, cursor + 2) == "====") {
            active.add(RichFormatAction.HIGHLIGHT)
        }
        if (cursor >= 1 && cursor + 1 <= text.length && text.substring(cursor - 1, cursor + 1) == "``") {
            active.add(RichFormatAction.CODE)
        }

        val inlineSpans = parseInlineSpans(currentLine)
        for (span in inlineSpans) {
            if (lineCursor in span.innerStart..span.innerEnd) {
                when (span.kind) {
                    InlineKind.BOLD -> active.add(RichFormatAction.BOLD)
                    InlineKind.ITALIC -> active.add(RichFormatAction.ITALIC)
                    InlineKind.UNDERLINE -> active.add(RichFormatAction.UNDERLINE)
                    InlineKind.STRIKETHROUGH -> active.add(RichFormatAction.STRIKETHROUGH)
                    InlineKind.CODE -> active.add(RichFormatAction.CODE)
                    InlineKind.HIGHLIGHT -> active.add(RichFormatAction.HIGHLIGHT)
                }
            }
        }
        return active
    }

    // --- Interactive Table Helpers ---

    fun serializeTable(rows: List<List<String>>): String {
        if (rows.isEmpty()) return ""
        val maxCols = rows.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
        val norm = rows.map { row ->
            List(maxCols) { colIdx ->
                row.getOrElse(colIdx) { "" }
                    .replace("\n", " ")
                    .replace("|", "\\|")
                    .trim()
            }
        }
        val sb = StringBuilder()
        sb.append("| ").append(norm.first().joinToString(" | ")).append(" |\n")
        sb.append("| ").append(List(maxCols) { "---" }.joinToString(" | ")).append(" |")
        for (r in 1 until norm.size) {
            sb.append('\n')
            sb.append("| ").append(norm[r].joinToString(" | ")).append(" |")
        }
        return sb.toString()
    }

    private fun parsePipeRow(line: String): List<String> {
        val trimmed = line.trim().removePrefix("|").removeSuffix("|")
        val cells = mutableListOf<String>()
        val cur = StringBuilder()
        var idx = 0
        while (idx < trimmed.length) {
            val ch = trimmed[idx]
            if (ch == '\\' && idx + 1 < trimmed.length && trimmed[idx + 1] == '|') {
                cur.append('|')
                idx += 2
            } else if (ch == '|') {
                cells.add(cur.toString().trim())
                cur.setLength(0)
                idx++
            } else {
                cur.append(ch)
                idx++
            }
        }
        cells.add(cur.toString().trim())
        return cells
    }

    fun parseAllTables(text: String): List<MarkdownTableData> {
        if (text.isBlank()) return emptyList()
        val lines = text.split('\n')
        val lineStarts = IntArray(lines.size)
        var acc = 0
        for (i in lines.indices) {
            lineStarts[i] = acc
            acc += lines[i].length + 1
        }

        val tables = mutableListOf<MarkdownTableData>()
        var i = 0
        while (i < lines.size) {
            val cur = lines[i].trim()
            if (cur.startsWith("|") && i + 1 < lines.size && tableDividerRegex.matches(lines[i + 1].trim())) {
                val startOffset = lineStarts[i]
                val headerCells = parsePipeRow(cur)
                val parsedRows = mutableListOf(headerCells)
                var j = i + 2
                while (j < lines.size && lines[j].trim().startsWith("|")) {
                    parsedRows.add(parsePipeRow(lines[j]))
                    j++
                }
                val lastLineIdx = j - 1
                val endOffset = (lineStarts[lastLineIdx] + lines[lastLineIdx].length).coerceAtMost(text.length)
                val maxCols = parsedRows.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
                val normalizedRows = parsedRows.map { r ->
                    List(maxCols) { c -> r.getOrElse(c) { "" } }
                }
                tables.add(
                    MarkdownTableData(
                        rows = normalizedRows,
                        startOffset = startOffset,
                        endOffset = endOffset
                    )
                )
                i = j
            } else {
                i++
            }
        }
        return tables
    }

    fun insertTableAtSelection(
        text: String,
        selection: TextRange,
        tableMarkdown: String
    ): TextFieldValue {
        val cursor = selection.max.coerceIn(0, text.length)
        val leadingNewlines = when {
            cursor == 0 -> ""
            cursor >= 2 && text.substring(cursor - 2, cursor) == "\n\n" -> ""
            text[cursor - 1] == '\n' -> "\n"
            else -> "\n\n"
        }
        val trailingNewlines = when {
            cursor >= text.length -> "\n"
            text[cursor] == '\n' -> ""
            else -> "\n\n"
        }
        val insert = leadingNewlines + tableMarkdown + trailingNewlines
        val updated = text.substring(0, cursor) + insert + text.substring(cursor)
        val nextCursor = (cursor + insert.length).coerceIn(0, updated.length)
        return TextFieldValue(updated, TextRange(nextCursor))
    }

    fun replaceTableAtRange(
        text: String,
        startOffset: Int,
        endOffset: Int,
        tableMarkdown: String
    ): TextFieldValue {
        if (startOffset !in 0..text.length || endOffset !in startOffset..text.length) {
            return insertTableAtSelection(text, TextRange(text.length), tableMarkdown)
        }
        val updated = text.substring(0, startOffset) + tableMarkdown + text.substring(endOffset)
        val nextCursor = (startOffset + tableMarkdown.length).coerceIn(0, updated.length)
        return TextFieldValue(updated, TextRange(nextCursor))
    }

    fun deleteTableAtRange(text: String, startOffset: Int, endOffset: Int): String {
        if (startOffset !in 0..text.length || endOffset !in startOffset..text.length) return text
        var cutStart = startOffset
        var cutEnd = endOffset
        while (cutEnd < text.length && text[cutEnd] == '\n' && (cutStart == 0 || text[cutStart - 1] == '\n')) {
            cutEnd++
        }
        return text.removeRange(cutStart, cutEnd).trimEnd('\n')
    }

    fun toggleChecklistLineAtOffset(text: String, lineStartOffset: Int): String {
        if (lineStartOffset !in 0..text.length) return text
        val sub = text.substring(lineStartOffset)
        return when {
            sub.startsWith("- [ ] ") -> {
                text.substring(0, lineStartOffset) + "- [x] " + sub.drop(6)
            }
            sub.startsWith("- [x] ") || sub.startsWith("- [X] ") -> {
                text.substring(0, lineStartOffset) + "- [ ] " + sub.drop(6)
            }
            else -> text
        }
    }

    // --- Inline Token Scanner ---

    private enum class InlineKind {
        BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, CODE, HIGHLIGHT
    }

    private data class InlineSpan(
        val kind: InlineKind,
        val openTag: String,
        val closeTag: String,
        val rawStart: Int,
        val innerStart: Int,
        val innerEnd: Int,
        val rawEnd: Int
    )

    private fun parseInlineSpans(line: String): List<InlineSpan> {
        val result = mutableListOf<InlineSpan>()
        var i = 0
        while (i < line.length) {
            when {
                line.startsWith("**", i) -> {
                    val close = findClosingBold(line, i + 2)
                    if (close >= 0) {
                        result.add(InlineSpan(InlineKind.BOLD, "**", "**", i, i + 2, close, close + 2))
                        i = close + 2
                        continue
                    }
                }
                line.startsWith("<u>", i) -> {
                    val close = line.indexOf("</u>", i + 3)
                    if (close >= 0) {
                        result.add(InlineSpan(InlineKind.UNDERLINE, "<u>", "</u>", i, i + 3, close, close + 4))
                        i = close + 4
                        continue
                    }
                }
                line.startsWith("~~", i) -> {
                    val close = line.indexOf("~~", i + 2)
                    if (close >= 0) {
                        result.add(InlineSpan(InlineKind.STRIKETHROUGH, "~~", "~~", i, i + 2, close, close + 2))
                        i = close + 2
                        continue
                    }
                }
                line.startsWith("==", i) -> {
                    val close = line.indexOf("==", i + 2)
                    if (close >= 0) {
                        result.add(InlineSpan(InlineKind.HIGHLIGHT, "==", "==", i, i + 2, close, close + 2))
                        i = close + 2
                        continue
                    }
                }
                line[i] == '`' -> {
                    val close = line.indexOf('`', i + 1)
                    if (close >= 0) {
                        result.add(InlineSpan(InlineKind.CODE, "`", "`", i, i + 1, close, close + 1))
                        i = close + 1
                        continue
                    }
                }
                line[i] == '*' && !line.startsWith("**", i) -> {
                    val close = findClosingSingleAsterisk(line, i + 1)
                    if (close > i) {
                        result.add(InlineSpan(InlineKind.ITALIC, "*", "*", i, i + 1, close, close + 1))
                        i = close + 1
                        continue
                    }
                }
            }
            i++
        }
        return result
    }

    private fun findClosingBold(line: String, from: Int): Int {
        var idx = from
        while (idx < line.length - 1) {
            if (line[idx] == '*' && line[idx + 1] == '*') {
                return idx
            }
            idx++
        }
        return -1
    }

    private fun findClosingSingleAsterisk(line: String, from: Int): Int {
        var idx = from
        while (idx < line.length) {
            if (line[idx] == '*') {
                val nextIsAsterisk = idx + 1 < line.length && line[idx + 1] == '*'
                if (!nextIsAsterisk) return idx
                idx += 2
                continue
            }
            idx++
        }
        return -1
    }

    private fun spanStyleForKind(
        kind: InlineKind,
        accentColor: Color,
        isDark: Boolean
    ): SpanStyle {
        return when (kind) {
            InlineKind.BOLD -> SpanStyle(fontWeight = FontWeight.ExtraBold)
            InlineKind.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            InlineKind.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
            InlineKind.STRIKETHROUGH -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            InlineKind.CODE -> SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = accentColor.copy(alpha = if (isDark) 0.24f else 0.14f),
                color = accentColor
            )
            InlineKind.HIGHLIGHT -> SpanStyle(
                background = Color(0xFFFBBF24).copy(alpha = if (isDark) 0.34f else 0.42f),
                fontWeight = FontWeight.Medium
            )
        }
    }

    /**
     * Renders a single raw line (with nested inline markers like `**<u>text</u>**`) to an [AnnotatedString]
     * for Read Mode & Vault Browser & VisualTransformation.
     */
    fun renderInlineRichAnnotatedString(
        rawLine: String,
        accentColor: Color,
        isDark: Boolean,
        extraInnerStyle: SpanStyle? = null
    ): AnnotatedString {
        val builder = AnnotatedString.Builder()
        appendInlineSegment(
            builder = builder,
            raw = rawLine,
            start = 0,
            end = rawLine.length,
            accentColor = accentColor,
            isDark = isDark,
            inheritedStyle = extraInnerStyle
        )
        return builder.toAnnotatedString()
    }

    private fun appendInlineSegment(
        builder: AnnotatedString.Builder,
        raw: String,
        start: Int,
        end: Int,
        accentColor: Color,
        isDark: Boolean,
        inheritedStyle: SpanStyle?
    ) {
        var cursor = start
        while (cursor < end) {
            val sub = raw.substring(cursor, end)
            val matched = firstInlineMatch(sub)
            if (matched == null) {
                val text = raw.substring(cursor, end)
                val outStart = builder.length
                builder.append(text)
                if (inheritedStyle != null && builder.length > outStart) {
                    builder.addStyle(inheritedStyle, outStart, builder.length)
                }
                return
            }
            if (matched.rawStart > 0) {
                val plain = sub.substring(0, matched.rawStart)
                val outStart = builder.length
                builder.append(plain)
                if (inheritedStyle != null && builder.length > outStart) {
                    builder.addStyle(inheritedStyle, outStart, builder.length)
                }
            }
            val kindStyle = spanStyleForKind(matched.kind, accentColor, isDark)
            val merged = mergeInlineStyles(inheritedStyle, kindStyle)
            val absInnerStart = cursor + matched.innerStart
            val absInnerEnd = cursor + matched.innerEnd
            if (matched.kind == InlineKind.CODE) {
                val codeText = raw.substring(absInnerStart, absInnerEnd)
                val outStart = builder.length
                builder.append(codeText)
                if (builder.length > outStart) {
                    builder.addStyle(merged, outStart, builder.length)
                }
            } else {
                appendInlineSegment(
                    builder = builder,
                    raw = raw,
                    start = absInnerStart,
                    end = absInnerEnd,
                    accentColor = accentColor,
                    isDark = isDark,
                    inheritedStyle = merged
                )
            }
            cursor += matched.rawEnd
        }
    }

    private fun mergeInlineStyles(parent: SpanStyle?, child: SpanStyle): SpanStyle {
        if (parent == null) return child
        val pDeco = parent.textDecoration
        val cDeco = child.textDecoration
        val combinedDeco = when {
            pDeco != null && cDeco != null -> TextDecoration.combine(listOf(pDeco, cDeco))
            else -> cDeco ?: pDeco
        }
        return parent.merge(child).copy(textDecoration = combinedDeco)
    }

    private fun firstInlineMatch(sub: String): InlineSpan? {
        var i = 0
        while (i < sub.length) {
            when {
                sub.startsWith("**", i) -> {
                    val close = findClosingBold(sub, i + 2)
                    if (close >= 0) return InlineSpan(InlineKind.BOLD, "**", "**", i, i + 2, close, close + 2)
                }
                sub.startsWith("<u>", i) -> {
                    val close = sub.indexOf("</u>", i + 3)
                    if (close >= 0) return InlineSpan(InlineKind.UNDERLINE, "<u>", "</u>", i, i + 3, close, close + 4)
                }
                sub.startsWith("~~", i) -> {
                    val close = sub.indexOf("~~", i + 2)
                    if (close >= 0) return InlineSpan(InlineKind.STRIKETHROUGH, "~~", "~~", i, i + 2, close, close + 2)
                }
                sub.startsWith("==", i) -> {
                    val close = sub.indexOf("==", i + 2)
                    if (close >= 0) return InlineSpan(InlineKind.HIGHLIGHT, "==", "==", i, i + 2, close, close + 2)
                }
                sub[i] == '`' -> {
                    val close = sub.indexOf('`', i + 1)
                    if (close >= 0) return InlineSpan(InlineKind.CODE, "`", "`", i, i + 1, close, close + 1)
                }
                sub[i] == '*' && !sub.startsWith("**", i) -> {
                    val close = findClosingSingleAsterisk(sub, i + 1)
                    if (close > i) return InlineSpan(InlineKind.ITALIC, "*", "*", i, i + 1, close, close + 1)
                }
            }
            i++
        }
        return null
    }

    // --- Live WYSIWYG VisualTransformation for BasicTextField ---

    fun createRichVisualTransformation(
        accentColor: Color,
        metaColor: Color,
        isDark: Boolean
    ): VisualTransformation {
        return VisualTransformation { annotatedString ->
            transformForEditor(annotatedString.text, accentColor, metaColor, isDark)
        }
    }

    internal fun transformForEditor(
        raw: String,
        accentColor: Color,
        metaColor: Color,
        isDark: Boolean
    ): TransformedText {
        if (raw.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val sb = StringBuilder(raw.length)
        val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
        val origToTrans = IntArray(raw.length + 1)
        val transToOrigList = ArrayList<Int>(raw.length + 1)
        var cursorOrig = 0

        fun advanceHidden(upToOrig: Int) {
            val clamped = upToOrig.coerceIn(cursorOrig, raw.length)
            while (cursorOrig < clamped) {
                origToTrans[cursorOrig] = sb.length
                cursorOrig++
            }
        }

        fun emitRawSlice(start: Int, end: Int, style: SpanStyle? = null) {
            advanceHidden(start)
            val transStart = sb.length
            for (idx in start until end) {
                origToTrans[idx] = sb.length
                sb.append(raw[idx])
                transToOrigList.add(idx)
            }
            cursorOrig = end
            if (style != null && sb.length > transStart) {
                spans.add(AnnotatedString.Range(style, transStart, sb.length))
            }
        }

        fun emitMappedReplacement(origStart: Int, origEnd: Int, display: String, style: SpanStyle? = null) {
            advanceHidden(origStart)
            val transStart = sb.length
            for (k in display.indices) {
                sb.append(display[k])
                transToOrigList.add(if (k == 0) origStart else origEnd)
            }
            for (idx in origStart until origEnd) {
                origToTrans[idx] = if (idx == origStart) transStart else sb.length
            }
            cursorOrig = origEnd
            if (style != null && sb.length > transStart) {
                spans.add(AnnotatedString.Range(style, transStart, sb.length))
            }
        }

        fun emitInlineWithOffsets(absStart: Int, absEnd: Int, inheritedStyle: SpanStyle?) {
            var pos = absStart
            while (pos < absEnd) {
                val sub = raw.substring(pos, absEnd)
                val match = firstInlineMatch(sub)
                if (match == null) {
                    emitRawSlice(pos, absEnd, inheritedStyle)
                    return
                }
                if (match.rawStart > 0) {
                    emitRawSlice(pos, pos + match.rawStart, inheritedStyle)
                }
                val kindStyle = spanStyleForKind(match.kind, accentColor, isDark)
                val merged = mergeInlineStyles(inheritedStyle, kindStyle)
                val openAbsStart = pos + match.rawStart
                val innerAbsStart = pos + match.innerStart
                val innerAbsEnd = pos + match.innerEnd
                val closeAbsEnd = pos + match.rawEnd

                advanceHidden(innerAbsStart)
                if (match.kind == InlineKind.CODE) {
                    emitRawSlice(innerAbsStart, innerAbsEnd, merged)
                } else {
                    emitInlineWithOffsets(innerAbsStart, innerAbsEnd, merged)
                }
                advanceHidden(closeAbsEnd)
                // If somehow match had zero chars before openAbsStart, ensure progress
                if (cursorOrig < openAbsStart) advanceHidden(openAbsStart)
                pos = closeAbsEnd
            }
        }

        var lineStart = 0
        var inCodeFence = false
        while (lineStart <= raw.length) {
            val nl = raw.indexOf('\n', lineStart)
            val lineEnd = if (nl < 0) raw.length else nl
            val line = raw.substring(lineStart, lineEnd)

            if (line.trim().startsWith("```")) {
                inCodeFence = !inCodeFence
                emitRawSlice(
                    lineStart,
                    lineEnd,
                    SpanStyle(
                        color = metaColor.copy(alpha = 0.55f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )
            } else if (inCodeFence) {
                emitRawSlice(
                    lineStart,
                    lineEnd,
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = accentColor.copy(alpha = if (isDark) 0.16f else 0.08f)
                    )
                )
            } else {
                when {
                    line.startsWith("### ") -> {
                        advanceHidden(lineStart + 4)
                        emitInlineWithOffsets(
                            lineStart + 4,
                            lineEnd,
                            SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        )
                    }
                    line.startsWith("## ") -> {
                        advanceHidden(lineStart + 3)
                        emitInlineWithOffsets(
                            lineStart + 3,
                            lineEnd,
                            SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        )
                    }
                    line.startsWith("# ") -> {
                        advanceHidden(lineStart + 2)
                        emitInlineWithOffsets(
                            lineStart + 2,
                            lineEnd,
                            SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Black)
                        )
                    }
                    line.startsWith("- [x] ") || line.startsWith("- [X] ") -> {
                        emitMappedReplacement(
                            lineStart,
                            lineStart + 6,
                            "☑ ",
                            SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)
                        )
                        emitInlineWithOffsets(
                            lineStart + 6,
                            lineEnd,
                            SpanStyle(color = metaColor, textDecoration = TextDecoration.LineThrough)
                        )
                    }
                    line.startsWith("- [ ] ") -> {
                        emitMappedReplacement(
                            lineStart,
                            lineStart + 6,
                            "☐ ",
                            SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)
                        )
                        emitInlineWithOffsets(lineStart + 6, lineEnd, null)
                    }
                    line.startsWith("- ") -> {
                        emitMappedReplacement(
                            lineStart,
                            lineStart + 2,
                            "• ",
                            SpanStyle(color = accentColor, fontWeight = FontWeight.Black)
                        )
                        emitInlineWithOffsets(lineStart + 2, lineEnd, null)
                    }
                    line.startsWith("> 💡 ") -> {
                        emitMappedReplacement(
                            lineStart,
                            lineStart + 5,
                            "💡 ",
                            SpanStyle(color = accentColor)
                        )
                        emitInlineWithOffsets(
                            lineStart + 5,
                            lineEnd,
                            SpanStyle(
                                fontWeight = FontWeight.Medium,
                                background = accentColor.copy(alpha = if (isDark) 0.16f else 0.10f)
                            )
                        )
                    }
                    line.startsWith("> ") -> {
                        emitMappedReplacement(
                            lineStart,
                            lineStart + 2,
                            "┃ ",
                            SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)
                        )
                        emitInlineWithOffsets(
                            lineStart + 2,
                            lineEnd,
                            SpanStyle(fontStyle = FontStyle.Italic, color = metaColor)
                        )
                    }
                    line.trim() == "---" -> {
                        emitRawSlice(
                            lineStart,
                            lineEnd,
                            SpanStyle(color = metaColor.copy(alpha = 0.45f))
                        )
                    }
                    else -> {
                        val numMatch = numberedPrefixRegex.find(line)
                        if (numMatch != null) {
                            val prefixLen = numMatch.value.length
                            emitRawSlice(
                                lineStart,
                                lineStart + prefixLen,
                                SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)
                            )
                            emitInlineWithOffsets(lineStart + prefixLen, lineEnd, null)
                        } else {
                            emitInlineWithOffsets(lineStart, lineEnd, null)
                        }
                    }
                }
            }

            if (nl >= 0) {
                emitRawSlice(nl, nl + 1, null)
                lineStart = nl + 1
            } else {
                break
            }
        }

        advanceHidden(raw.length)
        origToTrans[raw.length] = sb.length
        transToOrigList.add(raw.length)
        val transToOrig = transToOrigList.toIntArray()

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, raw.length)
                return origToTrans[safe].coerceIn(0, sb.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safe = offset.coerceIn(0, sb.length)
                return transToOrig[safe].coerceIn(0, raw.length)
            }
        }

        return TransformedText(
            AnnotatedString(text = sb.toString(), spanStyles = spans),
            mapping
        )
    }

    // --- Structured Block Parser (Read Mode & Vault Draft Browser) ---

    fun parseRichBlocks(
        rawText: String,
        accentColor: Color,
        isDark: Boolean
    ): List<RichBlock> {
        if (rawText.isBlank()) return emptyList()
        val lines = rawText.split('\n')
        val lineStarts = IntArray(lines.size)
        var offsetAcc = 0
        for (i in lines.indices) {
            lineStarts[i] = offsetAcc
            offsetAcc += lines[i].length + 1
        }

        val blocks = mutableListOf<RichBlock>()
        var i = 0
        while (i < lines.size) {
            val rawLine = lines[i]
            val trimmed = rawLine.trim()

            // Code block
            if (trimmed.startsWith("```")) {
                val codeLines = mutableListOf<String>()
                var j = i + 1
                while (j < lines.size && !lines[j].trim().startsWith("```")) {
                    codeLines.add(lines[j])
                    j++
                }
                blocks.add(RichBlock.CodeBlock(codeLines.joinToString("\n")))
                i = if (j < lines.size) j + 1 else j
                continue
            }

            // Pipe table
            if (trimmed.startsWith("|") && i + 1 < lines.size && tableDividerRegex.matches(lines[i + 1].trim())) {
                val tableStart = lineStarts[i]
                val rows = mutableListOf(parsePipeRow(trimmed))
                var j = i + 2
                while (j < lines.size && lines[j].trim().startsWith("|")) {
                    rows.add(parsePipeRow(lines[j]))
                    j++
                }
                val lastIdx = j - 1
                val tableEnd = (lineStarts[lastIdx] + lines[lastIdx].length).coerceAtMost(rawText.length)
                val maxCols = rows.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
                val normalized = rows.map { r -> List(maxCols) { c -> r.getOrElse(c) { "" } } }
                blocks.add(RichBlock.Table(MarkdownTableData(normalized, tableStart, tableEnd)))
                i = j
                continue
            }

            when {
                trimmed == "---" || trimmed == "***" -> {
                    blocks.add(RichBlock.Divider)
                }
                rawLine.startsWith("### ") -> {
                    blocks.add(
                        RichBlock.Heading(
                            3,
                            renderInlineRichAnnotatedString(rawLine.drop(4), accentColor, isDark)
                        )
                    )
                }
                rawLine.startsWith("## ") -> {
                    blocks.add(
                        RichBlock.Heading(
                            2,
                            renderInlineRichAnnotatedString(rawLine.drop(3), accentColor, isDark)
                        )
                    )
                }
                rawLine.startsWith("# ") -> {
                    blocks.add(
                        RichBlock.Heading(
                            1,
                            renderInlineRichAnnotatedString(rawLine.drop(2), accentColor, isDark)
                        )
                    )
                }
                rawLine.startsWith("- [x] ") || rawLine.startsWith("- [X] ") -> {
                    blocks.add(
                        RichBlock.ChecklistBlockItem(
                            checked = true,
                            lineStartOffset = lineStarts[i],
                            text = renderInlineRichAnnotatedString(rawLine.drop(6), accentColor, isDark)
                        )
                    )
                }
                rawLine.startsWith("- [ ] ") -> {
                    blocks.add(
                        RichBlock.ChecklistBlockItem(
                            checked = false,
                            lineStartOffset = lineStarts[i],
                            text = renderInlineRichAnnotatedString(rawLine.drop(6), accentColor, isDark)
                        )
                    )
                }
                rawLine.startsWith("- ") || rawLine.startsWith("• ") -> {
                    blocks.add(
                        RichBlock.BulletItem(
                            renderInlineRichAnnotatedString(rawLine.drop(2), accentColor, isDark)
                        )
                    )
                }
                rawLine.startsWith("> 💡 ") -> {
                    blocks.add(
                        RichBlock.Quote(
                            isCallout = true,
                            text = renderInlineRichAnnotatedString(rawLine.drop(5), accentColor, isDark)
                        )
                    )
                }
                rawLine.startsWith("> ") -> {
                    blocks.add(
                        RichBlock.Quote(
                            isCallout = false,
                            text = renderInlineRichAnnotatedString(rawLine.drop(2), accentColor, isDark)
                        )
                    )
                }
                else -> {
                    val numMatch = numberedPrefixRegex.find(rawLine)
                    if (numMatch != null) {
                        val numberLabel = "${numMatch.groupValues[1]}."
                        blocks.add(
                            RichBlock.NumberedItem(
                                numberLabel = numberLabel,
                                text = renderInlineRichAnnotatedString(
                                    rawLine.drop(numMatch.value.length),
                                    accentColor,
                                    isDark
                                )
                            )
                        )
                    } else if (rawLine.isNotEmpty()) {
                        blocks.add(
                            RichBlock.Paragraph(
                                renderInlineRichAnnotatedString(rawLine, accentColor, isDark)
                            )
                        )
                    } else {
                        blocks.add(RichBlock.Paragraph(AnnotatedString("")))
                    }
                }
            }
            i++
        }
        return blocks
    }
}
