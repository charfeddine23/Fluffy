package com.charfeddine.fluffy.scanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.charfeddine.fluffy.scanner.data.model.BarcodeScanResult
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

data class OcrTextBox(
    val text: String,
    val normRect: RectF
)

data class OcrExtractionResult(
    val fullText: String,
    val lines: List<OcrTextBox>
)

/**
 * 100% Offline Bilingual OCR & Vision Engine:
 *  - Google ML Kit Text Recognition for instant offline Latin (English / French / MRZ) recognition.
 *  - Tesseract4Android (`ara.traineddata` from `assets/tessdata/`) paired with adaptive binarization
 *    ([DocumentImageProcessor.prepareBitmapForArabicOcr]) for Arabic documents & Algerian IDs.
 *  - Bundled ML Kit Barcode Scanning for QR / PDF417 / EAN codes.
 */
object OfflineOcrAndBarcodeEngine {

    private const val TAG = "OfflineOcrEngine"
    private const val TESS_ASSET_RELATIVE = "tessdata/ara.traineddata"
    private const val TESS_DIR_NAME = "tesseract"
    private const val TESS_LANG_ARABIC = "ara"

    @Volatile
    private var appContext: Context? = null

    private val tessMutex = Mutex()

    @Volatile
    private var tessApi: TessBaseAPI? = null

    @Volatile
    private var tessInitAttempted = false

    @Volatile
    private var tessReady = false

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val barcodeScanner by lazy {
        val opts = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(opts)
    }

    /**
     * Binds an application [Context] so Tesseract can provision `ara.traineddata` from
     * `app/src/main/assets/tessdata/ara.traineddata` on first OCR execution.
     */
    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    suspend fun extractTextOffline(
        bitmap: Bitmap,
        context: Context? = null
    ): OcrExtractionResult {
        if (context != null) init(context)

        return coroutineScope {
            val latinDeferred = async(Dispatchers.Default) {
                extractLatinWithMlKit(bitmap)
            }
            val arabicDeferred = async(Dispatchers.IO) {
                extractArabicWithTesseract(bitmap)
            }

            val latinResult = latinDeferred.await()
            val arabicResult = arabicDeferred.await()
            mergeBilingualOcrResults(arabicResult, latinResult)
        }
    }

    private suspend fun extractLatinWithMlKit(bitmap: Bitmap): OcrExtractionResult =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            val w = bitmap.width.coerceAtLeast(1).toFloat()
            val h = bitmap.height.coerceAtLeast(1).toFloat()

            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val boxes = buildList {
                        visionText.textBlocks.forEach { block ->
                            block.lines.forEach { line ->
                                val box = line.boundingBox
                                if (box != null && line.text.isNotBlank()) {
                                    add(
                                        OcrTextBox(
                                            text = line.text.trim(),
                                            normRect = RectF(
                                                (box.left / w).coerceIn(0f, 1f),
                                                (box.top / h).coerceIn(0f, 1f),
                                                (box.right / w).coerceIn(0f, 1f),
                                                (box.bottom / h).coerceIn(0f, 1f)
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                    if (cont.isActive) {
                        cont.resume(OcrExtractionResult(fullText = visionText.text.trim(), lines = boxes))
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) {
                        cont.resume(OcrExtractionResult(fullText = "", lines = emptyList()))
                    }
                }
        }

    private suspend fun extractArabicWithTesseract(sourceBitmap: Bitmap): OcrExtractionResult {
        val ctx = appContext ?: return OcrExtractionResult("", emptyList())
        return tessMutex.withLock {
            val api = getOrInitArabicTesseract(ctx) ?: return@withLock OcrExtractionResult("", emptyList())
            val binarized = withContext(Dispatchers.Default) {
                DocumentImageProcessor.prepareBitmapForArabicOcr(sourceBitmap)
            }
            val bw = binarized.width.coerceAtLeast(1).toFloat()
            val bh = binarized.height.coerceAtLeast(1).toFloat()

            try {
                api.setImage(binarized)
                val boxes = mutableListOf<OcrTextBox>()
                val lineStrings = mutableListOf<String>()

                val recognizedOk = runCatching {
                    api.utF8Text
                }.getOrNull()

                val iterator = runCatching { api.resultIterator }.getOrNull()
                if (iterator != null) {
                    try {
                        iterator.begin()
                        val level = TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE
                        do {
                            val lineRaw = iterator.getUTF8Text(level)?.trim().orEmpty()
                            val conf = iterator.confidence(level)
                            val rect = iterator.getBoundingRect(level)
                            if (isMeaningfulOcrLine(lineRaw) && conf >= 28f && rect != null) {
                                lineStrings.add(lineRaw)
                                boxes.add(
                                    OcrTextBox(
                                        text = lineRaw,
                                        normRect = RectF(
                                            (rect.left / bw).coerceIn(0f, 1f),
                                            (rect.top / bh).coerceIn(0f, 1f),
                                            (rect.right / bw).coerceIn(0f, 1f),
                                            (rect.bottom / bh).coerceIn(0f, 1f)
                                        )
                                    )
                                )
                            }
                        } while (iterator.next(level))
                    } finally {
                        iterator.delete()
                    }
                } else if (!recognizedOk.isNullOrBlank()) {
                    recognizedOk.lines()
                        .map { it.trim() }
                        .filter { isMeaningfulOcrLine(it) }
                        .forEach { line -> lineStrings.add(line) }
                }

                OcrExtractionResult(
                    fullText = lineStrings.joinToString("\n").trim(),
                    lines = boxes
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Tesseract Arabic OCR failed cleanly", t)
                OcrExtractionResult("", emptyList())
            } finally {
                runCatching { api.clear() }
                if (binarized !== sourceBitmap && !binarized.isRecycled) {
                    binarized.recycle()
                }
            }
        }
    }

    private fun getOrInitArabicTesseract(context: Context): TessBaseAPI? {
        if (tessReady && tessApi != null) return tessApi
        if (tessInitAttempted && !tessReady) return null

        tessInitAttempted = true
        return try {
            val baseDir = File(context.filesDir, TESS_DIR_NAME)
            val tessDataDir = File(baseDir, "tessdata").apply { mkdirs() }
            val trainedFile = File(tessDataDir, "$TESS_LANG_ARABIC.traineddata")

            if (!trainedFile.exists() || trainedFile.length() == 0L) {
                val assetCopied = copyAssetIfExists(context, TESS_ASSET_RELATIVE, trainedFile)
                if (!assetCopied) {
                    Log.i(TAG, "assets/$TESS_ASSET_RELATIVE not bundled yet; using ML Kit offline OCR.")
                    return null
                }
            }

            val api = TessBaseAPI()
            val initialized = api.init(baseDir.absolutePath, TESS_LANG_ARABIC, TessBaseAPI.OEM_LSTM_ONLY) ||
                api.init(baseDir.absolutePath, TESS_LANG_ARABIC)
            if (!initialized) {
                api.recycle()
                Log.w(TAG, "TessBaseAPI failed to initialize for '$TESS_LANG_ARABIC'.")
                return null
            }

            api.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            tessApi = api
            tessReady = true
            api
        } catch (t: Throwable) {
            Log.w(TAG, "Failed setting up Tesseract4Android", t)
            null
        }
    }

    private fun copyAssetIfExists(context: Context, assetPath: String, destFile: File): Boolean {
        return runCatching {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            destFile.exists() && destFile.length() > 0L
        }.getOrDefault(false)
    }

    private fun isMeaningfulOcrLine(line: String): Boolean {
        if (line.length < 2) return false
        return line.any { ch ->
            Character.isLetterOrDigit(ch) || ch in '\u0600'..'\u06FF'
        }
    }

    private fun containsArabicScript(text: String): Boolean =
        text.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' }

    private fun mergeBilingualOcrResults(
        arabicResult: OcrExtractionResult,
        latinResult: OcrExtractionResult
    ): OcrExtractionResult {
        if (arabicResult.fullText.isBlank()) return latinResult
        if (latinResult.fullText.isBlank()) return arabicResult

        val mergedBoxes = mutableListOf<OcrTextBox>()
        val seenNormalized = mutableSetOf<String>()

        // Prefer Arabic lines from Tesseract when line contains Arabic glyphs
        arabicResult.lines.forEach { box ->
            if (containsArabicScript(box.text)) {
                val key = box.text.replace("\\s+".toRegex(), " ").trim().lowercase()
                if (seenNormalized.add(key)) {
                    mergedBoxes.add(box)
                }
            }
        }

        // Add Latin / numeric / MRZ lines from ML Kit
        latinResult.lines.forEach { box ->
            val key = box.text.replace("\\s+".toRegex(), " ").trim().lowercase()
            if (seenNormalized.add(key)) {
                mergedBoxes.add(box)
            }
        }

        // Order top-to-bottom vertically so searchable PDF text layer & plain text stay natural
        val orderedBoxes = mergedBoxes.sortedBy { it.normRect.top }
        val extraArabicLines = if (arabicResult.lines.isEmpty()) {
            arabicResult.fullText.lines()
                .map { it.trim() }
                .filter { isMeaningfulOcrLine(it) && containsArabicScript(it) }
        } else {
            emptyList()
        }
        val combinedText = buildList {
            extraArabicLines.forEach { add(it) }
            if (orderedBoxes.isNotEmpty()) {
                orderedBoxes.forEach { add(it.text) }
            } else {
                if (extraArabicLines.isEmpty() && arabicResult.fullText.isNotBlank()) add(arabicResult.fullText)
                if (latinResult.fullText.isNotBlank()) add(latinResult.fullText)
            }
        }.joinToString("\n").trim()

        return OcrExtractionResult(
            fullText = combinedText,
            lines = orderedBoxes
        )
    }

    fun release() {
        runCatching {
            tessApi?.recycle()
            tessApi = null
            tessReady = false
            tessInitAttempted = false
        }
    }

    fun scanBarcodeFromInputImage(
        image: InputImage,
        onDetected: (BarcodeScanResult?) -> Unit,
        onComplete: () -> Unit = {}
    ) {
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                if (first != null) {
                    val raw = first.rawValue.orEmpty()
                    val formatLabel = when (first.format) {
                        Barcode.FORMAT_QR_CODE -> "QR Code"
                        Barcode.FORMAT_EAN_13 -> "EAN-13"
                        Barcode.FORMAT_EAN_8 -> "EAN-8"
                        Barcode.FORMAT_CODE_128 -> "Code 128"
                        Barcode.FORMAT_PDF417 -> "PDF417"
                        else -> "Barcode"
                    }
                    val isUrl = first.valueType == Barcode.TYPE_URL ||
                        raw.startsWith("http://", ignoreCase = true) ||
                        raw.startsWith("https://", ignoreCase = true)
                    val title = when (first.valueType) {
                        Barcode.TYPE_URL -> first.url?.title?.takeIf { it.isNotBlank() } ?: "Website Link"
                        Barcode.TYPE_WIFI -> "Wi-Fi Network: ${first.wifi?.ssid.orEmpty()}"
                        Barcode.TYPE_CONTACT_INFO -> first.contactInfo?.name?.formattedName ?: "Contact Card"
                        Barcode.TYPE_EMAIL -> "Email Address"
                        Barcode.TYPE_PHONE -> "Phone Number"
                        else -> formatLabel
                    }
                    onDetected(
                        BarcodeScanResult(
                            rawValue = raw,
                            displayTitle = title,
                            formatLabel = formatLabel,
                            isUrl = isUrl
                        )
                    )
                }
            }
            .addOnCompleteListener { onComplete() }
    }
}
