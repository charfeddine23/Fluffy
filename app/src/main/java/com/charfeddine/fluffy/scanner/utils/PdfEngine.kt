package com.charfeddine.fluffy.scanner.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.charfeddine.fluffy.scanner.data.model.CompressionQuality
import com.charfeddine.fluffy.scanner.data.model.PdfPageSizePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.min

data class PdfRenderedPageInput(
    val bitmap: Bitmap,
    val ocrLines: List<OcrTextBox> = emptyList()
)

/**
 * 100% Offline PDF Engine:
 * - Generate Multi-Page PDF (A4, Letter, Legal, Auto-Fit) + optional Searchable OCR text layer
 * - Merge multiple PDFs
 * - Split PDF into individual pages / subsets
 * - Compress PDF (Low / Medium / High presets with byte size estimation)
 * - Password-Protect PDF (Standard PDF 1.4 RC4-128 Security Handler /V 2 /R 3)
 * - Export PDF / JPG / PNG to Downloads/FluffyScans and Android Share Sheet
 */
object PdfEngine {

    suspend fun buildPdfFromBitmaps(
        destFile: File,
        pages: List<PdfRenderedPageInput>,
        pageSizePreset: PdfPageSizePreset = PdfPageSizePreset.A4,
        searchableOcr: Boolean = true,
        jpegDownscaleMaxSide: Int = 2000
    ): File = withContext(Dispatchers.IO) {
        destFile.parentFile?.mkdirs()
        val pdfDocument = PdfDocument()
        try {
            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val ocrPaint = Paint().apply {
                color = Color.argb(2, 0, 0, 0) // Virtually invisible selectable text layer
            }

            pages.forEachIndexed { idx, item ->
                val bmp = downscaleIfLarger(item.bitmap, jpegDownscaleMaxSide)
                val pageW = if (pageSizePreset == PdfPageSizePreset.AUTO_FIT) {
                    bmp.width.coerceIn(300, 1800)
                } else {
                    pageSizePreset.widthPt
                }
                val pageH = if (pageSizePreset == PdfPageSizePreset.AUTO_FIT) {
                    bmp.height.coerceIn(300, 2600)
                } else {
                    pageSizePreset.heightPt
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, idx + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                val margin = if (pageSizePreset == PdfPageSizePreset.AUTO_FIT) 0f else 18f
                val availW = pageW - margin * 2f
                val availH = pageH - margin * 2f
                val scale = min(availW / bmp.width.coerceAtLeast(1), availH / bmp.height.coerceAtLeast(1))
                val drawW = bmp.width * scale
                val drawH = bmp.height * scale
                val left = (pageW - drawW) / 2f
                val top = (pageH - drawH) / 2f
                val destRect = RectF(left, top, left + drawW, top + drawH)

                if (searchableOcr && item.ocrLines.isNotEmpty()) {
                    item.ocrLines.forEach { line ->
                        val boxH = ((line.normRect.bottom - line.normRect.top) * drawH).coerceIn(6f, 26f)
                        ocrPaint.textSize = boxH
                        canvas.drawText(
                            line.text,
                            left + line.normRect.left * drawW,
                            top + line.normRect.bottom * drawH,
                            ocrPaint
                        )
                    }
                }

                canvas.drawBitmap(bmp, null, destRect, bitmapPaint)
                pdfDocument.finishPage(page)
                if (bmp !== item.bitmap) bmp.recycle()
            }

            FileOutputStream(destFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
        } finally {
            pdfDocument.close()
        }
        destFile
    }

    suspend fun mergePdfUris(
        context: Context,
        sourceUris: List<Uri>,
        destFile: File
    ): File = withContext(Dispatchers.IO) {
        destFile.parentFile?.mkdirs()
        val pdfDocument = PdfDocument()
        var pageNum = 1
        try {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            sourceUris.forEach { uri ->
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val renderer = PdfRenderer(pfd)
                    try {
                        for (i in 0 until renderer.pageCount) {
                            val srcPage = renderer.openPage(i)
                            val w = srcPage.width.coerceIn(400, 1600)
                            val h = srcPage.height.coerceIn(400, 2200)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            Canvas(bmp).drawColor(Color.WHITE)
                            srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            srcPage.close()

                            val outInfo = PdfDocument.PageInfo.Builder(595, (595f * h / w).toInt().coerceAtLeast(300), pageNum++).create()
                            val outPage = pdfDocument.startPage(outInfo)
                            outPage.canvas.drawBitmap(bmp, null, RectF(0f, 0f, outInfo.pageWidth.toFloat(), outInfo.pageHeight.toFloat()), paint)
                            pdfDocument.finishPage(outPage)
                            bmp.recycle()
                        }
                    } finally {
                        renderer.close()
                    }
                }
            }
            FileOutputStream(destFile).use { pdfDocument.writeTo(it) }
        } finally {
            pdfDocument.close()
        }
        destFile
    }

    suspend fun splitPdfUri(
        context: Context,
        sourceUri: Uri,
        outputDir: File,
        baseName: String
    ): List<File> = withContext(Dispatchers.IO) {
        outputDir.mkdirs()
        val created = mutableListOf<File>()
        context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
            val renderer = PdfRenderer(pfd)
            try {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                for (i in 0 until renderer.pageCount) {
                    val srcPage = renderer.openPage(i)
                    val w = srcPage.width.coerceIn(400, 1600)
                    val h = srcPage.height.coerceIn(400, 2200)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    Canvas(bmp).drawColor(Color.WHITE)
                    srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    srcPage.close()

                    val singleDoc = PdfDocument()
                    val info = PdfDocument.PageInfo.Builder(595, (595f * h / w).toInt().coerceAtLeast(300), 1).create()
                    val p = singleDoc.startPage(info)
                    p.canvas.drawBitmap(bmp, null, RectF(0f, 0f, info.pageWidth.toFloat(), info.pageHeight.toFloat()), paint)
                    singleDoc.finishPage(p)
                    bmp.recycle()

                    val outFile = File(outputDir, "${baseName}_page_${i + 1}.pdf")
                    FileOutputStream(outFile).use { singleDoc.writeTo(it) }
                    singleDoc.close()
                    created.add(outFile)
                }
            } finally {
                renderer.close()
            }
        }
        created
    }

    suspend fun compressPdfUri(
        context: Context,
        sourceUri: Uri,
        destFile: File,
        quality: CompressionQuality
    ): File = withContext(Dispatchers.IO) {
        destFile.parentFile?.mkdirs()
        val bitmaps = mutableListOf<Bitmap>()
        context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
            val renderer = PdfRenderer(pfd)
            try {
                for (i in 0 until renderer.pageCount) {
                    val srcPage = renderer.openPage(i)
                    val targetW = min(srcPage.width * 2, quality.maxDimensionPx).coerceAtLeast(420)
                    val targetH = ((targetW.toFloat() / srcPage.width.coerceAtLeast(1)) * srcPage.height).toInt().coerceAtLeast(420)
                    val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                    Canvas(bmp).drawColor(Color.WHITE)
                    srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    srcPage.close()

                    // Re-compress via JPEG buffer to reduce PDF stream entropy & size cleanly
                    val jpegStream = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, quality.jpegQuality, jpegStream)
                    bmp.recycle()
                    val compressedBmp = android.graphics.BitmapFactory.decodeByteArray(jpegStream.toByteArray(), 0, jpegStream.size())
                    if (compressedBmp != null) bitmaps.add(compressedBmp)
                }
            } finally {
                renderer.close()
            }
        }
        buildPdfFromBitmaps(
            destFile = destFile,
            pages = bitmaps.map { PdfRenderedPageInput(it) },
            pageSizePreset = PdfPageSizePreset.AUTO_FIT,
            searchableOcr = false,
            jpegDownscaleMaxSide = quality.maxDimensionPx
        )
        bitmaps.forEach { it.recycle() }
        destFile
    }

    fun estimateCompressedSizeBytes(originalBytes: Long, quality: CompressionQuality): Long {
        if (originalBytes <= 0L) return 0L
        val factor = when (quality) {
            CompressionQuality.LOW -> 0.28f
            CompressionQuality.MEDIUM -> 0.52f
            CompressionQuality.HIGH -> 0.78f
        }
        return (originalBytes * factor).toLong().coerceAtLeast(12_000L)
    }

    /**
     * Generates a password-encrypted PDF 1.4 file (Standard Security Handler R=3, 128-bit RC4)
     * from any existing PDF URI or list of rendered page bitmaps.
     */
    suspend fun createPasswordProtectedPdfFromUri(
        context: Context,
        sourceUri: Uri,
        password: String,
        destFile: File
    ): File = withContext(Dispatchers.IO) {
        val pageBitmaps = mutableListOf<Bitmap>()
        context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
            val renderer = PdfRenderer(pfd)
            try {
                for (i in 0 until renderer.pageCount) {
                    val p = renderer.openPage(i)
                    val w = (p.width * 2).coerceIn(595, 1240)
                    val h = ((w.toFloat() / p.width.coerceAtLeast(1)) * p.height).toInt().coerceIn(595, 1754)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    Canvas(bmp).drawColor(Color.WHITE)
                    p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    p.close()
                    pageBitmaps.add(bmp)
                }
            } finally {
                renderer.close()
            }
        }
        writeEncryptedPdfFromBitmaps(pageBitmaps, password, destFile)
        pageBitmaps.forEach { it.recycle() }
        destFile
    }

    suspend fun writeEncryptedPdfFromBitmaps(
        bitmaps: List<Bitmap>,
        userPassword: String,
        destFile: File
    ): File = withContext(Dispatchers.IO) {
        destFile.parentFile?.mkdirs()
        val fileId = MessageDigest.getInstance("MD5")
            .digest("FLUFFY-${System.currentTimeMillis()}".toByteArray())
        val security = PdfRc4Security(userPassword, userPassword, -4, fileId) // Print & read allowed once unlocked

        val out = ByteArrayOutputStream()
        val offsets = mutableListOf<Int>()
        out.write("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n".toByteArray(Charsets.ISO_8859_1))

        val pageCount = bitmaps.size.coerceAtLeast(1)
        // Object layout:
        // 1: Catalog
        // 2: Pages
        // 3: Encrypt dictionary
        // For each page i (0..N-1):
        //   4 + i*3 + 0: Page dict
        //   4 + i*3 + 1: Contents stream
        //   4 + i*3 + 2: Image XObject stream

        fun writeObjHeader(objNum: Int) {
            offsets.add(out.size())
            out.write("$objNum 0 obj\n".toByteArray(Charsets.US_ASCII))
        }

        // Obj 1: Catalog
        writeObjHeader(1)
        out.write("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".toByteArray(Charsets.US_ASCII))

        // Obj 2: Pages
        val kids = (0 until pageCount).joinToString(" ") { "${4 + it * 3} 0 R" }
        writeObjHeader(2)
        out.write("<< /Type /Pages /Kids [ $kids ] /Count $pageCount >>\nendobj\n".toByteArray(Charsets.US_ASCII))

        // Obj 3: Encrypt dict
        writeObjHeader(3)
        val oHex = security.oEntry.toHexString()
        val uHex = security.uEntry.toHexString()
        out.write(
            "<< /Filter /Standard /V 2 /R 3 /Length 128 /P -4 /O <$oHex> /U <$uHex> >>\nendobj\n"
                .toByteArray(Charsets.US_ASCII)
        )

        bitmaps.forEachIndexed { i, rawBmp ->
            val pageObjNum = 4 + i * 3
            val contentObjNum = pageObjNum + 1
            val imgObjNum = pageObjNum + 2

            val bmp = downscaleIfLarger(rawBmp, 1400)
            val jpegBaos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 82, jpegBaos)
            val jpegBytes = jpegBaos.toByteArray()
            val imgW = bmp.width
            val imgH = bmp.height
            val ptW = 595
            val ptH = ((595f * imgH) / imgW.coerceAtLeast(1)).toInt().coerceAtLeast(300)
            if (bmp !== rawBmp) bmp.recycle()

            // Page obj
            writeObjHeader(pageObjNum)
            out.write(
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $ptW $ptH] /Resources << /XObject << /Im0 $imgObjNum 0 R >> >> /Contents $contentObjNum 0 R >>\nendobj\n"
                    .toByteArray(Charsets.US_ASCII)
            )

            // Content stream (encrypted)
            val contentPlain = "q\n$ptW 0 0 $ptH 0 0 cm\n/Im0 Do\nQ\n".toByteArray(Charsets.US_ASCII)
            val contentEnc = security.encryptObjectBytes(contentPlain, contentObjNum, 0)
            writeObjHeader(contentObjNum)
            out.write("<< /Length ${contentEnc.size} >>\nstream\n".toByteArray(Charsets.US_ASCII))
            out.write(contentEnc)
            out.write("\nendstream\nendobj\n".toByteArray(Charsets.US_ASCII))

            // Image XObject stream (DCTDecode encrypted)
            val imgEnc = security.encryptObjectBytes(jpegBytes, imgObjNum, 0)
            writeObjHeader(imgObjNum)
            out.write(
                "<< /Type /XObject /Subtype /Image /Width $imgW /Height $imgH /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${imgEnc.size} >>\nstream\n"
                    .toByteArray(Charsets.US_ASCII)
            )
            out.write(imgEnc)
            out.write("\nendstream\nendobj\n".toByteArray(Charsets.US_ASCII))
        }

        val xrefOffset = out.size()
        val totalObjs = offsets.size + 1
        out.write("xref\n0 $totalObjs\n0000000000 65535 f \n".toByteArray(Charsets.US_ASCII))
        offsets.forEach { off ->
            out.write(String.format("%010d 00000 n \n", off).toByteArray(Charsets.US_ASCII))
        }
        val idHex = fileId.toHexString()
        out.write(
            "trailer\n<< /Size $totalObjs /Root 1 0 R /Encrypt 3 0 R /ID [<$idHex> <$idHex>] >>\nstartxref\n$xrefOffset\n%%EOF"
                .toByteArray(Charsets.US_ASCII)
        )

        FileOutputStream(destFile).use { it.write(out.toByteArray()) }
        destFile
    }

    /**
     * Saves any local file (PDF, JPG, PNG) to Downloads/FluffyScans via MediaStore (API 29+)
     * or app external exports directory (API < 29).
     */
    suspend fun saveToDownloadsFluffyScans(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String
    ): String? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/FluffyScans")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { it.copyTo(out) }
            }
            "Downloads/FluffyScans/$displayName"
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "FluffyScans").apply { mkdirs() }
            val dest = File(dir, displayName)
            sourceFile.copyTo(dest, overwrite = true)
            dest.absolutePath
        }
    }

    fun shareFileViaSheet(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun getFileSizeBytes(context: Context, uri: Uri): Long {
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize.takeIf { it > 0 } ?: 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    fun renderPdfFirstPagePreview(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd: ParcelFileDescriptor ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) return@use null
                    renderer.openPage(0).use { page ->
                        val w = 360
                        val h = ((360f / page.width.coerceAtLeast(1)) * page.height).toInt().coerceAtLeast(360)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        Canvas(bmp).drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        }.getOrNull()
    }

    private fun downscaleIfLarger(bmp: Bitmap, maxSide: Int): Bitmap {
        val largest = maxOf(bmp.width, bmp.height)
        if (largest <= maxSide) return bmp
        val scale = maxSide.toFloat() / largest
        return Bitmap.createScaledBitmap(
            bmp,
            (bmp.width * scale).toInt().coerceAtLeast(100),
            (bmp.height * scale).toInt().coerceAtLeast(100),
            true
        )
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }
}

/**
 * Pure Kotlin PDF Standard Security Handler (RC4-128 / V=2 / R=3) per ISO 32000-1 §7.6.3.
 */
private class PdfRc4Security(
    userPassword: String,
    ownerPassword: String,
    permissions: Int,
    fileId: ByteArray
) {
    private val padding = byteArrayOf(
        0x28, 0xBF.toByte(), 0x4E, 0x5E, 0x4E, 0x75, 0x8A.toByte(), 0x41,
        0x64, 0x00, 0x4E, 0x56, 0xFF.toByte(), 0xFA.toByte(), 0x01, 0x08,
        0x2E, 0x2E, 0x00, 0xB6.toByte(), 0xD0.toByte(), 0x68, 0x3E, 0x80.toByte(),
        0x2F, 0x0C, 0xA9.toByte(), 0xFE.toByte(), 0x64, 0x53, 0x69, 0x7A
    )

    val oEntry: ByteArray
    val uEntry: ByteArray
    private val encryptionKey: ByteArray

    init {
        val paddedOwner = padPassword(ownerPassword)
        val md5 = MessageDigest.getInstance("MD5")
        var ownerDigest = md5.digest(paddedOwner)
        repeat(50) {
            md5.reset()
            ownerDigest = md5.digest(ownerDigest.copyOfRange(0, 16))
        }
        val ownerKey = ownerDigest.copyOfRange(0, 16)
        val paddedUser = padPassword(userPassword)
        var encOwner = paddedUser.copyOf()
        for (i in 0..19) {
            val k = ByteArray(16) { idx -> (ownerKey[idx].toInt() xor i).toByte() }
            encOwner = rc4(k, encOwner)
        }
        oEntry = encOwner

        // Compute file encryption key (16 bytes)
        md5.reset()
        md5.update(paddedUser)
        md5.update(oEntry)
        val pBytes = byteArrayOf(
            (permissions and 0xFF).toByte(),
            ((permissions shr 8) and 0xFF).toByte(),
            ((permissions shr 16) and 0xFF).toByte(),
            ((permissions shr 24) and 0xFF).toByte()
        )
        md5.update(pBytes)
        md5.update(fileId)
        var digest = md5.digest()
        repeat(50) {
            md5.reset()
            digest = md5.digest(digest.copyOfRange(0, 16))
        }
        encryptionKey = digest.copyOfRange(0, 16)

        // Compute U entry (R=3)
        md5.reset()
        md5.update(padding)
        md5.update(fileId)
        var uHash = md5.digest()
        for (i in 0..19) {
            val k = ByteArray(16) { idx -> (encryptionKey[idx].toInt() xor i).toByte() }
            uHash = rc4(k, uHash)
        }
        val finalU = ByteArray(32)
        System.arraycopy(uHash, 0, finalU, 0, 16)
        uEntry = finalU
    }

    fun encryptObjectBytes(data: ByteArray, objNum: Int, genNum: Int): ByteArray {
        val md5 = MessageDigest.getInstance("MD5")
        md5.update(encryptionKey)
        md5.update(
            byteArrayOf(
                (objNum and 0xFF).toByte(),
                ((objNum shr 8) and 0xFF).toByte(),
                ((objNum shr 16) and 0xFF).toByte(),
                (genNum and 0xFF).toByte(),
                ((genNum shr 8) and 0xFF).toByte()
            )
        )
        val objKey = md5.digest().copyOfRange(0, 16)
        return rc4(objKey, data)
    }

    private fun padPassword(pwd: String): ByteArray {
        val raw = pwd.toByteArray(Charsets.ISO_8859_1)
        val out = ByteArray(32)
        val copyLen = min(raw.size, 32)
        System.arraycopy(raw, 0, out, 0, copyLen)
        if (copyLen < 32) {
            System.arraycopy(padding, 0, out, copyLen, 32 - copyLen)
        }
        return out
    }

    private fun rc4(key: ByteArray, data: ByteArray): ByteArray {
        val s = IntArray(256) { it }
        var j = 0
        for (i in 0 until 256) {
            j = (j + s[i] + (key[i % key.size].toInt() and 0xFF)) and 0xFF
            val tmp = s[i]
            s[i] = s[j]
            s[j] = tmp
        }
        val out = ByteArray(data.size)
        var i = 0
        j = 0
        for (k in data.indices) {
            i = (i + 1) and 0xFF
            j = (j + s[i]) and 0xFF
            val tmp = s[i]
            s[i] = s[j]
            s[j] = tmp
            val rnd = s[(s[i] + s[j]) and 0xFF]
            out[k] = (data[k].toInt() xor rnd).toByte()
        }
        return out
    }
}
