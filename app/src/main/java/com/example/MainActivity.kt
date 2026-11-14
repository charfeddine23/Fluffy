package com.example

import android.os.Bundle
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import com.example.ui.components.UpdateAvailableDialog
import com.example.ui.screens.AuthWelcomeScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.FluffyHomeScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.FluffyTheme
import com.example.update.AppUpdateManager
import com.example.viewmodel.FluffyViewModel

private sealed interface FluffyScreenState {
    data object Welcome : FluffyScreenState
    data object Home : FluffyScreenState
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
            val isArabic by viewModel.isArabic.collectAsState()
            val isAppLocked by viewModel.isAppLocked.collectAsState()
            val editingNote by viewModel.editingNote.collectAsState()
            val isVaultOpen by viewModel.isVaultOpen.collectAsState()
            val vaultItems by viewModel.vaultItems.collectAsState()
            val updateState by viewModel.updateState.collectAsState()
            val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
            val currentUser by viewModel.currentUser.collectAsState()
            val isGuestMode by viewModel.isGuestMode.collectAsState()
            val isAuthBusy by viewModel.isAuthBusy.collectAsState()

            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current

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

            val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                FluffyTheme(darkTheme = isDark) {
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
                                editingNote != null -> FluffyScreenState.Editor(editingNote!!)
                                else -> FluffyScreenState.Home
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                AnimatedContent(
                                    targetState = activeScreen,
                                    contentKey = { screen ->
                                        when (screen) {
                                            FluffyScreenState.Welcome -> "welcome"
                                            FluffyScreenState.Home -> "home"
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
                                            targetState is FluffyScreenState.Editor -> {
                                                (slideInHorizontally { width -> if (isArabic) -width else width } + fadeIn())
                                                    .togetherWith(slideOutHorizontally { width -> if (isArabic) width else -width } + fadeOut())
                                            }
                                            else -> {
                                                (slideInHorizontally { width -> if (isArabic) width else -width } + fadeIn())
                                                    .togetherWith(slideOutHorizontally { width -> if (isArabic) -width else width } + fadeOut())
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
                                                onToggleTheme = { viewModel.toggleDarkMode() },
                                                onToggleLanguage = { viewModel.toggleLanguage() },
                                                onGoogleIdToken = { idToken ->
                                                    viewModel.signInWithGoogleIdToken(idToken)
                                                },
                                                onContinueAsGuest = { viewModel.enterGuestMode() }
                                            )
                                        }
                                        FluffyScreenState.Vault -> {
                                            VaultScreen(
                                                vaultItems = vaultItems,
                                                isDark = isDark,
                                                isArabic = isArabic,
                                                onSaveLiteralText = { title, content ->
                                                    viewModel.saveLiteralTextToVault(title, content)
                                                },
                                                onSaveFileUri = { uri ->
                                                    viewModel.saveFileToVault(uri)
                                                },
                                                onExportItemToUri = { item, destUri ->
                                                    viewModel.exportVaultItemToUri(item, destUri)
                                                },
                                                onBack = { viewModel.closeVault() }
                                            )
                                        }
                                        is FluffyScreenState.Editor -> {
                                            NoteEditorScreen(
                                                note = screen.note,
                                                isDark = isDark,
                                                isArabic = isArabic,
                                                onBack = { viewModel.closeEditor() },
                                                onSave = { title, content, category, colorPreset, isPinned, isFav, checklist, audioDuration ->
                                                    viewModel.saveNote(
                                                        title = title,
                                                        content = content,
                                                        category = category,
                                                        colorPreset = colorPreset,
                                                        isPinned = isPinned,
                                                        isFavorite = isFav,
                                                        checklist = checklist,
                                                        audioDurationSec = audioDuration
                                                    )
                                                },
                                                onDelete = { viewModel.deleteCurrentNote() },
                                                onAutoSave = { title, content, category, colorPreset, isPinned, isFav, checklist, audioDuration ->
                                                    viewModel.autoSaveNote(
                                                        title = title,
                                                        content = content,
                                                        category = category,
                                                        colorPreset = colorPreset,
                                                        isPinned = isPinned,
                                                        isFavorite = isFav,
                                                        checklist = checklist,
                                                        audioDurationSec = audioDuration
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
                            }
                        }
                    }
                }
            }
        }
    }
}
