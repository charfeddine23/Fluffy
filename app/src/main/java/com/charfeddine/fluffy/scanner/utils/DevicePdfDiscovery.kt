package com.charfeddine.fluffy.scanner.utils

import android.content.ContentUris
import android.content.Context
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
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.Color as ComposeColor
import com.charfeddine.fluffy.scanner.data.local.PdfReadingProgressStore
import com.charfeddine.fluffy.scanner.data.model.DevicePdfEntry
import com.charfeddine.fluffy.scanner.data.model.PdfSourceKind
import com.charfeddine.fluffy.scanner.data.model.ScannedDocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Zero-permission device-wide PDF discovery and read-only rendering/export helper:
 *  - MediaStore.Files + MediaStore.Downloads
 *  - Readable shared directories (Downloads, Documents, root storage, Downloads/FluffyScans)
 *  - Unlocked internal Fluffy Scanner PDFs
 *  - Persisted Storage Access Framework (SAF) folder trees & individual PDF grants
 */
object DevicePdfDiscovery {

    suspend fun discoverAllDevicePdfs(
        context: Context,
        progressStore: PdfReadingProgressStore,
        scannerDocs: List<ScannedDocumentEntity> = emptyList()
    ): List<DevicePdfEntry> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<DevicePdfEntry>()
        val seenSignatures = HashSet<String>()
        val seenUriKeys = HashSet<String>()

        fun addIfUnique(entry: DevicePdfEntry) {
            if (entry.uriString.isBlank()) return
            if (!seenUriKeys.add(entry.key)) return
            val sig = "${entry.fileName.lowercase(Locale.ROOT)}:${entry.sizeBytes}"
            if (entry.sizeBytes > 0L && !seenSignatures.add(sig)) return
            discovered.add(entry)
        }

        // 1. Persisted SAF Folder Trees (scans entire user-granted folder trees recursively)
        runCatching {
            context.contentResolver.persistedUriPermissions
                .filter { it.isReadPermission }
                .forEach { perm ->
                    val uri = perm.uri
                    if (DocumentsContract.isTreeUri(uri)) {
                        collectPdfsFromSafTree(context, uri, ::addIfUnique)
                    } else {
                        readSingleContentPdfEntry(context, uri, PdfSourceKind.SAF_DOC)?.let(::addIfUnique)
                    }
                }
        }

        // 2. Persisted manually-picked SAF URIs stored in preferences
        progressStore.getManualUris().forEach { rawUri ->
            runCatching {
                val uri = Uri.parse(rawUri)
                readSingleContentPdfEntry(context, uri, PdfSourceKind.SAF_DOC)?.let(::addIfUnique)
            }
        }

        // 3. MediaStore.Downloads + MediaStore.Files (all indexed & accessible PDFs on device)
        queryMediaStorePdfs(context, ::addIfUnique)

        // 4. Readable shared storage directories (Downloads, Documents, FluffyScans, external root)
        val scanRoots = linkedMapOf<File, String>()
        runCatching {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let {
                scanRoots[it] = "Downloads"
            }
        }
        runCatching {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)?.let {
                scanRoots[it] = "Documents"
            }
        }
        runCatching {
            Environment.getExternalStorageDirectory()?.let {
                scanRoots[it] = "Storage"
            }
        }
        scanRoots.forEach { (rootDir, fallbackLabel) ->
            scanDirectoryForPdfs(
                dir = rootDir,
                fallbackFolder = fallbackLabel,
                maxDepth = 5,
                onFound = ::addIfUnique
            )
        }

        // 5. Unlocked Fluffy Scanner generated PDFs
        val scannerDocByPdfPath = scannerDocs
            .filter { !it.isLocked && !it.isTrashed && it.pdfPath.isNotBlank() }
            .associateBy { runCatching { File(it.pdfPath).canonicalPath }.getOrDefault(it.pdfPath) }
        val lockedPaths = scannerDocs
            .filter { it.isLocked && it.pdfPath.isNotBlank() }
            .mapTo(HashSet()) { runCatching { File(it.pdfPath).canonicalPath }.getOrDefault(it.pdfPath) }

        val docsDir = File(context.filesDir, "scanner_docs")
        if (docsDir.exists() && docsDir.isDirectory) {
            docsDir.listFiles()?.forEach { file ->
                if (!file.isFile || !file.name.endsWith(".pdf", ignoreCase = true) || file.length() <= 0L) {
                    return@forEach
                }
                val canonical = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
                if (canonical in lockedPaths) return@forEach
                val docMeta = scannerDocByPdfPath[canonical]
                val uriStr = Uri.fromFile(file).toString()
                val title = docMeta?.title?.takeIf { it.isNotBlank() }
                    ?: file.nameWithoutExtension
                addIfUnique(
                    DevicePdfEntry(
                        key = PdfReadingProgressStore.buildStablePdfKey(uriStr, file.name, file.length()),
                        uriString = uriStr,
                        title = title,
                        fileName = file.name,
                        sizeBytes = file.length(),
                        lastModifiedMs = docMeta?.updatedAt ?: file.lastModified(),
                        folderLabel = "Fluffy Scanner",
                        pageCount = docMeta?.pageCount ?: 0,
                        sourceKind = PdfSourceKind.SCANNER_DOC
                    )
                )
            }
        }

        discovered.sortedByDescending { it.lastModifiedMs }
    }

    private fun queryMediaStorePdfs(
        context: Context,
        onFound: (DevicePdfEntry) -> Unit
    ) {
        val resolver = context.contentResolver
        val collections = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Downloads.EXTERNAL_CONTENT_URI)
            }
            add(MediaStore.Files.getContentUri("external"))
        }

        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.MediaColumns.MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.MediaColumns.DATA)
            }
        }.toTypedArray()

        val selection = "${MediaStore.MediaColumns.MIME_TYPE} = ? OR LOWER(${MediaStore.MediaColumns.DISPLAY_NAME}) LIKE ?"
        val selectionArgs = arrayOf("application/pdf", "%.pdf")

        collections.forEach { collectionUri ->
            runCatching {
                resolver.query(
                    collectionUri,
                    projection,
                    selection,
                    selectionArgs,
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                    val relCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    } else -1
                    @Suppress("DEPRECATION")
                    val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    } else -1

                    while (cursor.moveToNext()) {
                        val id = if (idCol >= 0) cursor.getLong(idCol) else continue
                        val fileName = (if (nameCol >= 0) cursor.getString(nameCol) else null)
                            ?.takeIf { it.isNotBlank() } ?: continue
                        if (!fileName.endsWith(".pdf", ignoreCase = true)) continue
                        val size = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else 0L
                        val modSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L

                        val dataFile = if (dataCol >= 0) {
                            cursor.getString(dataCol)?.let(::File)?.takeIf { it.exists() && it.canRead() }
                        } else null

                        val contentUri = ContentUris.withAppendedId(collectionUri, id)
                        val resolvedUri = dataFile?.let { Uri.fromFile(it) } ?: contentUri
                        val uriStr = resolvedUri.toString()
                        val canOpen = runCatching {
                            resolver.openFileDescriptor(resolvedUri, "r")?.use { true } ?: false
                        }.getOrDefault(false)
                        if (!canOpen) continue

                        val folder = when {
                            relCol >= 0 -> cursor.getString(relCol)
                                ?.trim('/')
                                ?.substringAfterLast('/')
                                ?.takeIf { it.isNotBlank() }
                                ?: "Device"
                            dataFile != null -> dataFile.parentFile?.name ?: "Device"
                            else -> "Device"
                        }

                        onFound(
                            DevicePdfEntry(
                                key = PdfReadingProgressStore.buildStablePdfKey(uriStr, fileName, size),
                                uriString = uriStr,
                                title = fileName.removeSuffix(".pdf").removeSuffix(".PDF"),
                                fileName = fileName,
                                sizeBytes = size,
                                lastModifiedMs = if (modSec > 0L) modSec * 1000L else System.currentTimeMillis(),
                                folderLabel = folder,
                                sourceKind = PdfSourceKind.MEDIA_STORE
                            )
                        )
                    }
                }
            }
        }
    }

    private fun scanDirectoryForPdfs(
        dir: File,
        fallbackFolder: String,
        maxDepth: Int,
        currentDepth: Int = 0,
        onFound: (DevicePdfEntry) -> Unit
    ) {
        if (currentDepth > maxDepth || !dir.exists() || !dir.isDirectory || !dir.canRead()) return
        val name = dir.name
        if (name.startsWith(".") || name.equals("Android", ignoreCase = true)) return

        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                scanDirectoryForPdfs(child, fallbackFolder, maxDepth, currentDepth + 1, onFound)
            } else if (child.isFile &&
                child.canRead() &&
                child.length() > 0L &&
                child.name.endsWith(".pdf", ignoreCase = true)
            ) {
                val uriStr = Uri.fromFile(child).toString()
                val folder = child.parentFile?.name?.takeIf { it.isNotBlank() } ?: fallbackFolder
                onFound(
                    DevicePdfEntry(
                        key = PdfReadingProgressStore.buildStablePdfKey(uriStr, child.name, child.length()),
                        uriString = uriStr,
                        title = child.nameWithoutExtension,
                        fileName = child.name,
                        sizeBytes = child.length(),
                        lastModifiedMs = child.lastModified(),
                        folderLabel = folder,
                        sourceKind = PdfSourceKind.LOCAL_FILE
                    )
                )
            }
        }
    }

    private fun collectPdfsFromSafTree(
        context: Context,
        treeUri: Uri,
        onFound: (DevicePdfEntry) -> Unit
    ) {
        val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return
        val resolver = context.contentResolver

        fun walk(parentDocId: String, folderName: String, depth: Int) {
            if (depth > 8) return
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )
            runCatching {
                resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) else continue
                        val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx).orEmpty() else ""

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            if (!name.startsWith(".") && !name.equals("Android", ignoreCase = true)) {
                                walk(docId, name, depth + 1)
                            }
                        } else if (mime == "application/pdf" || name.endsWith(".pdf", ignoreCase = true)) {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val uriStr = docUri.toString()
                            val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx).coerceAtLeast(0L) else 0L
                            val mod = if (modIdx >= 0) cursor.getLong(modIdx) else System.currentTimeMillis()
                            onFound(
                                DevicePdfEntry(
                                    key = PdfReadingProgressStore.buildStablePdfKey(uriStr, name, size),
                                    uriString = uriStr,
                                    title = name.removeSuffix(".pdf").removeSuffix(".PDF"),
                                    fileName = name,
                                    sizeBytes = size,
                                    lastModifiedMs = mod,
                                    folderLabel = folderName.ifBlank { "Folder" },
                                    sourceKind = PdfSourceKind.SAF_TREE
                                )
                            )
                        }
                    }
                }
            }
        }

        walk(rootDocId, "Device Folder", 0)
    }

    fun readSingleContentPdfEntry(
        context: Context,
        uri: Uri,
        sourceKind: PdfSourceKind = PdfSourceKind.SAF_DOC
    ): DevicePdfEntry? {
        val resolver = context.contentResolver
        val canRead = runCatching {
            resolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
        if (!canRead) return null

        var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Document.pdf"
        var sizeBytes = 0L
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val nIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (nIdx >= 0) c.getString(nIdx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
                    if (sIdx >= 0) sizeBytes = c.getLong(sIdx).coerceAtLeast(0L)
                }
            }
        }
        if (sizeBytes <= 0L) {
            sizeBytes = PdfEngine.getFileSizeBytes(context, uri)
        }
        val cleanFileName = if (displayName.endsWith(".pdf", ignoreCase = true)) displayName else "$displayName.pdf"
        val uriStr = uri.toString()
        val folderLabel = if (uri.scheme == "file") {
            uri.path?.let { File(it).parentFile?.name } ?: "Local"
        } else {
            "Imported"
        }

        return DevicePdfEntry(
            key = PdfReadingProgressStore.buildStablePdfKey(uriStr, cleanFileName, sizeBytes),
            uriString = uriStr,
            title = cleanFileName.removeSuffix(".pdf").removeSuffix(".PDF"),
            fileName = cleanFileName,
            sizeBytes = sizeBytes,
            lastModifiedMs = System.currentTimeMillis(),
            folderLabel = folderLabel,
            sourceKind = sourceKind
        )
    }

    suspend fun loadPdfMetadataAndThumb(
        context: Context,
        uri: Uri,
        maxThumbSide: Int = 240
    ): Pair<Int, Bitmap?> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount
                    if (count <= 0) return@use 0 to null
                    val thumb = renderer.openPage(0).use { page ->
                        val pw = page.width.coerceAtLeast(1)
                        val ph = page.height.coerceAtLeast(1)
                        val scale = maxThumbSide.toFloat() / maxOf(pw, ph)
                        val w = (pw * scale).roundToInt().coerceIn(72, maxThumbSide)
                        val h = (ph * scale).roundToInt().coerceIn(72, maxThumbSide)
                        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
                            Canvas(bmp).drawColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                    count to thumb
                }
            } ?: (0 to null)
        }.getOrDefault(0 to null)
    }

    suspend fun renderPdfPageBitmap(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetMaxWidth: Int = 1480
    ): Pair<Bitmap?, Int> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount
                    if (count <= 0 || pageIndex !in 0 until count) return@use null to count
                    val bmp = renderer.openPage(pageIndex).use { page ->
                        val pw = page.width.coerceAtLeast(1)
                        val ph = page.height.coerceAtLeast(1)
                        val scale = (targetMaxWidth.toFloat() / maxOf(pw, ph)).coerceIn(0.4f, 3.2f)
                        val w = (pw * scale).roundToInt().coerceIn(180, 2200)
                        val h = (ph * scale).roundToInt().coerceIn(180, 3000)
                        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { canvasBmp ->
                            Canvas(canvasBmp).drawColor(Color.WHITE)
                            page.render(canvasBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                    bmp to count
                }
            } ?: (null to 0)
        }.getOrDefault(null to 0)
    }

    /**
     * Samples the outer perimeter of a rendered PDF page to adapt Focus Mode's surrounding
     * reading surface smoothly to the document's natural lighting & paper warmth.
     */
    fun sampleAdaptiveBackdropColor(
        bitmap: Bitmap?,
        preferDarkEnvironment: Boolean
    ): ComposeColor {
        if (preferDarkEnvironment) {
            return ComposeColor(0xFF0A0D14)
        }
        if (bitmap == null || bitmap.isRecycled || bitmap.width < 12 || bitmap.height < 12) {
            return ComposeColor(0xFFF3EFE6)
        }
        val w = bitmap.width
        val h = bitmap.height
        val sampleCoords = listOf(
            4 to 4, w - 5 to 4, 4 to h - 5, w - 5 to h - 5,
            w / 2 to 4, w / 2 to h - 5, 4 to h / 2, w - 5 to h / 2
        )
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        sampleCoords.forEach { (sx, sy) ->
            val p = bitmap.getPixel(sx.coerceIn(0, w - 1), sy.coerceIn(0, h - 1))
            rSum += Color.red(p)
            gSum += Color.green(p)
            bSum += Color.blue(p)
        }
        val n = sampleCoords.size
        val avgR = (rSum / n).toInt()
        val avgG = (gSum / n).toInt()
        val avgB = (bSum / n).toInt()
        val lum = (avgR * 77 + avgG * 150 + avgB * 29) shr 8
        return if (lum < 110) {
            // Document page itself has a dark background -> blend deeply around it
            ComposeColor(
                red = (avgR * 0.78f / 255f).coerceIn(0.02f, 0.25f),
                green = (avgG * 0.78f / 255f).coerceIn(0.02f, 0.25f),
                blue = (avgB * 0.78f / 255f).coerceIn(0.02f, 0.28f),
                alpha = 1f
            )
        } else {
            // Soften harsh pure white edges slightly into warm matte reading paper
            val softR = ((avgR * 0.70f + 242f * 0.30f) / 255f).coerceIn(0.84f, 0.96f)
            val softG = ((avgG * 0.70f + 237f * 0.30f) / 255f).coerceIn(0.82f, 0.95f)
            val softB = ((avgB * 0.70f + 225f * 0.30f) / 255f).coerceIn(0.76f, 0.93f)
            ComposeColor(red = softR, green = softG, blue = softB, alpha = 1f)
        }
    }

    suspend fun savePdfPageAsImageToGallery(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        titleBase: String,
        asPng: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        val (bmp, _) = renderPdfPageBitmap(context, uri, pageIndex, targetMaxWidth = 2000)
        if (bmp == null) return@withContext null
        val safeTitle = titleBase.replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF ]"), "_").trim().ifBlank { "PDF" }
        val ext = if (asPng) "png" else "jpg"
        val mime = if (asPng) "image/png" else "image/jpeg"
        val tmpFile = File(context.cacheDir, "pdf_page_save_${System.currentTimeMillis()}.$ext")
        try {
            FileOutputStream(tmpFile).use { fos ->
                bmp.compress(
                    if (asPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                    95,
                    fos
                )
            }
            PdfEngine.saveImageToGallery(
                context = context,
                sourceFile = tmpFile,
                displayName = "${safeTitle}_p${pageIndex + 1}.$ext",
                mimeType = mime
            )
        } finally {
            if (!bmp.isRecycled) bmp.recycle()
            tmpFile.delete()
        }
    }

    suspend fun savePdfPagesAsStandalonePdf(
        context: Context,
        uri: Uri,
        pageIndices: List<Int>,
        titleBase: String
    ): String? = withContext(Dispatchers.IO) {
        if (pageIndices.isEmpty()) return@withContext null
        val safeTitle = titleBase.replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF ]"), "_").trim().ifBlank { "PDF" }
        val outTmp = File(context.cacheDir, "pdf_extract_${System.currentTimeMillis()}.pdf")
        val pdfDoc = PdfDocument()
        try {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            var outPageNum = 1
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount
                    pageIndices.distinct().sorted().filter { it in 0 until count }.forEach { idx ->
                        val srcPage = renderer.openPage(idx)
                        val w = srcPage.width.coerceIn(400, 1800)
                        val h = srcPage.height.coerceIn(400, 2400)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        Canvas(bmp).drawColor(Color.WHITE)
                        srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        srcPage.close()

                        val info = PdfDocument.PageInfo.Builder(595, (595f * h / w).toInt().coerceAtLeast(300), outPageNum++).create()
                        val page = pdfDoc.startPage(info)
                        page.canvas.drawBitmap(bmp, null, RectF(0f, 0f, info.pageWidth.toFloat(), info.pageHeight.toFloat()), paint)
                        pdfDoc.finishPage(page)
                        bmp.recycle()
                    }
                }
            }
            if (outPageNum == 1) return@withContext null
            FileOutputStream(outTmp).use { pdfDoc.writeTo(it) }
            val suffix = if (pageIndices.size == 1) "page_${pageIndices.first() + 1}" else "saved_${pageIndices.size}_pages"
            PdfEngine.saveToDownloadsFluffyScans(
                context = context,
                sourceFile = outTmp,
                displayName = "${safeTitle}_$suffix.pdf",
                mimeType = "application/pdf"
            )
        } finally {
            pdfDoc.close()
            outTmp.delete()
        }
    }

    suspend fun shareSinglePdfPageImage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        titleBase: String,
        chooserTitle: String
    ): Boolean = withContext(Dispatchers.IO) {
        val (bmp, _) = renderPdfPageBitmap(context, uri, pageIndex, targetMaxWidth = 1800)
        if (bmp == null) return@withContext false
        val safeTitle = titleBase.replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF ]"), "_").trim().ifBlank { "PDF" }
        val shareDir = File(context.cacheDir, "shared_pdf_pages").apply { mkdirs() }
        val shareFile = File(shareDir, "${safeTitle}_page_${pageIndex + 1}.jpg")
        try {
            FileOutputStream(shareFile).use { fos ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 94, fos)
            }
            withContext(Dispatchers.Main) {
                PdfEngine.shareFileViaSheet(context, shareFile, "image/jpeg", chooserTitle)
            }
            true
        } finally {
            if (!bmp.isRecycled) bmp.recycle()
        }
    }
}
