package com.charfeddine.fluffy.scanner

import android.app.Application
import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.test.core.app.ApplicationProvider
import com.charfeddine.fluffy.scanner.data.local.ScannerDatabase
import com.charfeddine.fluffy.scanner.data.model.IdCardPrintMode
import com.charfeddine.fluffy.scanner.data.model.PdfPageSizePreset
import com.charfeddine.fluffy.scanner.data.model.ScanMode
import com.charfeddine.fluffy.scanner.data.model.ScannerFolderEntity
import com.charfeddine.fluffy.scanner.data.model.SortOption
import com.charfeddine.fluffy.scanner.domain.ScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScannerPersistenceTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("fluffy_scanner_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        val db = ScannerDatabase.getDatabase(app)
        runBlocking(Dispatchers.IO) {
            db.clearAllTables()
        }
    }

    @Test
    fun scannerPreferencesAndViewModePersistAcrossRestarts() {
        val vm1 = ScannerViewModel(app)
        val folder = ScannerFolderEntity(id = "folder-work", name = "Work")
        runBlocking(Dispatchers.IO) {
            vm1.repository.upsertFolder(folder)
        }

        assertTrue(vm1.isGridView.value)
        vm1.toggleGridView()
        vm1.setSortOption(SortOption.NAME_ASC)
        vm1.setCurrentFolderId("folder-work")
        vm1.setSelectedTagFilter("Invoices")
        vm1.setOnlyStarredFilter(true)
        vm1.setActiveScanMode(ScanMode.ID_CARD)
        vm1.toggleIdCardPrintMode()
        vm1.setCameraShowGrid(true)
        vm1.setCameraAutoCapture(true)
        vm1.setCameraFlashMode(ImageCapture.FLASH_MODE_ON)
        vm1.setPenColorArgb(0xFF10B981L)
        vm1.setRedactionBlack(false)
        vm1.setPdfExportAsImages(true)
        vm1.setPdfExportPageSize(PdfPageSizePreset.LETTER)
        vm1.setPdfExportSearchableOcr(true)

        // Simulate full app process restart with a fresh ViewModel reading from disk
        val vm2 = ScannerViewModel(app)
        assertFalse(vm2.isGridView.value)
        assertEquals(SortOption.NAME_ASC, vm2.sortOption.value)
        assertEquals("folder-work", vm2.currentFolderId.value)
        assertEquals("Invoices", vm2.selectedTagFilter.value)
        assertTrue(vm2.onlyStarredFilter.value)
        assertEquals(ScanMode.ID_CARD, vm2.activeScanMode.value)
        assertEquals(IdCardPrintMode.RECTO_VERSO, vm2.idCardPrintMode.value)
        assertTrue(vm2.cameraShowGrid.value)
        assertTrue(vm2.cameraAutoCapture.value)
        assertEquals(ImageCapture.FLASH_MODE_ON, vm2.cameraFlashMode.value)
        assertEquals(0xFF10B981L, vm2.penColorArgb.value)
        assertFalse(vm2.redactionBlack.value)
        assertTrue(vm2.pdfExportAsImages.value)
        assertEquals(PdfPageSizePreset.LETTER, vm2.pdfExportPageSize.value)
        assertTrue(vm2.pdfExportSearchableOcr.value)

        // Verify clearing filters & switching back to grid also persists
        vm2.toggleGridView()
        vm2.clearFilters()
        val vm3 = ScannerViewModel(app)
        assertTrue(vm3.isGridView.value)
        assertNull(vm3.selectedTagFilter.value)
        assertFalse(vm3.onlyStarredFilter.value)
    }

    @Test
    fun lockedOrDeletedFolderResetsOnRestart() {
        val vm1 = ScannerViewModel(app)
        val lockedFolder = ScannerFolderEntity(id = "folder-locked", name = "Secret", isLocked = true)
        runBlocking(Dispatchers.IO) {
            vm1.repository.upsertFolder(lockedFolder)
        }
        vm1.setCurrentFolderId("folder-locked")

        val vm2 = ScannerViewModel(app)
        val deadline = System.currentTimeMillis() + 5_000L
        while (vm2.currentFolderId.value != null && System.currentTimeMillis() < deadline) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            Thread.sleep(25L)
        }
        assertNull(vm2.currentFolderId.value)
    }
}
