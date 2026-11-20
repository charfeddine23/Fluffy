package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.BiometricAuthManager
import com.example.auth.BiometricAvailability
import com.example.data.model.NoteEntity
import com.example.ui.components.FirebaseAuthDialog
import com.example.ui.components.PremiumUpgradeDialog
import com.example.ui.components.UpdateAvailableDialog
import com.example.ui.screens.AccountProfileScreen
import com.example.ui.screens.ArchiveScreen
import com.example.ui.screens.AuthWelcomeScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.FluffyHomeScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.ThemesScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.AppThemeRegistry
import com.example.ui.theme.FluffyTheme
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.translateText
import com.example.update.AppUpdateManager
import com.example.viewmodel.FluffyViewModel
import com.charfeddine.fluffy.scanner.ui.components.ScannerRootHost

private sealed interface FluffyScreenState {
    data object Welcome : FluffyScreenState
    data object Home : FluffyScreenState
    data object Themes : FluffyScreenState
    data object Trash : FluffyScreenState
    data object Archive : FluffyScreenState
    data object Account : FluffyScreenState
    data object ScannerHub : FluffyScreenState
    data class Editor(val note: NoteEntity) : FluffyScreenState
    data object Vault : FluffyScreenState
}

class MainActivity : FragmentActivity() {

    private lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricAuthManager = BiometricAuthManager(this)
        enableEdgeToEdge()

        setContent {
            val viewModel: FluffyViewModel = viewModel()
            val isDark by viewModel.isDarkMode.collectAsState()
            val appLanguage by viewModel.appLanguage.collectAsState()
            val isArabic by viewModel.isArabic.collectAsState()
            val selectedThemeId by viewModel.selectedThemeId.collectAsState()
            val isColoredBordersEnabled by viewModel.isColoredBordersEnabled.collectAsState()
            val isScreenshotProtectionEnabled by viewModel.isScreenshotProtectionEnabled.collectAsState()
            val isVaultLockEnabled by viewModel.isVaultLockEnabled.collectAsState()
            val hasVaultPasskey by viewModel.hasVaultPasskey.collectAsState()
            val isVaultUnlockedForSession by viewModel.isVaultUnlockedForSession.collectAsState()
            val vaultSessionId by viewModel.vaultSessionId.collectAsState()
            val isThemesScreenOpen by viewModel.isThemesScreenOpen.collectAsState()
            val isTrashScreenOpen by viewModel.isTrashScreenOpen.collectAsState()
            val isArchiveScreenOpen by viewModel.isArchiveScreenOpen.collectAsState()
            val isAccountScreenOpen by viewModel.isAccountScreenOpen.collectAsState()
            val isScannerHubOpen by viewModel.isScannerHubOpen.collectAsState()
            val trashedNotes by viewModel.trashedNotes.collectAsState()
            val archivedNotes by viewModel.archivedNotes.collectAsState()
            val isAppLocked by viewModel.isAppLocked.collectAsState()
            val editingNote by viewModel.editingNote.collectAsState()
            val isCreatingNew by viewModel.isCreatingNew.collectAsState()
            val isVaultOpen by viewModel.isVaultOpen.collectAsState()
            val vaultItems by viewModel.vaultItems.collectAsState()
            val vaultViewMode by viewModel.vaultViewMode.collectAsState()
            val vaultNoteMetaMap by viewModel.vaultNoteMetaMap.collectAsState()
            val updateState by viewModel.updateState.collectAsState()
            val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
            val currentUser by viewModel.currentUser.collectAsState()
            val isGuestMode by viewModel.isGuestMode.collectAsState()
            val isAuthBusy by viewModel.isAuthBusy.collectAsState()
            val signInErrorMessage by viewModel.authErrorMessage.collectAsState()
            val signInInfoMessage by viewModel.authInfoMessage.collectAsState()
            val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsState()
            val showAuthDialog by viewModel.showAuthDialog.collectAsState()
            val showPremiumUpgradeDialog by viewModel.showPremiumUpgradeDialog.collectAsState()
            val isPremium by viewModel.isPremium.collectAsState()
            val profileFirstName by viewModel.profileFirstName.collectAsState()
            val profileLastName by viewModel.profileLastName.collectAsState()
            val profileAvatarChoice by viewModel.profileAvatarChoice.collectAsState()
            val useGoogleAccountName by viewModel.useGoogleAccountName.collectAsState()
            val snackbarMessage by viewModel.snackbarMessage.collectAsState()

            val activeThemePreset = remember(selectedThemeId, isDark) {
                AppThemeRegistry.resolve(selectedThemeId, isDark)
            }

            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(isScreenshotProtectionEnabled) {
                if (isScreenshotProtectionEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            LaunchedEffect(isVaultOpen) {
                if (!isVaultOpen) {
                    biometricAuthManager.cancelPrompt(tag = "vault")
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.retryInstallAfterPermissionIfReady()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            var authErrorMessage by remember { mutableStateOf<String?>(null) }

            val layoutDirection = if (appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                LocalAppLanguage provides appLanguage
            ) {
                FluffyTheme(
                    darkTheme = isDark,
                    themePreset = activeThemePreset,
                    coloredContainerBorders = isColoredBordersEnabled
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        if (isAppLocked) {
                            BiometricLockScreen(
                                isDark = isDark,
                                isArabic = isArabic,
                                onTriggerAuth = {
                                    authErrorMessage = null
                                    val availability = biometricAuthManager.checkAvailability()
                                    if (availability == BiometricAvailability.NO_HARDWARE) {
                                        // Fallback if no biometric hardware
                                        viewModel.unlockApp()
                                    } else {
                                        biometricAuthManager.promptBiometricAuth(
                                            title = if (isArabic) "حماية ملاحظات Fluffy" else "Fluffy Notes Protection",
                                            subtitle = if (isArabic) "قم بتأكيد بصمة الإصبع أو رمز قفل الجهاز" else "Confirm with biometrics or device PIN",
                                            onSuccess = {
                                                authErrorMessage = null
                                                viewModel.unlockApp()
                                            },
                                            onError = { err ->
                                                authErrorMessage = err
                                            }
                                        )
                                    }
                                },
                                errorMessage = authErrorMessage
                            )
                        } else {
                            val activeScreen = when {
                                currentUser == null && !isGuestMode -> FluffyScreenState.Welcome
                                isVaultOpen -> FluffyScreenState.Vault
                                isScannerHubOpen -> FluffyScreenState.ScannerHub
                                editingNote != null -> FluffyScreenState.Editor(editingNote!!)
                                isAccountScreenOpen -> FluffyScreenState.Account
                                isTrashScreenOpen -> FluffyScreenState.Trash
                                isArchiveScreenOpen -> FluffyScreenState.Archive
                                isThemesScreenOpen -> FluffyScreenState.Themes
                                else -> FluffyScreenState.Home
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                AnimatedContent(
                                    targetState = activeScreen,
                                    contentKey = { screen ->
                                        when (screen) {
                                            FluffyScreenState.Welcome -> "welcome"
                                            FluffyScreenState.Home -> "home"
                                            FluffyScreenState.Themes -> "themes"
                                            FluffyScreenState.Trash -> "trash"
                                            FluffyScreenState.Archive -> "archive"
                                            FluffyScreenState.Account -> "account"
                                            FluffyScreenState.ScannerHub -> "scanner_hub"
                                            FluffyScreenState.Vault -> "vault"
                                            is FluffyScreenState.Editor -> "editor"
                                        }
                                    },
                                    transitionSpec = {
                                        when {
                                            targetState is FluffyScreenState.Welcome || initialState is FluffyScreenState.Welcome -> {
                                                fadeIn().togetherWith(fadeOut())
                                            }
                                            targetState is FluffyScreenState.Vault -> {
                                                (slideInVertically { -it / 2 } + fadeIn())
                                                    .togetherWith(fadeOut())
                                            }
                                            initialState is FluffyScreenState.Vault -> {
                                                fadeIn()
                                                    .togetherWith(slideOutVertically { -it / 2 } + fadeOut())
                                            }
                                            targetState is FluffyScreenState.Editor ||
                                                targetState is FluffyScreenState.Themes ||
                                                targetState is FluffyScreenState.Trash ||
                                                targetState is FluffyScreenState.Archive ||
                                                targetState is FluffyScreenState.Account ||
                                                targetState is FluffyScreenState.ScannerHub -> {
                                                (slideInHorizontally { width -> if (appLanguage.isRtl) -width else width } + fadeIn())
                                                    .togetherWith(slideOutHorizontally { width -> if (appLanguage.isRtl) width else -width } + fadeOut())
                                            }
                                            else -> {
                                                (slideInHorizontally { width -> if (appLanguage.isRtl) width else -width } + fadeIn())
                                                    .togetherWith(slideOutHorizontally { width -> if (appLanguage.isRtl) -width else width } + fadeOut())
                                            }
                                        }
                                    },
                                    label = "screen_transition"
                                ) { screen ->
                                    when (screen) {
                                        FluffyScreenState.Welcome -> {
                                            AuthWelcomeScreen(
                                                isDark = isDark,
                                                isArabic = isArabic,
                                                isAuthBusy = isAuthBusy,
                                                authErrorMessage = signInErrorMessage,
                                                authInfoMessage = signInInfoMessage,
                                                onToggleTheme = { viewModel.toggleDarkMode() },
                                                onToggleLanguage = { viewModel.toggleLanguage() },
                                                onGoogleIdToken = { idToken ->
                                                    viewModel.signInWithGoogleIdToken(idToken)
                                                },
                                                onEmailSignIn = { email, password ->
                                                    viewModel.signInWithEmailPassword(email, password)
                                                },
                                                onEmailSignUp = { username, email, password, confirm ->
                                                    viewModel.signUpWithEmailPassword(username, email, password, confirm)
                                                },
                                                onResetPassword = { email ->
                                                    viewModel.sendPasswordResetEmail(email)
                                                },
                                                onClearAuthMessages = {
                                                    viewModel.clearAuthMessages()
                                                },
                                                onEnterGuestMode = { guestName ->
                                                    viewModel.enterGuestMode(guestName)
                                                }
                                            )
                                        }
                                        FluffyScreenState.Themes -> {
                                            ThemesScreen(
                                                selectedThemeId = selectedThemeId,
                                                isDark = isDark,
                                                isArabic = isArabic,
                                                onSelectTheme = { preset ->
                                                    viewModel.selectTheme(preset)
                                                },
                                                onBack = { viewModel.closeThemesScreen() }
                                            )
                                        }
                                        FluffyScreenState.Trash -> {
                                            TrashScreen(
                                                trashedNotes = trashedNotes,
                                                isDark = isDark,
                                                isArabic = isArabic,
                                                onRestoreNote = { note -> viewModel.restoreFromTrash(note) },
                                                onDeletePermanently = { note -> viewModel.deletePermanentlyFromTrash(note) },
                                                onEmptyTrash = { viewModel.emptyTrash() },
                                                onBack = { viewModel.closeTrashScreen() }
                                            )
                                        }
                                        FluffyScreenState.Archive -> {
                                            ArchiveScreen(
                                                archivedNotes = archivedNotes,
                                                isDark = isDark,
                                                isArabic = isArabic,
                                                onOpenNote = { note -> viewModel.openExistingNote(note) },
                                                onUnarchiveNote = { note -> viewModel.toggleArchive(note) },
                                                onMoveToVault = { note -> viewModel.moveNoteToVault(note) },
                                                onMoveToTrash = { note -> viewModel.deleteNoteDirectly(note) },
                                                onBack = { viewModel.closeArchiveScreen() }
                                            )
                                        }
                                        FluffyScreenState.Account -> {
                                            AccountProfileScreen(
                                                currentUser = currentUser,
                                                isGuestMode = isGuestMode,
                                                firstName = profileFirstName,
                                                lastName = profileLastName,
                                                avatarChoice = profileAvatarChoice,
                                                useGoogleAccountName = useGoogleAccountName,
                                                cloudSyncStatus = cloudSyncStatus,
                                                isDark = isDark,
                                                isArabic = isArabic,
                                                isPremium = isPremium,
                                                onOpenPremiumUpgrade = { viewModel.openPremiumUpgradeDialog() },
                                                onUpdateProfileName = { first, last ->
                                                    viewModel.updateProfileName(first, last)
                                                },
                                                onToggleUseGoogleAccountName = { enabled ->
                                                    viewModel.setUseGoogleAccountName(enabled)
                                                },
                                                onSelectCustomPhoto = { uri ->
                                                    viewModel.selectCustomProfilePhoto(uri)
                                                },
                                                onSelectPresetAvatar = { presetId ->
                                                    viewModel.selectPresetAvatar(presetId)
                                                },
                                                onUseGooglePhoto = {
                                                    viewModel.useGoogleProfilePhoto()
                                                },
                                                onUseInitialsAvatar = {
                                                    viewModel.resetProfileAvatarToInitials()
                                                },
                                                onOpenAuthDialog = { viewModel.openAuthDialog() },
                                                onSyncCloudNow = { viewModel.syncCloudNow() },
                                                onSignOutFirebase = {
                                                    viewModel.closeAccountScreen()
                                                    viewModel.signOutFirebase()
                                                },
                                                onExportLocalBackup = { uri ->
                                                    viewModel.exportFullBackupToPhoneUri(uri)
                                                },
                                                onImportLocalBackup = { uri ->
                                                    viewModel.restoreFullBackupFromPhoneUri(uri)
                                                },
                                                snackbarMessage = snackbarMessage,
                                                onClearSnackbar = { viewModel.clearSnackbar() },
                                                onBack = { viewModel.closeAccountScreen() }
                                            )
                                        }
                                        FluffyScreenState.ScannerHub -> {
                                            ScannerRootHost(
                                                isArabic = isArabic,
                                                onTriggerBiometricPrompt = { title, subtitle, onSuccess, onError ->
                                                    val availability = biometricAuthManager.checkAvailability()
                                                    if (availability == BiometricAvailability.NO_HARDWARE) {
                                                        onError(if (isArabic) "لا توجد بصمة على الجهاز، استخدم رمز PIN" else "Biometric hardware unavailable, use PIN")
                                                    } else {
                                                        biometricAuthManager.promptBiometricAuth(
                                                            title = title,
                                                            subtitle = subtitle,
                                                            onSuccess = onSuccess,
                                                            onError = onError
                                                        )
                                                    }
                                                },
                                                onSaveQrAsFluffyNote = { title, content ->
                                                    viewModel.saveQuickScannedNote(title, content)
                                                },
                                                onExitScanner = { viewModel.closeScannerHub() }
                                            )
                                        }
                                        FluffyScreenState.Vault -> {
                                            VaultScreen(
                                                vaultItems = vaultItems,
                                                isDark = isDark,
                                                isArabic = isArabic,
                                                viewMode = vaultViewMode,
                                                onSelectViewMode = { mode -> viewModel.setVaultViewMode(mode) },
                                                vaultNoteMetaMap = vaultNoteMetaMap,
                                                isPremium = isPremium,
                                                onRequestPremiumUpgrade = { viewModel.openPremiumUpgradeDialog() },
                                                onSaveLiteralText = { title, content ->
                                                    viewModel.saveLiteralTextToVault(title, content)
                                                },
                                                onCreateRichNote = { title, content, pagesJson, category, colorPreset, backgroundId, customColorArgb, isPinned, checklist, audioDurationSec, audioFilePath, audioByteSize ->
                                                    viewModel.createRichNoteInVault(
                                                        title = title,
                                                        content = content,
                                                        pagesJson = pagesJson,
                                                        category = category,
                                                        colorPreset = colorPreset,
                                                        backgroundId = backgroundId,
                                                        customColorArgb = customColorArgb,
                                                        isPinned = isPinned,
                                                        checklist = checklist,
                                                        audioDurationSec = audioDurationSec,
                                                        audioFilePath = audioFilePath,
                                                        audioByteSize = audioByteSize
                                                    )
                                                },
                                                onUpdateVaultNote = { existingItem, existingMeta, title, content, pagesJson, category, colorPreset, backgroundId, customColorArgb, isPinned, checklist, audioDurationSec, audioFilePath, audioByteSize ->
                                                    viewModel.updateVaultNoteItem(
                                                        existingItem = existingItem,
                                                        existingMeta = existingMeta,
                                                        title = title,
                                                        content = content,
                                                        pagesJson = pagesJson,
                                                        category = category,
                                                        colorPreset = colorPreset,
                                                        backgroundId = backgroundId,
                                                        customColorArgb = customColorArgb,
                                                        isPinned = isPinned,
                                                        checklist = checklist,
                                                        audioDurationSec = audioDurationSec,
                                                        audioFilePath = audioFilePath,
                                                        audioByteSize = audioByteSize
                                                    )
                                                },
                                                onSaveFileUri = { uri ->
                                                    viewModel.saveFileToVault(uri)
                                                },
                                                onExportItemToUri = { item, destUri ->
                                                    viewModel.exportVaultItemToUri(item, destUri)
                                                },
                                                onMoveToNotes = { item ->
                                                    viewModel.moveVaultItemToNotes(item)
                                                },
                                                onDeleteItem = { item ->
                                                    viewModel.deleteVaultItem(item)
                                                },
                                                onBack = {
                                                    biometricAuthManager.cancelPrompt(tag = "vault")
                                                    viewModel.closeVault()
                                                },
                                                isVaultLockEnabled = isVaultLockEnabled,
                                                isVaultUnlocked = isVaultUnlockedForSession,
                                                isVaultActive = isVaultOpen,
                                                vaultSessionId = vaultSessionId,
                                                hasVaultPasskey = hasVaultPasskey,
                                                snackbarMessage = snackbarMessage,
                                                onClearSnackbar = { viewModel.clearSnackbar() },
                                                onTriggerBiometricUnlock = {
                                                    if (!viewModel.isVaultOpen.value || viewModel.isVaultUnlockedForSession.value) {
                                                        return@VaultScreen
                                                    }
                                                    val availability = biometricAuthManager.checkAvailability()
                                                    if (availability == BiometricAvailability.NO_HARDWARE) {
                                                        if (!hasVaultPasskey && viewModel.isVaultOpen.value) {
                                                            viewModel.markVaultUnlocked()
                                                        }
                                                    } else {
                                                        biometricAuthManager.promptBiometricAuth(
                                                            title = translateText(
                                                                appLanguage,
                                                                ar = "حماية خزنة Vault",
                                                                fr = "Protection du coffre Vault",
                                                                en = "Vault Protection",
                                                                tzm = "Ammesten n Vault"
                                                            ),
                                                            subtitle = translateText(
                                                                appLanguage,
                                                                ar = "قم بتأكيد هويتك لفتح الـ Vault",
                                                                fr = "Confirmez votre identité pour ouvrir le Vault",
                                                                en = "Confirm your identity to unlock Vault",
                                                                tzm = "Sentem timagit-ik i wakken ad teldiḍ Vault"
                                                            ),
                                                            tag = "vault",
                                                            onSuccess = {
                                                                if (viewModel.isVaultOpen.value) {
                                                                    viewModel.markVaultUnlocked()
                                                                }
                                                            },
                                                            onError = { }
                                                        )
                                                    }
                                                },
                                                onVerifyPasskey = { passkey ->
                                                    viewModel.verifyVaultPasskey(passkey)
                                                }
                                            )
                                        }
                                        is FluffyScreenState.Editor -> {
                                            NoteEditorScreen(
                                                note = screen.note,
                                                isCreatingNew = isCreatingNew,
                                                isDark = isDark,
                                                isArabic = isArabic,
                                                isPremium = isPremium,
                                                onRequestPremiumUpgrade = { viewModel.openPremiumUpgradeDialog() },
                                                onBack = { viewModel.closeEditor() },
                                                onSave = { title, content, pagesJson, category, colorPreset, backgroundId, customColorArgb, isPinned, isFav, checklist, audioDuration, audioPath, audioSize ->
                                                    viewModel.saveNote(
                                                        title = title,
                                                        content = content,
                                                        pagesJson = pagesJson,
                                                        category = category,
                                                        colorPreset = colorPreset,
                                                        backgroundId = backgroundId,
                                                        customColorArgb = customColorArgb,
                                                        isPinned = isPinned,
                                                        isFavorite = isFav,
                                                        checklist = checklist,
                                                        audioDurationSec = audioDuration,
                                                        audioFilePath = audioPath,
                                                        audioByteSize = audioSize
                                                    )
                                                },
                                                onDelete = { viewModel.deleteCurrentNote() },
                                                onToggleArchive = { viewModel.toggleArchive(screen.note) },
                                                onMoveToVault = { updatedNote -> viewModel.moveNoteToVault(updatedNote) },
                                                onAutoSave = { title, content, pagesJson, category, colorPreset, backgroundId, customColorArgb, isPinned, isFav, checklist, audioDuration, audioPath, audioSize ->
                                                    viewModel.autoSaveNote(
                                                        title = title,
                                                        content = content,
                                                        pagesJson = pagesJson,
                                                        category = category,
                                                        colorPreset = colorPreset,
                                                        backgroundId = backgroundId,
                                                        customColorArgb = customColorArgb,
                                                        isPinned = isPinned,
                                                        isFavorite = isFav,
                                                        checklist = checklist,
                                                        audioDurationSec = audioDuration,
                                                        audioFilePath = audioPath,
                                                        audioByteSize = audioSize
                                                    )
                                                }
                                            )
                                        }
                                        FluffyScreenState.Home -> {
                                            FluffyHomeScreen(
                                                viewModel = viewModel,
                                                onToggleBiometricAuth = { enable ->
                                                    if (enable) {
                                                        // Verify identity first before enabling
                                                        biometricAuthManager.promptBiometricAuth(
                                                            title = if (isArabic) "تفعيل قفل البصمة" else "Enable Biometric Lock",
                                                            subtitle = if (isArabic) "يرجى تأكيد هويتك لتفعيل القفل" else "Please confirm your identity to enable lock",
                                                            onSuccess = {
                                                                viewModel.setBiometricEnabled(true)
                                                                viewModel.showSnackbar(
                                                                    if (isArabic) "تم تفعيل قفل البصمة بنجاح 🔒" else "Biometric lock enabled 🔒"
                                                                )
                                                            },
                                                            onError = {
                                                                viewModel.showSnackbar(
                                                                    if (isArabic) "تعذر تفعيل البصمة: $it" else "Authentication cancelled: $it"
                                                                )
                                                            }
                                                        )
                                                    } else {
                                                        // Confirm before disabling
                                                        biometricAuthManager.promptBiometricAuth(
                                                            title = if (isArabic) "إلغاء قفل البصمة" else "Disable Biometric Lock",
                                                            subtitle = if (isArabic) "يرجى تأكيد هويتك لإلغاء القفل" else "Confirm your identity to disable lock",
                                                            onSuccess = {
                                                                viewModel.setBiometricEnabled(false)
                                                                viewModel.showSnackbar(
                                                                    if (isArabic) "تم إلغاء قفل البصمة" else "Biometric lock disabled"
                                                                )
                                                            },
                                                            onError = {
                                                                viewModel.showSnackbar(
                                                                    if (isArabic) "تعذر إلغاء القفل: $it" else "Authentication cancelled: $it"
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                // Global Liquid Glass Google Account & Guest Mode Sync Dialog
                                FirebaseAuthDialog(
                                    visible = showAuthDialog,
                                    currentUser = currentUser,
                                    isGuestMode = isGuestMode,
                                    isAuthBusy = isAuthBusy,
                                    cloudSyncStatus = cloudSyncStatus,
                                    isDark = isDark,
                                    isArabic = isArabic,
                                    onDismiss = { viewModel.closeAuthDialog() },
                                    onGoogleIdToken = { idToken -> viewModel.signInWithGoogleIdToken(idToken) },
                                    onSyncNow = { viewModel.syncCloudNow() },
                                    onSignOut = { viewModel.signOutFirebase() }
                                )

                                // Floating Liquid Glass OTA Update Dialog (Compact, Dynamic, Theme-Matched)
                                UpdateAvailableDialog(
                                    visible = showUpdateDialog && !isAppLocked && !isVaultOpen,
                                    updateState = updateState,
                                    isDark = isDark,
                                    isArabic = isArabic,
                                    onDismiss = { viewModel.dismissUpdateDialog() },
                                    onStartDownload = { info -> viewModel.startUpdateDownload(info) },
                                    onInstallNow = { viewModel.installDownloadedUpdate() },
                                    onRequestPermission = {
                                        AppUpdateManager.createInstallPermissionIntent(context)?.let { intent ->
                                            context.startActivity(intent)
                                        }
                                    }
                                )

                                // Floating Liquid Glass Premium Upgrade & Redeem Code Dialog
                                PremiumUpgradeDialog(
                                    visible = showPremiumUpgradeDialog,
                                    isPremium = isPremium,
                                    isDark = isDark,
                                    isArabic = isArabic,
                                    onRedeemCode = { code -> viewModel.redeemPremiumCode(code) },
                                    onDismiss = { viewModel.closePremiumUpgradeDialog() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
