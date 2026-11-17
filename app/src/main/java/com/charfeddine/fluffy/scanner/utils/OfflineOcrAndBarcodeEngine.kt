package com.charfeddine.fluffy.scanner.utils

import android.graphics.Bitmap
import android.graphics.RectF
import com.charfeddine.fluffy.scanner.data.model.BarcodeScanResult
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
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
 * 100% Offline ML Kit Vision Engine using bundled on-device Text Recognition & Barcode models.
 */
object OfflineOcrAndBarcodeEngine {

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val barcodeScanner by lazy {
        val opts = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(opts)
    }

    suspend fun extractTextOffline(bitmap: Bitmap): OcrExtractionResult =
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
                                            text = line.text,
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
                        cont.resume(OcrExtractionResult(fullText = visionText.text, lines = boxes))
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) {
                        cont.resume(OcrExtractionResult(fullText = "", lines = emptyList()))
                    }
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
