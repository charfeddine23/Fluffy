package com.charfeddine.fluffy.scanner.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

data class MlKitScanPayload(
    val pageImageUris: List<Uri>,
    val pdfUri: Uri?
)

/**
 * Primary high-precision Document Scanner backed by Google ML Kit ([GmsDocumentScanning])
 * providing automatic boundary detection, perspective correction, and illumination cleanup,
 * with graceful fallback to Fluffy's custom local CameraX + CLAHE pipeline when unavailable.
 */
object MlKitDocumentScannerHelper {

    private fun buildOptions(): GmsDocumentScannerOptions =
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(30)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

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
        onFallbackToCameraX: () -> Unit
    ) {
        val activity = findActivity(context)
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onFallbackToCameraX()
            return
        }

        runCatching {
            val client = GmsDocumentScanning.getClient(buildOptions())
            client.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    runCatching {
                        launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }.onFailure {
                        onFallbackToCameraX()
                    }
                }
                .addOnFailureListener {
                    onFallbackToCameraX()
                }
        }.onFailure {
            onFallbackToCameraX()
        }
    }

    fun parseResult(data: Intent?): MlKitScanPayload? {
        val result = GmsDocumentScanningResult.fromActivityResultIntent(data) ?: return null
        val pageUris = result.pages?.map { it.imageUri }.orEmpty()
        val pdfUri = result.pdf?.uri
        if (pageUris.isEmpty() && pdfUri == null) return null
        return MlKitScanPayload(
            pageImageUris = pageUris,
            pdfUri = pdfUri
        )
    }
}
