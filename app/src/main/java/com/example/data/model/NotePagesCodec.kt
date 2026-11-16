package com.example.data.model

import org.json.JSONArray
import java.util.Locale

/**
 * Codec and policy helpers for multi-page note drafts.
 * - Page 1 is stored in [NoteEntity.content] for zero-overhead previews & backwards compatibility.
 * - Pages 2..100 are stored as a JSON array string in [NoteEntity.pagesJson].
 * - Hard limits per single note: up to 100 pages OR total UTF-8 size <= 900 KB (921,600 bytes).
 */
object NotePagesCodec {
    const val MAX_NOTE_PAGES = 100
    const val MAX_NOTE_BYTES = 900L * 1024L // 921,600 bytes (900 KB)

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

    fun encodeExtraPages(pages: List<String>): String {
        if (pages.size <= 1) return ""
        val arr = JSONArray()
        val limit = minOf(pages.size, MAX_NOTE_PAGES)
        for (i in 1 until limit) {
            arr.put(pages[i])
        }
        return arr.toString()
    }

    fun pageCount(note: NoteEntity): Int {
        return decodePages(note.content, note.pagesJson).size
    }

    fun hasAnyContent(content: String, pagesJson: String): Boolean {
        if (content.isNotEmpty()) return true
        return decodePages(content, pagesJson).any { it.isNotEmpty() }
    }

    fun hasAnyContent(pages: List<String>): Boolean {
        return pages.any { it.isNotEmpty() }
    }

    fun searchableBody(note: NoteEntity): String {
        if (note.pagesJson.isBlank()) return note.content
        return decodePages(note.content, note.pagesJson).joinToString("\n")
    }

    fun estimateNoteBytes(
        title: String,
        pages: List<String>,
        checklist: List<ChecklistItem>
    ): Long {
        val page1Bytes = (pages.firstOrNull() ?: "").toByteArray(Charsets.UTF_8).size.toLong()
        val extraPagesJson = encodeExtraPages(pages)
        val extraPagesBytes = if (extraPagesJson.isEmpty()) 0L else extraPagesJson.toByteArray(Charsets.UTF_8).size.toLong()
        val titleBytes = title.toByteArray(Charsets.UTF_8).size.toLong()
        val activeChecklist = checklist.filter { it.text.isNotEmpty() }
        val checklistBytes = if (activeChecklist.isEmpty()) 0L else {
            ChecklistItem.listToJson(activeChecklist).toByteArray(Charsets.UTF_8).size.toLong()
        }
        return titleBytes + page1Bytes + extraPagesBytes + checklistBytes
    }

    fun estimateNoteBytes(note: NoteEntity): Long {
        val titleBytes = note.title.toByteArray(Charsets.UTF_8).size.toLong()
        val contentBytes = note.content.toByteArray(Charsets.UTF_8).size.toLong()
        val pagesBytes = note.pagesJson.toByteArray(Charsets.UTF_8).size.toLong()
        val checklistBytes = note.checklistJson.toByteArray(Charsets.UTF_8).size.toLong()
        return titleBytes + contentBytes + pagesBytes + checklistBytes
    }

    /**
     * Fits a candidate string within the remaining byte budget when adding or pasting text.
     */
    fun fitWithinByteBudget(
        candidate: String,
        maxCandidateBytes: Long
    ): Pair<String, Boolean> {
        val rawBytes = candidate.toByteArray(Charsets.UTF_8)
        if (rawBytes.size.toLong() <= maxCandidateBytes) {
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
            val sliceBytes = candidate.substring(0, mid).toByteArray(Charsets.UTF_8).size.toLong()
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
