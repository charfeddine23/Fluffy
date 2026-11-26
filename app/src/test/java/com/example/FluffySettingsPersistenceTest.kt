package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FabMenuStyle
import com.example.data.model.NotesViewMode
import com.example.data.storage.FluffySettingsSnapshot
import com.example.data.storage.LocalDeviceStorageManager
import com.example.ui.theme.AppFontCatalog
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.AppThemeRegistry
import com.example.viewmodel.FluffyViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FluffySettingsPersistenceTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("fluffy_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        LocalDeviceStorageManager.saveSettingsSnapshot(
            app,
            FluffySettingsSnapshot(
                isDarkMode = true,
                isArabic = true,
                isGridView = true,
                isBiometricEnabled = false,
                selectedCategory = "ALL"
            )
        )
    }

    @Test
    fun allAppSettingsPersistAcrossViewModelRestart() {
        val vm1 = FluffyViewModel(app)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        vm1.toggleDarkMode() // false
        val lightPreset = AppThemeRegistry.allThemes.firstOrNull { !it.isDark && it.id != AppThemeRegistry.defaultLight.id }
            ?: AppThemeRegistry.defaultLight
        vm1.selectTheme(lightPreset)
        vm1.setAppLanguage(AppLanguage.ENGLISH)
        vm1.setViewMode(NotesViewMode.MAGAZINE)
        vm1.setVaultViewMode(NotesViewMode.LIST)
        vm1.setColoredBordersEnabled(false)
        vm1.setFabMenuStyle(FabMenuStyle.ORBITAL_ARC)
        vm1.setCustomFabMenuEnabled(false) // switches to ORIGINAL while keeping lastCustomFabMenuStyle
        vm1.selectFont(AppFontCatalog.allFonts.first { it.id == "serif" })
        vm1.selectCategory("Work")
        vm1.setBiometricEnabled(true)
        vm1.setScreenshotProtectionEnabled(true)
        vm1.setVaultLockEnabled(true)
        vm1.setVaultPasskey("4321")
        vm1.updateProfileName("Amine", "Charfeddine")
        vm1.selectPresetAvatar("fox")
        vm1.setEditorFontSizeSp(22f)
        vm1.setEditorTextAlign("center")
        vm1.enterGuestMode()

        // Allow IO coroutines to flush snapshot
        Thread.sleep(300L)

        val vm2 = FluffyViewModel(app)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertFalse(vm2.isDarkMode.value)
        assertEquals(lightPreset.id, vm2.selectedThemeId.value)
        assertEquals(AppLanguage.ENGLISH, vm2.appLanguage.value)
        assertFalse(vm2.isArabic.value)
        assertEquals(NotesViewMode.MAGAZINE, vm2.viewMode.value)
        assertEquals(NotesViewMode.LIST, vm2.vaultViewMode.value)
        assertFalse(vm2.isColoredBordersEnabled.value)
        assertEquals(FabMenuStyle.ORIGINAL, vm2.fabMenuStyle.value)
        vm2.setCustomFabMenuEnabled(true)
        assertEquals(FabMenuStyle.ORBITAL_ARC, vm2.fabMenuStyle.value)
        assertEquals("serif", vm2.selectedFontId.value)
        assertEquals("Work", vm2.selectedCategory.value)
        assertTrue(vm2.isBiometricEnabled.value)
        assertTrue(vm2.isAppLocked.value)
        assertTrue(vm2.isScreenshotProtectionEnabled.value)
        assertTrue(vm2.isVaultLockEnabled.value)
        assertTrue(vm2.hasVaultPasskey.value)
        assertTrue(vm2.verifyVaultPasskey("4321"))
        assertEquals("Amine", vm2.profileFirstName.value)
        assertEquals("Charfeddine", vm2.profileLastName.value)
        assertEquals("preset:fox", vm2.profileAvatarChoice.value)
        assertEquals(22f, vm2.editorFontSizeSp.value, 0.01f)
        assertEquals("center", vm2.editorTextAlign.value)
        assertTrue(vm2.isGuestMode.value)
    }

    @Test
    fun clearedPreferencesRecoverFromLiteralDiskSnapshotWithoutOverwriting() {
        val snapshot = FluffySettingsSnapshot(
            isDarkMode = false,
            isArabic = false,
            isGridView = true,
            isBiometricEnabled = true,
            selectedCategory = "Study",
            selectedThemeId = AppThemeRegistry.defaultLight.id,
            appLanguageCode = AppLanguage.FRENCH.code,
            nonScreenshotEnabled = true,
            coloredContainerBorders = false,
            notesViewMode = NotesViewMode.GALLERY.id,
            fabMenuStyle = FabMenuStyle.DIAMOND_MOSAIC.id,
            selectedFontId = "monospace",
            vaultViewMode = NotesViewMode.MAGAZINE.id,
            lastCustomFabMenuStyle = FabMenuStyle.DIAMOND_MOSAIC.id,
            isVaultLockEnabled = true,
            editorFontSizeSp = 24f,
            editorTextAlign = "end",
            useGoogleAccountName = false,
            profileFirstName = "Nadia",
            profileLastName = "K",
            profileAvatarChoice = "preset:owl"
        )
        LocalDeviceStorageManager.saveSettingsSnapshot(app, snapshot)
        app.getSharedPreferences("fluffy_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        val vm = FluffyViewModel(app)
        val deadline = System.currentTimeMillis() + 5_000L
        while (System.currentTimeMillis() < deadline) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            if (vm.selectedFontId.value == "monospace") break
            Thread.sleep(50L)
        }

        assertFalse(vm.isDarkMode.value)
        assertEquals(AppLanguage.FRENCH, vm.appLanguage.value)
        assertEquals(NotesViewMode.GALLERY, vm.viewMode.value)
        assertEquals(NotesViewMode.MAGAZINE, vm.vaultViewMode.value)
        assertFalse(vm.isColoredBordersEnabled.value)
        assertEquals(FabMenuStyle.DIAMOND_MOSAIC, vm.fabMenuStyle.value)
        assertEquals("monospace", vm.selectedFontId.value)
        assertEquals("Study", vm.selectedCategory.value)
        assertTrue(vm.isBiometricEnabled.value)
        assertTrue(vm.isAppLocked.value)
        assertTrue(vm.isScreenshotProtectionEnabled.value)
        assertTrue(vm.isVaultLockEnabled.value)
        assertEquals(24f, vm.editorFontSizeSp.value, 0.01f)
        assertEquals("end", vm.editorTextAlign.value)
        assertEquals("Nadia", vm.profileFirstName.value)
        assertEquals("K", vm.profileLastName.value)
        assertEquals("preset:owl", vm.profileAvatarChoice.value)
    }
}
