package com.charfeddine.fluffy.scanner

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.charfeddine.fluffy.scanner.data.local.PdfReadingProgressStore
import com.charfeddine.fluffy.scanner.data.local.ScannerDatabase
import com.charfeddine.fluffy.scanner.data.model.DevicePdfEntry
import com.charfeddine.fluffy.scanner.data.model.PdfReaderThemeMode
import com.charfeddine.fluffy.scanner.data.model.PdfScrollMode
import com.charfeddine.fluffy.scanner.data.model.PdfSourceKind
import com.charfeddine.fluffy.scanner.data.model.ScannedDocumentEntity
import com.charfeddine.fluffy.scanner.domain.ScannerSubRoute
import com.charfeddine.fluffy.scanner.domain.ScannerViewModel
import com.charfeddine.fluffy.scanner.ui.screens.PdfReaderScreen
import com.charfeddine.fluffy.scanner.ui.screens.ScannerHubScreen
import com.charfeddine.fluffy.scanner.utils.DevicePdfDiscovery
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
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfReaderAndToolsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var viewModel: ScannerViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("fluffy_pdf_reader_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        val db = ScannerDatabase.getDatabase(app)
        runBlocking(Dispatchers.IO) {
            db.clearAllTables()
        }
        viewModel = ScannerViewModel(app)
    }

    private fun createSamplePdfFile(name: String, pageCount: Int): File {
        val dir = File(app.filesDir, "test_pdfs").apply { mkdirs() }
        val file = File(dir, name)
        val doc = PdfDocument()
        try {
            val bgPaint = Paint().apply { color = Color.rgb(250, 248, 240) }
            val textPaint = Paint().apply {
                color = Color.rgb(28, 32, 40)
                textSize = 26f
                isAntiAlias = true
            }
            for (i in 0 until pageCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(320, 460, i + 1).create()
                val page = doc.startPage(pageInfo)
                page.canvas.drawRect(0f, 0f, 320f, 460f, bgPaint)
                page.canvas.drawText("Fluffy Reader Page ${i + 1}", 32f, 120f, textPaint)
                doc.finishPage(page)
            }
            FileOutputStream(file).use { out ->
                doc.writeTo(out)
            }
        } finally {
            doc.close()
        }
        return file
    }

    @Test
    fun scannerHubReplacesOpenPdfWithReadPdfAndMovesEditPdfIntoPdfTools() {
        composeRule.setContent {
            FluffyTheme {
                ScannerHubScreen(
                    scannerViewModel = viewModel,
                    isArabic = false,
                    onRequestProtectedAccess = { _, onUnlocked -> onUnlocked() },
                    onExitEcosystem = {}
                )
            }
        }

        composeRule.waitUntil(5_000L) {
            composeRule.onAllNodesWithTag("hub_read_pdf_btn").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("hub_read_pdf_btn").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Open PDF").fetchSemanticsNodes().size)

        // Clicking Read PDF routes to PdfLibrary
        composeRule.onNodeWithTag("hub_read_pdf_btn").performClick()
        composeRule.waitForIdle()
        assertTrue(viewModel.currentRoute.value is ScannerSubRoute.PdfLibrary)

        // Open PDF Tools sheet and verify "Edit PDF" is inside PDF Tools
        composeRule.onNodeWithTag("hub_pdf_tools_btn").performClick()
        composeRule.waitUntil(5_000L) {
            composeRule.onAllNodesWithTag("pdf_tool_tab_4").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("pdf_tool_tab_4").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Edit PDF").assertIsDisplayed()

        composeRule.onNodeWithTag("pdf_tool_tab_4").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Select PDF(s) to Edit").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("pdf_tool_edit_action").assertIsDisplayed()
    }

    @Test
    fun pdfReadingProgressStorePersistsPageBookmarksAndViewModesAcrossInstances() {
        val store = PdfReadingProgressStore(app)
        val entry = DevicePdfEntry(
            key = "local:/tmp/manual.pdf",
            uriString = "file:///tmp/manual.pdf",
            title = "Architecture Guide",
            fileName = "manual.pdf",
            folderLabel = "Downloads",
            sizeBytes = 20_480L,
            lastModifiedMs = 1_700_000_000_000L,
            pageCount = 12,
            sourceKind = PdfSourceKind.LOCAL_FILE
        )

        store.savePageProgress(entry, pageIndex = 6, totalPages = 12)
        store.toggleBookmark(entry, pageIndex = 1, totalPages = 12)
        store.toggleBookmark(entry, pageIndex = 6, totalPages = 12)
        store.updateReaderSettings(
            entry = entry,
            themeMode = PdfReaderThemeMode.SEPIA_WARM,
            scrollMode = PdfScrollMode.HORIZONTAL_PAGER,
            rotationDeg = 90,
            brightnessLevel = 0.72f
        )

        val reloadedStore = PdfReadingProgressStore(app)
        val restored = reloadedStore.getProgress(entry.key)
        assertNotNull(restored)
        assertEquals(6, restored!!.lastPageIndex)
        assertEquals(12, restored.totalPages)
        assertEquals(setOf(1, 6), restored.bookmarkedPages)
        assertEquals(PdfReaderThemeMode.SEPIA_WARM, restored.themeMode)
        assertEquals(PdfScrollMode.HORIZONTAL_PAGER, restored.scrollMode)
        assertEquals(90, restored.rotationDeg)
        assertEquals(0.72f, restored.brightnessLevel, 0.01f)
    }

    @Test
    fun pdfReaderResumesAtExactSavedPageSupportsBookmarksViewModesAndFocusMode() {
        val sampleFile = createSamplePdfFile("resume_book.pdf", pageCount = 4)
        val uriString = Uri.fromFile(sampleFile).toString()
        val entry = DevicePdfEntry(
            key = "local:${sampleFile.absolutePath}",
            uriString = uriString,
            title = "Resume Book",
            fileName = sampleFile.name,
            folderLabel = "Device",
            sizeBytes = sampleFile.length(),
            lastModifiedMs = sampleFile.lastModified(),
            pageCount = 4,
            sourceKind = PdfSourceKind.LOCAL_FILE
        )

        val progressStore = PdfReadingProgressStore(app)
        // Save progress at Page 3 (0-based index 2)
        progressStore.savePageProgress(entry, pageIndex = 2, totalPages = 4)

        composeRule.setContent {
            FluffyTheme {
                PdfReaderScreen(
                    pdfEntry = entry,
                    scannerViewModel = viewModel,
                    progressStore = progressStore,
                    isArabic = false,
                    onBackToLibrary = {}
                )
            }
        }

        // Wait until PDF finishes loading and resumes directly on Page 3 of 4
        composeRule.waitUntil(8_000L) {
            composeRule.onAllNodes(hasText("Page 3 of 4", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("pdf_reader_top_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("pdf_reader_bottom_bar").assertIsDisplayed()
        composeRule.onNodeWithText("3 / 4").assertIsDisplayed()

        // Toggle bookmark on Page 3
        composeRule.onNodeWithTag("pdf_reader_bookmark_btn").performClick()
        composeRule.waitForIdle()
        assertTrue(2 in (progressStore.getProgress(entry.key)?.bookmarkedPages ?: emptySet()))

        // Open Display & Reading Comfort sheet -> choose Sepia & Horizontal Pager & Enter Focus Mode
        composeRule.onNodeWithTag("pdf_reader_view_btn").performClick()
        composeRule.waitUntil(5_000L) {
            composeRule.onAllNodesWithTag("pdf_theme_sepia").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("pdf_theme_sepia").performClick()
        composeRule.onNodeWithTag("pdf_scroll_horizontal").performClick()
        composeRule.waitForIdle()

        val updatedSettings = progressStore.getProgress(entry.key)!!
        assertEquals(PdfReaderThemeMode.SEPIA_WARM, updatedSettings.themeMode)
        assertEquals(PdfScrollMode.HORIZONTAL_PAGER, updatedSettings.scrollMode)
        composeRule.onNodeWithTag("pdf_reader_horizontal_pager").assertIsDisplayed()

        // Enter Focus Mode from the Display Comfort sheet button ("Enter Focus Mode")
        composeRule.onNodeWithText("Enter Focus Mode").performClick()
        composeRule.waitUntil(5_000L) {
            composeRule.onAllNodesWithTag("pdf_reader_top_bar").fetchSemanticsNodes().isEmpty() &&
                composeRule.onAllNodesWithTag("pdf_reader_bottom_bar").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("pdf_reader_top_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("pdf_reader_bottom_bar").assertDoesNotExist()

        // Single tap on reading canvas restores controls cleanly
        composeRule.onNodeWithTag("pdf_reader_root").performClick()
        composeRule.waitUntil(5_000L) {
            composeRule.onAllNodesWithTag("pdf_reader_top_bar").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag("pdf_reader_bottom_bar").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("pdf_reader_top_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("pdf_reader_bottom_bar").assertIsDisplayed()
    }

    @Test
    fun devicePdfDiscoveryFindsUnlockedPdfsAndComputesAdaptiveBackdropColor() {
        val docsDir = File(app.filesDir, "scanner_docs").apply { mkdirs() }
        val unlockedPdf = createSamplePdfFile("temp_unlocked.pdf", 2)
            .copyTo(File(docsDir, "unlocked_report.pdf"), overwrite = true)
        val lockedPdf = createSamplePdfFile("temp_locked.pdf", 1)
            .copyTo(File(docsDir, "vault_secret.pdf"), overwrite = true)

        val lockedDoc = ScannedDocumentEntity(
            id = "locked-doc-1",
            title = "Vault Secret",
            pdfPath = lockedPdf.absolutePath,
            isLocked = true
        )
        val store = PdfReadingProgressStore(app)
        val found = runBlocking {
            DevicePdfDiscovery.discoverAllDevicePdfs(
                context = app,
                progressStore = store,
                scannerDocs = listOf(lockedDoc)
            )
        }

        val paths = found.map { it.uriString }
        assertTrue(paths.any { it.contains(unlockedPdf.name) })
        assertFalse(paths.any { it.contains(lockedPdf.name) })

        val warmPaper = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).drawColor(Color.rgb(245, 236, 215))
        }
        val lightBackdrop = DevicePdfDiscovery.sampleAdaptiveBackdropColor(warmPaper, preferDarkEnvironment = false)
        val darkBackdrop = DevicePdfDiscovery.sampleAdaptiveBackdropColor(warmPaper, preferDarkEnvironment = true)
        assertTrue(lightBackdrop.red > 0.7f && lightBackdrop.green > 0.65f)
        assertTrue(darkBackdrop.red < 0.25f && darkBackdrop.blue < 0.30f)
    }
}
