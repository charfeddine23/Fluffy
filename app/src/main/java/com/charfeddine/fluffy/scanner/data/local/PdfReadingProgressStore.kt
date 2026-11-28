package com.charfeddine.fluffy.scanner.data.local

import android.content.Context
import android.net.Uri
import com.charfeddine.fluffy.scanner.data.model.DevicePdfEntry
import com.charfeddine.fluffy.scanner.data.model.PdfLibrarySort
import com.charfeddine.fluffy.scanner.data.model.PdfReaderThemeMode
import com.charfeddine.fluffy.scanner.data.model.PdfReadingProgress
import com.charfeddine.fluffy.scanner.data.model.PdfScrollMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Synchronous-commit SharedPreferences store for per-PDF reading progress,
 * bookmarked pages, viewer preferences, and persisted SAF document/folder grants.
 */
class PdfReadingProgressStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _progressMap = MutableStateFlow<Map<String, PdfReadingProgress>>(loadAllProgress())
    val progressMap: StateFlow<Map<String, PdfReadingProgress>> = _progressMap

    private val _manualDocUris = MutableStateFlow<Set<String>>(loadManualUris())
    val manualDocUris: StateFlow<Set<String>> = _manualDocUris

    private val _librarySort = MutableStateFlow(
        PdfLibrarySort.fromId(prefs.getString(KEY_LIBRARY_SORT, PdfLibrarySort.RECENT_READ.id))
    )
    val librarySort: StateFlow<PdfLibrarySort> = _librarySort

    fun setLibrarySort(sort: PdfLibrarySort) {
        _librarySort.value = sort
        prefs.edit().putString(KEY_LIBRARY_SORT, sort.id).commit()
    }

    fun getProgress(pdfKey: String): PdfReadingProgress? = _progressMap.value[pdfKey]

    fun getManualUris(): Set<String> = _manualDocUris.value

    fun addManualUris(uris: Collection<String>) {
        if (uris.isEmpty()) return
        val next = (_manualDocUris.value + uris).toSet()
        _manualDocUris.value = next
        prefs.edit().putStringSet(KEY_MANUAL_URIS, next).commit()
    }

    fun savePageProgress(
        entry: DevicePdfEntry,
        pageIndex: Int,
        totalPages: Int
    ) {
        val safeTotal = totalPages.coerceAtLeast(entry.pageCount).coerceAtLeast(0)
        val clampedPage = if (safeTotal > 0) pageIndex.coerceIn(0, safeTotal - 1) else pageIndex.coerceAtLeast(0)
        val existing = _progressMap.value[entry.key]
        val updated = (existing ?: PdfReadingProgress(
            pdfKey = entry.key,
            uriString = entry.uriString,
            title = entry.title
        )).copy(
            uriString = entry.uriString,
            title = entry.title,
            lastPageIndex = clampedPage,
            totalPages = safeTotal,
            lastReadAtMs = System.currentTimeMillis()
        )
        persistEntry(updated)
    }

    fun toggleBookmark(
        entry: DevicePdfEntry,
        pageIndex: Int,
        totalPages: Int
    ): Set<Int> {
        val safeTotal = totalPages.coerceAtLeast(entry.pageCount).coerceAtLeast(0)
        val safePage = if (safeTotal > 0) pageIndex.coerceIn(0, safeTotal - 1) else pageIndex.coerceAtLeast(0)
        val existing = _progressMap.value[entry.key] ?: PdfReadingProgress(
            pdfKey = entry.key,
            uriString = entry.uriString,
            title = entry.title,
            lastPageIndex = safePage,
            totalPages = safeTotal,
            lastReadAtMs = System.currentTimeMillis()
        )
        val nextBookmarks = existing.bookmarkedPages.toMutableSet().apply {
            if (!add(safePage)) remove(safePage)
        }.toSet()
        val updated = existing.copy(
            uriString = entry.uriString,
            title = entry.title,
            totalPages = maxOf(existing.totalPages, safeTotal),
            bookmarkedPages = nextBookmarks
        )
        persistEntry(updated)
        return nextBookmarks
    }

    fun updateReaderSettings(
        entry: DevicePdfEntry,
        themeMode: PdfReaderThemeMode? = null,
        scrollMode: PdfScrollMode? = null,
        rotationDeg: Int? = null,
        brightnessLevel: Float? = null
    ) {
        val existing = _progressMap.value[entry.key] ?: PdfReadingProgress(
            pdfKey = entry.key,
            uriString = entry.uriString,
            title = entry.title,
            totalPages = entry.pageCount
        )
        val updated = existing.copy(
            uriString = entry.uriString,
            title = entry.title,
            themeMode = themeMode ?: existing.themeMode,
            scrollMode = scrollMode ?: existing.scrollMode,
            rotationDeg = ((rotationDeg ?: existing.rotationDeg) % 360 + 360) % 360,
            brightnessLevel = (brightnessLevel ?: existing.brightnessLevel).coerceIn(0.35f, 1.0f)
        )
        persistEntry(updated)
    }

    private fun persistEntry(progress: PdfReadingProgress) {
        val nextMap = _progressMap.value + (progress.pdfKey to progress)
        _progressMap.value = nextMap
        prefs.edit().putString(KEY_PROGRESS_MAP, encodeProgressMap(nextMap)).commit()
    }

    private fun loadManualUris(): Set<String> =
        prefs.getStringSet(KEY_MANUAL_URIS, emptySet())?.toSet() ?: emptySet()

    private fun loadAllProgress(): Map<String, PdfReadingProgress> {
        val raw = prefs.getString(KEY_PROGRESS_MAP, null) ?: return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            val result = mutableMapOf<String, PdfReadingProgress>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val o = root.optJSONObject(k) ?: continue
                val bmArr = o.optJSONArray("bookmarks")
                val bookmarks = buildSet {
                    if (bmArr != null) {
                        for (i in 0 until bmArr.length()) {
                            add(bmArr.optInt(i, -1))
                        }
                    }
                }.filter { it >= 0 }.toSet()
                result[k] = PdfReadingProgress(
                    pdfKey = k,
                    uriString = o.optString("uri", ""),
                    title = o.optString("title", ""),
                    lastPageIndex = o.optInt("page", 0).coerceAtLeast(0),
                    totalPages = o.optInt("total", 0).coerceAtLeast(0),
                    lastReadAtMs = o.optLong("ts", 0L),
                    bookmarkedPages = bookmarks,
                    themeMode = PdfReaderThemeMode.fromId(o.optString("theme", null)),
                    scrollMode = PdfScrollMode.fromId(o.optString("scroll", null)),
                    rotationDeg = o.optInt("rot", 0),
                    brightnessLevel = o.optDouble("bright", 1.0).toFloat().coerceIn(0.35f, 1.0f)
                )
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun encodeProgressMap(map: Map<String, PdfReadingProgress>): String {
        val root = JSONObject()
        map.forEach { (k, v) ->
            val obj = JSONObject().apply {
                put("uri", v.uriString)
                put("title", v.title)
                put("page", v.lastPageIndex)
                put("total", v.totalPages)
                put("ts", v.lastReadAtMs)
                put("theme", v.themeMode.id)
                put("scroll", v.scrollMode.id)
                put("rot", v.rotationDeg)
                put("bright", v.brightnessLevel.toDouble())
                put("bookmarks", JSONArray().apply {
                    v.bookmarkedPages.sorted().forEach { put(it) }
                })
            }
            root.put(k, obj)
        }
        return root.toString()
    }

    companion object {
        private const val PREFS_NAME = "fluffy_pdf_reader_prefs"
        private const val KEY_PROGRESS_MAP = "pdf_progress_map_json"
        private const val KEY_MANUAL_URIS = "pdf_manual_doc_uris"
        private const val KEY_LIBRARY_SORT = "pdf_library_sort"

        fun buildStablePdfKey(uriString: String, fileName: String, sizeBytes: Long): String {
            val parsed = runCatching { Uri.parse(uriString) }.getOrNull()
            if (parsed?.scheme == "file") {
                val path = parsed.path
                if (!path.isNullOrBlank()) {
                    val canonical = runCatching { File(path).canonicalPath }.getOrDefault(path)
                    return "file:$canonical"
                }
            }
            if (uriString.isNotBlank() && parsed?.scheme == "content") {
                return "uri:$uriString"
            }
            return "meta:${fileName.lowercase()}:$sizeBytes"
        }
    }
}
