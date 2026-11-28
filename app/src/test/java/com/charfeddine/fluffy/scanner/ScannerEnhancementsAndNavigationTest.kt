package com.charfeddine.fluffy.scanner

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.charfeddine.fluffy.scanner.data.local.ScannerDatabase
import com.charfeddine.fluffy.scanner.data.model.BatchCapturedFrame
import com.charfeddine.fluffy.scanner.data.model.ScannedDocumentEntity
import com.charfeddine.fluffy.scanner.data.model.ScannedPageEntity
import com.charfeddine.fluffy.scanner.data.model.ScannerFilterType
import com.charfeddine.fluffy.scanner.domain.ScannerSubRoute
import com.charfeddine.fluffy.scanner.domain.ScannerViewModel
import com.charfeddine.fluffy.scanner.ui.components.ScannerRootHost
import com.charfeddine.fluffy.scanner.ui.screens.CropAndFilterScreen
import com.charfeddine.fluffy.scanner.ui.screens.DocumentDetailEditorScreen
import com.charfeddine.fluffy.scanner.utils.DocumentImageProcessor
import com.example.ui.components.VoiceRecorderDialog
import com.example.ui.theme.FluffyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScannerEnhancementsAndNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var viewModel: ScannerViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        val db = ScannerDatabase.getDatabase(app)
        runBlocking(Dispatchers.IO) {
            db.clearAllTables()
        }
        viewModel = ScannerViewModel(app)
    }

    private fun createSyntheticDocumentBitmap(width: Int = 240, height: Int = 320): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        // Slightly yellowed/aged paper base
        canvas.drawColor(Color.rgb(236, 224, 192))
        // Uneven shadow band across upper-left quadrant
        val shadowPaint = Paint().apply { color = Color.rgb(120, 114, 98) }
        canvas.drawRect(0f, 0f, width * 0.48f, height * 0.45f, shadowPaint)
        // Faded gray text lines across the lower half
        val fadedInkPaint = Paint().apply {
            color = Color.rgb(138, 134, 128)
            strokeWidth = 6f
        }
        canvas.drawLine(24f, height * 0.62f, width - 24f, height * 0.62f, fadedInkPaint)
        canvas.drawLine(24f, height * 0.72f, width - 48f, height * 0.72f, fadedInkPaint)
        // Colored accent stamp for saturation / temperature tests
        val stampPaint = Paint().apply { color = Color.rgb(210, 70, 50) }
        canvas.drawRect(width * 0.65f, height * 0.15f, width * 0.88f, height * 0.30f, stampPaint)
        return bmp
    }

    private fun luminance(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (r * 77 + g * 150 + b * 29) shr 8
    }

    @Test
    fun professionalDocumentFiltersProduceExpectedClarityAndDistinctOutputs() {
        assertEquals(
            listOf(
                ScannerFilterType.ORIGINAL,
                ScannerFilterType.MAGIC_COLOR,
                ScannerFilterType.BW_DOCUMENT,
                ScannerFilterType.GRAYSCALE,
                ScannerFilterType.SHADOW_GLARE_REMOVAL,
                ScannerFilterType.SUPER_CLEAR,
                ScannerFilterType.DEEP_INK,
                ScannerFilterType.CLEAN_PAPER
            ),
            ScannerFilterType.entries
        )
        ScannerFilterType.entries.forEach { filter ->
            assertEquals(filter, ScannerFilterType.fromId(filter.id))
        }

        val source = createSyntheticDocumentBitmap()
        val origShadowLum = luminance(source.getPixel(36, 36))
        val origInkLum = luminance(source.getPixel(120, (source.height * 0.62f).toInt()))

        val shadowRemoved = DocumentImageProcessor.applyEnhancements(
            source = source,
            filter = ScannerFilterType.SHADOW_GLARE_REMOVAL,
            enableSuperRes = false
        )
        val superClear = DocumentImageProcessor.applyEnhancements(
            source = source,
            filter = ScannerFilterType.SUPER_CLEAR,
            enableSuperRes = false
        )
        val deepInk = DocumentImageProcessor.applyEnhancements(
            source = source,
            filter = ScannerFilterType.DEEP_INK,
            enableSuperRes = false
        )
        val cleanPaper = DocumentImageProcessor.applyEnhancements(
            source = source,
            filter = ScannerFilterType.CLEAN_PAPER,
            enableSuperRes = false
        )

        listOf(shadowRemoved, superClear, deepInk, cleanPaper).forEach { bmp ->
            assertFalse(bmp.isRecycled)
            assertEquals(source.width, bmp.width)
            assertEquals(source.height, bmp.height)
        }

        // Shadow Removal lifts dark shadow zone toward paper brightness
        val deshadowLum = luminance(shadowRemoved.getPixel(36, 36))
        assertTrue("Expected shadow lift: orig=$origShadowLum, out=$deshadowLum", deshadowLum > origShadowLum + 45)

        // Deep Ink darkens faded handwriting/print relative to original ink luminance
        val deepInkLum = luminance(deepInk.getPixel(120, (source.height * 0.62f).toInt()))
        assertTrue("Expected darkened ink: orig=$origInkLum, out=$deepInkLum", deepInkLum < origInkLum - 20)

        // Clean Paper whitens yellowed paper surface cleanly
        val cleanPaperPx = cleanPaper.getPixel(160, 260)
        assertTrue("Expected bright clean paper: lum=${luminance(cleanPaperPx)}", luminance(cleanPaperPx) >= 235)

        // Super Clear produces high-contrast crisp separation between paper and ink
        val scPaperLum = luminance(superClear.getPixel(160, 260))
        val scInkLum = luminance(superClear.getPixel(120, (source.height * 0.62f).toInt()))
        assertTrue("Expected crisp contrast: paper=$scPaperLum, ink=$scInkLum", scPaperLum - scInkLum >= 80)

        source.recycle()
        shadowRemoved.recycle()
        superClear.recycle()
        deepInk.recycle()
        cleanPaper.recycle()
    }

    @Test
    fun proAdjustmentsApplyShadowsHighlightsSaturationAndTemperature() {
        val source = createSyntheticDocumentBitmap()

        // Shadows boost lifts dark area luminance while highlights recovery dims bright paper
        val shadowBoosted = DocumentImageProcessor.applyEnhancements(
            source = source,
            filter = ScannerFilterType.ORIGINAL,
            shadows = 0.85f,
            enableSuperRes = false
        )
        assertTrue(
            luminance(shadowBoosted.getPixel(36, 36)) > luminance(source.getPixel(36, 36)) + 20
        )

        val highlightsPulled = DocumentImageProcessor.applyEnhancements(
            source = source,
            filter = ScannerFilterType.ORIGINAL,
            highlights = -0.85f,
            enableSuperRes = false
        )
        assertTrue(
            luminance(highlightsPulled.getPixel(160, 260)) < luminance(source.getPixel(160, 260)) - 15
        )

        // Saturation = 0f converts colored stamp pixel to grayscale (R == G == B within rounding)
        val desaturated = DocumentImageProcessor.applyEnhancements(
            source = source,
            filter = ScannerFilterType.ORIGINAL,
            saturation = 0f,
            enableSuperRes = false
        )
        val stampPx = desaturated.getPixel((source.width * 0.76f).toInt(), (source.height * 0.22f).toInt())
        assertTrue(abs(Color.red(stampPx) - Color.green(stampPx)) <= 3)
        assertTrue(abs(Color.green(stampPx) - Color.blue(stampPx)) <= 3)

        // Temperature warm vs cool shifts R - B balance noticeably
        val warm = DocumentImageProcessor.applyEnhancements(
            source = source,
            filter = ScannerFilterType.ORIGINAL,
            temperature = 0.85f,
            enableSuperRes = false
        )
        val cool = DocumentImageProcessor.applyEnhancements(
            source = source,
            filter = ScannerFilterType.ORIGINAL,
            temperature = -0.85f,
            enableSuperRes = false
        )
        val warmPx = warm.getPixel(160, 260)
        val coolPx = cool.getPixel(160, 260)
        assertTrue(
            (Color.red(warmPx) - Color.blue(warmPx)) > (Color.red(coolPx) - Color.blue(coolPx)) + 40
        )

        source.recycle()
        shadowBoosted.recycle()
        highlightsPulled.recycle()
        desaturated.recycle()
        warm.recycle()
        cool.recycle()
    }

    @Test
    fun editPageFromPage3AndSaveReturnsDirectlyToPage3AndSupportsMultiPageNav() {
        val doc = ScannedDocumentEntity(
            id = "doc-preserve-p3",
            title = "Three Page Contract",
            pageCount = 3,
            totalSizeBytes = 4096L,
            sortOrder = 1000L
        )
        val bmp = createSyntheticDocumentBitmap(200, 280)
        val pageFiles = (1..3).map { idx ->
            File(viewModel.repository.docsRootDir, "preserve_p_$idx.jpg").also {
                DocumentImageProcessor.saveBitmapAsJpeg(bmp, it, 92)
            }
        }
        bmp.recycle()

        runBlocking(Dispatchers.IO) {
            viewModel.repository.upsertDocument(doc)
            pageFiles.forEachIndexed { idx, file ->
                viewModel.repository.upsertPage(
                    ScannedPageEntity(
                        id = "p3-page-${idx + 1}",
                        documentId = doc.id,
                        pageOrder = idx,
                        originalImagePath = file.absolutePath,
                        processedImagePath = file.absolutePath
                    )
                )
            }
        }
        // Start on Page 3 (index 2) and host the full scanner route stack
        viewModel.setDetailPageIndex(doc.id, 2)
        viewModel.openDocumentDetail(doc.id)

        composeRule.setContent {
            ScannerRootHost(
                isArabic = false,
                onTriggerBiometricPrompt = { _, _, onSuccess, _ -> onSuccess() },
                onSaveQrAsFluffyNote = { _, _ -> },
                onExitScanner = {},
                scannerViewModel = viewModel
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(hasText("3 / 3")).fetchSemanticsNodes().isNotEmpty()
        }

        // Open Doc Scanner Edit screen from Page 3
        composeRule.onNodeWithTag("doc_edit_page_btn").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            viewModel.currentRoute.value is ScannerSubRoute.CropAndFilter &&
                composeRule.onAllNodesWithTag("crop_apply_next_btn").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify all 3 pages loaded inside editor starting on Page 3 (index 2 -> "3/3")
        assertEquals(3, viewModel.batchFrames.value.size)
        assertEquals(2, viewModel.activeCropIndex.value)
        composeRule.onNode(hasText("3/3")).assertIsDisplayed()

        // Enter Enhance tab/phase
        composeRule.onNodeWithTag("crop_apply_next_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("crop_save_document_btn").fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate Prev -> Page 2 and Next -> Page 3 seamlessly inside Enhance without exiting edit mode
        composeRule.onNodeWithTag("batch_prev_page_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) { viewModel.activeCropIndex.value == 1 }
        composeRule.onNode(hasText("2/3")).assertIsDisplayed()
        composeRule.onNodeWithTag("crop_save_document_btn").assertIsDisplayed()

        composeRule.onNodeWithTag("batch_next_page_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) { viewModel.activeCropIndex.value == 2 }
        composeRule.onNode(hasText("3/3")).assertIsDisplayed()

        // Apply one of the new professional filters on Page 3 and save
        composeRule.onNodeWithTag("enhance_filter_row")
            .performScrollToNode(hasTestTag("filter_chip_clean_paper"))
        composeRule.onNodeWithTag("filter_chip_clean_paper").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("crop_save_document_btn").performClick()

        // Verify we returned directly to DocumentDetail on Page 3 (index 2 -> "3 / 3"), never reset to Page 1!
        composeRule.waitUntil(timeoutMillis = 8_000L) {
            viewModel.currentRoute.value == ScannerSubRoute.DocumentDetail(doc.id) &&
                composeRule.onAllNodes(hasText("3 / 3")).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(2, viewModel.detailPageIndexByDocId.value[doc.id])

        val persistedPage3 = runBlocking(Dispatchers.IO) {
            viewModel.repository.getPagesOnce(doc.id).first { it.id == "p3-page-3" }
        }
        assertEquals(ScannerFilterType.CLEAN_PAPER.id, persistedPage3.filterId)
    }

    @Test
    fun documentDetailAllowsPageSwitchingEvenWhileMarkupModeIsActive() {
        val doc = ScannedDocumentEntity(
            id = "doc-markup-nav",
            title = "Markup Multi Page",
            pageCount = 2,
            totalSizeBytes = 2048L,
            sortOrder = 1000L
        )
        val bmp = createSyntheticDocumentBitmap(200, 280)
        val files = (1..2).map { idx ->
            File(viewModel.repository.docsRootDir, "markup_nav_$idx.jpg").also {
                DocumentImageProcessor.saveBitmapAsJpeg(bmp, it, 90)
            }
        }
        bmp.recycle()

        runBlocking(Dispatchers.IO) {
            viewModel.repository.upsertDocument(doc)
            files.forEachIndexed { idx, file ->
                viewModel.repository.upsertPage(
                    ScannedPageEntity(
                        id = "mk-page-$idx",
                        documentId = doc.id,
                        pageOrder = idx,
                        originalImagePath = file.absolutePath,
                        processedImagePath = file.absolutePath
                    )
                )
            }
        }

        composeRule.setContent {
            DocumentDetailEditorScreen(
                scannerViewModel = viewModel,
                documentId = doc.id,
                isArabic = false,
                onAddMorePages = {},
                onBackToHub = {}
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(hasText("1 / 2")).fetchSemanticsNodes().isNotEmpty()
        }

        // Enable Markup tool inside Doc Editor and verify Next Page button remains visible and functional
        composeRule.onNodeWithTag("doc_markup_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNode(androidx.compose.ui.test.hasContentDescription("Next page")).assertIsDisplayed().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(hasText("2 / 2")).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(1, viewModel.detailPageIndexByDocId.value[doc.id])
    }

    @Test
    fun enhanceAdjustTabExposesAllSevenProMetricsAndNewFilterChips() {
        val bmp = createSyntheticDocumentBitmap(200, 280)
        val file = File(viewModel.repository.docsRootDir, "adjust_pro_test.jpg").also {
            DocumentImageProcessor.saveBitmapAsJpeg(bmp, it, 90)
        }
        bmp.recycle()

        viewModel.batchFrames.value = listOf(
            BatchCapturedFrame(
                id = "adj-frame-1",
                rawImagePath = file.absolutePath,
                processedImagePath = file.absolutePath
            )
        )
        viewModel.activeCropIndex.value = 0

        composeRule.setContent {
            CropAndFilterScreen(
                scannerViewModel = viewModel,
                appendToDocId = null,
                isArabic = false,
                onBackToCamera = {}
            )
        }

        // Enter Enhance phase
        composeRule.onNodeWithTag("crop_apply_next_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("adjust_toggle_btn").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify all new professional filter chips exist
        listOf(
            "filter_chip_deshadow",
            "filter_chip_super_clear",
            "filter_chip_deep_ink",
            "filter_chip_clean_paper"
        ).forEach { tag ->
            composeRule.onNodeWithTag("enhance_filter_row")
                .performScrollToNode(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).assertExists()
        }

        // Open Adjust tab and verify all 7 precision metrics
        composeRule.onNodeWithTag("adjust_toggle_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("adjust_slider").fetchSemanticsNodes().isNotEmpty()
        }

        val metricTags = listOf(
            "adjust_metric_brightness",
            "adjust_metric_contrast",
            "adjust_metric_shadows",
            "adjust_metric_highlights",
            "adjust_metric_sharpness",
            "adjust_metric_saturation",
            "adjust_metric_temperature"
        )
        metricTags.forEach { tag ->
            composeRule.onNodeWithTag("adjust_metrics_row")
                .performScrollToNode(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }

        // Select Shadows, tap right side of slider track, verify frame shadows updated & Reset button restores defaults
        composeRule.onNodeWithTag("adjust_metrics_row")
            .performScrollToNode(hasTestTag("adjust_metric_shadows"))
        composeRule.onNodeWithTag("adjust_metric_shadows").performClick()
        composeRule.onNodeWithTag("adjust_slider").performTouchInput {
            val tapOffset = androidx.compose.ui.geometry.Offset(width * 0.85f, height * 0.5f)
            down(center)
            moveTo(tapOffset)
            up()
        }
        composeRule.waitForIdle()
        assertTrue(viewModel.batchFrames.value.first().shadows > 0f)

        composeRule.onNodeWithTag("adjust_reset_btn").performClick()
        composeRule.waitForIdle()
        val resetFrame = viewModel.batchFrames.value.first()
        assertEquals(0f, resetFrame.shadows, 0.001f)
        assertEquals(0f, resetFrame.highlights, 0.001f)
        assertEquals(1f, resetFrame.saturation, 0.001f)
        assertEquals(0f, resetFrame.temperature, 0.001f)
    }

    @Test
    fun voiceRecorderDialogRendersWithHighContrastOverlayCard() {
        composeRule.setContent {
            FluffyTheme(darkTheme = false) {
                VoiceRecorderDialog(
                    isDark = false,
                    isArabic = false,
                    onDismiss = {},
                    onSaveVoiceNote = { _, _, _ -> }
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("voice_recorder_dialog").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("voice_recorder_dialog").assertIsDisplayed()
        assertNotNull(composeRule.onAllNodes(hasTestTag("voice_recorder_dialog")).fetchSemanticsNodes().firstOrNull())
    }
}
