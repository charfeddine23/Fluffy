package com.charfeddine.fluffy.scanner

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.charfeddine.fluffy.scanner.data.local.ScannerDatabase
import com.charfeddine.fluffy.scanner.data.model.ScannedDocumentEntity
import com.charfeddine.fluffy.scanner.data.model.ScannedPageEntity
import com.charfeddine.fluffy.scanner.domain.ScannerViewModel
import com.charfeddine.fluffy.scanner.ui.screens.DocumentDetailEditorScreen
import com.charfeddine.fluffy.scanner.ui.screens.ScannerHubScreen
import com.charfeddine.fluffy.scanner.utils.DocumentImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class ScannerHubBatchSelectionTest {

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

    private fun insertDoc(id: String, title: String, sortOrder: Long = 0L): ScannedDocumentEntity {
        val doc = ScannedDocumentEntity(
            id = id,
            title = title,
            pageCount = 1,
            totalSizeBytes = 1024L,
            sortOrder = sortOrder
        )
        runBlocking(Dispatchers.IO) {
            viewModel.repository.upsertDocument(doc)
        }
        return doc
    }

    @Test
    fun longPressAndTapSecondCardEnablesBatchDelete() {
        insertDoc("doc-1", "Invoice One", sortOrder = 1000L)
        insertDoc("doc-2", "Receipt Two", sortOrder = 2000L)
        insertDoc("doc-3", "Contract Three", sortOrder = 3000L)

        composeRule.setContent {
            ScannerHubScreen(
                scannerViewModel = viewModel,
                isArabic = false,
                onRequestProtectedAccess = { _, onUnlocked -> onUnlocked() },
                onExitEcosystem = {}
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("doc_card_doc-1").fetchSemanticsNodes().isNotEmpty()
        }

        // Long press doc-1 for 550ms and lift -> enters selection mode with 1 selected doc
        composeRule.onNodeWithTag("doc_card_doc-1").performTouchInput {
            down(center)
        }
        ShadowSystemClock.advanceBy(Duration.ofMillis(560))
        composeRule.mainClock.advanceTimeBy(560L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("doc_card_doc-1").performTouchInput {
            up()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("scanner_contextual_top_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("doc_select_badge_doc-1", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("doc_select_badge_doc-2", useUnmergedTree = true).assertIsDisplayed()

        // Tap doc-2 -> expands into batch multi-selection top bar
        composeRule.onNodeWithTag("doc_card_doc-2").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("scanner_batch_selection_top_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("context_menu_selected_count").assertIsDisplayed()
        composeRule.onNodeWithText("2 selected").assertIsDisplayed()

        // Tap Batch Delete -> moves doc-1 and doc-2 to trash together, leaving doc-3 active
        composeRule.onNodeWithTag("context_menu_batch_delete_btn").performClick()
        composeRule.waitForIdle()
        val deadline = System.currentTimeMillis() + 5_000L
        while (System.currentTimeMillis() < deadline) {
            composeRule.waitForIdle()
            if (viewModel.visibleDocuments.value.size == 1 && viewModel.trashedDocs.value.size == 2) {
                break
            }
            Thread.sleep(50L)
        }
        val dbActive = runBlocking(Dispatchers.IO) { viewModel.repository.getActiveDocumentsOnce() }
        assertTrue(
            "visible=${viewModel.visibleDocuments.value.map { it.id }}, trashed=${viewModel.trashedDocs.value.map { it.id }}, dbActive=${dbActive.map { it.id }}",
            viewModel.visibleDocuments.value.size == 1 && viewModel.trashedDocs.value.size == 2
        )

        assertEquals(listOf("doc-3"), viewModel.visibleDocuments.value.map { it.id })
        assertTrue(viewModel.trashedDocs.value.map { it.id }.containsAll(listOf("doc-1", "doc-2")))
    }

    @Test
    fun documentDetailViewerRendersCleanTopAndBottomControls() {
        val doc = insertDoc("doc-viewer", "Scan 2026-11-25 Long Document Title Example Name", sortOrder = 0L)
        val imgFile = File(viewModel.repository.docsRootDir, "test_page_viewer.jpg")
        val bmp = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
        DocumentImageProcessor.saveBitmapAsJpeg(bmp, imgFile, 90)
        bmp.recycle()

        runBlocking(Dispatchers.IO) {
            viewModel.repository.upsertPage(
                ScannedPageEntity(
                    id = "page-viewer-1",
                    documentId = doc.id,
                    pageOrder = 0,
                    originalImagePath = imgFile.absolutePath,
                    processedImagePath = imgFile.absolutePath
                )
            )
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
            composeRule.onAllNodesWithTag("doc_export_btn").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("doc_export_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("doc_more_actions_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("doc_edit_page_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("doc_markup_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("doc_sign_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("doc_watermark_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("doc_ocr_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("doc_pages_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("doc_add_page_btn").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("1 page"))
                .fetchSemanticsNodes()
                .isEmpty()
        )
    }

    @Test
    fun verticalSwipeSwitchesBetweenScannerDocuments() {
        val doc1 = insertDoc("doc-a", "First Document", sortOrder = 1000L)
        val doc2 = insertDoc("doc-b", "Second Document", sortOrder = 2000L)
        val imgFile1 = File(viewModel.repository.docsRootDir, "test_swipe_1.jpg")
        val imgFile2 = File(viewModel.repository.docsRootDir, "test_swipe_2.jpg")
        val bmp = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
        DocumentImageProcessor.saveBitmapAsJpeg(bmp, imgFile1, 90)
        DocumentImageProcessor.saveBitmapAsJpeg(bmp, imgFile2, 90)
        bmp.recycle()

        runBlocking(Dispatchers.IO) {
            viewModel.repository.upsertPage(
                ScannedPageEntity(
                    id = "page-swipe-1",
                    documentId = doc1.id,
                    pageOrder = 0,
                    originalImagePath = imgFile1.absolutePath,
                    processedImagePath = imgFile1.absolutePath
                )
            )
            viewModel.repository.upsertPage(
                ScannedPageEntity(
                    id = "page-swipe-2",
                    documentId = doc2.id,
                    pageOrder = 0,
                    originalImagePath = imgFile2.absolutePath,
                    processedImagePath = imgFile2.absolutePath
                )
            )
        }
        viewModel.setSortOption(com.charfeddine.fluffy.scanner.data.model.SortOption.CUSTOM)
        viewModel.openDocumentDetail(doc1.id)

        composeRule.setContent {
            com.charfeddine.fluffy.scanner.ui.components.ScannerRootHost(
                isArabic = false,
                onTriggerBiometricPrompt = { _, _, onSuccess, _ -> onSuccess() },
                onSaveQrAsFluffyNote = { _, _ -> },
                onExitScanner = {},
                scannerViewModel = viewModel
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("First Document"))
                .fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Document page"))
                    .fetchSemanticsNodes().isNotEmpty()
        }

        // Swipe up -> switches to Second Document
        composeRule.onNode(androidx.compose.ui.test.hasContentDescription("Document page"))
            .performTouchInput {
                down(center)
                moveBy(androidx.compose.ui.geometry.Offset(0f, -260f))
                up()
            }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            viewModel.currentRoute.value == com.charfeddine.fluffy.scanner.domain.ScannerSubRoute.DocumentDetail(doc2.id)
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("Second Document"))
                .fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Document page"))
                    .fetchSemanticsNodes().isNotEmpty()
        }

        // Swipe down -> switches back to First Document
        composeRule.onNode(androidx.compose.ui.test.hasContentDescription("Document page"))
            .performTouchInput {
                down(center)
                moveBy(androidx.compose.ui.geometry.Offset(0f, 260f))
                up()
            }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            viewModel.currentRoute.value == com.charfeddine.fluffy.scanner.domain.ScannerSubRoute.DocumentDetail(doc1.id)
        }
    }

    @Test
    fun shortVerticalNudgeDoesNotSwitchScannerDocument() {
        val doc1 = insertDoc("doc-short-a", "Doc Alpha", sortOrder = 1000L)
        val doc2 = insertDoc("doc-short-b", "Doc Beta", sortOrder = 2000L)
        val imgFile1 = File(viewModel.repository.docsRootDir, "test_nudge_1.jpg")
        val imgFile2 = File(viewModel.repository.docsRootDir, "test_nudge_2.jpg")
        val bmp = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
        DocumentImageProcessor.saveBitmapAsJpeg(bmp, imgFile1, 90)
        DocumentImageProcessor.saveBitmapAsJpeg(bmp, imgFile2, 90)
        bmp.recycle()

        runBlocking(Dispatchers.IO) {
            viewModel.repository.upsertPage(
                ScannedPageEntity(
                    id = "page-nudge-1",
                    documentId = doc1.id,
                    pageOrder = 0,
                    originalImagePath = imgFile1.absolutePath,
                    processedImagePath = imgFile1.absolutePath
                )
            )
            viewModel.repository.upsertPage(
                ScannedPageEntity(
                    id = "page-nudge-2",
                    documentId = doc2.id,
                    pageOrder = 0,
                    originalImagePath = imgFile2.absolutePath,
                    processedImagePath = imgFile2.absolutePath
                )
            )
        }
        viewModel.setSortOption(com.charfeddine.fluffy.scanner.data.model.SortOption.CUSTOM)
        viewModel.openDocumentDetail(doc1.id)

        composeRule.setContent {
            com.charfeddine.fluffy.scanner.ui.components.ScannerRootHost(
                isArabic = false,
                onTriggerBiometricPrompt = { _, _, onSuccess, _ -> onSuccess() },
                onSaveQrAsFluffyNote = { _, _ -> },
                onExitScanner = {},
                scannerViewModel = viewModel
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("Doc Alpha"))
                .fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Document page"))
                    .fetchSemanticsNodes().isNotEmpty()
        }

        // Short vertical nudge (-120px) stays on Doc Alpha instead of switching documents
        composeRule.onNode(androidx.compose.ui.test.hasContentDescription("Document page"))
            .performTouchInput {
                down(center)
                moveBy(androidx.compose.ui.geometry.Offset(0f, -120f))
                up()
            }
        composeRule.waitForIdle()

        assertEquals(
            com.charfeddine.fluffy.scanner.domain.ScannerSubRoute.DocumentDetail(doc1.id),
            viewModel.currentRoute.value
        )
    }

    @Test
    fun horizontalSwipeSmoothlyNavigatesPagesWithinSameDocument() {
        val doc = ScannedDocumentEntity(
            id = "doc-multipage",
            title = "Multi Page Doc",
            pageCount = 3,
            totalSizeBytes = 2048L,
            sortOrder = 1000L
        )
        val bmp = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
        val files = (1..3).map { idx ->
            File(viewModel.repository.docsRootDir, "multi_page_$idx.jpg").also {
                DocumentImageProcessor.saveBitmapAsJpeg(bmp, it, 90)
            }
        }
        bmp.recycle()

        runBlocking(Dispatchers.IO) {
            viewModel.repository.upsertDocument(doc)
            files.forEachIndexed { idx, file ->
                viewModel.repository.upsertPage(
                    ScannedPageEntity(
                        id = "mp-page-$idx",
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
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("1 / 3"))
                .fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Document page"))
                    .fetchSemanticsNodes().isNotEmpty()
        }

        // Horizontal swipe left -> moves smoothly to page 2 / 3
        composeRule.onNode(androidx.compose.ui.test.hasContentDescription("Document page"))
            .performTouchInput {
                down(center)
                moveBy(androidx.compose.ui.geometry.Offset(-220f, 0f))
                up()
            }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("2 / 3"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Next page button -> moves smoothly to page 3 / 3
        composeRule.onNode(androidx.compose.ui.test.hasContentDescription("Next page"))
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("3 / 3"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun enhancePhaseKeepsModeWhileNavigatingPagesWithoutReturningToCrop() {
        val bmp = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
        val file1 = File(viewModel.repository.docsRootDir, "enhance_p1.jpg").also {
            DocumentImageProcessor.saveBitmapAsJpeg(bmp, it, 90)
        }
        val file2 = File(viewModel.repository.docsRootDir, "enhance_p2.jpg").also {
            DocumentImageProcessor.saveBitmapAsJpeg(bmp, it, 90)
        }
        bmp.recycle()

        viewModel.batchFrames.value = listOf(
            com.charfeddine.fluffy.scanner.data.model.BatchCapturedFrame(
                id = "enh-f1",
                rawImagePath = file1.absolutePath
            ),
            com.charfeddine.fluffy.scanner.data.model.BatchCapturedFrame(
                id = "enh-f2",
                rawImagePath = file2.absolutePath
            )
        )
        viewModel.activeCropIndex.value = 0

        composeRule.setContent {
            com.charfeddine.fluffy.scanner.ui.screens.CropAndFilterScreen(
                scannerViewModel = viewModel,
                appendToDocId = null,
                isArabic = false,
                onBackToCamera = {}
            )
        }

        // Move to Enhance phase
        composeRule.onNode(androidx.compose.ui.test.hasTestTag("crop_apply_next_btn")).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasTestTag("crop_save_document_btn"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate to next page directly inside Enhance via Next button
        composeRule.onNode(androidx.compose.ui.test.hasTestTag("batch_next_page_btn")).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            viewModel.activeCropIndex.value == 1
        }
        composeRule.waitForIdle()

        // Confirm we remained in Enhance phase without reverting to Crop
        composeRule.onNode(androidx.compose.ui.test.hasTestTag("crop_save_document_btn")).assertExists()
        composeRule.onNode(androidx.compose.ui.test.hasText("2/2")).assertExists()

        // Navigate back to first page directly via Prev button and still remain in Enhance
        composeRule.onNode(androidx.compose.ui.test.hasTestTag("batch_prev_page_btn")).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            viewModel.activeCropIndex.value == 0
        }
        composeRule.onNode(androidx.compose.ui.test.hasTestTag("crop_save_document_btn")).assertExists()
    }
}
