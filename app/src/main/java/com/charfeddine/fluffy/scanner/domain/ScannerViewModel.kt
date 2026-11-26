package com.charfeddine.fluffy.scanner.domain

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charfeddine.fluffy.scanner.data.local.ScannerDatabase
import com.charfeddine.fluffy.scanner.data.model.BatchCapturedFrame
import com.charfeddine.fluffy.scanner.data.model.CompressionQuality
import com.charfeddine.fluffy.scanner.data.model.DocumentQuad
import com.charfeddine.fluffy.scanner.data.model.IdCardPrintMode
import com.charfeddine.fluffy.scanner.data.model.IdCardSide
import com.charfeddine.fluffy.scanner.data.model.PageAnnotations
import com.charfeddine.fluffy.scanner.data.model.PdfPageSizePreset
import com.charfeddine.fluffy.scanner.data.model.PointNorm
import com.charfeddine.fluffy.scanner.data.model.SavedSignatureEntity
import com.charfeddine.fluffy.scanner.data.model.ScanMode
import com.charfeddine.fluffy.scanner.data.model.ScannedDocumentEntity
import com.charfeddine.fluffy.scanner.data.model.ScannedPageEntity
import com.charfeddine.fluffy.scanner.data.model.ScannerFilterType
import com.charfeddine.fluffy.scanner.data.model.ScannerFolderEntity
import com.charfeddine.fluffy.scanner.data.model.SignaturePlacement
import com.charfeddine.fluffy.scanner.data.model.SortOption
import com.charfeddine.fluffy.scanner.data.model.WatermarkConfig
import com.charfeddine.fluffy.scanner.data.repository.ScannerRepository
import com.charfeddine.fluffy.scanner.utils.CryptoVaultHelper
import com.charfeddine.fluffy.scanner.utils.DocumentImageProcessor
import com.charfeddine.fluffy.scanner.utils.MlKitScanPayload
import com.charfeddine.fluffy.scanner.utils.OfflineOcrAndBarcodeEngine
import com.charfeddine.fluffy.scanner.utils.PdfEngine
import com.charfeddine.fluffy.scanner.utils.PdfRenderedPageInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

sealed interface ScannerSubRoute {
    data object Hub : ScannerSubRoute
    data class Camera(val appendToDocId: String? = null) : ScannerSubRoute
    data class CropAndFilter(
        val appendToDocId: String? = null,
        val editDocumentId: String? = null,
        val editPageId: String? = null
    ) : ScannerSubRoute
    data class DocumentDetail(val documentId: String) : ScannerSubRoute
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ScannerDatabase.getDatabase(application)
    val repository = ScannerRepository(application, db.scannerDao())

    // Navigation inside the Scanner Ecosystem
    val currentRoute = MutableStateFlow<ScannerSubRoute>(ScannerSubRoute.Hub)
    val detailTransitionDirection = MutableStateFlow(0)

    private val scannerPrefs = application.getSharedPreferences("fluffy_scanner_prefs", android.content.Context.MODE_PRIVATE)

    private inline fun editScannerPrefsSync(block: android.content.SharedPreferences.Editor.() -> Unit) {
        scannerPrefs.edit().apply(block).commit()
    }

    // Hub Filters, Sorting, Folder & View state (Persisted across restarts)
    val currentFolderId = MutableStateFlow<String?>(
        scannerPrefs.getString("scanner_current_folder_id", null)
    )
    val searchQuery = MutableStateFlow("")
    val selectedTagFilter = MutableStateFlow<String?>(
        scannerPrefs.getString("scanner_selected_tag_filter", null)
    )
    val onlyStarredFilter = MutableStateFlow(
        scannerPrefs.getBoolean("scanner_only_starred_filter", false)
    )
    val showTrashView = MutableStateFlow(false)
    val sortOption = MutableStateFlow(
        SortOption.fromId(scannerPrefs.getString("scanner_sort_option", SortOption.DATE_DESC.id))
    )
    val isGridView = MutableStateFlow(
        scannerPrefs.getBoolean("scanner_is_grid_view", true)
    )

    // Biometric / PIN session unlock tracking
    val unlockedProtectedIds = MutableStateFlow<Set<String>>(emptySet())
    val hasPinConfigured = MutableStateFlow(CryptoVaultHelper.hasPinConfigured(application))

    // Active Capture / Camera / Viewer / Export Preferences (Persisted across restarts)
    val activeScanMode = MutableStateFlow(
        ScanMode.fromId(scannerPrefs.getString("scanner_active_scan_mode", ScanMode.ID_CARD.id))
    )
    val idCardPrintMode = MutableStateFlow(
        IdCardPrintMode.fromId(scannerPrefs.getString("scanner_id_card_print_mode", IdCardPrintMode.RECTO.id))
    )
    val cameraShowGrid = MutableStateFlow(
        scannerPrefs.getBoolean("scanner_camera_show_grid", true)
    )
    val cameraAutoCapture = MutableStateFlow(
        scannerPrefs.getBoolean("scanner_camera_auto_capture", false)
    )
    val cameraFlashMode = MutableStateFlow(
        scannerPrefs.getInt("scanner_camera_flash_mode", androidx.camera.core.ImageCapture.FLASH_MODE_OFF)
    )
    val penColorArgb = MutableStateFlow(
        scannerPrefs.getLong("scanner_pen_color_argb", 0xFF0EA5E9L)
    )
    val redactionBlack = MutableStateFlow(
        scannerPrefs.getBoolean("scanner_redaction_black", true)
    )
    val pdfExportAsImages = MutableStateFlow(
        scannerPrefs.getBoolean("scanner_export_as_images", false)
    )
    val pdfExportPageSize = MutableStateFlow(
        PdfPageSizePreset.fromId(scannerPrefs.getString("scanner_export_page_size", PdfPageSizePreset.A4.id))
    )
    val pdfExportSearchableOcr = MutableStateFlow(
        scannerPrefs.getBoolean("scanner_export_searchable_ocr", true)
    )
    val batchFrames = MutableStateFlow<List<BatchCapturedFrame>>(emptyList())
    val idCardFrontRawPath = MutableStateFlow<String?>(null)
    private var idCardFrontQuad: DocumentQuad? = null
    val activeCropIndex = MutableStateFlow(0)

    // Exit Auto-Save tracking for edited documents in DocumentDetailEditorScreen
    private val modifiedDocIdsForAutoSave = MutableStateFlow<Set<String>>(emptySet())
    private val autoSavingDocIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
    private val pendingMutationJobs = ConcurrentHashMap<String, Job>()

    // Async Processing & Snackbar Feedback
    val isBusy = MutableStateFlow(false)
    val statusMessage = MutableStateFlow<String?>(null)

    private val pageBitmapLruCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt().coerceIn(8 * 1024, 36 * 1024)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }
    }

    private fun bitmapCacheKey(pageId: String, processedPath: String, isLocked: Boolean): String =
        "$pageId|$processedPath|$isLocked"

    fun getCachedPageBitmap(pageId: String, processedPath: String, isLocked: Boolean): Bitmap? {
        val bmp = pageBitmapLruCache.get(bitmapCacheKey(pageId, processedPath, isLocked))
        return if (bmp != null && !bmp.isRecycled) bmp else null
    }

    suspend fun loadOrGetCachedPageBitmap(
        pageId: String,
        processedPath: String,
        isLocked: Boolean
    ): Bitmap? {
        val key = bitmapCacheKey(pageId, processedPath, isLocked)
        pageBitmapLruCache.get(key)?.let { cached ->
            if (!cached.isRecycled) return cached
        }
        val decoded = withContext(Dispatchers.IO) {
            repository.decodePageBitmap(processedPath)
        }
        if (decoded != null && !decoded.isRecycled) {
            pageBitmapLruCache.put(key, decoded)
        }
        return decoded
    }

    fun preloadAdjacentDocuments(docIds: List<String>, unlockedIds: Set<String>, docsById: Map<String, ScannedDocumentEntity>) {
        if (docIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            for (docId in docIds) {
                val doc = docsById[docId] ?: continue
                if (doc.isLocked && doc.id !in unlockedIds) continue
                val firstPage = repository.getPagesOnce(docId).firstOrNull() ?: continue
                val key = bitmapCacheKey(firstPage.id, firstPage.processedImagePath, doc.isLocked)
                val existing = pageBitmapLruCache.get(key)
                if (existing == null || existing.isRecycled) {
                    val bmp = repository.decodePageBitmap(firstPage.processedImagePath)
                    if (bmp != null && !bmp.isRecycled) {
                        pageBitmapLruCache.put(key, bmp)
                    }
                }
            }
        }
    }

    val allFolders: StateFlow<List<ScannerFolderEntity>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActiveDocs: StateFlow<List<ScannedDocumentEntity>> = repository.activeDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedDocs: StateFlow<List<ScannedDocumentEntity>> = repository.trashedDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedSignatures: StateFlow<List<SavedSignatureEntity>> = repository.savedSignatures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleDocuments: StateFlow<List<ScannedDocumentEntity>> = combine(
        allActiveDocs,
        currentFolderId,
        searchQuery,
        selectedTagFilter,
        onlyStarredFilter,
        sortOption
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val docs = values[0] as List<ScannedDocumentEntity>
        val folderId = values[1] as String?
        val query = (values[2] as String).trim()
        val tagFilter = values[3] as String?
        val onlyStarred = values[4] as Boolean
        val sort = values[5] as SortOption

        val filtered = docs.filter { doc ->
            val matchesFolder = if (query.isNotBlank() || onlyStarred || tagFilter != null) {
                true
            } else {
                doc.folderId == folderId
            }
            val matchesQuery = query.isBlank() ||
                doc.title.contains(query, ignoreCase = true) ||
                doc.tagsCsv.contains(query, ignoreCase = true) ||
                doc.ocrFullText.contains(query, ignoreCase = true)
            val matchesTag = tagFilter == null ||
                doc.tagsCsv.split(",").any { it.trim().equals(tagFilter, ignoreCase = true) }
            val matchesStarred = !onlyStarred || doc.isStarred
            matchesFolder && matchesQuery && matchesTag && matchesStarred
        }

        val baseSorted = when (sort) {
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.updatedAt }
            SortOption.CUSTOM -> filtered.sortedWith(
                compareBy<ScannedDocumentEntity> { it.sortOrder }
                    .thenByDescending { it.updatedAt }
            )
            SortOption.NAME_ASC -> filtered.sortedBy { it.title.lowercase(Locale.getDefault()) }
            SortOption.SIZE_DESC -> filtered.sortedByDescending { it.totalSizeBytes }
            SortOption.PAGE_COUNT_DESC -> filtered.sortedByDescending { it.pageCount }
        }
        baseSorted.sortedByDescending { it.isPinned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        OfflineOcrAndBarcodeEngine.init(application)
        viewModelScope.launch {
            repository.purgeExpiredTrash()
            val savedFolderId = currentFolderId.value
            if (savedFolderId != null) {
                val folders = withContext(Dispatchers.IO) { repository.folders.first() }
                val targetFolder = folders.firstOrNull { it.id == savedFolderId }
                if (targetFolder == null || (targetFolder.isLocked && targetFolder.id !in unlockedProtectedIds.value)) {
                    setCurrentFolderId(null)
                }
            }
        }
    }

    fun setSortOption(option: SortOption) {
        sortOption.value = option
        editScannerPrefsSync { putString("scanner_sort_option", option.id) }
    }

    fun setGridView(grid: Boolean) {
        isGridView.value = grid
        editScannerPrefsSync { putBoolean("scanner_is_grid_view", grid) }
    }

    fun toggleGridView() {
        setGridView(!isGridView.value)
    }

    fun setCurrentFolderId(folderId: String?) {
        currentFolderId.value = folderId
        editScannerPrefsSync {
            if (folderId != null) putString("scanner_current_folder_id", folderId)
            else remove("scanner_current_folder_id")
        }
    }

    fun setSelectedTagFilter(tag: String?) {
        selectedTagFilter.value = tag
        editScannerPrefsSync {
            if (tag != null) putString("scanner_selected_tag_filter", tag)
            else remove("scanner_selected_tag_filter")
        }
    }

    fun setOnlyStarredFilter(onlyStarred: Boolean) {
        onlyStarredFilter.value = onlyStarred
        editScannerPrefsSync { putBoolean("scanner_only_starred_filter", onlyStarred) }
    }

    fun clearFilters() {
        onlyStarredFilter.value = false
        selectedTagFilter.value = null
        editScannerPrefsSync {
            putBoolean("scanner_only_starred_filter", false)
            remove("scanner_selected_tag_filter")
        }
    }

    fun setActiveScanMode(mode: ScanMode) {
        activeScanMode.value = mode
        editScannerPrefsSync { putString("scanner_active_scan_mode", mode.id) }
    }

    fun setCameraShowGrid(show: Boolean) {
        cameraShowGrid.value = show
        editScannerPrefsSync { putBoolean("scanner_camera_show_grid", show) }
    }

    fun setCameraAutoCapture(enabled: Boolean) {
        cameraAutoCapture.value = enabled
        editScannerPrefsSync { putBoolean("scanner_camera_auto_capture", enabled) }
    }

    fun setCameraFlashMode(mode: Int) {
        cameraFlashMode.value = mode
        editScannerPrefsSync { putInt("scanner_camera_flash_mode", mode) }
    }

    fun setPenColorArgb(argb: Long) {
        penColorArgb.value = argb
        editScannerPrefsSync { putLong("scanner_pen_color_argb", argb) }
    }

    fun setRedactionBlack(black: Boolean) {
        redactionBlack.value = black
        editScannerPrefsSync { putBoolean("scanner_redaction_black", black) }
    }

    fun setPdfExportAsImages(asImages: Boolean) {
        pdfExportAsImages.value = asImages
        editScannerPrefsSync { putBoolean("scanner_export_as_images", asImages) }
    }

    fun setPdfExportPageSize(pageSize: PdfPageSizePreset) {
        pdfExportPageSize.value = pageSize
        editScannerPrefsSync { putString("scanner_export_page_size", pageSize.id) }
    }

    fun setPdfExportSearchableOcr(searchable: Boolean) {
        pdfExportSearchableOcr.value = searchable
        editScannerPrefsSync { putBoolean("scanner_export_searchable_ocr", searchable) }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    fun postMessage(msg: String) {
        statusMessage.value = msg
    }

    fun openCameraScanner(appendToDocId: String? = null, initialMode: ScanMode? = null) {
        if (initialMode != null) {
            setActiveScanMode(initialMode)
        }
        detailTransitionDirection.value = 0
        batchFrames.value = emptyList()
        idCardFrontRawPath.value = null
        idCardFrontQuad = null
        activeCropIndex.value = 0
        currentRoute.value = ScannerSubRoute.Camera(appendToDocId)
    }

    fun openDocumentDetail(docId: String, transitionDirection: Int = 0) {
        detailTransitionDirection.value = transitionDirection
        currentRoute.value = ScannerSubRoute.DocumentDetail(docId)
    }

    fun navigateBackToHub() {
        detailTransitionDirection.value = 0
        batchFrames.value = emptyList()
        idCardFrontRawPath.value = null
        idCardFrontQuad = null
        currentRoute.value = ScannerSubRoute.Hub
    }

    fun toggleIdCardPrintMode() {
        val next = if (idCardPrintMode.value == IdCardPrintMode.RECTO) {
            IdCardPrintMode.RECTO_VERSO
        } else {
            IdCardPrintMode.RECTO
        }
        idCardPrintMode.value = next
        editScannerPrefsSync { putString("scanner_id_card_print_mode", next.id) }
        val current = batchFrames.value
        if (current.any { it.idCardSide != IdCardSide.NONE || it.idCardGroupId != null }) {
            batchFrames.value = current.map { frame ->
                if (frame.idCardSide != IdCardSide.NONE || frame.idCardGroupId != null) {
                    frame.copy(idCardPrintMode = next)
                } else {
                    frame
                }
            }
        }
    }

    fun updateIdCardPairPrintMode(groupId: String, mode: IdCardPrintMode) {
        idCardPrintMode.value = mode
        editScannerPrefsSync { putString("scanner_id_card_print_mode", mode.id) }
        batchFrames.value = batchFrames.value.map { frame ->
            if (frame.idCardGroupId == groupId) frame.copy(idCardPrintMode = mode) else frame
        }
    }

    fun markDocumentModified(docId: String) {
        modifiedDocIdsForAutoSave.value = modifiedDocIdsForAutoSave.value + docId
    }

    private fun trackDocMutation(
        docId: String,
        markModified: Boolean = true,
        block: suspend () -> Unit
    ) {
        if (markModified) {
            markDocumentModified(docId)
        }
        val prevJob = pendingMutationJobs[docId]
        val job = viewModelScope.launch {
            prevJob?.join()
            block()
        }
        pendingMutationJobs[docId] = job
        job.invokeOnCompletion {
            pendingMutationJobs.remove(docId, job)
        }
    }

    fun autoSaveModifiedDocumentOnExit(docId: String, isArabic: Boolean) {
        if (docId !in modifiedDocIdsForAutoSave.value) return
        modifiedDocIdsForAutoSave.value = modifiedDocIdsForAutoSave.value - docId
        if (!autoSavingDocIds.add(docId)) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                while (true) {
                    val pending = pendingMutationJobs[docId] ?: break
                    pending.join()
                }
                val doc = repository.getDocumentOnce(docId) ?: return@launch
                if (doc.isTrashed || doc.isLocked) return@launch
                val pages = repository.getPagesOnce(docId)
                if (pages.isEmpty()) return@launch

                val sigs = runCatching { repository.savedSignatures.first() }
                    .getOrDefault(savedSignatures.value)
                    .associateBy { it.id }
                val safeTitle = doc.title.replace(Regex("[^A-Za-z0-9\\u0600-\\u06FF_-]"), "_")
                    .ifBlank { "FluffyScan" }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

                val pdfInputs = mutableListOf<PdfRenderedPageInput>()
                var savedGalleryCount = 0
                pages.forEachIndexed { idx, p ->
                    val base = repository.decodePageBitmap(p.processedImagePath) ?: return@forEachIndexed
                    val ann = PageAnnotations.fromJson(p.annotationsJson)
                    val placements = SignaturePlacement.listFromJson(p.signaturesJson).mapNotNull { pl ->
                        val sigEnt = sigs[pl.signatureId] ?: return@mapNotNull null
                        val sigBmp = repository.loadEncryptedSignatureBitmap(sigEnt) ?: return@mapNotNull null
                        pl to sigBmp
                    }
                    val wm = WatermarkConfig.fromJson(p.watermarkJson)
                    val composited = DocumentImageProcessor.compositePageWithMarkups(base, ann, placements, wm)
                    base.recycle()
                    placements.forEach { it.second.recycle() }

                    val imgFileName = "${safeTitle}_p${idx + 1}_$stamp.jpg"
                    val tempImg = File(repository.shareCacheDir, imgFileName)
                    DocumentImageProcessor.saveBitmapAsJpeg(composited, tempImg, 94)
                    if (PdfEngine.saveImageToGallery(getApplication(), tempImg, imgFileName, "image/jpeg") != null) {
                        savedGalleryCount++
                    }

                    val ocrLines = OfflineOcrAndBarcodeEngine.extractTextOffline(composited).lines
                    val srcAspect = composited.width.toFloat() / composited.height.coerceAtLeast(1).toFloat()
                    val isA4Sheet = composited.width >= 1600 && kotlin.math.abs(srcAspect - (210f / 297f)) < 0.015f
                    pdfInputs.add(PdfRenderedPageInput(composited, ocrLines, exactA4Sheet = isA4Sheet))
                }

                if (pdfInputs.isNotEmpty()) {
                    val pdfFileName = "${safeTitle}_$stamp.pdf"
                    val docPdfFile = File(repository.docsRootDir, "doc_$docId.pdf")
                    PdfEngine.buildPdfFromBitmaps(
                        destFile = docPdfFile,
                        pages = pdfInputs,
                        pageSizePreset = PdfPageSizePreset.A4,
                        searchableOcr = true
                    )
                    PdfEngine.saveToDownloadsFluffyScans(
                        context = getApplication(),
                        sourceFile = docPdfFile,
                        displayName = pdfFileName,
                        mimeType = "application/pdf"
                    )
                    pdfInputs.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
                    repository.syncDocumentStats(docId)
                }

                statusMessage.value = if (isArabic) {
                    if (savedGalleryCount > 0) {
                        "تم حفظ الصور في المعرض ونسخة PDF في تخزين الهاتف تلقائياً ✓"
                    } else {
                        "تم حفظ نسخة PDF في تخزين الهاتف تلقائياً ✓"
                    }
                } else {
                    if (savedGalleryCount > 0) {
                        "Auto-saved to Gallery & PDF saved to phone storage ✓"
                    } else {
                        "PDF auto-saved to phone storage ✓"
                    }
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (_: Throwable) {
                // Keep exit navigation smooth if background media write fails on restricted storage
            } finally {
                autoSavingDocIds.remove(docId)
            }
        }
    }

    fun markProtectedUnlocked(id: String) {
        unlockedProtectedIds.value = unlockedProtectedIds.value + id
    }

    fun saveScannerPin(pin: String) {
        CryptoVaultHelper.savePin(getApplication(), pin)
        hasPinConfigured.value = CryptoVaultHelper.hasPinConfigured(getApplication())
    }

    fun verifyScannerPin(candidate: String): Boolean = CryptoVaultHelper.verifyPin(getApplication(), candidate)

    // --- Folder Operations ---
    fun createFolder(name: String, colorHex: Long, isArabic: Boolean) {
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            repository.upsertFolder(
                ScannerFolderEntity(
                    parentFolderId = currentFolderId.value,
                    name = clean,
                    colorHex = colorHex
                )
            )
            statusMessage.value = if (isArabic) "تم إنشاء المجلد \"$clean\"" else "Folder \"$clean\" created"
        }
    }

    fun toggleFolderLock(folder: ScannerFolderEntity, locked: Boolean, isArabic: Boolean) {
        viewModelScope.launch {
            repository.upsertFolder(folder.copy(isLocked = locked))
            statusMessage.value = if (locked) {
                if (isArabic) "تم قفل المجلد بالبصمة" else "Folder locked with biometrics"
            } else {
                if (isArabic) "تم فتح قفل المجلد" else "Folder unlocked"
            }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.deleteFolderAndUnassignDocs(folderId)
            if (currentFolderId.value == folderId) setCurrentFolderId(null)
        }
    }

    // --- Camera Capture & Gallery Import Pipeline ---
    fun processCapturedFile(
        file: File,
        appendToDocId: String?,
        isArabic: Boolean,
        capturedQuad: DocumentQuad? = null,
        capturedPreviewCorners: Array<PointF>? = null,
        idGuidePreviewCorners: Array<PointF>? = null,
        previewWidth: Int = 0,
        previewHeight: Int = 0,
        analyzerWidth: Int = 0,
        analyzerHeight: Int = 0,
        autoNavigateToCrop: Boolean = false
    ) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                val mode = activeScanMode.value
                val framesToAdd = withContext(Dispatchers.Default) {
                    val decodedOrig = withContext(Dispatchers.IO) {
                        DocumentImageProcessor.decodeSampledBitmap(file, 2560)
                    } ?: return@withContext emptyList()

                    // Crop raw captured bitmap to match PreviewView's FILL_CENTER viewport in BOOK & ID_CARD
                    // so live guide coordinates, edge lock, and `1. Crop` share exact pixel framing.
                    val rawBmp = if ((mode == ScanMode.BOOK || mode == ScanMode.ID_CARD) &&
                        previewWidth > 0 && previewHeight > 0
                    ) {
                        val cropped = DocumentImageProcessor.cropBitmapToPreviewViewport(
                            source = decodedOrig,
                            previewWidth = previewWidth,
                            previewHeight = previewHeight
                        )
                        if (cropped !== decodedOrig && !decodedOrig.isRecycled) {
                            decodedOrig.recycle()
                        }
                        cropped
                    } else {
                        decodedOrig
                    }

                    withContext(Dispatchers.IO) {
                        // Persist upright, preview-matched pixels cleanly back to disk
                        DocumentImageProcessor.saveBitmapAsJpeg(rawBmp, file, 95)
                    }

                    val mappedLiveQuad = when {
                        capturedPreviewCorners != null && capturedPreviewCorners.size == 4 &&
                            previewWidth > 0 && previewHeight > 0 ->
                            DocumentImageProcessor.mapPreviewCornersToBitmapQuad(
                                previewCorners = capturedPreviewCorners,
                                previewWidth = previewWidth,
                                previewHeight = previewHeight,
                                bitmapWidth = rawBmp.width,
                                bitmapHeight = rawBmp.height
                            )

                        capturedQuad != null ->
                            DocumentImageProcessor.mapAnalyzerQuadToCaptureDimensions(
                                quad = capturedQuad,
                                analyzerWidth = analyzerWidth,
                                analyzerHeight = analyzerHeight,
                                captureWidth = rawBmp.width,
                                captureHeight = rawBmp.height
                            )

                        else -> null
                    }

                    val mappedGuideQuad = if (idGuidePreviewCorners != null &&
                        idGuidePreviewCorners.size == 4 &&
                        previewWidth > 0 && previewHeight > 0
                    ) {
                        DocumentImageProcessor.mapPreviewCornersToBitmapQuad(
                            previewCorners = idGuidePreviewCorners,
                            previewWidth = previewWidth,
                            previewHeight = previewHeight,
                            bitmapWidth = rawBmp.width,
                            bitmapHeight = rawBmp.height
                        )
                    } else {
                        null
                    }

                    when (mode) {
                        ScanMode.ID_CARD -> {
                            val printMode = idCardPrintMode.value
                            val firstPath = idCardFrontRawPath.value
                            if (firstPath == null) {
                                val detectedFront = DocumentImageProcessor.detectIdCardQuad(
                                    bitmap = rawBmp,
                                    guideQuad = mappedGuideQuad
                                )
                                idCardFrontQuad = when {
                                    DocumentImageProcessor.isValidDocumentQuad(detectedFront, isIdCardHint = true) -> detectedFront
                                    mappedLiveQuad != null && DocumentImageProcessor.isValidDocumentQuad(mappedLiveQuad, isIdCardHint = true) -> mappedLiveQuad
                                    mappedGuideQuad != null -> mappedGuideQuad
                                    else -> DocumentQuad.SAFE_DEFAULT_MARGIN
                                }
                                idCardFrontRawPath.value = file.absolutePath
                                rawBmp.recycle()
                                return@withContext emptyList()
                            } else {
                                val frontBmp = withContext(Dispatchers.IO) {
                                    DocumentImageProcessor.decodeSampledBitmap(File(firstPath), 1600)
                                } ?: rawBmp
                                val frontQuad = idCardFrontQuad
                                    ?: DocumentImageProcessor.detectIdCardQuad(
                                        bitmap = frontBmp,
                                        guideQuad = mappedGuideQuad
                                    )
                                val frontWarped = DocumentImageProcessor.warpPerspective(frontBmp, frontQuad, 1200)
                                val frontEnhanced = DocumentImageProcessor.autoEnhanceCapturedDocument(
                                    warped = frontWarped,
                                    isIdCardMode = true
                                )
                                if (frontBmp !== rawBmp) frontBmp.recycle()
                                if (frontWarped !== frontEnhanced) frontWarped.recycle()

                                val detectedBack = DocumentImageProcessor.detectIdCardQuad(
                                    bitmap = rawBmp,
                                    guideQuad = mappedGuideQuad
                                )
                                val backQuad = when {
                                    DocumentImageProcessor.isValidDocumentQuad(detectedBack, isIdCardHint = true) -> detectedBack
                                    mappedLiveQuad != null && DocumentImageProcessor.isValidDocumentQuad(mappedLiveQuad, isIdCardHint = true) -> mappedLiveQuad
                                    mappedGuideQuad != null -> mappedGuideQuad
                                    else -> DocumentQuad.SAFE_DEFAULT_MARGIN
                                }
                                val backWarped = DocumentImageProcessor.warpPerspective(rawBmp, backQuad, 1200)
                                val backEnhanced = DocumentImageProcessor.autoEnhanceCapturedDocument(
                                    warped = backWarped,
                                    isIdCardMode = true
                                )
                                rawBmp.recycle()
                                if (backWarped !== backEnhanced) backWarped.recycle()

                                val frontThumbFile = File(repository.docsRootDir, "thumb_idf_${UUID.randomUUID()}.jpg")
                                val secondThumbFile = File(repository.docsRootDir, "thumb_idm_${UUID.randomUUID()}.jpg")
                                val secondPreview = if (printMode == IdCardPrintMode.RECTO) {
                                    DocumentImageProcessor.mergeIdCardFrontAndBack(frontEnhanced, backEnhanced)
                                } else {
                                    DocumentImageProcessor.createIdCardSingleFaceSheet(backEnhanced)
                                }
                                withContext(Dispatchers.IO) {
                                    DocumentImageProcessor.saveBitmapAsJpeg(frontEnhanced, frontThumbFile, 90)
                                    DocumentImageProcessor.saveBitmapAsJpeg(secondPreview, secondThumbFile, 90)
                                }
                                frontEnhanced.recycle()
                                backEnhanced.recycle()
                                secondPreview.recycle()

                                val pairGroupId = UUID.randomUUID().toString()
                                idCardFrontRawPath.value = null
                                idCardFrontQuad = null
                                listOf(
                                    BatchCapturedFrame(
                                        rawImagePath = firstPath,
                                        processedImagePath = frontThumbFile.absolutePath,
                                        detectedQuad = frontQuad,
                                        filter = ScannerFilterType.ORIGINAL,
                                        idCardGroupId = pairGroupId,
                                        idCardSide = IdCardSide.FRONT,
                                        idCardPrintMode = printMode
                                    ),
                                    BatchCapturedFrame(
                                        rawImagePath = file.absolutePath,
                                        processedImagePath = secondThumbFile.absolutePath,
                                        detectedQuad = backQuad,
                                        filter = ScannerFilterType.ORIGINAL,
                                        idCardGroupId = pairGroupId,
                                        idCardSide = IdCardSide.BACK,
                                        idCardPrintMode = printMode
                                    )
                                )
                            }
                        }

                        ScanMode.BOOK -> {
                            val spreadQuad = if (mappedLiveQuad != null && DocumentImageProcessor.isValidDocumentQuad(mappedLiveQuad)) {
                                mappedLiveQuad
                            } else {
                                val detectedSpread = DocumentImageProcessor.detectQuadFromBitmap(rawBmp)
                                if (DocumentImageProcessor.isValidDocumentQuad(detectedSpread)) detectedSpread
                                else DocumentQuad.SAFE_DEFAULT_MARGIN
                            }
                            val flatSpread = DocumentImageProcessor.warpPerspective(rawBmp, spreadQuad, 2400)
                            rawBmp.recycle()

                            val (leftRaw, rightRaw) = DocumentImageProcessor.splitBookSpread(flatSpread)
                            flatSpread.recycle()

                            val leftEnhanced = DocumentImageProcessor.autoEnhanceCapturedDocument(leftRaw, isIdCardMode = false)
                            if (leftRaw !== leftEnhanced) leftRaw.recycle()
                            val rightEnhanced = DocumentImageProcessor.autoEnhanceCapturedDocument(rightRaw, isIdCardMode = false)
                            if (rightRaw !== rightEnhanced) rightRaw.recycle()

                            val leftFile = File(repository.docsRootDir, "book_l_${UUID.randomUUID()}.jpg")
                            val rightFile = File(repository.docsRootDir, "book_r_${UUID.randomUUID()}.jpg")
                            withContext(Dispatchers.IO) {
                                DocumentImageProcessor.saveBitmapAsJpeg(leftEnhanced, leftFile, 94)
                                DocumentImageProcessor.saveBitmapAsJpeg(rightEnhanced, rightFile, 94)
                            }
                            leftEnhanced.recycle()
                            rightEnhanced.recycle()
                            listOf(
                                BatchCapturedFrame(
                                    rawImagePath = leftFile.absolutePath,
                                    processedImagePath = leftFile.absolutePath,
                                    detectedQuad = DocumentQuad.FULL_FRAME,
                                    filter = ScannerFilterType.MAGIC_COLOR
                                ),
                                BatchCapturedFrame(
                                    rawImagePath = rightFile.absolutePath,
                                    processedImagePath = rightFile.absolutePath,
                                    detectedQuad = DocumentQuad.FULL_FRAME,
                                    filter = ScannerFilterType.MAGIC_COLOR
                                )
                            )
                        }

                        ScanMode.QR_BARCODE -> emptyList()
                    }
                }

                if (mode == ScanMode.ID_CARD && idCardFrontRawPath.value != null && framesToAdd.isEmpty()) {
                    statusMessage.value = if (isArabic) {
                        "تم التقاط الوجه الأمامي ✓ صوّر الآن الوجه الخلفي للبطاقة"
                    } else {
                        "Front captured ✓ Now scan the back of the ID card"
                    }
                } else if (framesToAdd.isNotEmpty()) {
                    batchFrames.value = batchFrames.value + framesToAdd
                    if (autoNavigateToCrop) {
                        activeCropIndex.value = 0
                        currentRoute.value = ScannerSubRoute.CropAndFilter(appendToDocId)
                    } else {
                        statusMessage.value = if (isArabic) {
                            "تمت إضافة الصفحة (${batchFrames.value.size})"
                        } else {
                            "Page added (${batchFrames.value.size} in batch)"
                        }
                    }
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (_: Throwable) {
                statusMessage.value = if (isArabic) "تعذر معالجة الصورة، حاول مجدداً" else "Unable to process captured photo, please retry"
            } finally {
                isBusy.value = false
            }
        }
    }

    fun importGalleryUris(uris: List<Uri>, appendToDocId: String?, isArabic: Boolean) {
        importDeviceFiles(uris, appendToDocId, isArabic)
    }

    fun importDeviceFiles(uris: List<Uri>, appendToDocId: String?, isArabic: Boolean) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            isBusy.value = true
            try {
                val imported = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val frames = mutableListOf<BatchCapturedFrame>()
                    uris.forEach { uri ->
                        val mimeType = resolver.getType(uri).orEmpty().lowercase(Locale.US)
                        val displayName = runCatching {
                            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                                if (c.moveToFirst()) c.getString(0) else null
                            }
                        }.getOrNull().orEmpty().lowercase(Locale.US)
                        val isPdf = mimeType.contains("pdf") || displayName.endsWith(".pdf")

                        if (isPdf) {
                            runCatching {
                                resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                                    val renderer = PdfRenderer(pfd)
                                    try {
                                        val limit = renderer.pageCount.coerceAtMost(30)
                                        for (pageIndex in 0 until limit) {
                                            val page = renderer.openPage(pageIndex)
                                            val maxSide = kotlin.math.max(page.width, page.height).coerceAtLeast(1)
                                            val scale = 2000f / maxSide
                                            val renderW = (page.width * scale).toInt().coerceIn(480, 2200)
                                            val renderH = (page.height * scale).toInt().coerceIn(480, 2800)
                                            val bmp = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                                            Canvas(bmp).drawColor(Color.WHITE)
                                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                            page.close()

                                            val rawFile = File(repository.docsRootDir, "imp_pdf_${UUID.randomUUID()}.jpg")
                                            DocumentImageProcessor.saveBitmapAsJpeg(bmp, rawFile, 94)
                                            bmp.recycle()
                                            frames.add(
                                                BatchCapturedFrame(
                                                    rawImagePath = rawFile.absolutePath,
                                                    processedImagePath = rawFile.absolutePath,
                                                    detectedQuad = DocumentQuad(
                                                        PointNorm(0.01f, 0.01f),
                                                        PointNorm(0.99f, 0.01f),
                                                        PointNorm(0.99f, 0.99f),
                                                        PointNorm(0.01f, 0.99f)
                                                    ),
                                                    filter = ScannerFilterType.ORIGINAL
                                                )
                                            )
                                        }
                                    } finally {
                                        renderer.close()
                                    }
                                }
                            }
                        } else {
                            runCatching {
                                val bmp = DocumentImageProcessor.decodeOrientedBitmapFromUri(
                                    contentResolver = resolver,
                                    uri = uri,
                                    maxSide = 2560
                                ) ?: return@runCatching
                                val rawFile = File(repository.docsRootDir, "imp_${UUID.randomUUID()}.jpg")
                                DocumentImageProcessor.saveBitmapAsJpeg(bmp, rawFile, 94)
                                val quad = withContext(Dispatchers.Default) {
                                    DocumentImageProcessor.detectQuadFromBitmap(bmp)
                                }
                                bmp.recycle()
                                frames.add(
                                    BatchCapturedFrame(
                                        rawImagePath = rawFile.absolutePath,
                                        processedImagePath = rawFile.absolutePath,
                                        detectedQuad = quad,
                                        filter = ScannerFilterType.MAGIC_COLOR
                                    )
                                )
                            }
                        }
                    }
                    frames
                }
                if (imported.isNotEmpty()) {
                    batchFrames.value = batchFrames.value + imported
                    activeCropIndex.value = (batchFrames.value.size - imported.size).coerceAtLeast(0)
                    currentRoute.value = ScannerSubRoute.CropAndFilter(appendToDocId)
                } else {
                    statusMessage.value = if (isArabic) "تعذر استيراد الملف المحدد" else "Unable to import selected file"
                }
            } finally {
                isBusy.value = false
            }
        }
    }

    fun importMlKitScanResult(
        payload: MlKitScanPayload,
        appendToDocId: String?,
        isArabic: Boolean
    ) {
        if (payload.pageImageUris.isEmpty() && payload.pdfUri == null) return
        viewModelScope.launch {
            isBusy.value = true
            try {
                val targetDocId = withContext(Dispatchers.IO) {
                    val now = System.currentTimeMillis()
                    val existingDoc = appendToDocId?.let { repository.getDocumentOnce(it) }
                    val docId = existingDoc?.id ?: UUID.randomUUID().toString()
                    val existingPages = if (existingDoc != null) repository.getPagesOnce(docId) else emptyList()
                    var nextOrder = existingPages.size
                    val resolver = getApplication<Application>().contentResolver

                    val createdPages = mutableListOf<ScannedPageEntity>()
                    val pdfInputs = mutableListOf<PdfRenderedPageInput>()

                    payload.pageImageUris.forEach { pageUri ->
                        val orientedBmp = DocumentImageProcessor.decodeOrientedBitmapFromUri(
                            contentResolver = resolver,
                            uri = pageUri,
                            maxSide = 2560
                        ) ?: return@forEach

                        val rawFile = File(repository.docsRootDir, "mlk_raw_${UUID.randomUUID()}.jpg")
                        val procFile = File(repository.docsRootDir, "mlk_page_${UUID.randomUUID()}.jpg")
                        DocumentImageProcessor.saveBitmapAsJpeg(orientedBmp, rawFile, 95)
                        DocumentImageProcessor.saveBitmapAsJpeg(orientedBmp, procFile, 95)

                        val ocrRes = withContext(Dispatchers.Default) {
                            OfflineOcrAndBarcodeEngine.extractTextOffline(orientedBmp)
                        }

                        if (existingDoc == null) {
                            val pdfScale = 1650f / kotlin.math.max(orientedBmp.width, orientedBmp.height).coerceAtLeast(1)
                            val pdfBmp = if (pdfScale < 0.98f) {
                                Bitmap.createScaledBitmap(
                                    orientedBmp,
                                    (orientedBmp.width * pdfScale).toInt().coerceAtLeast(64),
                                    (orientedBmp.height * pdfScale).toInt().coerceAtLeast(64),
                                    true
                                )
                            } else {
                                orientedBmp.copy(Bitmap.Config.ARGB_8888, false)
                            }
                            pdfInputs.add(PdfRenderedPageInput(pdfBmp, ocrRes.lines))
                        }

                        if (!orientedBmp.isRecycled) orientedBmp.recycle()

                        createdPages.add(
                            ScannedPageEntity(
                                documentId = docId,
                                pageOrder = nextOrder++,
                                originalImagePath = rawFile.absolutePath,
                                processedImagePath = procFile.absolutePath,
                                cornersJson = DocumentQuad.FULL_FRAME.toJson(),
                                filterId = ScannerFilterType.ORIGINAL.id,
                                brightness = 0f,
                                contrast = 1f,
                                sharpness = 0f,
                                rotationDeg = 0,
                                ocrText = ocrRes.fullText
                            )
                        )
                    }

                    if (createdPages.isEmpty() && payload.pdfUri == null) {
                        return@withContext null
                    }

                    if (existingDoc == null) {
                        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(now))
                        val defaultTitle = if (isArabic) "مستند $stamp" else "Scan $stamp"
                        val pdfFile = File(repository.docsRootDir, "doc_$docId.pdf")

                        if (pdfInputs.isNotEmpty()) {
                            PdfEngine.buildPdfFromBitmaps(
                                destFile = pdfFile,
                                pages = pdfInputs,
                                pageSizePreset = PdfPageSizePreset.A4,
                                searchableOcr = true
                            )
                            pdfInputs.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
                        } else if (payload.pdfUri != null) {
                            runCatching {
                                resolver.openInputStream(payload.pdfUri)?.use { input ->
                                    pdfFile.outputStream().use { output -> input.copyTo(output) }
                                }
                            }
                        }

                        val totalSize = createdPages.sumOf { File(it.processedImagePath).length() } +
                            (pdfFile.takeIf { it.exists() }?.length() ?: 0L)
                        val topOrder = repository.nextTopSortOrder()
                        val docEntity = ScannedDocumentEntity(
                            id = docId,
                            folderId = currentFolderId.value,
                            title = defaultTitle,
                            createdAt = now,
                            updatedAt = now,
                            pageCount = createdPages.size,
                            totalSizeBytes = totalSize,
                            coverThumbnailPath = createdPages.firstOrNull()?.processedImagePath.orEmpty(),
                            pdfPath = pdfFile.absolutePath,
                            ocrFullText = createdPages.mapNotNull { it.ocrText.takeIf(String::isNotBlank) }.joinToString("\n\n"),
                            isSearchablePdf = true,
                            sortOrder = topOrder
                        )
                        repository.upsertDocument(docEntity)
                        if (createdPages.isNotEmpty()) {
                            repository.upsertPages(createdPages)
                        }
                    } else {
                        pdfInputs.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
                        if (createdPages.isNotEmpty()) {
                            repository.upsertPages(createdPages)
                            repository.syncDocumentStats(docId)
                            markDocumentModified(docId)
                        }
                    }
                    docId
                }

                if (targetDocId != null) {
                    currentRoute.value = ScannerSubRoute.DocumentDetail(targetDocId)
                    statusMessage.value = if (isArabic) "تم حفظ المستند في الهاتف بنجاح ✨" else "Document saved locally ✨"
                } else {
                    statusMessage.value = if (isArabic) "تعذر استيراد نتيجة المسح" else "Unable to import scan result"
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (_: Throwable) {
                statusMessage.value = if (isArabic) "تعذر معالجة المسح، حاول مجدداً" else "Unable to process scan, please retry"
            } finally {
                isBusy.value = false
            }
        }
    }

    fun proceedToCropReview(appendToDocId: String?) {
        if (batchFrames.value.isEmpty()) return
        activeCropIndex.value = 0
        currentRoute.value = ScannerSubRoute.CropAndFilter(appendToDocId)
    }

    fun updateFrameQuad(index: Int, quad: DocumentQuad) {
        val list = batchFrames.value.toMutableList()
        if (index !in list.indices) return
        list[index] = list[index].copy(detectedQuad = quad)
        batchFrames.value = list
    }

    fun updateFrameQuadById(frameId: String, quad: DocumentQuad) {
        val list = batchFrames.value.toMutableList()
        val index = list.indexOfFirst { it.id == frameId }
        if (index == -1) return
        list[index] = list[index].copy(detectedQuad = quad)
        batchFrames.value = list
    }

    fun updateFrameEnhancement(
        index: Int,
        filter: ScannerFilterType,
        brightness: Float,
        contrast: Float,
        sharpness: Float,
        rotationDeg: Int
    ) {
        val list = batchFrames.value.toMutableList()
        if (index !in list.indices) return
        val updated = list[index].copy(
            filter = filter,
            brightness = brightness,
            contrast = contrast,
            sharpness = sharpness,
            rotationDeg = rotationDeg
        )
        list[index] = updated
        val groupId = updated.idCardGroupId
        if (groupId != null) {
            for (i in list.indices) {
                if (i != index && list[i].idCardGroupId == groupId) {
                    list[i] = list[i].copy(
                        filter = filter,
                        brightness = brightness,
                        contrast = contrast,
                        sharpness = sharpness,
                        rotationDeg = rotationDeg
                    )
                }
            }
        }
        batchFrames.value = list
    }

    fun removeBatchFrame(index: Int) {
        val list = batchFrames.value.toMutableList()
        if (index !in list.indices) return
        val removed = list.removeAt(index)
        val groupId = removed.idCardGroupId
        if (groupId != null) {
            for (i in list.indices) {
                if (list[i].idCardGroupId == groupId) {
                    list[i] = list[i].copy(
                        idCardGroupId = null,
                        idCardSide = IdCardSide.NONE
                    )
                }
            }
        }
        batchFrames.value = list
        if (list.isEmpty()) {
            currentRoute.value = ScannerSubRoute.Camera(null)
        } else {
            activeCropIndex.value = activeCropIndex.value.coerceIn(0, list.lastIndex)
        }
    }

    /**
     * Finalizes all cropped + filtered frames, merges paired Front/Back ID cards onto a 300-DPI
     * 2-in-1 A4 sheet, runs optional offline OCR, generates a Document & local PDF, and saves into Room.
     */
    fun commitBatchToDocument(
        docTitleInput: String,
        appendToDocId: String?,
        isArabic: Boolean
    ) {
        val frames = batchFrames.value
        if (frames.isEmpty()) return

        viewModelScope.launch {
            isBusy.value = true
            try {
                val targetDocId = withContext(Dispatchers.IO) {
                    val now = System.currentTimeMillis()
                    val existingDoc = appendToDocId?.let { repository.getDocumentOnce(it) }
                    val docId = existingDoc?.id ?: UUID.randomUUID().toString()
                    val existingPages = if (existingDoc != null) repository.getPagesOnce(docId) else emptyList()
                    var nextOrder = existingPages.size

                    val createdPages = mutableListOf<ScannedPageEntity>()
                    val pdfInputs = mutableListOf<PdfRenderedPageInput>()
                    val processedIndices = mutableSetOf<Int>()

                    frames.forEachIndexed { idx, frame ->
                        if (idx in processedIndices) return@forEachIndexed

                        val partnerIdx = frame.idCardGroupId?.let { gid ->
                            frames.indices.firstOrNull { i -> i != idx && frames[i].idCardGroupId == gid }
                        }

                        if (partnerIdx != null) {
                            processedIndices.add(idx)
                            processedIndices.add(partnerIdx)
                            val partner = frames[partnerIdx]
                            val frontFrame = if (frame.idCardSide == IdCardSide.FRONT || partner.idCardSide == IdCardSide.BACK) frame else partner
                            val backFrame = if (frontFrame.id == frame.id) partner else frame
                            val effectivePrintMode = if (
                                frontFrame.idCardPrintMode == IdCardPrintMode.RECTO_VERSO ||
                                backFrame.idCardPrintMode == IdCardPrintMode.RECTO_VERSO
                            ) {
                                IdCardPrintMode.RECTO_VERSO
                            } else {
                                IdCardPrintMode.RECTO
                            }

                            val frontRaw = DocumentImageProcessor.decodeSampledBitmap(File(frontFrame.rawImagePath), 2000)
                            val backRaw = DocumentImageProcessor.decodeSampledBitmap(File(backFrame.rawImagePath), 2000)
                            if (frontRaw == null || backRaw == null) {
                                frontRaw?.recycle()
                                backRaw?.recycle()
                                return@forEachIndexed
                            }

                            val composedSheets = withContext(Dispatchers.Default) {
                                val frontWarped = DocumentImageProcessor.warpPerspective(frontRaw, frontFrame.detectedQuad, 1600)
                                if (frontRaw !== frontWarped) frontRaw.recycle()
                                val frontAuto = DocumentImageProcessor.autoEnhanceCapturedDocument(frontWarped, isIdCardMode = true)
                                if (frontWarped !== frontAuto && !frontWarped.isRecycled) frontWarped.recycle()
                                val frontFinal = DocumentImageProcessor.applyEnhancements(
                                    source = frontAuto,
                                    filter = frame.filter,
                                    brightness = frame.brightness,
                                    contrast = frame.contrast,
                                    sharpness = frame.sharpness,
                                    rotationDeg = 0,
                                    enableSuperRes = false
                                )
                                if (frontAuto !== frontFinal && !frontAuto.isRecycled) frontAuto.recycle()

                                val backWarped = DocumentImageProcessor.warpPerspective(backRaw, backFrame.detectedQuad, 1600)
                                if (backRaw !== backWarped) backRaw.recycle()
                                val backAuto = DocumentImageProcessor.autoEnhanceCapturedDocument(backWarped, isIdCardMode = true)
                                if (backWarped !== backAuto && !backWarped.isRecycled) backWarped.recycle()
                                val backFinal = DocumentImageProcessor.applyEnhancements(
                                    source = backAuto,
                                    filter = frame.filter,
                                    brightness = frame.brightness,
                                    contrast = frame.contrast,
                                    sharpness = frame.sharpness,
                                    rotationDeg = 0,
                                    enableSuperRes = false
                                )
                                if (backAuto !== backFinal && !backAuto.isRecycled) backAuto.recycle()

                                fun maybeRotateSheet(sheet: Bitmap): Bitmap {
                                    return if (frame.rotationDeg % 360 != 0) {
                                        val rotated = DocumentImageProcessor.rotateBitmap(sheet, frame.rotationDeg.toFloat())
                                        if (rotated !== sheet && !sheet.isRecycled) sheet.recycle()
                                        rotated
                                    } else {
                                        sheet
                                    }
                                }

                                if (effectivePrintMode == IdCardPrintMode.RECTO_VERSO) {
                                    val frontSheet = maybeRotateSheet(
                                        DocumentImageProcessor.createIdCardSingleFaceSheet(frontFinal)
                                    )
                                    val backSheet = maybeRotateSheet(
                                        DocumentImageProcessor.createIdCardSingleFaceSheet(backFinal)
                                    )
                                    if (!frontFinal.isRecycled) frontFinal.recycle()
                                    if (!backFinal.isRecycled) backFinal.recycle()
                                    listOf(frontFrame to frontSheet, backFrame to backSheet)
                                } else {
                                    val merged = maybeRotateSheet(
                                        DocumentImageProcessor.mergeIdCardFrontAndBack(frontFinal, backFinal)
                                    )
                                    if (!frontFinal.isRecycled) frontFinal.recycle()
                                    if (!backFinal.isRecycled) backFinal.recycle()
                                    listOf(backFrame to merged)
                                }
                            }

                            composedSheets.forEach { (sourceFrame, sheetBmp) ->
                                val outFile = File(repository.docsRootDir, "page_${UUID.randomUUID()}.jpg")
                                DocumentImageProcessor.saveBitmapAsJpeg(sheetBmp, outFile, 94)
                                val ocrRes = OfflineOcrAndBarcodeEngine.extractTextOffline(sheetBmp)
                                if (existingDoc == null) {
                                    val pdfScale = 1800f / kotlin.math.max(sheetBmp.width, sheetBmp.height).coerceAtLeast(1)
                                    val pdfBmp = if (pdfScale < 0.98f) {
                                        Bitmap.createScaledBitmap(
                                            sheetBmp,
                                            (sheetBmp.width * pdfScale).toInt().coerceAtLeast(64),
                                            (sheetBmp.height * pdfScale).toInt().coerceAtLeast(64),
                                            true
                                        )
                                    } else {
                                        sheetBmp
                                    }
                                    if (pdfBmp !== sheetBmp && !sheetBmp.isRecycled) sheetBmp.recycle()
                                    pdfInputs.add(PdfRenderedPageInput(pdfBmp, ocrRes.lines, exactA4Sheet = true))
                                } else if (!sheetBmp.isRecycled) {
                                    sheetBmp.recycle()
                                }

                                createdPages.add(
                                    ScannedPageEntity(
                                        documentId = docId,
                                        pageOrder = nextOrder++,
                                        originalImagePath = sourceFrame.rawImagePath,
                                        processedImagePath = outFile.absolutePath,
                                        cornersJson = sourceFrame.detectedQuad.toJson(),
                                        filterId = frame.filter.id,
                                        brightness = frame.brightness,
                                        contrast = frame.contrast,
                                        sharpness = frame.sharpness,
                                        rotationDeg = frame.rotationDeg,
                                        ocrText = ocrRes.fullText
                                    )
                                )
                            }
                        } else {
                            processedIndices.add(idx)
                            val rawBmp = DocumentImageProcessor.decodeSampledBitmap(File(frame.rawImagePath), 2400)
                                ?: return@forEachIndexed
                            val isIdCard = frame.idCardSide != IdCardSide.NONE
                            val enhanced = withContext(Dispatchers.Default) {
                                val warped = DocumentImageProcessor.warpPerspective(rawBmp, frame.detectedQuad, 2400)
                                if (rawBmp !== warped) rawBmp.recycle()
                                val prepared = if (isIdCard) {
                                    val auto = DocumentImageProcessor.autoEnhanceCapturedDocument(warped, isIdCardMode = true)
                                    if (warped !== auto && !warped.isRecycled) warped.recycle()
                                    auto
                                } else {
                                    warped
                                }
                                val out = DocumentImageProcessor.applyEnhancements(
                                    source = prepared,
                                    filter = frame.filter,
                                    brightness = frame.brightness,
                                    contrast = frame.contrast,
                                    sharpness = frame.sharpness,
                                    rotationDeg = frame.rotationDeg
                                )
                                if (prepared !== out && !prepared.isRecycled) prepared.recycle()
                                out
                            }
                            val outFile = File(repository.docsRootDir, "page_${UUID.randomUUID()}.jpg")
                            DocumentImageProcessor.saveBitmapAsJpeg(enhanced, outFile, 94)
                            val ocrRes = OfflineOcrAndBarcodeEngine.extractTextOffline(enhanced)
                            if (existingDoc == null) {
                                val pdfScale = 1650f / kotlin.math.max(enhanced.width, enhanced.height).coerceAtLeast(1)
                                val pdfBmp = if (pdfScale < 0.98f) {
                                    Bitmap.createScaledBitmap(
                                        enhanced,
                                        (enhanced.width * pdfScale).toInt().coerceAtLeast(64),
                                        (enhanced.height * pdfScale).toInt().coerceAtLeast(64),
                                        true
                                    )
                                } else {
                                    enhanced
                                }
                                if (pdfBmp !== enhanced && !enhanced.isRecycled) enhanced.recycle()
                                pdfInputs.add(PdfRenderedPageInput(pdfBmp, ocrRes.lines))
                            } else if (!enhanced.isRecycled) {
                                enhanced.recycle()
                            }

                            createdPages.add(
                                ScannedPageEntity(
                                    documentId = docId,
                                    pageOrder = nextOrder++,
                                    originalImagePath = frame.rawImagePath,
                                    processedImagePath = outFile.absolutePath,
                                    cornersJson = frame.detectedQuad.toJson(),
                                    filterId = frame.filter.id,
                                    brightness = frame.brightness,
                                    contrast = frame.contrast,
                                    sharpness = frame.sharpness,
                                    rotationDeg = frame.rotationDeg,
                                    ocrText = ocrRes.fullText
                                )
                            )
                        }
                    }

                    val defaultTitle = docTitleInput.trim().ifBlank {
                        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(now))
                        if (isArabic) "مستند $stamp" else "Scan $stamp"
                    }

                    if (existingDoc == null) {
                        val pdfFile = File(repository.docsRootDir, "doc_$docId.pdf")
                        PdfEngine.buildPdfFromBitmaps(
                            destFile = pdfFile,
                            pages = pdfInputs,
                            pageSizePreset = PdfPageSizePreset.A4,
                            searchableOcr = true
                        )
                        pdfInputs.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }

                        val totalSize = createdPages.sumOf { File(it.processedImagePath).length() } +
                            (pdfFile.takeIf { it.exists() }?.length() ?: 0L)
                        val topOrder = repository.nextTopSortOrder()
                        val docEntity = ScannedDocumentEntity(
                            id = docId,
                            folderId = currentFolderId.value,
                            title = defaultTitle,
                            createdAt = now,
                            updatedAt = now,
                            pageCount = createdPages.size,
                            totalSizeBytes = totalSize,
                            coverThumbnailPath = createdPages.firstOrNull()?.processedImagePath.orEmpty(),
                            pdfPath = pdfFile.absolutePath,
                            ocrFullText = createdPages.mapNotNull { it.ocrText.takeIf(String::isNotBlank) }.joinToString("\n\n"),
                            isSearchablePdf = true,
                            sortOrder = topOrder
                        )
                        repository.upsertDocument(docEntity)
                        repository.upsertPages(createdPages)
                    } else {
                        pdfInputs.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
                        repository.upsertPages(createdPages)
                        repository.syncDocumentStats(docId)
                        markDocumentModified(docId)
                    }
                    docId
                }

                currentRoute.value = ScannerSubRoute.DocumentDetail(targetDocId)
                batchFrames.value = emptyList()
                statusMessage.value = if (isArabic) "تم حفظ المستند في الهاتف بنجاح ✨" else "Document saved locally ✨"
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (_: Throwable) {
                statusMessage.value = if (isArabic) "تعذر حفظ بعض الصفحات، حاول مجدداً" else "Failed to save document, please try again"
            } finally {
                isBusy.value = false
            }
        }
    }

    fun openPageCropEditor(docId: String, page: ScannedPageEntity) {
        val rawPath = page.originalImagePath.takeIf { it.isNotBlank() && File(it).exists() }
            ?: page.processedImagePath
        if (rawPath.isBlank() || !File(rawPath).exists()) return
        val frame = BatchCapturedFrame(
            id = page.id,
            rawImagePath = rawPath,
            processedImagePath = page.processedImagePath.ifBlank { rawPath },
            detectedQuad = DocumentQuad.fromJson(page.cornersJson),
            filter = ScannerFilterType.fromId(page.filterId),
            brightness = page.brightness,
            contrast = page.contrast,
            sharpness = page.sharpness,
            rotationDeg = page.rotationDeg,
            ocrText = page.ocrText
        )
        batchFrames.value = listOf(frame)
        idCardFrontRawPath.value = null
        idCardFrontQuad = null
        activeCropIndex.value = 0
        currentRoute.value = ScannerSubRoute.CropAndFilter(
            appendToDocId = null,
            editDocumentId = docId,
            editPageId = page.id
        )
    }

    fun saveEditedPageFrame(docId: String, pageId: String, isArabic: Boolean) {
        val frame = batchFrames.value.firstOrNull() ?: return
        viewModelScope.launch {
            isBusy.value = true
            try {
                val updated = withContext(Dispatchers.IO) {
                    val existingDoc = repository.getDocumentOnce(docId) ?: return@withContext false
                    val existingPages = repository.getPagesOnce(docId)
                    val targetPage = existingPages.firstOrNull { it.id == pageId } ?: return@withContext false

                    val rawPath = targetPage.originalImagePath.takeIf { it.isNotBlank() && File(it).exists() }
                        ?: frame.rawImagePath
                    val rawBmp = repository.decodeSampledPageBitmap(rawPath, 2400)
                        ?: return@withContext false

                    val enhanced = withContext(Dispatchers.Default) {
                        val warped = DocumentImageProcessor.warpPerspective(rawBmp, frame.detectedQuad, 2400)
                        if (rawBmp !== warped && !rawBmp.isRecycled) rawBmp.recycle()
                        val out = DocumentImageProcessor.applyEnhancements(
                            source = warped,
                            filter = frame.filter,
                            brightness = frame.brightness,
                            contrast = frame.contrast,
                            sharpness = frame.sharpness,
                            rotationDeg = frame.rotationDeg
                        )
                        if (warped !== out && !warped.isRecycled) warped.recycle()
                        out
                    }

                    val outFile = File(repository.docsRootDir, "page_${UUID.randomUUID()}.jpg")
                    DocumentImageProcessor.saveBitmapAsJpeg(enhanced, outFile, 94)
                    val ocrRes = OfflineOcrAndBarcodeEngine.extractTextOffline(enhanced)
                    if (!enhanced.isRecycled) enhanced.recycle()

                    repository.encryptFileIfLocked(outFile, existingDoc.isLocked)

                    val oldProcessed = targetPage.processedImagePath
                    if (oldProcessed.isNotBlank() &&
                        oldProcessed != outFile.absolutePath &&
                        oldProcessed != targetPage.originalImagePath
                    ) {
                        runCatching { File(oldProcessed).delete() }
                    }

                    val updatedPage = targetPage.copy(
                        originalImagePath = rawPath,
                        processedImagePath = outFile.absolutePath,
                        cornersJson = frame.detectedQuad.toJson(),
                        filterId = frame.filter.id,
                        brightness = frame.brightness,
                        contrast = frame.contrast,
                        sharpness = frame.sharpness,
                        rotationDeg = frame.rotationDeg,
                        ocrText = ocrRes.fullText.ifBlank { targetPage.ocrText }
                    )
                    repository.upsertPage(updatedPage)
                    repository.syncDocumentStats(docId)
                    markDocumentModified(docId)
                    true
                }

                if (updated) {
                    batchFrames.value = emptyList()
                    currentRoute.value = ScannerSubRoute.DocumentDetail(docId)
                    statusMessage.value = if (isArabic) {
                        "تم تحديث حواف وتحسينات الصفحة ✓"
                    } else {
                        "Page edges & enhancements updated ✓"
                    }
                } else {
                    statusMessage.value = if (isArabic) {
                        "تعذر حفظ تعديل الصفحة، حاول مجدداً"
                    } else {
                        "Unable to save page changes, please retry"
                    }
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (_: Throwable) {
                statusMessage.value = if (isArabic) {
                    "تعذر حفظ تعديل الصفحة، حاول مجدداً"
                } else {
                    "Unable to save page changes, please retry"
                }
            } finally {
                isBusy.value = false
            }
        }
    }

    // --- Document Detail & Page Management Operations ---
    fun observeDocument(docId: String): Flow<ScannedDocumentEntity?> = repository.observeDocument(docId)

    fun observePages(docId: String): Flow<List<ScannedPageEntity>> = repository.observePages(docId)

    fun renameDocument(doc: ScannedDocumentEntity, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isBlank()) return
        trackDocMutation(doc.id) {
            repository.updateDocument(doc.copy(title = clean, updatedAt = System.currentTimeMillis()))
        }
    }

    fun toggleTagOnDocument(doc: ScannedDocumentEntity, tagName: String) {
        val current = doc.tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
        if (!current.add(tagName)) current.remove(tagName)
        trackDocMutation(doc.id) {
            repository.updateDocument(
                doc.copy(tagsCsv = current.joinToString(","), updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun toggleStarred(doc: ScannedDocumentEntity) {
        trackDocMutation(doc.id) {
            repository.updateDocument(doc.copy(isStarred = !doc.isStarred, updatedAt = System.currentTimeMillis()))
        }
    }

    fun togglePinned(doc: ScannedDocumentEntity, isArabic: Boolean) {
        val nextPinned = !doc.isPinned
        viewModelScope.launch {
            repository.updateDocument(doc.copy(isPinned = nextPinned, updatedAt = System.currentTimeMillis()))
            statusMessage.value = if (nextPinned) {
                if (isArabic) "تم تثبيت المستند في الأعلى 📌" else "Pinned to top 📌"
            } else {
                if (isArabic) "تم إلغاء تثبيت المستند" else "Unpinned"
            }
        }
    }

    fun moveDocumentToFolder(docId: String, folderId: String?) {
        viewModelScope.launch { repository.moveDocumentToFolder(docId, folderId) }
    }

    fun reorderDocuments(reorderedVisibleDocs: List<ScannedDocumentEntity>) {
        if (reorderedVisibleDocs.size <= 1) return
        val previousSort = sortOption.value
        setSortOption(SortOption.CUSTOM)
        viewModelScope.launch(Dispatchers.IO) {
            val rawDocs = repository.getActiveDocumentsOnce()
            if (rawDocs.isEmpty()) return@launch

            val baseSortedAll = when (previousSort) {
                SortOption.DATE_DESC -> rawDocs.sortedByDescending { it.updatedAt }
                SortOption.CUSTOM -> rawDocs.sortedWith(
                    compareBy<ScannedDocumentEntity> { it.sortOrder }
                        .thenByDescending { it.updatedAt }
                )
                SortOption.NAME_ASC -> rawDocs.sortedBy { it.title.lowercase(Locale.getDefault()) }
                SortOption.SIZE_DESC -> rawDocs.sortedByDescending { it.totalSizeBytes }
                SortOption.PAGE_COUNT_DESC -> rawDocs.sortedByDescending { it.pageCount }
            }.sortedByDescending { it.isPinned }

            val visibleIds = reorderedVisibleDocs.map { it.id }.toHashSet()
            val queue = ArrayDeque(reorderedVisibleDocs)
            val mergedGlobal = mutableListOf<ScannedDocumentEntity>()

            for (doc in baseSortedAll) {
                if (doc.id in visibleIds && queue.isNotEmpty()) {
                    mergedGlobal.add(queue.removeFirst())
                } else {
                    mergedGlobal.add(doc)
                }
            }
            while (queue.isNotEmpty()) {
                mergedGlobal.add(queue.removeFirst())
            }

            val partitionedGlobal = mergedGlobal.filter { it.isPinned } +
                mergedGlobal.filter { !it.isPinned }

            val hasDuplicateSortOrders = rawDocs.map { it.sortOrder }.distinct().size != rawDocs.size
            val changedDocs = mutableListOf<ScannedDocumentEntity>()
            partitionedGlobal.forEachIndexed { index, doc ->
                val targetSortOrder = index * 1000L
                if (hasDuplicateSortOrders || doc.sortOrder != targetSortOrder) {
                    changedDocs.add(doc.copy(sortOrder = targetSortOrder))
                }
            }

            if (changedDocs.isNotEmpty()) {
                repository.updateDocuments(changedDocs)
            }
        }
    }

    fun toggleDocumentLock(docId: String, lock: Boolean, isArabic: Boolean) {
        viewModelScope.launch {
            isBusy.value = true
            repository.toggleDocumentLock(docId, lock)
            if (!lock) markProtectedUnlocked(docId)
            isBusy.value = false
            statusMessage.value = if (lock) {
                if (isArabic) "تم تشفير المستند AES-256 وقفله 🔒" else "Document encrypted (AES-256) & locked 🔒"
            } else {
                if (isArabic) "تم فك قفل المستند" else "Document unlocked"
            }
        }
    }

    fun moveToTrash(docId: String, isArabic: Boolean) {
        modifiedDocIdsForAutoSave.value = modifiedDocIdsForAutoSave.value - docId
        viewModelScope.launch {
            repository.moveToTrash(docId)
            if (currentRoute.value is ScannerSubRoute.DocumentDetail) {
                currentRoute.value = ScannerSubRoute.Hub
            }
            statusMessage.value = if (isArabic) "تم النقل إلى سلة المهملات (تحذف تلقائياً بعد 30 يوماً)" else "Moved to Trash (30-day retention)"
        }
    }

    fun moveDocumentsToTrash(docIds: Collection<String>, isArabic: Boolean) {
        val uniqueIds = docIds.distinct()
        if (uniqueIds.isEmpty()) return
        if (uniqueIds.size == 1) {
            moveToTrash(uniqueIds.first(), isArabic)
            return
        }
        modifiedDocIdsForAutoSave.value = modifiedDocIdsForAutoSave.value - uniqueIds.toSet()
        viewModelScope.launch {
            uniqueIds.forEach { docId ->
                repository.moveToTrash(docId)
            }
            val activeRoute = currentRoute.value
            if (activeRoute is ScannerSubRoute.DocumentDetail && activeRoute.documentId in uniqueIds) {
                currentRoute.value = ScannerSubRoute.Hub
            }
            statusMessage.value = if (isArabic) {
                "تم نقل ${uniqueIds.size} مستندات إلى سلة المهملات (تحذف تلقائياً بعد 30 يوماً)"
            } else {
                "Moved ${uniqueIds.size} documents to Trash (30-day retention)"
            }
        }
    }

    fun restoreFromTrash(docId: String) {
        viewModelScope.launch { repository.restoreFromTrash(docId) }
    }

    fun deletePermanently(docId: String) {
        modifiedDocIdsForAutoSave.value = modifiedDocIdsForAutoSave.value - docId
        viewModelScope.launch { repository.deleteDocumentPermanently(docId) }
    }

    fun emptyTrash(isArabic: Boolean) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                val removedCount = repository.emptyTrash()
                statusMessage.value = if (isArabic) {
                    if (removedCount > 0) "تم مسح سلة المهملات بالكامل ($removedCount) 🗑️"
                    else "سلة المهملات فارغة بالفعل"
                } else {
                    if (removedCount > 0) "Cleared Recycle Bin ($removedCount deleted) 🗑️"
                    else "Recycle Bin is already empty"
                }
            } finally {
                isBusy.value = false
            }
        }
    }

    fun movePageOrder(docId: String, fromIndex: Int, toIndex: Int) {
        trackDocMutation(docId) {
            val list = repository.getPagesOnce(docId).toMutableList()
            if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return@trackDocMutation
            val moved = list.removeAt(fromIndex)
            list.add(toIndex, moved)
            val reindexed = list.mapIndexed { idx, p -> p.copy(pageOrder = idx) }
            repository.upsertPages(reindexed)
            repository.syncDocumentStats(docId)
        }
    }

    fun duplicatePage(docId: String, page: ScannedPageEntity, isArabic: Boolean) {
        trackDocMutation(docId) {
            withContext(Dispatchers.IO) {
                val srcFile = File(page.processedImagePath)
                val copyFile = File(repository.docsRootDir, "dup_${UUID.randomUUID()}.jpg")
                if (srcFile.exists()) srcFile.copyTo(copyFile, overwrite = true)

                val srcOrig = File(page.originalImagePath)
                val copyOrigPath = if (page.originalImagePath.isNotBlank() &&
                    page.originalImagePath != page.processedImagePath &&
                    srcOrig.exists()
                ) {
                    val rawDup = File(repository.docsRootDir, "dup_raw_${UUID.randomUUID()}.jpg")
                    srcOrig.copyTo(rawDup, overwrite = true)
                    rawDup.absolutePath
                } else {
                    copyFile.absolutePath
                }

                val pages = repository.getPagesOnce(docId)
                val inserted = page.copy(
                    id = UUID.randomUUID().toString(),
                    pageOrder = page.pageOrder + 1,
                    originalImagePath = copyOrigPath,
                    processedImagePath = copyFile.absolutePath
                )
                val updated = buildList {
                    pages.forEach { p ->
                        if (p.pageOrder <= page.pageOrder) add(p)
                        else add(p.copy(pageOrder = p.pageOrder + 1))
                    }
                    add(inserted)
                }.sortedBy { it.pageOrder }.mapIndexed { i, p -> p.copy(pageOrder = i) }
                repository.upsertPages(updated)
                repository.syncDocumentStats(docId)
            }
            statusMessage.value = if (isArabic) "تم تكرار الصفحة ✓" else "Page duplicated ✓"
        }
    }

    fun deletePage(docId: String, pageId: String, isArabic: Boolean) {
        trackDocMutation(docId) {
            repository.deletePageAndRecalculate(docId, pageId)
            statusMessage.value = if (isArabic) "تم حذف الصفحة" else "Page deleted"
        }
    }

    fun extractPageAsNewDocument(sourceDoc: ScannedDocumentEntity, page: ScannedPageEntity, isArabic: Boolean) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                withContext(Dispatchers.IO) {
                    val newDocId = UUID.randomUUID().toString()
                    val destImg = File(repository.docsRootDir, "ext_${UUID.randomUUID()}.jpg")
                    val bmp = repository.decodePageBitmap(page.processedImagePath)
                    if (bmp != null) {
                        DocumentImageProcessor.saveBitmapAsJpeg(bmp, destImg, 92)
                        val destRawPath = if (page.originalImagePath.isNotBlank() &&
                            page.originalImagePath != page.processedImagePath &&
                            File(page.originalImagePath).exists()
                        ) {
                            val rawExtFile = File(repository.docsRootDir, "ext_raw_${UUID.randomUUID()}.jpg")
                            val rawOrigBmp = repository.decodeSampledPageBitmap(page.originalImagePath, 2560)
                            if (rawOrigBmp != null) {
                                DocumentImageProcessor.saveBitmapAsJpeg(rawOrigBmp, rawExtFile, 95)
                                rawOrigBmp.recycle()
                                rawExtFile.absolutePath
                            } else {
                                destImg.absolutePath
                            }
                        } else {
                            destImg.absolutePath
                        }
                        val pdfFile = File(repository.docsRootDir, "doc_$newDocId.pdf")
                        PdfEngine.buildPdfFromBitmaps(pdfFile, listOf(PdfRenderedPageInput(bmp)))
                        bmp.recycle()
                        val topOrder = repository.nextTopSortOrder()
                        val newDoc = ScannedDocumentEntity(
                            id = newDocId,
                            folderId = sourceDoc.folderId,
                            title = "${sourceDoc.title} (P${page.pageOrder + 1})",
                            pageCount = 1,
                            totalSizeBytes = destImg.length() + pdfFile.length(),
                            coverThumbnailPath = destImg.absolutePath,
                            pdfPath = pdfFile.absolutePath,
                            ocrFullText = page.ocrText,
                            isSearchablePdf = true,
                            sortOrder = topOrder
                        )
                        val newPage = page.copy(
                            id = UUID.randomUUID().toString(),
                            documentId = newDocId,
                            pageOrder = 0,
                            originalImagePath = destRawPath,
                            processedImagePath = destImg.absolutePath
                        )
                        repository.upsertDocument(newDoc)
                        repository.upsertPage(newPage)
                    }
                }
                statusMessage.value = if (isArabic) "تم استخراج الصفحة كمستند مستقل ✓" else "Page extracted as separate document ✓"
            } finally {
                isBusy.value = false
            }
        }
    }

    fun updatePageMarkups(
        page: ScannedPageEntity,
        annotations: PageAnnotations,
        signatures: List<SignaturePlacement>,
        watermark: WatermarkConfig?
    ) {
        trackDocMutation(page.documentId) {
            repository.upsertPage(
                page.copy(
                    annotationsJson = annotations.toJson(),
                    signaturesJson = SignaturePlacement.listToJson(signatures),
                    watermarkJson = watermark?.toJson().orEmpty()
                )
            )
            repository.syncDocumentStats(page.documentId)
        }
    }

    fun saveNewSignature(name: String, bitmap: Bitmap, colorArgb: Long, isArabic: Boolean) {
        viewModelScope.launch {
            repository.saveEncryptedSignature(name.ifBlank { "Signature" }, bitmap, colorArgb)
            statusMessage.value = if (isArabic) "تم حفظ التوقيع مشفراً محلياً ✍️" else "Signature saved encrypted locally ✍️"
        }
    }

    fun deleteSignature(signature: SavedSignatureEntity) {
        viewModelScope.launch { repository.deleteSignature(signature) }
    }

    fun runOfflineOcrOnPage(page: ScannedPageEntity, isArabic: Boolean, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                val extracted = withContext(Dispatchers.IO) {
                    val bmp = repository.decodePageBitmap(page.processedImagePath) ?: return@withContext ""
                    val res = OfflineOcrAndBarcodeEngine.extractTextOffline(bmp)
                    bmp.recycle()
                    if (res.fullText.isNotBlank()) {
                        repository.upsertPage(page.copy(ocrText = res.fullText))
                        repository.syncDocumentStats(page.documentId)
                        markDocumentModified(page.documentId)
                    }
                    res.fullText
                }
                onResult(extracted)
                if (extracted.isBlank()) {
                    statusMessage.value = if (isArabic) "لم يتم العثور على نص واضح في الصفحة" else "No readable text detected on this page"
                }
            } finally {
                isBusy.value = false
            }
        }
    }

    // --- Export & Share (PDF / JPG / PNG to Downloads/FluffyScans or Share Sheet) ---
    fun exportDocument(
        doc: ScannedDocumentEntity,
        pageSize: PdfPageSizePreset,
        asImages: Boolean,
        searchableOcr: Boolean,
        password: String?,
        shareInsteadOfDownload: Boolean,
        isArabic: Boolean
    ) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                val pages = repository.getPagesOnce(doc.id)
                val sigs = savedSignatures.value.associateBy { it.id }

                val resultMsg = withContext(Dispatchers.IO) {
                    val renderedBitmaps = mutableListOf<PdfRenderedPageInput>()
                    pages.forEach { p ->
                        val base = repository.decodePageBitmap(p.processedImagePath) ?: return@forEach
                        val ann = PageAnnotations.fromJson(p.annotationsJson)
                        val placements = SignaturePlacement.listFromJson(p.signaturesJson).mapNotNull { pl ->
                            val sigEnt = sigs[pl.signatureId] ?: return@mapNotNull null
                            val sigBmp = repository.loadEncryptedSignatureBitmap(sigEnt) ?: return@mapNotNull null
                            pl to sigBmp
                        }
                        val wm = WatermarkConfig.fromJson(p.watermarkJson)
                        val composited = DocumentImageProcessor.compositePageWithMarkups(base, ann, placements, wm)
                        base.recycle()
                        placements.forEach { it.second.recycle() }

                        val ocrLines = if (searchableOcr && !asImages) {
                            OfflineOcrAndBarcodeEngine.extractTextOffline(composited).lines
                        } else {
                            emptyList()
                        }
                        renderedBitmaps.add(PdfRenderedPageInput(composited, ocrLines))
                    }

                    if (renderedBitmaps.isEmpty()) return@withContext null

                    val safeTitle = doc.title.replace(Regex("[^A-Za-z0-9\\u0600-\\u06FF_-]"), "_").ifBlank { "FluffyScan" }
                    if (asImages) {
                        var savedPath: String? = null
                        val files = renderedBitmaps.mapIndexed { idx, input ->
                            val fileName = "${safeTitle}_p${idx + 1}.jpg"
                            val tempFile = File(repository.shareCacheDir, fileName)
                            DocumentImageProcessor.saveBitmapAsJpeg(input.bitmap, tempFile, 94)
                            input.bitmap.recycle()
                            if (!shareInsteadOfDownload) {
                                savedPath = PdfEngine.saveToDownloadsFluffyScans(
                                    getApplication(),
                                    tempFile,
                                    fileName,
                                    "image/jpeg"
                                )
                            }
                            tempFile
                        }
                        if (shareInsteadOfDownload && files.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                PdfEngine.shareFileViaSheet(getApplication(), files.first(), "image/jpeg", doc.title)
                            }
                            if (isArabic) "تم فتح قائمة المشاركة" else "Opened Share Sheet"
                        } else {
                            if (isArabic) "تم تصدير الصور إلى Downloads/FluffyScans ✓" else "Images saved to ${savedPath ?: "Downloads/FluffyScans"} ✓"
                        }
                    } else {
                        val fileName = "$safeTitle.pdf"
                        val outFile = File(repository.shareCacheDir, fileName)
                        if (!password.isNullOrBlank()) {
                            PdfEngine.writeEncryptedPdfFromBitmaps(
                                bitmaps = renderedBitmaps.map { it.bitmap },
                                userPassword = password,
                                destFile = outFile
                            )
                        } else {
                            PdfEngine.buildPdfFromBitmaps(
                                destFile = outFile,
                                pages = renderedBitmaps,
                                pageSizePreset = pageSize,
                                searchableOcr = searchableOcr
                            )
                        }
                        renderedBitmaps.forEach { it.bitmap.recycle() }

                        if (shareInsteadOfDownload) {
                            withContext(Dispatchers.Main) {
                                PdfEngine.shareFileViaSheet(getApplication(), outFile, "application/pdf", doc.title)
                            }
                            if (isArabic) "تم تجهيز ملف PDF للمشاركة ✓" else "PDF ready to share ✓"
                        } else {
                            val saved = PdfEngine.saveToDownloadsFluffyScans(
                                getApplication(),
                                outFile,
                                fileName,
                                "application/pdf"
                            )
                            if (isArabic) "تم حفظ PDF في ${saved ?: "Downloads/FluffyScans"} ✓" else "PDF saved to ${saved ?: "Downloads/FluffyScans"} ✓"
                        }
                    }
                }
                if (resultMsg != null) statusMessage.value = resultMsg
            } finally {
                isBusy.value = false
            }
        }
    }

    // --- PDF Utility Tools (Merge, Split, Compress, Password-Protect) ---
    fun mergePdfs(uris: List<Uri>, outputName: String, isArabic: Boolean) {
        if (uris.size < 2) return
        viewModelScope.launch {
            isBusy.value = true
            try {
                val saved = withContext(Dispatchers.IO) {
                    val safeName = outputName.trim().ifBlank { "Merged_${System.currentTimeMillis()}" } + ".pdf"
                    val temp = File(repository.exportsDir, safeName)
                    PdfEngine.mergePdfUris(getApplication(), uris, temp)
                    PdfEngine.saveToDownloadsFluffyScans(getApplication(), temp, safeName, "application/pdf")
                }
                statusMessage.value = if (isArabic) "تم دمج ملفات PDF في ${saved ?: "Downloads/FluffyScans"} ✓"
                else "Merged PDF saved to ${saved ?: "Downloads/FluffyScans"} ✓"
            } finally {
                isBusy.value = false
            }
        }
    }

    fun splitPdf(uri: Uri, baseName: String, isArabic: Boolean) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                val count = withContext(Dispatchers.IO) {
                    val safe = baseName.trim().ifBlank { "Split_${System.currentTimeMillis()}" }
                    val files = PdfEngine.splitPdfUri(getApplication(), uri, repository.exportsDir, safe)
                    files.forEach { file ->
                        PdfEngine.saveToDownloadsFluffyScans(getApplication(), file, file.name, "application/pdf")
                    }
                    files.size
                }
                statusMessage.value = if (isArabic) "تم تقسيم PDF إلى $count ملفات في Downloads/FluffyScans ✓"
                else "Split into $count PDFs in Downloads/FluffyScans ✓"
            } finally {
                isBusy.value = false
            }
        }
    }

    fun compressPdf(uri: Uri, quality: CompressionQuality, outputName: String, isArabic: Boolean) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                val info = withContext(Dispatchers.IO) {
                    val safeName = outputName.trim().ifBlank { "Compressed_${System.currentTimeMillis()}" } + ".pdf"
                    val temp = File(repository.exportsDir, safeName)
                    PdfEngine.compressPdfUri(getApplication(), uri, temp, quality)
                    val saved = PdfEngine.saveToDownloadsFluffyScans(getApplication(), temp, safeName, "application/pdf")
                    val kb = (temp.length() / 1024L).coerceAtLeast(1L)
                    saved to kb
                }
                statusMessage.value = if (isArabic) {
                    "تم ضغط PDF (${info.second} ك.ب) في Downloads/FluffyScans ✓"
                } else {
                    "Compressed PDF (${info.second} KB) saved to Downloads/FluffyScans ✓"
                }
            } finally {
                isBusy.value = false
            }
        }
    }

    fun passwordProtectPdf(uri: Uri, password: String, outputName: String, isArabic: Boolean) {
        if (password.isBlank()) return
        viewModelScope.launch {
            isBusy.value = true
            try {
                val saved = withContext(Dispatchers.IO) {
                    val safeName = outputName.trim().ifBlank { "Protected_${System.currentTimeMillis()}" } + ".pdf"
                    val temp = File(repository.exportsDir, safeName)
                    PdfEngine.createPasswordProtectedPdfFromUri(getApplication(), uri, password, temp)
                    PdfEngine.saveToDownloadsFluffyScans(getApplication(), temp, safeName, "application/pdf")
                }
                statusMessage.value = if (isArabic) {
                    "تم تشفير PDF بكلمة مرور وحفظه في Downloads/FluffyScans 🔒"
                } else {
                    "Password-protected PDF saved to ${saved ?: "Downloads/FluffyScans"} 🔒"
                }
            } finally {
                isBusy.value = false
            }
        }
    }
}
