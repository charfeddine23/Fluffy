package com.charfeddine.fluffy.scanner.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

data class MlKitScanPayload(
    val pageImageUris: List<Uri>,
    val pdfUri: Uri?
)

/**
 * Primary native Google ML Kit Document Scanner wrapper (`GmsDocumentScanning`).
 * Configured in SCANNER_MODE_FULL for automatic boundary detection, perspective correction,
 * shadow/stain removal, and built-in Magic Color / Grayscale / B&W filters.
 * Automatically invokes fallback callback if Play Services / hardware prerequisites are unavailable.
 */
object MlKitDocumentScannerHelper {

    private fun buildOptions(pageLimit: Int = 25): GmsDocumentScannerOptions {
        return GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(pageLimit.coerceIn(1, 50))
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()
    }

    fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    fun launch(
        context: Context,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        pageLimit: Int = 25,
        onFallbackToCustomCamera: () -> Unit
    ) {
        val activity = findActivity(context)
        if (activity == null) {
            onFallbackToCustomCamera()
            return
        }
        runCatching {
            val scanner = GmsDocumentScanning.getClient(buildOptions(pageLimit))
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    runCatching {
                        launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }.onFailure {
                        onFallbackToCustomCamera()
                    }
                }
                .addOnFailureListener {
                    onFallbackToCustomCamera()
                }
        }.onFailure {
            onFallbackToCustomCamera()
        }
    }

    fun parseResult(data: Intent?): MlKitScanPayload? {
        if (data == null) return null
        val result = GmsDocumentScanningResult.fromActivityResultIntent(data) ?: return null
        val pageUris = result.pages?.mapNotNull { it.imageUri } ?: emptyList()
        val pdfUri = result.pdf?.uri
        if (pageUris.isEmpty() && pdfUri == null) return null
        return MlKitScanPayload(pageImageUris = pageUris, pdfUri = pdfUri)
    }
}
