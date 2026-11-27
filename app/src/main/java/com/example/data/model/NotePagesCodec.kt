package com.example.data.model

import org.json.JSONArray
import java.util.Locale

/**
 * Codec and policy helpers for multi-page note drafts.
 * - Page 1 is stored in [NoteEntity.content] for zero-overhead previews & backwards compatibility.
 * - Pages 2..100 are stored as a JSON array string in [NoteEntity.pagesJson].
 * - Optional per-page titles are encoded cleanly inside each page entry so they persist
 *   across Room, local JSON backups, Vault transfers, and Firebase sync without schema changes.
 * - Hard limits per single note: up to 100 pages OR total UTF-8 size <= 900 KB (921,600 bytes).
 */
object NotePagesCodec {
    const val MAX_FREE_NOTE_PAGES = 5
    const val MAX_NOTE_PAGES = 100
    const val MAX_NOTE_BYTES = 900L * 1024L // 921,600 bytes (900 KB)

    private const val PAGE_TITLE_PREFIX = "[[FLUFFY_PAGE_TITLE:"
    private const val PAGE_TITLE_SUFFIX = "]]"

    data class DraftPage(
        val title: String = "",
        val body: String = ""
    )

    fun extractPageTitle(rawPage: String): String {
        if (!rawPage.startsWith(PAGE_TITLE_PREFIX)) return ""
        val firstNewline = rawPage.indexOf('\n')
        val header = if (firstNewline >= 0) rawPage.substring(0, firstNewline) else rawPage
        if (!header.endsWith(PAGE_TITLE_SUFFIX)) return ""
        val encoded = header.substring(PAGE_TITLE_PREFIX.length, header.length - PAGE_TITLE_SUFFIX.length)
        return unescapeTitle(encoded)
    }

    fun extractPageBody(rawPage: String): String {
        if (!rawPage.startsWith(PAGE_TITLE_PREFIX)) return rawPage
        val firstNewline = rawPage.indexOf('\n')
        val header = if (firstNewline >= 0) rawPage.substring(0, firstNewline) else rawPage
        if (!header.endsWith(PAGE_TITLE_SUFFIX)) return rawPage
        return if (firstNewline >= 0 && firstNewline + 1 <= rawPage.length) {
            rawPage.substring(firstNewline + 1)
        } else {
            ""
        }
    }

    fun encodePageEntry(title: String, body: String): String {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return body
        val header = "$PAGE_TITLE_PREFIX${escapeTitle(cleanTitle)}$PAGE_TITLE_SUFFIX"
        return if (body.isEmpty()) "$header\n" else "$header\n$body"
    }

    private fun escapeTitle(raw: String): String = raw
        .replace("\\", "\\\\")
        .replace("\n", " ")
        .replace("\r", "")
        .replace("]]", "\\]\\]")

    private fun unescapeTitle(encoded: String): String = encoded
        .replace("\\]\\]", "]]")
        .replace("\\\\", "\\")

    fun maxPagesForTier(isPremium: Boolean): Int =
        if (isPremium) MAX_NOTE_PAGES else MAX_FREE_NOTE_PAGES

    fun decodePages(content: String, pagesJson: String): List<String> {
        if (pagesJson.isBlank()) {
            return listOf(content)
        }
        val extra = mutableListOf<String>()
        try {
            val arr = JSONArray(pagesJson)
            val limit = minOf(arr.length(), MAX_NOTE_PAGES - 1)
            for (i in 0 until limit) {
                extra.add(arr.optString(i, ""))
            }
        } catch (_: Exception) {
            // Fallback gracefully if malformed
        }
        return listOf(content) + extra
    }

    fun decodeDraftPages(content: String, pagesJson: String): List<DraftPage> {
        return decodePages(content, pagesJson).map { raw ->
            DraftPage(
                title = extractPageTitle(raw),
                body = extractPageBody(raw)
            )
        }
    }

    fun encodeExtraPages(pages: List<String>): String {
        if (pages.size <= 1) return ""
        val arr = JSONArray()
        val limit = minOf(pages.size, MAX_NOTE_PAGES)
        for (i in 1 until limit) {
            arr.put(pages[i])
        }
        return arr.toString()
    }

    fun encodeDraftPages(pages: List<DraftPage>): Pair<String, String> {
        if (pages.isEmpty()) return "" to ""
        val encoded = pages.map { encodePageEntry(it.title, it.body) }
        return encoded.first() to encodeExtraPages(encoded)
    }

    /**
     * Pure deterministic helper for removing a page at [index] without dropping below 1 page.
     * Returns updated list + safe next active page index.
     */
    fun <T> removePageSafely(
        pages: List<T>,
        index: Int,
        currentPage: Int
    ): Pair<List<T>, Int> {
        if (pages.size <= 1 || index !in pages.indices) {
            return pages to currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        }
        val mutable = pages.toMutableList()
        mutable.removeAt(index)
        val nextPage = when {
            currentPage == index -> (index - 1).coerceAtLeast(0)
            currentPage > index -> currentPage - 1
            else -> currentPage
        }.coerceIn(0, mutable.lastIndex)
        return mutable to nextPage
    }

    fun pageCount(note: NoteEntity): Int {
        return decodePages(note.content, note.pagesJson).size
    }

    fun hasAnyContent(content: String, pagesJson: String): Boolean {
        if (extractPageTitle(content).isNotEmpty() || extractPageBody(content).isNotEmpty()) return true
        return decodeDraftPages(content, pagesJson).any { it.title.isNotEmpty() || it.body.isNotEmpty() }
    }

    fun hasAnyContent(pages: List<String>): Boolean {
        return pages.any { extractPageTitle(it).isNotEmpty() || extractPageBody(it).isNotEmpty() }
    }

    fun searchableBody(note: NoteEntity): String {
        val decoded = decodeDraftPages(note.content, note.pagesJson)
        return decoded.joinToString("\n") { page ->
            val parts = listOfNotNull(
                page.title.takeIf { it.isNotBlank() },
                RichTextCodec.stripFormattingForPreview(page.body).takeIf { it.isNotBlank() }
            )
            parts.joinToString("\n")
        }
    }

    /**
     * Clean human-readable snippet for cards & search results (strips internal title tags,
     * markdown delimiters, and formats pipe tables cleanly).
     */
    fun plainTextSnippet(rawPage: String): String {
        val pageTitle = extractPageTitle(rawPage)
        val cleanBody = RichTextCodec.stripFormattingForPreview(extractPageBody(rawPage)).trim()
        return when {
            pageTitle.isNotBlank() && cleanBody.isNotBlank() -> "$pageTitle\n$cleanBody"
            cleanBody.isNotBlank() -> cleanBody
            pageTitle.isNotBlank() -> pageTitle
            else -> ""
        }
    }

    fun plainTextSnippet(note: NoteEntity): String {
        return plainTextSnippet(note.content)
    }

    fun countWords(rawPages: List<String>): Int {
        return rawPages.sumOf { raw ->
            val pageTitle = extractPageTitle(raw)
            val body = RichTextCodec.stripFormattingForPreview(extractPageBody(raw))
            val titleWords = if (pageTitle.isBlank()) 0 else pageTitle.trim().split("\\s+".toRegex()).size
            val bodyWords = if (body.isBlank()) 0 else body.trim().split("\\s+".toRegex()).size
            titleWords + bodyWords
        }
    }

    fun buildShareableDraftText(
        title: String,
        rawPages: List<String>,
        checklist: List<ChecklistItem>,
        isArabic: Boolean
    ): String {
        val sb = StringBuilder()
        if (title.isNotBlank()) sb.append("$title\n\n")
        val draftPages = rawPages.map { DraftPage(extractPageTitle(it), extractPageBody(it)) }
        if (draftPages.size <= 1) {
            val single = draftPages.firstOrNull()
            if (single != null) {
                if (single.title.isNotBlank()) sb.append("${single.title}\n")
                val cleanBody = RichTextCodec.stripFormattingForPreview(single.body)
                if (cleanBody.isNotBlank()) sb.append("$cleanBody\n\n")
            }
        } else {
            draftPages.forEachIndexed { idx, page ->
                val cleanBody = RichTextCodec.stripFormattingForPreview(page.body)
                if (page.title.isNotBlank() || cleanBody.isNotBlank()) {
                    val baseLabel = if (isArabic) "صفحة ${idx + 1}" else "Page ${idx + 1}"
                    val label = if (page.title.isNotBlank()) "--- $baseLabel: ${page.title} ---" else "--- $baseLabel ---"
                    sb.append("$label\n")
                    if (cleanBody.isNotBlank()) sb.append("$cleanBody\n")
                    sb.append('\n')
                }
            }
        }
        if (checklist.isNotEmpty()) {
            checklist.forEach {
                val mark = if (it.isDone) "[✓]" else "[ ]"
                sb.append("$mark ${it.text}\n")
            }
        }
        sb.append("\n— Shared from Fluffy Notes ✨")
        return sb.toString()
    }

    /**
     * Fast zero-allocation UTF-8 byte counter so keystroke byte-budget checks in Compose
     * never allocate heap ByteArrays or trigger GC pauses on large multi-page notes.
     */
    fun utf8ByteLength(seq: CharSequence, start: Int = 0, end: Int = seq.length): Long {
        var count = 0L
        var i = start.coerceIn(0, seq.length)
        val len = end.coerceIn(i, seq.length)
        while (i < len) {
            val c = seq[i]
            when {
                c.code < 0x80 -> count += 1L
                c.code < 0x800 -> count += 2L
                Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(seq[i + 1]) -> {
                    count += 4L
                    i++
                }
                else -> count += 3L
            }
            i++
        }
        return count
    }

    fun estimateNoteBytes(
        title: String,
        pages: List<String>,
        checklist: List<ChecklistItem>
    ): Long {
        val page1Bytes = utf8ByteLength(pages.firstOrNull() ?: "")
        val extraPagesBytes = if (pages.size <= 1) 0L else {
            val limit = minOf(pages.size, MAX_NOTE_PAGES)
            var sum = 2L // []
            for (i in 1 until limit) {
                sum += utf8ByteLength(pages[i]) + 3L // quotes + comma overhead
            }
            sum
        }
        val titleBytes = utf8ByteLength(title)
        var checklistBytes = 0L
        for (item in checklist) {
            if (item.text.isNotEmpty()) {
                checklistBytes += utf8ByteLength(item.text) + 28L
            }
        }
        return titleBytes + page1Bytes + extraPagesBytes + checklistBytes
    }

    fun estimateNoteBytes(note: NoteEntity): Long {
        return utf8ByteLength(note.title) +
            utf8ByteLength(note.content) +
            utf8ByteLength(note.pagesJson) +
            utf8ByteLength(note.checklistJson)
    }

    /**
     * Fits a candidate string within the remaining byte budget when adding or pasting text.
     */
    fun fitWithinByteBudget(
        candidate: String,
        maxCandidateBytes: Long
    ): Pair<String, Boolean> {
        if (utf8ByteLength(candidate) <= maxCandidateBytes) {
            return candidate to false
        }
        if (maxCandidateBytes <= 0L) {
            return "" to true
        }
        // Binary search on character index so multi-byte UTF-8 characters (Arabic/emoji) are never split mid-codepoint
        var low = 0
        var high = candidate.length
        while (low < high) {
            val mid = (low + high + 1) / 2
            val sliceBytes = utf8ByteLength(candidate, 0, mid)
            if (sliceBytes <= maxCandidateBytes) {
                low = mid
            } else {
                high = mid - 1
            }
        }
        // Avoid splitting surrogate pair
        if (low > 0 && low < candidate.length && Character.isHighSurrogate(candidate[low - 1])) {
            low -= 1
        }
        return candidate.substring(0, low) to true
    }

    fun formatSizeKb(bytes: Long, isArabic: Boolean): String {
        return when {
            bytes < 1024L -> if (isArabic) "$bytes ب" else "$bytes B"
            else -> {
                val kb = bytes.toDouble() / 1024.0
                val formatted = if (kb >= 100.0) {
                    String.format(Locale.US, "%.0f", kb)
                } else {
                    String.format(Locale.US, "%.1f", kb)
                }
                if (isArabic) "$formatted ك.ب" else "$formatted KB"
            }
        }
    }
}
