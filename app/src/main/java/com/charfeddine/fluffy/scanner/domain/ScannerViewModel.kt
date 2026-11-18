package com.charfeddine.fluffy.scanner.domain

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charfeddine.fluffy.scanner.data.local.ScannerDatabase
import com.charfeddine.fluffy.scanner.data.model.BatchCapturedFrame
import com.charfeddine.fluffy.scanner.data.model.CompressionQuality
import com.charfeddine.fluffy.scanner.data.model.DocumentQuad
import com.charfeddine.fluffy.scanner.data.model.PageAnnotations
import com.charfeddine.fluffy.scanner.data.model.PdfPageSizePreset
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
import com.charfeddine.fluffy.scanner.utils.OfflineOcrAndBarcodeEngine
import com.charfeddine.fluffy.scanner.utils.PdfEngine
import com.charfeddine.fluffy.scanner.utils.PdfRenderedPageInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface ScannerSubRoute {
    data object Hub : ScannerSubRoute
    data class Camera(val appendToDocId: String? = null) : ScannerSubRoute
    data class CropAndFilter(val appendToDocId: String? = null) : ScannerSubRoute
    data class DocumentDetail(val documentId: String) : ScannerSubRoute
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ScannerDatabase.getDatabase(application)
    val repository = ScannerRepository(application, db.scannerDao())

    // Navigation inside the Scanner Ecosystem
    val currentRoute = MutableStateFlow<ScannerSubRoute>(ScannerSubRoute.Hub)

    // Hub Filters, Sorting, Folder & View state
    val currentFolderId = MutableStateFlow<String?>(null)
    val searchQuery = MutableStateFlow("")
    val selectedTagFilter = MutableStateFlow<String?>(null)
    val onlyStarredFilter = MutableStateFlow(false)
    val showTrashView = MutableStateFlow(false)
    val sortOption = MutableStateFlow(SortOption.DATE_DESC)
    val isGridView = MutableStateFlow(true)

    // Biometric / PIN session unlock tracking
    val unlockedProtectedIds = MutableStateFlow<Set<String>>(emptySet())
    val hasPinConfigured = MutableStateFlow(CryptoVaultHelper.hasPinConfigured(application))

    // Active Capture / Batch / Crop Pipeline State
    val activeScanMode = MutableStateFlow(ScanMode.SINGLE)
    val batchFrames = MutableStateFlow<List<BatchCapturedFrame>>(emptyList())
    val idCardFrontRawPath = MutableStateFlow<String?>(null)
    val activeCropIndex = MutableStateFlow(0)

    // Async Processing & Snackbar Feedback
    val isBusy = MutableStateFlow(false)
    val statusMessage = MutableStateFlow<String?>(null)

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

        when (sort) {
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.updatedAt }
            SortOption.NAME_ASC -> filtered.sortedBy { it.title.lowercase(Locale.getDefault()) }
            SortOption.SIZE_DESC -> filtered.sortedByDescending { it.totalSizeBytes }
            SortOption.PAGE_COUNT_DESC -> filtered.sortedByDescending { it.pageCount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.purgeExpiredTrash()
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    fun postMessage(msg: String) {
        statusMessage.value = msg
    }

    fun openCameraScanner(appendToDocId: String? = null, initialMode: ScanMode? = null) {
        if (initialMode != null) {
            activeScanMode.value = initialMode
        }
        batchFrames.value = emptyList()
        idCardFrontRawPath.value = null
        activeCropIndex.value = 0
        currentRoute.value = ScannerSubRoute.Camera(appendToDocId)
    }

    fun openDocumentDetail(docId: String) {
        currentRoute.value = ScannerSubRoute.DocumentDetail(docId)
    }

    fun navigateBackToHub() {
        batchFrames.value = emptyList()
        idCardFrontRawPath.value = null
        currentRoute.value = ScannerSubRoute.Hub
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
            if (currentFolderId.value == folderId) currentFolderId.value = null
        }
    }

    // --- Camera Capture & Gallery Import Pipeline ---
    fun processCapturedFile(
        file: File,
        appendToDocId: String?,
        isArabic: Boolean,
        capturedQuad: DocumentQuad? = null,
        analyzerWidth: Int = 0,
        analyzerHeight: Int = 0,
        autoNavigateToCrop: Boolean = false
    ) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                val mode = activeScanMode.value
                val framesToAdd = withContext(Dispatchers.Default) {
                    val rawBmp = withContext(Dispatchers.IO) {
                        DocumentImageProcessor.decodeSampledBitmap(file, 4096)?.also { decoded ->
                            // Save upright 98% JPEG so Crop & Preview share exact sensor orientation
                            DocumentImageProcessor.saveBitmapAsJpeg(decoded, file, 98)
                        }
                    } ?: return@withContext emptyList()

                    val mappedLiveQuad = capturedQuad?.let { q ->
                        DocumentImageProcessor.mapAnalyzerQuadToCaptureDimensions(
                            quad = q,
                            analyzerWidth = analyzerWidth,
                            analyzerHeight = analyzerHeight,
                            captureWidth = rawBmp.width,
                            captureHeight = rawBmp.height
                        )
                    }

                    when (mode) {
                        ScanMode.ID_CARD -> {
                            val firstPath = idCardFrontRawPath.value
                            if (firstPath == null) {
                                idCardFrontRawPath.value = file.absolutePath
                                rawBmp.recycle()
                                return@withContext emptyList()
                            } else {
                                val frontBmp = withContext(Dispatchers.IO) {
                                    DocumentImageProcessor.decodeSampledBitmap(File(firstPath), 4096)
                                } ?: rawBmp
                                val frontQuad = DocumentImageProcessor.detectQuadFromBitmap(frontBmp)
                                val frontWarped = DocumentImageProcessor.warpPerspective(frontBmp, frontQuad, 4096)
                                val frontEnhanced = DocumentImageProcessor.autoEnhanceCapturedDocument(
                                    warped = frontWarped,
                                    isIdCardMode = true
                                )
                                if (frontBmp !== rawBmp) frontBmp.recycle()
                                if (frontWarped !== frontEnhanced) frontWarped.recycle()

                                val detectedBack = DocumentImageProcessor.detectQuadFromBitmap(rawBmp)
                                val backQuad = if (detectedBack != DocumentQuad.SAFE_DEFAULT_MARGIN) {
                                    detectedBack
                                } else {
                                    mappedLiveQuad ?: DocumentQuad.SAFE_DEFAULT_MARGIN
                                }
                                val backWarped = DocumentImageProcessor.warpPerspective(rawBmp, backQuad, 4096)
                                val backEnhanced = DocumentImageProcessor.autoEnhanceCapturedDocument(
                                    warped = backWarped,
                                    isIdCardMode = true
                                )
                                rawBmp.recycle()
                                if (backWarped !== backEnhanced) backWarped.recycle()

                                val merged = DocumentImageProcessor.mergeIdCardFrontAndBack(frontEnhanced, backEnhanced)
                                frontEnhanced.recycle()
                                backEnhanced.recycle()

                                val mergedFile = File(repository.docsRootDir, "cap_id_${UUID.randomUUID()}.jpg")
                                withContext(Dispatchers.IO) {
                                    DocumentImageProcessor.saveBitmapAsJpeg(merged, mergedFile, 98)
                                }
                                merged.recycle()
                                idCardFrontRawPath.value = null
                                listOf(
                                    BatchCapturedFrame(
                                        rawImagePath = mergedFile.absolutePath,
                                        processedImagePath = mergedFile.absolutePath,
                                        detectedQuad = DocumentQuad.FULL_FRAME,
                                        filter = ScannerFilterType.ORIGINAL
                                    )
                                )
                            }
                        }

                        ScanMode.BOOK -> {
                            val detectedSpread = DocumentImageProcessor.detectQuadFromBitmap(rawBmp)
                            val spreadQuad = if (detectedSpread != DocumentQuad.SAFE_DEFAULT_MARGIN) {
                                detectedSpread
                            } else {
                                mappedLiveQuad ?: DocumentQuad.SAFE_DEFAULT_MARGIN
                            }
                            val flatSpread = DocumentImageProcessor.warpPerspective(rawBmp, spreadQuad, 4096)
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
                                DocumentImageProcessor.saveBitmapAsJpeg(leftEnhanced, leftFile, 98)
                                DocumentImageProcessor.saveBitmapAsJpeg(rightEnhanced, rightFile, 98)
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

                        else -> {
                            val detectedOnFullPhoto = DocumentImageProcessor.detectQuadFromBitmap(rawBmp)
                            val quad = if (detectedOnFullPhoto != DocumentQuad.SAFE_DEFAULT_MARGIN) {
                                detectedOnFullPhoto
                            } else {
                                mappedLiveQuad ?: DocumentQuad.SAFE_DEFAULT_MARGIN
                            }
                            val warpedPreview = DocumentImageProcessor.warpPerspective(rawBmp, quad, 4096)
                            if (warpedPreview !== rawBmp) rawBmp.recycle() else Unit
                            val autoEnhanced = DocumentImageProcessor.autoEnhanceCapturedDocument(
                                warped = warpedPreview,
                                isIdCardMode = false
                            )
                            if (warpedPreview !== autoEnhanced && !warpedPreview.isRecycled) {
                                warpedPreview.recycle()
                            }
                            val warpedFile = File(repository.docsRootDir, "warp_${UUID.randomUUID()}.jpg")
                            withContext(Dispatchers.IO) {
                                DocumentImageProcessor.saveBitmapAsJpeg(autoEnhanced, warpedFile, 98)
                            }
                            if (!autoEnhanced.isRecycled) autoEnhanced.recycle()
                            listOf(
                                BatchCapturedFrame(
                                    rawImagePath = file.absolutePath,
                                    processedImagePath = warpedFile.absolutePath,
                                    detectedQuad = quad,
                                    filter = ScannerFilterType.MAGIC_COLOR
                                )
                            )
                        }
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
            } finally {
                isBusy.value = false
            }
        }
    }

    fun importGalleryUris(uris: List<Uri>, appendToDocId: String?, isArabic: Boolean) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            isBusy.value = true
            try {
                val imported = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    uris.mapNotNull { uri ->
                        runCatching {
                            val bmp = DocumentImageProcessor.decodeOrientedBitmapFromUri(
                                contentResolver = resolver,
                                uri = uri,
                                maxSide = 4096
                            ) ?: return@mapNotNull null
                            val rawFile = File(repository.docsRootDir, "imp_${UUID.randomUUID()}.jpg")
                            DocumentImageProcessor.saveBitmapAsJpeg(bmp, rawFile, 95)
                            val quad = withContext(Dispatchers.Default) {
                                DocumentImageProcessor.detectQuadFromBitmap(bmp)
                            }
                            bmp.recycle()
                            BatchCapturedFrame(
                                rawImagePath = rawFile.absolutePath,
                                processedImagePath = rawFile.absolutePath,
                                detectedQuad = quad,
                                filter = ScannerFilterType.MAGIC_COLOR
                            )
                        }.getOrNull()
                    }
                }
                if (imported.isNotEmpty()) {
                    batchFrames.value = batchFrames.value + imported
                    activeCropIndex.value = (batchFrames.value.size - imported.size).coerceAtLeast(0)
                    currentRoute.value = ScannerSubRoute.CropAndFilter(appendToDocId)
                } else {
                    statusMessage.value = if (isArabic) "تعذر استيراد الصورة" else "Unable to import image"
                }
            } finally {
                isBusy.value = false
            }
        }
    }

    fun importMlKitScanResult(
        pageUris: List<Uri>,
        pdfUri: Uri?,
        appendToDocId: String?,
        isArabic: Boolean
    ) {
        if (pageUris.isEmpty() && pdfUri == null) return
        viewModelScope.launch {
            isBusy.value = true
            try {
                val targetDocId = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val now = System.currentTimeMillis()
                    val existingDoc = appendToDocId?.let { repository.getDocumentOnce(it) }
                    val docId = existingDoc?.id ?: UUID.randomUUID().toString()
                    val existingPages = if (existingDoc != null) repository.getPagesOnce(docId) else emptyList()
                    var nextOrder = existingPages.size

                    val createdPages = mutableListOf<ScannedPageEntity>()
                    val pdfInputs = mutableListOf<PdfRenderedPageInput>()

                    pageUris.forEach { uri ->
                        runCatching {
                            resolver.openInputStream(uri)?.use { input ->
                                val bmp = BitmapFactory.decodeStream(input) ?: return@use
                                val pageFile = File(repository.docsRootDir, "mlkit_${UUID.randomUUID()}.jpg")
                                DocumentImageProcessor.saveBitmapAsJpeg(bmp, pageFile, 93)
                                val ocrRes = OfflineOcrAndBarcodeEngine.extractTextOffline(bmp)
                                createdPages.add(
                                    ScannedPageEntity(
                                        documentId = docId,
                                        pageOrder = nextOrder++,
                                        originalImagePath = pageFile.absolutePath,
                                        processedImagePath = pageFile.absolutePath,
                                        cornersJson = DocumentQuad.FULL_FRAME.toJson(),
                                        filterId = ScannerFilterType.ORIGINAL.id,
                                        ocrText = ocrRes.fullText
                                    )
                                )
                                pdfInputs.add(PdfRenderedPageInput(bmp, ocrRes.lines))
                            }
                        }
                    }

                    if (createdPages.isEmpty()) return@withContext null

                    val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(now))
                    val defaultTitle = if (isArabic) "مسح ذكي $stamp" else "AI Scan $stamp"

                    if (existingDoc == null) {
                        val destPdf = File(repository.docsRootDir, "doc_$docId.pdf")
                        PdfEngine.buildPdfFromBitmaps(
                            destFile = destPdf,
                            pages = pdfInputs,
                            pageSizePreset = PdfPageSizePreset.A4,
                            searchableOcr = true
                        )
                        pdfInputs.forEach { it.bitmap.recycle() }

                        val totalSize = createdPages.sumOf { File(it.processedImagePath).length() } +
                            (destPdf.takeIf { it.exists() }?.length() ?: 0L)

                        val docEntity = ScannedDocumentEntity(
                            id = docId,
                            folderId = currentFolderId.value,
                            title = defaultTitle,
                            createdAt = now,
                            updatedAt = now,
                            pageCount = createdPages.size,
                            totalSizeBytes = totalSize,
                            coverThumbnailPath = createdPages.firstOrNull()?.processedImagePath.orEmpty(),
                            pdfPath = destPdf.absolutePath,
                            ocrFullText = createdPages.mapNotNull { it.ocrText.takeIf(String::isNotBlank) }.joinToString("\n\n"),
                            isSearchablePdf = true
                        )
                        repository.upsertDocument(docEntity)
                        repository.upsertPages(createdPages)
                    } else {
                        pdfInputs.forEach { it.bitmap.recycle() }
                        repository.upsertPages(createdPages)
                        repository.syncDocumentStats(docId)
                    }
                    docId
                }

                if (targetDocId != null) {
                    currentRoute.value = ScannerSubRoute.DocumentDetail(targetDocId)
                    statusMessage.value = if (isArabic) {
                        "تم حفظ المستند بالماسح الذكي ✨"
                    } else {
                        "Scanned & saved with AI Document Scanner ✨"
                    }
                } else {
                    statusMessage.value = if (isArabic) "تعذر معالجة الصفحات" else "Unable to process scanned pages"
                }
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
        list[index] = list[index].copy(
            filter = filter,
            brightness = brightness,
            contrast = contrast,
            sharpness = sharpness,
            rotationDeg = rotationDeg
        )
        batchFrames.value = list
    }

    fun removeBatchFrame(index: Int) {
        val list = batchFrames.value.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        batchFrames.value = list
        if (list.isEmpty()) {
            currentRoute.value = ScannerSubRoute.Camera(null)
        } else {
            activeCropIndex.value = activeCropIndex.value.coerceIn(0, list.lastIndex)
        }
    }

    /**
     * Finalizes all cropped + filtered frames, runs optional offline OCR, generates a Document
     * & local multi-page PDF, and saves into Room.
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

                    frames.forEach { frame ->
                        val rawBmp = DocumentImageProcessor.decodeSampledBitmap(File(frame.rawImagePath), 4096)
                            ?: return@forEach
                        val warped = withContext(Dispatchers.Default) {
                            DocumentImageProcessor.warpPerspective(rawBmp, frame.detectedQuad, 4096)
                        }
                        val enhanced = withContext(Dispatchers.Default) {
                            DocumentImageProcessor.applyEnhancements(
                                source = warped,
                                filter = frame.filter,
                                brightness = frame.brightness,
                                contrast = frame.contrast,
                                sharpness = frame.sharpness,
                                rotationDeg = frame.rotationDeg
                            )
                        }
                        val outFile = File(repository.docsRootDir, "page_${UUID.randomUUID()}.jpg")
                        DocumentImageProcessor.saveBitmapAsJpeg(enhanced, outFile, 98)
                        val ocrRes = OfflineOcrAndBarcodeEngine.extractTextOffline(enhanced)

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
                        pdfInputs.add(PdfRenderedPageInput(enhanced, ocrRes.lines))
                        if (rawBmp !== warped) rawBmp.recycle()
                        warped.recycle()
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
                        pdfInputs.forEach { it.bitmap.recycle() }

                        val totalSize = createdPages.sumOf { File(it.processedImagePath).length() } +
                            (pdfFile.takeIf { it.exists() }?.length() ?: 0L)
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
                            isSearchablePdf = true
                        )
                        repository.upsertDocument(docEntity)
                        repository.upsertPages(createdPages)
                    } else {
                        pdfInputs.forEach { it.bitmap.recycle() }
                        repository.upsertPages(createdPages)
                        repository.syncDocumentStats(docId)
                    }
                    docId
                }

                batchFrames.value = emptyList()
                currentRoute.value = ScannerSubRoute.DocumentDetail(targetDocId)
                statusMessage.value = if (isArabic) "تم حفظ المستند وتنسيق النص بنجاح ✨" else "Document saved locally ✨"
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
        viewModelScope.launch {
            repository.updateDocument(doc.copy(title = clean, updatedAt = System.currentTimeMillis()))
        }
    }

    fun toggleTagOnDocument(doc: ScannedDocumentEntity, tagName: String) {
        val current = doc.tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
        if (!current.add(tagName)) current.remove(tagName)
        viewModelScope.launch {
            repository.updateDocument(
                doc.copy(tagsCsv = current.joinToString(","), updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun toggleStarred(doc: ScannedDocumentEntity) {
        viewModelScope.launch {
            repository.updateDocument(doc.copy(isStarred = !doc.isStarred, updatedAt = System.currentTimeMillis()))
        }
    }

    fun moveDocumentToFolder(docId: String, folderId: String?) {
        viewModelScope.launch { repository.moveDocumentToFolder(docId, folderId) }
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
        viewModelScope.launch {
            repository.moveToTrash(docId)
            if (currentRoute.value is ScannerSubRoute.DocumentDetail) {
                currentRoute.value = ScannerSubRoute.Hub
            }
            statusMessage.value = if (isArabic) "تم النقل إلى سلة المهملات (تحذف تلقائياً بعد 30 يوماً)" else "Moved to Trash (30-day retention)"
        }
    }

    fun restoreFromTrash(docId: String) {
        viewModelScope.launch { repository.restoreFromTrash(docId) }
    }

    fun deletePermanently(docId: String) {
        viewModelScope.launch { repository.deleteDocumentPermanently(docId) }
    }

    fun movePageOrder(docId: String, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val list = repository.getPagesOnce(docId).toMutableList()
            if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return@launch
            val moved = list.removeAt(fromIndex)
            list.add(toIndex, moved)
            val reindexed = list.mapIndexed { idx, p -> p.copy(pageOrder = idx) }
            repository.upsertPages(reindexed)
            repository.syncDocumentStats(docId)
        }
    }

    fun duplicatePage(docId: String, page: ScannedPageEntity, isArabic: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val srcFile = File(page.processedImagePath)
                val copyFile = File(repository.docsRootDir, "dup_${UUID.randomUUID()}.jpg")
                if (srcFile.exists()) srcFile.copyTo(copyFile, overwrite = true)
                val pages = repository.getPagesOnce(docId)
                val inserted = page.copy(
                    id = UUID.randomUUID().toString(),
                    pageOrder = page.pageOrder + 1,
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
        viewModelScope.launch {
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
                        val pdfFile = File(repository.docsRootDir, "doc_$newDocId.pdf")
                        PdfEngine.buildPdfFromBitmaps(pdfFile, listOf(PdfRenderedPageInput(bmp)))
                        bmp.recycle()
                        val newDoc = ScannedDocumentEntity(
                            id = newDocId,
                            folderId = sourceDoc.folderId,
                            title = "${sourceDoc.title} (P${page.pageOrder + 1})",
                            pageCount = 1,
                            totalSizeBytes = destImg.length() + pdfFile.length(),
                            coverThumbnailPath = destImg.absolutePath,
                            pdfPath = pdfFile.absolutePath,
                            ocrFullText = page.ocrText,
                            isSearchablePdf = true
                        )
                        val newPage = page.copy(
                            id = UUID.randomUUID().toString(),
                            documentId = newDocId,
                            pageOrder = 0,
                            originalImagePath = destImg.absolutePath,
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
        viewModelScope.launch {
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
