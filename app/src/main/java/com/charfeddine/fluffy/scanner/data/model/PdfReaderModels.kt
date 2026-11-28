package com.charfeddine.fluffy.scanner.data.model

enum class PdfSourceKind {
    MEDIA_STORE,
    LOCAL_FILE,
    SAF_TREE,
    SAF_DOC,
    SCANNER_DOC
}

enum class PdfReaderThemeMode(val id: String, val labelEn: String, val labelAr: String) {
    ADAPTIVE_AUTO("auto", "Adaptive", "تكيفي"),
    PAPER_LIGHT("paper", "Daylight", "ورق نقي"),
    SEPIA_WARM("sepia", "Sepia", "دافئ"),
    DARK_SLATE("dark", "Night", "ليلي"),
    AMOLED_BLACK("amoled", "AMOLED", "داكن عميق");

    companion object {
        fun fromId(id: String?): PdfReaderThemeMode =
            entries.firstOrNull { it.id == id } ?: ADAPTIVE_AUTO
    }
}

enum class PdfScrollMode(val id: String, val labelEn: String, val labelAr: String) {
    VERTICAL_CONTINUOUS("vertical", "Vertical", "عمودي"),
    HORIZONTAL_PAGER("horizontal", "Page Flip", "تقليب");

    companion object {
        fun fromId(id: String?): PdfScrollMode =
            entries.firstOrNull { it.id == id } ?: VERTICAL_CONTINUOUS
    }
}

enum class PdfLibrarySort(val id: String, val labelEn: String, val labelAr: String) {
    RECENT_READ("recent", "Recent Read", "المقروءة حديثاً"),
    NEWEST("newest", "Newest", "الأحدث"),
    NAME_ASC("name", "Name A-Z", "الاسم"),
    SIZE_DESC("size", "Largest", "الأكبر حجماً");

    companion object {
        fun fromId(id: String?): PdfLibrarySort =
            entries.firstOrNull { it.id == id } ?: RECENT_READ
    }
}

data class DevicePdfEntry(
    val key: String,
    val uriString: String,
    val title: String,
    val fileName: String,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
    val folderLabel: String,
    val pageCount: Int = 0,
    val sourceKind: PdfSourceKind
)

data class PdfReadingProgress(
    val pdfKey: String,
    val uriString: String,
    val title: String,
    val lastPageIndex: Int = 0,
    val totalPages: Int = 0,
    val lastReadAtMs: Long = 0L,
    val bookmarkedPages: Set<Int> = emptySet(),
    val themeMode: PdfReaderThemeMode = PdfReaderThemeMode.ADAPTIVE_AUTO,
    val scrollMode: PdfScrollMode = PdfScrollMode.VERTICAL_CONTINUOUS,
    val rotationDeg: Int = 0,
    val brightnessLevel: Float = 1f
) {
    val progressFraction: Float
        get() = if (totalPages <= 1) {
            if (totalPages == 1 && lastReadAtMs > 0L) 1f else 0f
        } else {
            ((lastPageIndex + 1).toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)
        }
}
