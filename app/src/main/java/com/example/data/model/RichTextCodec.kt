package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EditorFontFamily
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

sealed interface EditorPageSegment {
    data class TextSegment(val startOffset: Int, val endOffset: Int, val text: String) : EditorPageSegment
    data class TableSegment(val index: Int, val table: MarkdownTableData) : EditorPageSegment
}

data class EditorFormatOutcome(
    val fieldValue: TextFieldValue,
    val pendingStyles: Set<RichFormatAction>
)

private val inlineStyleOrder = listOf(
    RichFormatAction.HIGHLIGHT,
    RichFormatAction.STRIKETHROUGH,
    RichFormatAction.UNDERLINE,
    RichFormatAction.BOLD,
    RichFormatAction.ITALIC,
    RichFormatAction.CODE
)

private val inlineActionSet = setOf(
    RichFormatAction.BOLD,
    RichFormatAction.ITALIC,
    RichFormatAction.UNDERLINE,
    RichFormatAction.STRIKETHROUGH,
    RichFormatAction.HIGHLIGHT,
    RichFormatAction.CODE
)

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

    fun splitPageIntoEditorSegments(rawText: String): List<EditorPageSegment> {
        val tables = parseAllTables(rawText)
        if (tables.isEmpty()) {
            return listOf(EditorPageSegment.TextSegment(0, rawText.length, rawText))
        }
        val segments = mutableListOf<EditorPageSegment>()
        var cursor = 0
        tables.forEachIndexed { idx, table ->
            val segStart = cursor.coerceIn(0, rawText.length)
            val segEnd = table.startOffset.coerceIn(segStart, rawText.length)
            val rawSlice = rawText.substring(segStart, segEnd)
            val trimmedDisplay = if (rawSlice.endsWith('\n')) rawSlice.dropLast(1) else rawSlice
            if (trimmedDisplay.isNotEmpty() || idx == 0) {
                segments.add(EditorPageSegment.TextSegment(segStart, segEnd, trimmedDisplay))
            }
            segments.add(EditorPageSegment.TableSegment(idx, table))
            cursor = table.endOffset
            if (cursor < rawText.length && rawText[cursor] == '\n') {
                cursor++
            }
        }
        val tailStart = cursor.coerceIn(0, rawText.length)
        val tailText = rawText.substring(tailStart)
        segments.add(EditorPageSegment.TextSegment(tailStart, rawText.length, tailText))
        return segments
    }

    fun replaceSegmentInPage(
        rawText: String,
        segStart: Int,
        segEnd: Int,
        newSegmentText: String,
        hasTableBefore: Boolean,
        hasTableAfter: Boolean
    ): String {
        val safeStart = segStart.coerceIn(0, rawText.length)
        val safeEnd = segEnd.coerceIn(safeStart, rawText.length)
        val prefix = rawText.substring(0, safeStart)
        val suffix = rawText.substring(safeEnd)
        val leadingSep = if (hasTableBefore && newSegmentText.isNotEmpty() && !prefix.endsWith("\n")) "\n" else ""
        val trailingSep = if (hasTableAfter && newSegmentText.isNotEmpty() && !suffix.startsWith("\n")) "\n" else ""
        return prefix + leadingSep + newSegmentText + trailingSep + suffix
    }

    fun toggleFormatForEditor(
        value: TextFieldValue,
        action: RichFormatAction,
        pendingStyles: Set<RichFormatAction>
    ): EditorFormatOutcome {
        if (action !in inlineActionSet) {
            val next = applyRichAction(value, action)
            val nextPending = if (action == RichFormatAction.CLEAR_FORMATTING) emptySet() else pendingStyles
            return EditorFormatOutcome(next, nextPending)
        }

        val text = value.text
        val start = value.selection.min.coerceIn(0, text.length)
        val end = value.selection.max.coerceIn(0, text.length)

        // 1. Selected range -> apply directly on selection
        if (start != end) {
            return EditorFormatOutcome(applyRichAction(value, action), pendingStyles)
        }

        // 2. If user is actively typing with this inline style pending and taps it OFF at the end of a span,
        // exit past the closing marker instead of stripping formatting from the word they just typed.
        val kind = kindForAction(action)
        if (action in pendingStyles && kind != null) {
            val endSpan = findSpanEndingAtCursor(text, start, kind)
            if (endSpan != null) {
                val exitCursor = (lineOffsetOf(text, start) + endSpan.rawEnd).coerceIn(0, text.length)
                return EditorFormatOutcome(
                    fieldValue = TextFieldValue(text, TextRange(exitCursor)),
                    pendingStyles = pendingStyles - action
                )
            }
        }

        // 3. Collapsed cursor inside an existing span of this exact style -> unwrap it cleanly
        if (kind != null && isCursorInsideInlineKind(text, start, kind)) {
            return EditorFormatOutcome(applyRichAction(value, action), pendingStyles - action)
        }

        // 4. Collapsed cursor inside a word -> toggle style on that word
        if (start > 0 && start < text.length && isWordChar(text[start - 1]) && isWordChar(text[start])) {
            return EditorFormatOutcome(applyRichAction(value, action), pendingStyles)
        }

        // 5. Collapsed cursor in empty space / end of word -> toggle pending style (zero dangling tags!)
        val updatedPending = if (action in pendingStyles) pendingStyles - action else pendingStyles + action
        return EditorFormatOutcome(value, updatedPending)
    }

    private fun lineOffsetOf(text: String, cursor: Int): Int =
        text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }

    private fun findSpanEndingAtCursor(text: String, cursor: Int, kind: InlineKind): InlineSpan? {
        val lineStart = lineOffsetOf(text, cursor)
        val lineEnd = text.indexOf('\n', cursor).let { if (it < 0) text.length else it }
        if (cursor < lineStart || cursor > lineEnd) return null
        val lineCursor = cursor - lineStart
        val spans = parseInlineSpans(text.substring(lineStart, lineEnd))
        return spans.firstOrNull { span ->
            matchesInlineKind(span.kind, kind) &&
                (lineCursor == span.innerEnd || lineCursor == span.rawEnd)
        }
    }

    private fun matchesInlineKind(spanKind: InlineKind, target: InlineKind): Boolean =
        spanKind == target ||
            (spanKind == InlineKind.BOLD_ITALIC && (target == InlineKind.BOLD || target == InlineKind.ITALIC))

    fun applyPendingStylesOnEdit(
        prevText: String,
        nextVal: TextFieldValue,
        pendingStyles: Set<RichFormatAction>
    ): EditorFormatOutcome {
        val nextText = nextVal.text
        if (nextText == prevText) {
            return EditorFormatOutcome(nextVal, pendingStyles)
        }

        // Find exact diff (prefix/suffix) between prevText and nextText (IME-safe)
        var prefix = 0
        val minLen = kotlin.math.min(prevText.length, nextText.length)
        while (prefix < minLen && prevText[prefix] == nextText[prefix]) {
            prefix++
        }
        var suffix = 0
        while (suffix < (minLen - prefix) &&
            prevText[prevText.length - 1 - suffix] == nextText[nextText.length - 1 - suffix]
        ) {
            suffix++
        }

        val deletedLen = (prevText.length - suffix - prefix).coerceAtLeast(0)
        val insertEnd = (nextText.length - suffix).coerceIn(prefix, nextText.length)
        val inserted = nextText.substring(prefix, insertEnd)

        // 1. Pure Deletion / Backspace: never leave clipped closing tags or broken block prefixes visible!
        if (inserted.isEmpty() && deletedLen > 0) {
            val repaired = repairTextAfterDeletion(
                prevText = prevText,
                nextText = nextText,
                delStart = prefix,
                delEndPrev = prevText.length - suffix,
                rawCursor = nextVal.selection.max
            )
            return EditorFormatOutcome(repaired, pendingStyles)
        }

        // Pressing Enter finishes inline typing styles
        if (inserted.contains('\n')) {
            val cleaned = cleanupOrphanInlineMarkers(nextText, nextVal.selection.max)
            return EditorFormatOutcome(cleaned, emptySet())
        }

        // Repair closing markers clipped by Android IME composing/autocorrect replacements
        // (e.g. Gboard replacing raw `[innerStart..rawEnd]` `"ك*"` -> `"كتاب"` in `"*ك*"` -> `"*كتاب"`).
        var workingText = nextText
        var workingInsertEnd = insertEnd
        var workingVal = nextVal
        if (deletedLen > 0 && inserted.isNotEmpty()) {
            val delEndPrev = prevText.length - suffix
            val prevLineStart = lineOffsetOf(prevText, prefix)
            val prevLineEnd = prevText.indexOf('\n', prefix).let { if (it < 0) prevText.length else it }
            if (delEndPrev <= prevLineEnd) {
                val prevLine = prevText.substring(prevLineStart, prevLineEnd)
                val clippedSpans = parseInlineSpans(prevLine)
                    .filter { span ->
                        val absRawStart = prevLineStart + span.rawStart
                        val absInnerStart = prevLineStart + span.innerStart
                        val absInnerEnd = prevLineStart + span.innerEnd
                        val absRawEnd = prevLineStart + span.rawEnd
                        absRawStart < prefix &&
                            absInnerStart <= prefix &&
                            absInnerEnd < delEndPrev &&
                            delEndPrev <= absRawEnd
                    }
                    .sortedByDescending { it.rawStart }

                if (clippedSpans.isNotEmpty()) {
                    var restoredClosing = ""
                    val nextLineEnd = workingText.indexOf('\n', workingInsertEnd).let { if (it < 0) workingText.length else it }
                    val tailAfterInsert = workingText.substring(workingInsertEnd, nextLineEnd)
                    for (span in clippedSpans) {
                        val expectedSoFar = restoredClosing
                        if (!tailAfterInsert.startsWith(expectedSoFar + span.closeTag)) {
                            restoredClosing += span.closeTag
                        }
                    }
                    if (restoredClosing.isNotEmpty()) {
                        workingText = workingText.substring(0, workingInsertEnd) +
                            restoredClosing +
                            workingText.substring(workingInsertEnd)
                        val oldSelMax = nextVal.selection.max.coerceIn(0, nextText.length)
                        val adjustedCursor = if (oldSelMax >= workingInsertEnd) {
                            (oldSelMax + restoredClosing.length).coerceIn(0, workingText.length)
                        } else {
                            oldSelMax.coerceIn(0, workingText.length)
                        }
                        workingVal = TextFieldValue(
                            text = workingText,
                            selection = TextRange(adjustedCursor),
                            composition = null
                        )
                    }
                }
            }
        }

        if (pendingStyles.isEmpty()) {
            val cleaned = if (deletedLen > 0) cleanupOrphanInlineMarkers(workingText, workingVal.selection.max) else workingVal
            return EditorFormatOutcome(cleaned, emptySet())
        }

        // Pure whitespace stays plain until non-space text is typed
        if (inserted.isBlank()) {
            return EditorFormatOutcome(workingVal, pendingStyles)
        }

        // Check active inline styles around the insertion point (using repaired text if closing markers were restored)
        val existingAtPoint = inspectActiveInlineActionsAtOffset(workingText, workingInsertEnd)
        val missingStyles = pendingStyles - existingAtPoint
        if (missingStyles.isEmpty()) {
            return EditorFormatOutcome(workingVal, pendingStyles)
        }

        val wrapped = wrapTextWithStyles(inserted, missingStyles)
        val leftText = workingText.substring(0, prefix)
        val rightText = workingText.substring(workingInsertEnd)
        val (mergedText, mergedCursor) = mergeSeamInlineMarkers(
            leftText = leftText,
            wrapped = wrapped,
            rightText = rightText,
            pendingStyles = missingStyles
        )
        return EditorFormatOutcome(
            fieldValue = TextFieldValue(
                text = mergedText,
                selection = TextRange(mergedCursor.coerceIn(0, mergedText.length)),
                composition = null
            ),
            pendingStyles = pendingStyles
        )
    }

    private fun inspectActiveInlineActionsAtOffset(text: String, offset: Int): Set<RichFormatAction> {
        val lineStart = lineOffsetOf(text, offset)
        val lineEnd = text.indexOf('\n', offset).let { if (it < 0) text.length else it }
        if (offset < lineStart || offset > lineEnd) return emptySet()
        val lineCursor = offset - lineStart
        val spans = parseInlineSpans(text.substring(lineStart, lineEnd))
        val out = mutableSetOf<RichFormatAction>()
        for (span in spans) {
            if (lineCursor in span.innerStart..span.innerEnd) {
                when (span.kind) {
                    InlineKind.BOLD_ITALIC -> {
                        out.add(RichFormatAction.BOLD)
                        out.add(RichFormatAction.ITALIC)
                    }
                    InlineKind.BOLD -> out.add(RichFormatAction.BOLD)
                    InlineKind.ITALIC -> out.add(RichFormatAction.ITALIC)
                    InlineKind.UNDERLINE -> out.add(RichFormatAction.UNDERLINE)
                    InlineKind.STRIKETHROUGH -> out.add(RichFormatAction.STRIKETHROUGH)
                    InlineKind.CODE -> out.add(RichFormatAction.CODE)
                    InlineKind.HIGHLIGHT -> out.add(RichFormatAction.HIGHLIGHT)
                }
            }
        }
        return out
    }

    private fun repairTextAfterDeletion(
        prevText: String,
        nextText: String,
        delStart: Int,
        delEndPrev: Int,
        rawCursor: Int
    ): TextFieldValue {
        val delLen = delEndPrev - delStart

        // A. Check if Backspace clipped an inline tag's opening or closing marker in prevText
        if (delLen in 1..4) {
            val prevLineStart = lineOffsetOf(prevText, delStart)
            val prevLineEnd = prevText.indexOf('\n', delStart).let { if (it < 0) prevText.length else it }
            if (delEndPrev <= prevLineEnd) {
                val prevLine = prevText.substring(prevLineStart, prevLineEnd)
                val spans = parseInlineSpans(prevLine)
                // Prefer innermost span whose marker was touched
                val hitSpan = spans.lastOrNull { span ->
                    val absRawStart = prevLineStart + span.rawStart
                    val absInnerStart = prevLineStart + span.innerStart
                    val absInnerEnd = prevLineStart + span.innerEnd
                    val absRawEnd = prevLineStart + span.rawEnd
                    val clippedClose = delStart >= absInnerEnd && delEndPrev <= absRawEnd && absInnerEnd < absRawEnd
                    val clippedOpen = delStart >= absRawStart && delEndPrev <= absInnerStart && absRawStart < absInnerStart
                    clippedClose || clippedOpen
                }
                if (hitSpan != null) {
                    val absRawStart = prevLineStart + hitSpan.rawStart
                    val absInnerStart = prevLineStart + hitSpan.innerStart
                    val absInnerEnd = prevLineStart + hitSpan.innerEnd
                    val absRawEnd = prevLineStart + hitSpan.rawEnd
                    val inner = prevText.substring(absInnerStart, absInnerEnd)
                    val updated = prevText.substring(0, absRawStart) + inner + prevText.substring(absRawEnd)
                    val targetCursor = if (delStart >= absInnerEnd) {
                        absRawStart + inner.length
                    } else {
                        absRawStart
                    }
                    return cleanupOrphanInlineMarkers(updated, targetCursor.coerceIn(0, updated.length))
                }
            }
        }

        // B. Check if Backspace clipped a block prefix (`- [ ] `, `## `, `> `, etc.), divider (`---`), or code fence (```)
        val prevLineStart = lineOffsetOf(prevText, delStart)
        val prevLineEnd = prevText.indexOf('\n', delStart).let { if (it < 0) prevText.length else it }
        val prevLine = prevText.substring(prevLineStart, prevLineEnd)

        val nextLineStart = lineOffsetOf(nextText, rawCursor.coerceIn(0, nextText.length))
        val nextLineEnd = nextText.indexOf('\n', nextLineStart).let { if (it < 0) nextText.length else it }
        val nextLine = nextText.substring(nextLineStart, nextLineEnd)

        if (prevLine.trim() == "---" || prevLine.trim() == "***" || prevLine.trim().startsWith("```")) {
            if (nextLine.trim() != prevLine.trim() && nextLine.isNotEmpty() && delStart in prevLineStart..prevLineEnd) {
                val updated = nextText.removeRange(nextLineStart, nextLineEnd)
                return cleanupOrphanInlineMarkers(updated, nextLineStart)
            }
        }

        val prevPrefixMatch = linePrefixRegex.find(prevLine)
        if (prevPrefixMatch != null && prevPrefixMatch.range.first == 0) {
            val prefixLen = prevPrefixMatch.value.length
            if (delStart < prevLineStart + prefixLen && delEndPrev <= prevLineEnd) {
                val stillHasSamePrefix = linePrefixRegex.find(nextLine)?.value == prevPrefixMatch.value
                if (!stillHasSamePrefix) {
                    val cleanRemainder = prevLine.substring(prefixLen)
                    val rebuilt = nextText.substring(0, nextLineStart) + cleanRemainder + nextText.substring(nextLineEnd)
                    return cleanupOrphanInlineMarkers(rebuilt, nextLineStart)
                }
            }
        }

        // Handle multi-line selection deletion landing partway inside a line prefix on the end line
        if (delEndPrev > prevLineEnd && delEndPrev > delStart) {
            val endTouchOffset = (delEndPrev - 1).coerceIn(0, prevText.length)
            val endPrevLineStart = lineOffsetOf(prevText, endTouchOffset)
            val endPrevLineEnd = prevText.indexOf('\n', endTouchOffset).let { if (it < 0) prevText.length else it }
            val endPrevLine = prevText.substring(endPrevLineStart, endPrevLineEnd)
            val endPrefixMatch = linePrefixRegex.find(endPrevLine)
            if (endPrefixMatch != null && endPrefixMatch.range.first == 0) {
                val pLen = endPrefixMatch.value.length
                val clipEndInEndPrefix = (delEndPrev - endPrevLineStart).coerceIn(0, pLen)
                if (clipEndInEndPrefix in 1 until pLen) {
                    val brokenPrefixTail = endPrevLine.substring(clipEndInEndPrefix, pLen)
                    if (brokenPrefixTail.isNotEmpty() &&
                        nextText.startsWith(brokenPrefixTail, delStart.coerceIn(0, nextText.length))
                    ) {
                        val safeDelStart = delStart.coerceIn(0, nextText.length)
                        val rebuilt = nextText.removeRange(safeDelStart, safeDelStart + brokenPrefixTail.length)
                        return sanitizeDamagedLineMarkers(
                            prevText = prevText,
                            nextText = rebuilt,
                            editLineStart = lineOffsetOf(rebuilt, safeDelStart),
                            cursor = safeDelStart
                        )
                    }
                }
            }
        }

        return sanitizeDamagedLineMarkers(
            prevText = prevText,
            nextText = nextText,
            editLineStart = nextLineStart,
            cursor = rawCursor
        )
    }

    private fun sanitizeDamagedLineMarkers(
        prevText: String,
        nextText: String,
        editLineStart: Int,
        cursor: Int
    ): TextFieldValue {
        val baseCleaned = cleanupOrphanInlineMarkers(nextText, cursor)
        val text = baseCleaned.text
        var cur = baseCleaned.selection.max.coerceIn(0, text.length)
        val lineStart = lineOffsetOf(text, editLineStart.coerceIn(0, text.length))
        val lineEnd = text.indexOf('\n', lineStart).let { if (it < 0) text.length else it }
        var line = text.substring(lineStart, lineEnd)
        var lineCur = (cur - lineStart).coerceIn(0, line.length)

        fun removeRangeFromLine(start: Int, endExclusive: Int) {
            if (start < 0 || endExclusive <= start || endExclusive > line.length) return
            val removed = endExclusive - start
            line = line.removeRange(start, endExclusive)
            if (lineCur > start) {
                lineCur = (lineCur - removed).coerceAtLeast(start)
            }
        }

        // 0. Strip partially deleted divider or code fence line
        val trimmedLine = line.trim()
        if ((trimmedLine == "--" && prevText.contains("---")) ||
            ((trimmedLine == "``" || trimmedLine == "`") && prevText.contains("```") && !prevText.lines().any { it.trim() == trimmedLine })
        ) {
            line = ""
            lineCur = 0
        }

        // 1. Strip half-broken HTML underline tags anywhere on the edited line (e.g. `</u`, `<u`, `</`, or `u>` / `/u>`)
        val halfTagRegex = Regex("""</u(?![>])|</(?![u>])|<u(?![>])""")
        while (true) {
            val m = halfTagRegex.find(line) ?: break
            removeRangeFromLine(m.range.first, m.range.last + 1)
        }
        if (prevText.contains("<u>") || prevText.contains("</u>")) {
            val clippedOpenEnd = Regex("""(?<![<])/u>|(?<![</])u>""")
            while (true) {
                val m = clippedOpenEnd.find(line) ?: break
                removeRangeFromLine(m.range.first, m.range.last + 1)
            }
        }

        // Balance all <u> ... </u> pairs on the edited line (handles multiple spans on one line)
        if (prevText.contains("<u>") || prevText.contains("</u>")) {
            val openStack = ArrayDeque<Int>()
            val orphanCloseIndices = mutableListOf<Int>()
            var idx = 0
            while (idx < line.length) {
                when {
                    line.startsWith("<u>", idx) -> {
                        openStack.addLast(idx)
                        idx += 3
                    }
                    line.startsWith("</u>", idx) -> {
                        if (openStack.isNotEmpty()) {
                            openStack.removeLast()
                        } else {
                            orphanCloseIndices.add(idx)
                        }
                        idx += 4
                    }
                    else -> idx++
                }
            }
            val orphanRemovals = (openStack.map { it to (it + 3) } + orphanCloseIndices.map { it to (it + 4) })
                .sortedByDescending { it.first }
            for ((s, e) in orphanRemovals) {
                removeRangeFromLine(s, e)
            }
        }

        // 2. Clean broken double-char tokens (`~~` / `~`, `==` / `=`) and odd backticks (`)
        for ((tok, singleChar) in listOf("~~" to '~', "==" to '=')) {
            val countPrev = countOccurrences(prevText, tok)
            val countNow = countOccurrences(line, tok)
            if (countPrev % 2 == 0 && countNow % 2 == 1) {
                val idx = line.lastIndexOf(tok)
                if (idx >= 0) removeRangeFromLine(idx, idx + tok.length)
            }
            if (prevText.contains(tok) && !prevText.replace(tok, "").contains(singleChar)) {
                while (true) {
                    val badIdx = line.indices.firstOrNull { i ->
                        line[i] == singleChar &&
                            (i == 0 || line[i - 1] != singleChar) &&
                            (i + 1 >= line.length || line[i + 1] != singleChar)
                    } ?: break
                    removeRangeFromLine(badIdx, badIdx + 1)
                }
            }
        }
        if (!trimmedLine.startsWith("```")) {
            val countPrevCode = countOccurrences(prevText, "`")
            val countNowCode = countOccurrences(line, "`")
            if (countPrevCode % 2 == 0 && countNowCode % 2 == 1) {
                val idx = line.lastIndexOf('`')
                if (idx >= 0) removeRangeFromLine(idx, idx + 1)
            }
        }

        // 3. Clean orphaned `*` / `**` / `***` outside valid matched spans and outside code spans
        val prevHadAsteriskSyntax = parseInlineSpans(prevText).any { it.openTag.contains('*') } ||
            prevText.contains("**") || prevText.contains("***")
        if (prevHadAsteriskSyntax && line.contains('*') && trimmedLine != "***" && !line.startsWith("* ")) {
            val validSpans = parseInlineSpans(line)
            val protectedMask = BooleanArray(line.length)
            for (span in validSpans) {
                if (span.kind == InlineKind.CODE) {
                    for (k in span.rawStart until span.rawEnd.coerceAtMost(line.length)) {
                        protectedMask[k] = true
                    }
                } else {
                    for (k in span.rawStart until span.innerStart.coerceAtMost(line.length)) {
                        protectedMask[k] = true
                    }
                    for (k in span.innerEnd until span.rawEnd.coerceAtMost(line.length)) {
                        protectedMask[k] = true
                    }
                }
            }
            val orphanStars = line.indices.filter { k -> line[k] == '*' && !protectedMask[k] }
                .sortedDescending()
            for (starIdx in orphanStars) {
                removeRangeFromLine(starIdx, starIdx + 1)
            }
        }

        val out = text.substring(0, lineStart) + line + text.substring(lineEnd)
        cur = (lineStart + lineCur).coerceIn(0, out.length)
        return cleanupOrphanInlineMarkers(out, cur)
    }

    private fun countOccurrences(haystack: String, needle: String): Int {
        if (needle.isEmpty()) return 0
        var count = 0
        var idx = haystack.indexOf(needle)
        while (idx >= 0) {
            count++
            idx = haystack.indexOf(needle, idx + needle.length)
        }
        return count
    }

    private fun wrapTextWithStyles(raw: String, styles: Set<RichFormatAction>): String {
        var result = raw
        for (style in inlineStyleOrder) {
            if (style !in styles) continue
            result = when (style) {
                RichFormatAction.CODE -> "`$result`"
                RichFormatAction.ITALIC -> "*$result*"
                RichFormatAction.BOLD -> "**$result**"
                RichFormatAction.UNDERLINE -> "<u>$result</u>"
                RichFormatAction.STRIKETHROUGH -> "~~$result~~"
                RichFormatAction.HIGHLIGHT -> "==$result=="
                else -> result
            }
        }
        return result
    }

    private fun mergeSeamInlineMarkers(
        leftText: String,
        wrapped: String,
        rightText: String,
        pendingStyles: Set<RichFormatAction>
    ): Pair<String, Int> {
        var left = leftText
        var mid = wrapped
        var right = rightText

        // 1. Merge matching adjacent outer wrapper tokens (outer-to-inner) at the left seam
        val outerToInnerPairs = listOf(
            RichFormatAction.HIGHLIGHT to ("==" to "=="),
            RichFormatAction.STRIKETHROUGH to ("~~" to "~~"),
            RichFormatAction.UNDERLINE to ("</u>" to "<u>"),
            RichFormatAction.CODE to ("`" to "`")
        )
        for ((style, pair) in outerToInnerPairs) {
            if (style in pendingStyles && left.endsWith(pair.first) && mid.startsWith(pair.second)) {
                left = left.dropLast(pair.first.length)
                mid = mid.drop(pair.second.length)
            }
        }

        // 2. Exact asterisk seam merging for BOLD (**), ITALIC (*), or BOTH (***)
        val leftStars = left.takeLastWhile { it == '*' }.length
        val hasBold = RichFormatAction.BOLD in pendingStyles
        val hasItalic = RichFormatAction.ITALIC in pendingStyles
        var starsToStrip = 0
        if (hasBold && (leftStars == 2 || (leftStars == 3 && hasItalic))) {
            starsToStrip += 2
        }
        if (hasItalic && (leftStars == 1 || (leftStars == 3 && hasBold))) {
            starsToStrip += 1
        }
        if (starsToStrip > 0 && mid.startsWith("*".repeat(starsToStrip))) {
            left = left.dropLast(starsToStrip)
            mid = mid.drop(starsToStrip)
        }

        // 3. Also merge closing tags cleanly if typing right before a matching opening tag at right seam
        for ((style, pair) in outerToInnerPairs) {
            val closeLeft = if (style == RichFormatAction.UNDERLINE) "</u>" else pair.first
            val openRight = if (style == RichFormatAction.UNDERLINE) "<u>" else pair.second
            if (style in pendingStyles && mid.endsWith(closeLeft) && right.startsWith(openRight)) {
                mid = mid.dropLast(closeLeft.length)
                right = right.drop(openRight.length)
            }
        }

        val combined = left + mid + right
        val cursor = (left.length + mid.length).coerceIn(0, combined.length)
        return combined to cursor
    }

    private fun cleanupOrphanInlineMarkers(text: String, cursor: Int): TextFieldValue {
        var out = text
        var cur = cursor
        val emptyShells = listOf("******", "****", "<u></u>", "~~~~", "====", "``")
        var changed = true
        while (changed) {
            changed = false
            for (shell in emptyShells) {
                val idx = out.indexOf(shell)
                if (idx >= 0) {
                    out = out.removeRange(idx, idx + shell.length)
                    if (cur > idx) cur = (cur - shell.length).coerceAtLeast(idx)
                    changed = true
                }
            }
        }
        return TextFieldValue(out, TextRange(cur.coerceIn(0, out.length)))
    }

    private fun kindForAction(action: RichFormatAction): InlineKind? = when (action) {
        RichFormatAction.BOLD -> InlineKind.BOLD
        RichFormatAction.ITALIC -> InlineKind.ITALIC
        RichFormatAction.UNDERLINE -> InlineKind.UNDERLINE
        RichFormatAction.STRIKETHROUGH -> InlineKind.STRIKETHROUGH
        RichFormatAction.HIGHLIGHT -> InlineKind.HIGHLIGHT
        RichFormatAction.CODE -> InlineKind.CODE
        else -> null
    }

    private fun isCursorInsideInlineKind(text: String, cursor: Int, kind: InlineKind): Boolean {
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it < 0) text.length else it }
        if (cursor < lineStart || cursor > lineEnd) return false
        val lineCursor = cursor - lineStart
        val spans = parseInlineSpans(text.substring(lineStart, lineEnd))
        return spans.any { matchesInlineKind(it.kind, kind) && lineCursor in it.innerStart..it.innerEnd }
    }

    private fun isWordChar(c: Char): Boolean =
        c.isLetterOrDigit() || c == '_' || c.category == CharCategory.NON_SPACING_MARK

    private fun normalizeInlineSelectionRange(text: String, rawStart: Int, rawEnd: Int): Pair<Int, Int> {
        var start = rawStart
        var end = rawEnd
        // Do not eat a line-start block prefix (e.g. `# `, `- [ ] `, `> `) into inline markers
        val startLineStart = lineOffsetOf(text, start)
        if (start == startLineStart) {
            val restOfLine = text.substring(startLineStart, text.indexOf('\n', startLineStart).let { if (it < 0) text.length else it })
            val prefixMatch = linePrefixRegex.find(restOfLine)
            if (prefixMatch != null && prefixMatch.range.first == 0) {
                val pEnd = startLineStart + prefixMatch.value.length
                if (pEnd < end) start = pEnd
            }
        }

        // Expand `start` to include opening markers when visual selection begins at `innerStart`
        // and extends past the span's `rawEnd` (prevents splitting `*hello* world` into `hello* world`).
        val startLineEnd = text.indexOf('\n', startLineStart).let { if (it < 0) text.length else it }
        val startLineSpans = parseInlineSpans(text.substring(startLineStart, startLineEnd))
        var expanded = true
        while (expanded) {
            expanded = false
            for (span in startLineSpans) {
                val absRawStart = startLineStart + span.rawStart
                val absInnerStart = startLineStart + span.innerStart
                val absRawEnd = startLineStart + span.rawEnd
                if (absRawStart < start && start == absInnerStart && end > absRawEnd) {
                    start = absRawStart
                    expanded = true
                    break
                }
            }
        }

        // Trim closing marker accidentally absorbed by VisualTransformation's OffsetMapping
        // when selecting up to the visual end of any span (`*`, `**`, `***`, `<u>`, `~~`, `==`, `` ` ``).
        val endLineStart = lineOffsetOf(text, end)
        val endLineEnd = text.indexOf('\n', endLineStart).let { if (it < 0) text.length else it }
        val endLineSpans = parseInlineSpans(text.substring(endLineStart, endLineEnd))
        var trimmed = true
        while (trimmed) {
            trimmed = false
            for (span in endLineSpans) {
                val absRawStart = endLineStart + span.rawStart
                val absInnerStart = endLineStart + span.innerStart
                val absInnerEnd = endLineStart + span.innerEnd
                val absRawEnd = endLineStart + span.rawEnd
                if (absRawStart < start && absInnerStart <= start && end == absRawEnd && absInnerEnd < absRawEnd) {
                    end = absInnerEnd
                    trimmed = true
                    break
                }
            }
        }
        return start to end.coerceAtLeast(start)
    }

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

            // Case B: cursor inside existing span of the same tag -> unwrap it cleanly (handling *** for bold too)
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

        // 2. Multi-line selection -> toggle line-by-line preserving line prefixes and block separators
        val (normStart, normEnd) = normalizeInlineSelectionRange(text, start, end)
        if (normStart >= normEnd) {
            return toggleInlineWrap(TextFieldValue(text, TextRange(normStart)), openTag, closeTag, isBold)
        }
        val selected = text.substring(normStart, normEnd)
        if (selected.contains('\n')) {
            return applyInlineAcrossLines(text, normStart, normEnd, openTag, closeTag, isBold)
        }

        val wrappedOutside = normStart >= openTag.length &&
            normEnd + closeTag.length <= text.length &&
            text.substring(normStart - openTag.length, normStart) == openTag &&
            text.substring(normEnd, normEnd + closeTag.length) == closeTag
        if (wrappedOutside) {
            val updated = text.substring(0, normStart - openTag.length) + selected + text.substring(normEnd + closeTag.length)
            val newStart = normStart - openTag.length
            return TextFieldValue(updated, TextRange(newStart, newStart + selected.length))
        }

        val wrappedInside = selected.length >= openTag.length + closeTag.length &&
            selected.startsWith(openTag) &&
            selected.endsWith(closeTag)
        if (wrappedInside) {
            val unwrapped = selected.substring(openTag.length, selected.length - closeTag.length)
            val updated = text.substring(0, normStart) + unwrapped + text.substring(normEnd)
            return TextFieldValue(updated, TextRange(normStart, normStart + unwrapped.length))
        }

        // If selection sits inside an enclosing span of the same tag, unwrap that enclosing span cleanly
        val enclosing = findEnclosingTagSpan(text, normStart, openTag, closeTag, isBold)
        if (enclosing != null && enclosing.first <= normStart && normEnd <= enclosing.second) {
            val (spanStart, spanEnd) = enclosing
            val inner = text.substring(spanStart + openTag.length, spanEnd - closeTag.length)
            val updated = text.substring(0, spanStart) + inner + text.substring(spanEnd)
            val newStart = (normStart - openTag.length).coerceIn(spanStart, updated.length)
            val newEnd = (normEnd - openTag.length).coerceIn(newStart, updated.length)
            return TextFieldValue(updated, TextRange(newStart, newEnd))
        }

        // Strip any redundant inner markers of the exact same tag before wrapping once
        val cleanSelected = if (openTag == closeTag) {
            selected.replace(openTag, "")
        } else {
            selected.replace(openTag, "").replace(closeTag, "")
        }
        val updated = text.substring(0, normStart) + openTag + cleanSelected + closeTag + text.substring(normEnd)
        return TextFieldValue(
            text = updated,
            selection = TextRange(normStart + openTag.length, normStart + openTag.length + cleanSelected.length)
        )
    }

    private fun applyInlineAcrossLines(
        text: String,
        selStart: Int,
        selEnd: Int,
        openTag: String,
        closeTag: String,
        isBold: Boolean,
        isItalic: Boolean = false
    ): TextFieldValue {
        val rawSlice = text.substring(selStart, selEnd)
        val lines = rawSlice.split('\n')
        val outLines = lines.mapIndexed { idx, part ->
            val trimmed = part.trim()
            if (trimmed.isEmpty() || trimmed == "---" || trimmed == "***" || trimmed.startsWith("```") || trimmed.startsWith("|")) {
                part
            } else {
                val atLineStart = idx > 0 || selStart == 0 || text[selStart - 1] == '\n'
                val prefixLen = if (atLineStart) {
                    linePrefixRegex.find(part)?.takeIf { it.range.first == 0 }?.value?.length ?: 0
                } else {
                    0
                }
                val prefix = part.substring(0, prefixLen)
                val body = part.substring(prefixLen)
                if (body.isEmpty()) {
                    part
                } else {
                    val subVal = TextFieldValue(body, TextRange(0, body.length))
                    val res = if (isItalic) toggleItalicWrap(subVal) else toggleInlineWrap(subVal, openTag, closeTag, isBold)
                    prefix + res.text
                }
            }
        }
        val replaced = outLines.joinToString("\n")
        val updated = text.substring(0, selStart) + replaced + text.substring(selEnd)
        return TextFieldValue(updated, TextRange(selStart, (selStart + replaced.length).coerceIn(selStart, updated.length)))
    }

    /**
     * Separate italic handler so single-asterisk `*` never clashes with double-asterisk `**` bold,
     * and handles multi-word / Arabic Tashkeel / mixed bold+italic selections cleanly.
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
            return TextFieldValue(updated, TextRange(start + 1))
        }

        val (normStart, normEnd) = normalizeInlineSelectionRange(text, start, end)
        if (normStart >= normEnd) {
            return toggleItalicWrap(TextFieldValue(text, TextRange(normStart)))
        }
        val selected = text.substring(normStart, normEnd)
        if (selected.contains('\n')) {
            return applyInlineAcrossLines(text, normStart, normEnd, "*", "*", isBold = false, isItalic = true)
        }

        if (hasSingleItalicAtBoundary(normStart, normEnd)) {
            val updated = text.substring(0, normStart - 1) + selected + text.substring(normEnd + 1)
            return TextFieldValue(updated, TextRange(normStart - 1, normStart - 1 + selected.length))
        }

        val selSpans = parseInlineSpans(selected)
        val fullItalicSpan = selSpans.firstOrNull {
            (it.kind == InlineKind.ITALIC || it.kind == InlineKind.BOLD_ITALIC) &&
                it.rawStart == 0 && it.rawEnd == selected.length
        }
        if (fullItalicSpan != null) {
            val unwrapped = selected.substring(1, selected.length - 1)
            val updated = text.substring(0, normStart) + unwrapped + text.substring(normEnd)
            return TextFieldValue(updated, TextRange(normStart, normStart + unwrapped.length))
        }

        val enclosing = findEnclosingSingleAsteriskSpan(text, normStart)
        if (enclosing != null && enclosing.first <= normStart && normEnd <= enclosing.second) {
            val (s, e) = enclosing
            val inner = text.substring(s + 1, e - 1)
            val updated = text.substring(0, s) + inner + text.substring(e)
            val newStart = (normStart - 1).coerceIn(s, updated.length)
            val newEnd = (normEnd - 1).coerceIn(newStart, updated.length)
            return TextFieldValue(updated, TextRange(newStart, newEnd))
        }

        // If selection contains inner italic spans (e.g. `*hello* world` or `*hello* *world*`),
        // strip inner single italic markers cleanly without breaking `**bold**` or `` `code` ``.
        val italicSpansInSel = selSpans.filter { it.kind == InlineKind.ITALIC || it.kind == InlineKind.BOLD_ITALIC }
        var cleanSelected = selected
        if (italicSpansInSel.isNotEmpty()) {
            val covered = BooleanArray(selected.length)
            for (sp in italicSpansInSel) {
                for (k in sp.rawStart until sp.rawEnd.coerceAtMost(selected.length)) {
                    covered[k] = true
                }
            }
            val hasNonWs = selected.any { !it.isWhitespace() }
            val allAlreadyItalic = hasNonWs && selected.indices.all { selected[it].isWhitespace() || covered[it] }

            val starRemovals = italicSpansInSel
                .flatMap { listOf(it.rawStart, it.innerEnd) }
                .distinct()
                .sortedDescending()
            val sb = StringBuilder(selected)
            for (idx in starRemovals) {
                if (idx in 0 until sb.length && sb[idx] == '*') {
                    sb.deleteCharAt(idx)
                }
            }
            cleanSelected = sb.toString()

            if (allAlreadyItalic) {
                val updated = text.substring(0, normStart) + cleanSelected + text.substring(normEnd)
                return TextFieldValue(updated, TextRange(normStart, normStart + cleanSelected.length))
            }
        }

        // Wrap `cleanSelected` in italic while avoiding `***` ambiguity at partial bold boundaries
        val cleanSpans = parseInlineSpans(cleanSelected)
        val fullBold = cleanSpans.any { it.kind == InlineKind.BOLD && it.rawStart == 0 && it.rawEnd == cleanSelected.length }
        val wrapped = if (!fullBold && (cleanSelected.startsWith("**") || cleanSelected.endsWith("**"))) {
            wrapRunsSafelyWithItalic(cleanSelected)
        } else {
            "*$cleanSelected*"
        }
        val updated = text.substring(0, normStart) + wrapped + text.substring(normEnd)
        val selStart = if (wrapped.startsWith("*") && !cleanSelected.startsWith("**")) normStart + 1 else normStart
        val selEnd = if (wrapped.endsWith("*") && !cleanSelected.endsWith("**") && selStart > normStart) {
            normStart + wrapped.length - 1
        } else {
            normStart + wrapped.length
        }
        return TextFieldValue(updated, TextRange(selStart, selEnd.coerceIn(selStart, updated.length)))
    }

    private fun wrapRunsSafelyWithItalic(segment: String): String {
        val sb = StringBuilder(segment.length + 8)
        var idx = 0
        while (idx < segment.length) {
            val sub = segment.substring(idx)
            val m = firstInlineMatch(sub)
            if (m != null && m.rawStart == 0 && m.kind == InlineKind.BOLD) {
                sb.append("***").append(segment.substring(idx + 2, idx + m.innerEnd)).append("***")
                idx += m.rawEnd
            } else {
                val nextBold = if (m != null && m.kind == InlineKind.BOLD) idx + m.rawStart else segment.length
                val plain = segment.substring(idx, nextBold)
                if (plain.isNotBlank()) {
                    val leadWs = plain.takeWhile { it.isWhitespace() }
                    val trailWs = plain.takeLastWhile { it.isWhitespace() }
                    val core = plain.substring(leadWs.length, plain.length - trailWs.length)
                    sb.append(leadWs).append('*').append(core).append('*').append(trailWs)
                } else {
                    sb.append(plain)
                }
                idx = nextBold
            }
        }
        return sb.toString()
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
            if (cursor in absInnerStart..absInnerEnd) {
                if (span.openTag == openTag && span.closeTag == closeTag && (!isBold || openTag == "**")) {
                    return (lineStart + span.rawStart) to (lineStart + span.rawEnd)
                }
                if (isBold && span.kind == InlineKind.BOLD_ITALIC) {
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
            if (span.kind == InlineKind.ITALIC || span.kind == InlineKind.BOLD_ITALIC) {
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
                    InlineKind.BOLD_ITALIC -> {
                        active.add(RichFormatAction.BOLD)
                        active.add(RichFormatAction.ITALIC)
                    }
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
        val needLeadingNl = startOffset > 0 && text[startOffset - 1] != '\n'
        val needTrailingNl = endOffset < text.length && text[endOffset] != '\n'
        val replacement = (if (needLeadingNl) "\n" else "") + tableMarkdown + (if (needTrailingNl) "\n" else "")
        val updated = text.substring(0, startOffset) + replacement + text.substring(endOffset)
        val nextCursor = (startOffset + replacement.length).coerceIn(0, updated.length)
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
        BOLD_ITALIC, BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, CODE, HIGHLIGHT
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

    private fun parseInlineSpans(line: String, baseOffset: Int = 0): List<InlineSpan> {
        val result = mutableListOf<InlineSpan>()
        var i = 0
        while (i < line.length) {
            when {
                line.startsWith("***", i) -> {
                    val close = findClosingTripleAsterisk(line, i + 3)
                    if (close >= 0) {
                        result.add(InlineSpan(InlineKind.BOLD_ITALIC, "***", "***", baseOffset + i, baseOffset + i + 3, baseOffset + close, baseOffset + close + 3))
                        result.addAll(parseInlineSpans(line.substring(i + 3, close), baseOffset + i + 3))
                        i = close + 3
                        continue
                    }
                    val closeBold = findClosingBold(line, i + 2)
                    if (closeBold >= 0) {
                        result.add(InlineSpan(InlineKind.BOLD, "**", "**", baseOffset + i, baseOffset + i + 2, baseOffset + closeBold, baseOffset + closeBold + 2))
                        result.addAll(parseInlineSpans(line.substring(i + 2, closeBold), baseOffset + i + 2))
                        i = closeBold + 2
                        continue
                    }
                }
                line.startsWith("**", i) -> {
                    val close = findClosingBold(line, i + 2)
                    if (close >= 0) {
                        result.add(InlineSpan(InlineKind.BOLD, "**", "**", baseOffset + i, baseOffset + i + 2, baseOffset + close, baseOffset + close + 2))
                        result.addAll(parseInlineSpans(line.substring(i + 2, close), baseOffset + i + 2))
                        i = close + 2
                        continue
                    }
                }
                line.startsWith("<u>", i) -> {
                    val close = line.indexOf("</u>", i + 3)
                    if (close >= 0) {
                        result.add(InlineSpan(InlineKind.UNDERLINE, "<u>", "</u>", baseOffset + i, baseOffset + i + 3, baseOffset + close, baseOffset + close + 4))
                        result.addAll(parseInlineSpans(line.substring(i + 3, close), baseOffset + i + 3))
                        i = close + 4
                        continue
                    }
                }
                line.startsWith("~~", i) -> {
                    val close = line.indexOf("~~", i + 2)
                    if (close >= 0) {
                        result.add(InlineSpan(InlineKind.STRIKETHROUGH, "~~", "~~", baseOffset + i, baseOffset + i + 2, baseOffset + close, baseOffset + close + 2))
                        result.addAll(parseInlineSpans(line.substring(i + 2, close), baseOffset + i + 2))
                        i = close + 2
                        continue
                    }
                }
                line.startsWith("==", i) -> {
                    val close = line.indexOf("==", i + 2)
                    if (close >= 0) {
                        result.add(InlineSpan(InlineKind.HIGHLIGHT, "==", "==", baseOffset + i, baseOffset + i + 2, baseOffset + close, baseOffset + close + 2))
                        result.addAll(parseInlineSpans(line.substring(i + 2, close), baseOffset + i + 2))
                        i = close + 2
                        continue
                    }
                }
                line[i] == '`' -> {
                    val close = line.indexOf('`', i + 1)
                    if (close >= 0) {
                        result.add(InlineSpan(InlineKind.CODE, "`", "`", baseOffset + i, baseOffset + i + 1, baseOffset + close, baseOffset + close + 1))
                        i = close + 1
                        continue
                    }
                }
                line[i] == '*' && !line.startsWith("**", i) -> {
                    val close = findClosingSingleAsterisk(line, i + 1)
                    if (close > i) {
                        result.add(InlineSpan(InlineKind.ITALIC, "*", "*", baseOffset + i, baseOffset + i + 1, baseOffset + close, baseOffset + close + 1))
                        result.addAll(parseInlineSpans(line.substring(i + 1, close), baseOffset + i + 1))
                        i = close + 1
                        continue
                    }
                }
            }
            i++
        }
        return result
    }

    private fun findClosingTripleAsterisk(line: String, from: Int): Int {
        var idx = from
        while (idx <= line.length - 3) {
            if (line.startsWith("***", idx)) {
                return idx
            }
            idx++
        }
        return -1
    }

    private fun findClosingBold(line: String, from: Int): Int {
        var idx = from
        var unmatchedSingle = false
        while (idx < line.length - 1) {
            if (line[idx] == '*') {
                var count = 0
                while (idx + count < line.length && line[idx + count] == '*') count++
                if (count == 1) {
                    unmatchedSingle = !unmatchedSingle
                    idx += 1
                    continue
                }
                if (count == 3 && unmatchedSingle) {
                    return idx + 1
                }
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
            InlineKind.BOLD_ITALIC -> SpanStyle(
                fontFamily = EditorFontFamily,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All
            )
            InlineKind.BOLD -> SpanStyle(
                fontFamily = EditorFontFamily,
                fontWeight = FontWeight.Bold,
                fontSynthesis = FontSynthesis.All
            )
            InlineKind.ITALIC -> SpanStyle(
                fontFamily = EditorFontFamily,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All
            )
            InlineKind.UNDERLINE -> SpanStyle(
                fontFamily = EditorFontFamily,
                textDecoration = TextDecoration.Underline
            )
            InlineKind.STRIKETHROUGH -> SpanStyle(
                fontFamily = EditorFontFamily,
                textDecoration = TextDecoration.LineThrough
            )
            InlineKind.CODE -> SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = accentColor.copy(alpha = if (isDark) 0.24f else 0.14f),
                color = accentColor
            )
            InlineKind.HIGHLIGHT -> SpanStyle(
                fontFamily = EditorFontFamily,
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
                sub.startsWith("***", i) -> {
                    val close = findClosingTripleAsterisk(sub, i + 3)
                    if (close >= 0) return InlineSpan(InlineKind.BOLD_ITALIC, "***", "***", i, i + 3, close, close + 3)
                    val closeBold = findClosingBold(sub, i + 2)
                    if (closeBold >= 0) return InlineSpan(InlineKind.BOLD, "**", "**", i, i + 2, closeBold, closeBold + 2)
                }
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
                emitMappedReplacement(
                    lineStart,
                    lineEnd,
                    if (inCodeFence) "⟨ Code ⟩" else "⟨/⟩",
                    SpanStyle(
                        color = if (inCodeFence) accentColor.copy(alpha = 0.78f) else metaColor.copy(alpha = 0.45f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
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
                            SpanStyle(
                                fontFamily = EditorFontFamily,
                                fontSynthesis = FontSynthesis.All,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        )
                    }
                    line.startsWith("## ") -> {
                        advanceHidden(lineStart + 3)
                        emitInlineWithOffsets(
                            lineStart + 3,
                            lineEnd,
                            SpanStyle(
                                fontFamily = EditorFontFamily,
                                fontSynthesis = FontSynthesis.All,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                    line.startsWith("# ") -> {
                        advanceHidden(lineStart + 2)
                        emitInlineWithOffsets(
                            lineStart + 2,
                            lineEnd,
                            SpanStyle(
                                fontFamily = EditorFontFamily,
                                fontSynthesis = FontSynthesis.All,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
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
                            SpanStyle(
                                fontFamily = EditorFontFamily,
                                fontSynthesis = FontSynthesis.All,
                                color = metaColor,
                                textDecoration = TextDecoration.LineThrough
                            )
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
                                fontFamily = EditorFontFamily,
                                fontSynthesis = FontSynthesis.All,
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
                            SpanStyle(
                                fontFamily = EditorFontFamily,
                                fontStyle = FontStyle.Italic,
                                fontSynthesis = FontSynthesis.All,
                                color = metaColor
                            )
                        )
                    }
                    line.trim() == "---" || line.trim() == "***" -> {
                        emitMappedReplacement(
                            lineStart,
                            lineEnd,
                            "────────────────────",
                            SpanStyle(color = metaColor.copy(alpha = 0.42f))
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
