package com.charfeddine.fluffy.scanner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ScanMode(val id: String, val labelEn: String, val labelAr: String) {
    DOCUMENT("document", "Document", "مستند"),
    ID_CARD("id_card", "ID", "بطاقة ID"),
    BOOK("book", "Book Split", "صفحتا كتاب"),
    QR_BARCODE("qr_barcode", "QR / Barcode", "QR وباركود");

    companion object {
        fun fromId(id: String): ScanMode = entries.firstOrNull { it.id == id } ?: DOCUMENT
    }
}

enum class ScannerFilterType(val id: String, val labelEn: String, val labelAr: String) {
    ORIGINAL("original", "Original", "الأصلي"),
    MAGIC_COLOR("magic_color", "Magic Color", "ألوان سحرية"),
    BW_DOCUMENT("bw_doc", "B&W crisp", "أبيض وأسود"),
    GRAYSCALE("grayscale", "Grayscale", "رمادي"),
    SHADOW_GLARE_REMOVAL("deshadow", "De-Shadow", "إزالة الظل");

    companion object {
        fun fromId(id: String): ScannerFilterType = entries.firstOrNull { it.id == id } ?: ORIGINAL
    }
}

enum class PdfPageSizePreset(val id: String, val labelEn: String, val labelAr: String, val widthPt: Int, val heightPt: Int) {
    A4("a4", "A4", "A4 قياسي", 595, 842),
    LETTER("letter", "Letter", "Letter", 612, 792),
    LEGAL("legal", "Legal", "Legal", 612, 1008),
    AUTO_FIT("auto_fit", "Auto-Fit", "تناسب تلقائي", 0, 0)
}

enum class CompressionQuality(val id: String, val labelEn: String, val labelAr: String, val jpegQuality: Int, val maxDimensionPx: Int) {
    LOW("low", "Max Compression", "ضغط عالي (أصغر حجم)", 42, 1100),
    MEDIUM("medium", "Balanced", "متوازن (جودة وحجم)", 68, 1650),
    HIGH("high", "High Clarity", "دقة عالية", 88, 2400)
}

enum class SortOption(val id: String, val labelEn: String, val labelAr: String) {
    DATE_DESC("date_desc", "Newest", "الأحدث"),
    NAME_ASC("name_asc", "Name A-Z", "الاسم أ-ي"),
    SIZE_DESC("size_desc", "Largest Size", "الأكبر حجماً"),
    PAGE_COUNT_DESC("pages_desc", "Most Pages", "الأكثر صفحات")
}

enum class AnnotationTool {
    NONE,
    PEN,
    HIGHLIGHTER,
    REDACTION
}

data class PointNorm(val x: Float, val y: Float)

data class DocumentQuad(
    val topLeft: PointNorm = PointNorm(0.08f, 0.08f),
    val topRight: PointNorm = PointNorm(0.92f, 0.08f),
    val bottomRight: PointNorm = PointNorm(0.92f, 0.92f),
    val bottomLeft: PointNorm = PointNorm(0.08f, 0.92f)
) {
    fun asList(): List<PointNorm> = listOf(topLeft, topRight, bottomRight, bottomLeft)

    fun toJson(): String = JSONObject().apply {
        put("tlx", topLeft.x.toDouble())
        put("tly", topLeft.y.toDouble())
        put("trx", topRight.x.toDouble())
        put("try", topRight.y.toDouble())
        put("brx", bottomRight.x.toDouble())
        put("bry", bottomRight.y.toDouble())
        put("blx", bottomLeft.x.toDouble())
        put("bly", bottomLeft.y.toDouble())
    }.toString()

    companion object {
        val SAFE_DEFAULT_MARGIN = DocumentQuad(
            PointNorm(0.08f, 0.08f),
            PointNorm(0.92f, 0.08f),
            PointNorm(0.92f, 0.92f),
            PointNorm(0.08f, 0.92f)
        )

        val FULL_FRAME = DocumentQuad(
            PointNorm(0.05f, 0.05f),
            PointNorm(0.95f, 0.05f),
            PointNorm(0.95f, 0.95f),
            PointNorm(0.05f, 0.95f)
        )

        fun fromJson(raw: String?): DocumentQuad {
            if (raw.isNullOrBlank()) return FULL_FRAME
            return runCatching {
                val o = JSONObject(raw)
                DocumentQuad(
                    topLeft = PointNorm(o.optDouble("tlx", 0.05).toFloat(), o.optDouble("tly", 0.05).toFloat()),
                    topRight = PointNorm(o.optDouble("trx", 0.95).toFloat(), o.optDouble("try", 0.05).toFloat()),
                    bottomRight = PointNorm(o.optDouble("brx", 0.95).toFloat(), o.optDouble("bry", 0.95).toFloat()),
                    bottomLeft = PointNorm(o.optDouble("blx", 0.05).toFloat(), o.optDouble("bly", 0.95).toFloat())
                )
            }.getOrDefault(FULL_FRAME)
        }
    }
}

data class InkStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<PointNorm>,
    val colorArgb: Long,
    val strokeWidthNorm: Float,
    val isHighlighter: Boolean = false
)

data class RedactionRect(
    val id: String = UUID.randomUUID().toString(),
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val colorArgb: Long = 0xFF000000L
)

data class PageAnnotations(
    val strokes: List<InkStroke> = emptyList(),
    val redactions: List<RedactionRect> = emptyList()
) {
    fun toJson(): String {
        val root = JSONObject()
        val strokesArr = JSONArray()
        strokes.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("color", s.colorArgb)
                put("width", s.strokeWidthNorm.toDouble())
                put("hl", s.isHighlighter)
                val pts = JSONArray()
                s.points.forEach { p ->
                    pts.put(JSONObject().apply {
                        put("x", p.x.toDouble())
                        put("y", p.y.toDouble())
                    })
                }
                put("pts", pts)
            }
            strokesArr.put(obj)
        }
        val redArr = JSONArray()
        redactions.forEach { r ->
            redArr.put(JSONObject().apply {
                put("id", r.id)
                put("l", r.left.toDouble())
                put("t", r.top.toDouble())
                put("r", r.right.toDouble())
                put("b", r.bottom.toDouble())
                put("c", r.colorArgb)
            })
        }
        root.put("strokes", strokesArr)
        root.put("redactions", redArr)
        return root.toString()
    }

    companion object {
        fun fromJson(raw: String?): PageAnnotations {
            if (raw.isNullOrBlank()) return PageAnnotations()
            return runCatching {
                val root = JSONObject(raw)
                val sArr = root.optJSONArray("strokes") ?: JSONArray()
                val strokes = buildList {
                    for (i in 0 until sArr.length()) {
                        val o = sArr.getJSONObject(i)
                        val pArr = o.optJSONArray("pts") ?: JSONArray()
                        val pts = buildList {
                            for (j in 0 until pArr.length()) {
                                val p = pArr.getJSONObject(j)
                                add(PointNorm(p.getDouble("x").toFloat(), p.getDouble("y").toFloat()))
                            }
                        }
                        add(
                            InkStroke(
                                id = o.optString("id", UUID.randomUUID().toString()),
                                points = pts,
                                colorArgb = o.optLong("color", 0xFF000000L),
                                strokeWidthNorm = o.optDouble("width", 0.008).toFloat(),
                                isHighlighter = o.optBoolean("hl", false)
                            )
                        )
                    }
                }
                val rArr = root.optJSONArray("redactions") ?: JSONArray()
                val redactions = buildList {
                    for (i in 0 until rArr.length()) {
                        val o = rArr.getJSONObject(i)
                        add(
                            RedactionRect(
                                id = o.optString("id", UUID.randomUUID().toString()),
                                left = o.optDouble("l").toFloat(),
                                top = o.optDouble("t").toFloat(),
                                right = o.optDouble("r").toFloat(),
                                bottom = o.optDouble("b").toFloat(),
                                colorArgb = o.optLong("c", 0xFF000000L)
                            )
                        )
                    }
                }
                PageAnnotations(strokes = strokes, redactions = redactions)
            }.getOrDefault(PageAnnotations())
        }
    }
}

data class SignaturePlacement(
    val placementId: String = UUID.randomUUID().toString(),
    val signatureId: String,
    val centerXNorm: Float = 0.5f,
    val centerYNorm: Float = 0.72f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f
) {
    companion object {
        fun listToJson(items: List<SignaturePlacement>): String {
            val arr = JSONArray()
            items.forEach { item ->
                arr.put(JSONObject().apply {
                    put("pid", item.placementId)
                    put("sid", item.signatureId)
                    put("x", item.centerXNorm.toDouble())
                    put("y", item.centerYNorm.toDouble())
                    put("s", item.scale.toDouble())
                    put("r", item.rotationDeg.toDouble())
                })
            }
            return arr.toString()
        }

        fun listFromJson(raw: String?): List<SignaturePlacement> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            SignaturePlacement(
                                placementId = o.optString("pid", UUID.randomUUID().toString()),
                                signatureId = o.getString("sid"),
                                centerXNorm = o.optDouble("x", 0.5).toFloat(),
                                centerYNorm = o.optDouble("y", 0.7).toFloat(),
                                scale = o.optDouble("s", 1.0).toFloat(),
                                rotationDeg = o.optDouble("r", 0.0).toFloat()
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}

data class WatermarkConfig(
    val text: String = "",
    val opacity: Float = 0.22f,
    val angleDeg: Float = -35f,
    val colorArgb: Long = 0xFF64748BL
) {
    fun toJson(): String = JSONObject().apply {
        put("text", text)
        put("opacity", opacity.toDouble())
        put("angle", angleDeg.toDouble())
        put("color", colorArgb)
    }.toString()

    companion object {
        fun fromJson(raw: String?): WatermarkConfig? {
            if (raw.isNullOrBlank()) return null
            return runCatching {
                val o = JSONObject(raw)
                val t = o.optString("text", "")
                if (t.isBlank()) null
                else WatermarkConfig(
                    text = t,
                    opacity = o.optDouble("opacity", 0.22).toFloat(),
                    angleDeg = o.optDouble("angle", -35.0).toFloat(),
                    colorArgb = o.optLong("color", 0xFF64748BL)
                )
            }.getOrNull()
        }
    }
}

enum class IdCardSide {
    NONE,
    FRONT,
    BACK
}

enum class IdCardPrintMode(val id: String, val label: String) {
    RECTO("recto", "Recto"),
    RECTO_VERSO("recto_verso", "Recto Verso")
}

data class BatchCapturedFrame(
    val id: String = UUID.randomUUID().toString(),
    val rawImagePath: String,
    val processedImagePath: String = rawImagePath,
    val detectedQuad: DocumentQuad = DocumentQuad.FULL_FRAME,
    val filter: ScannerFilterType = ScannerFilterType.MAGIC_COLOR,
    val brightness: Float = 0f,
    val contrast: Float = 1.05f,
    val sharpness: Float = 0.2f,
    val rotationDeg: Int = 0,
    val ocrText: String = "",
    val idCardGroupId: String? = null,
    val idCardSide: IdCardSide = IdCardSide.NONE,
    val idCardPrintMode: IdCardPrintMode = IdCardPrintMode.RECTO
)

@Entity(tableName = "scanner_folders")
data class ScannerFolderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val parentFolderId: String? = null,
    val name: String,
    val colorHex: Long = 0xFF22D3EEL,
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scanned_documents")
data class ScannedDocumentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val folderId: String? = null,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tagsCsv: String = "",
    val isPinned: Boolean = false,
    val isStarred: Boolean = false,
    val isLocked: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long = 0L,
    val ocrFullText: String = "",
    val pageCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val coverThumbnailPath: String = "",
    val pdfPath: String = "",
    val isSearchablePdf: Boolean = false
)

@Entity(tableName = "scanned_pages")
data class ScannedPageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val pageOrder: Int,
    val originalImagePath: String,
    val processedImagePath: String,
    val cornersJson: String = DocumentQuad.FULL_FRAME.toJson(),
    val filterId: String = ScannerFilterType.MAGIC_COLOR.id,
    val brightness: Float = 0f,
    val contrast: Float = 1.05f,
    val sharpness: Float = 0.2f,
    val rotationDeg: Int = 0,
    val annotationsJson: String = "",
    val signaturesJson: String = "",
    val watermarkJson: String = "",
    val ocrText: String = ""
)

@Entity(tableName = "saved_signatures")
data class SavedSignatureEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val encryptedFilePath: String,
    val colorArgb: Long = 0xFF0F172AL,
    val createdAt: Long = System.currentTimeMillis()
)

data class BarcodeScanResult(
    val rawValue: String,
    val displayTitle: String,
    val formatLabel: String,
    val isUrl: Boolean
)

object ScannerTagCatalog {
    data class TagPreset(val name: String, val colorArgb: Long)

    val presets = listOf(
        TagPreset("Receipts", 0xFF22D3EEL),
        TagPreset("IDs", 0xFFA855F7L),
        TagPreset("Work", 0xFF38BDF8L),
        TagPreset("Contracts", 0xFF34D399L),
        TagPreset("Invoices", 0xFFF59E0BL),
        TagPreset("Books", 0xFFF43F5EL)
    )
}
