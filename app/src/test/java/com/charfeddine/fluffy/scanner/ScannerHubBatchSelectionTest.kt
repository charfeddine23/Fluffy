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
    }
}
