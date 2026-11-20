package com.charfeddine.fluffy.scanner.ui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.charfeddine.fluffy.scanner.data.model.ScanMode
import com.charfeddine.fluffy.scanner.domain.ScannerSubRoute
import com.charfeddine.fluffy.scanner.domain.ScannerViewModel
import com.charfeddine.fluffy.scanner.ui.screens.CameraScannerScreen
import com.charfeddine.fluffy.scanner.ui.screens.CropAndFilterScreen
import com.charfeddine.fluffy.scanner.ui.screens.DocumentDetailEditorScreen
import com.charfeddine.fluffy.scanner.ui.screens.ScannerHubScreen
import com.charfeddine.fluffy.scanner.ui.theme.ScannerGlassPalette
import com.charfeddine.fluffy.scanner.ui.theme.scannerGlassSurface
import com.charfeddine.fluffy.scanner.utils.MlKitDocumentScannerHelper

@Composable
fun ScannerRootHost(
    isArabic: Boolean,
    onTriggerBiometricPrompt: (title: String, subtitle: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onSaveQrAsFluffyNote: (title: String, content: String) -> Unit,
    onExitScanner: () -> Unit,
    scannerViewModel: ScannerViewModel = viewModel()
) {
    val route by scannerViewModel.currentRoute.collectAsState()
    val statusMsg by scannerViewModel.statusMessage.collectAsState()
    val hasPin by scannerViewModel.hasPinConfigured.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    var pendingUnlockTarget by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var pendingScanAppendDocId by rememberSaveable { mutableStateOf<String?>(null) }

    val mlKitScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        val appendTarget = pendingScanAppendDocId
        pendingScanAppendDocId = null
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val payload = MlKitDocumentScannerHelper.parseResult(activityResult.data)
            if (payload != null) {
                scannerViewModel.importMlKitScanResult(
                    payload = payload,
                    appendToDocId = appendTarget,
                    isArabic = isArabic
                )
            } else {
                scannerViewModel.openCameraScanner(
                    appendToDocId = appendTarget,
                    initialMode = ScanMode.SINGLE
                )
            }
        }
    }

    fun launchPrimaryScanner(appendToDocId: String?) {
        pendingScanAppendDocId = appendToDocId
        MlKitDocumentScannerHelper.launch(
            context = context,
            launcher = mlKitScannerLauncher,
            onFallbackToCameraX = {
                pendingScanAppendDocId = null
                scannerViewModel.openCameraScanner(
                    appendToDocId = appendToDocId,
                    initialMode = ScanMode.SINGLE
                )
            }
        )
    }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            snackbarHostState.showSnackbar(it)
            scannerViewModel.clearStatusMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                (slideInHorizontally { if (isArabic) -it / 3 else it / 3 } + fadeIn())
                    .togetherWith(slideOutHorizontally { if (isArabic) it / 3 else -it / 3 } + fadeOut())
            },
            label = "scanner_sub_route"
        ) { current ->
            when (current) {
                ScannerSubRoute.Hub -> {
                    ScannerHubScreen(
                        scannerViewModel = scannerViewModel,
                        isArabic = isArabic,
                        onLaunchSmartScan = { launchPrimaryScanner(appendToDocId = null) },
                        onAddMoreCamera = { docId -> launchPrimaryScanner(appendToDocId = docId) },
                        onRequestProtectedAccess = { targetId, onUnlocked ->
                            pendingUnlockTarget = targetId to onUnlocked
                        },
                        onExitEcosystem = onExitScanner
                    )
                }

                is ScannerSubRoute.Camera -> {
                    CameraScannerScreen(
                        scannerViewModel = scannerViewModel,
                        appendToDocId = current.appendToDocId,
                        isArabic = isArabic,
                        onSaveQrAsNote = onSaveQrAsFluffyNote,
                        onBack = {
                            if (current.appendToDocId != null) {
                                scannerViewModel.openDocumentDetail(current.appendToDocId)
                            } else {
                                scannerViewModel.navigateBackToHub()
                            }
                        }
                    )
                }

                is ScannerSubRoute.CropAndFilter -> {
                    CropAndFilterScreen(
                        scannerViewModel = scannerViewModel,
                        appendToDocId = current.appendToDocId,
                        isArabic = isArabic,
                        onBackToCamera = {
                            scannerViewModel.currentRoute.value = ScannerSubRoute.Camera(current.appendToDocId)
                        }
                    )
                }

                is ScannerSubRoute.DocumentDetail -> {
                    DocumentDetailEditorScreen(
                        scannerViewModel = scannerViewModel,
                        documentId = current.documentId,
                        isArabic = isArabic,
                        onAddMorePages = {
                            launchPrimaryScanner(appendToDocId = current.documentId)
                        },
                        onBackToHub = { scannerViewModel.navigateBackToHub() }
                    )
                }
            }
        }

        // Dark Liquid Glass Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 86.dp, start = 18.dp, end = 18.dp)
        ) { data ->
            Box(
                modifier = Modifier
                    .scannerGlassSurface(RoundedCornerShape(16.dp), glowBorder = true)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = data.visuals.message,
                    color = ScannerGlassPalette.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }

    pendingUnlockTarget?.let { (_, onUnlocked) ->
        ScannerBiometricPinDialog(
            visible = true,
            isArabic = isArabic,
            hasPinConfigured = hasPin,
            onTriggerBiometric = {
                onTriggerBiometricPrompt(
                    if (isArabic) "فتح المستند المشفّر" else "Unlock Encrypted Document",
                    if (isArabic) "أكد هويتك بالبصمة أو الوجه" else "Authenticate with Fingerprint / Face",
                    {
                        pendingUnlockTarget = null
                        onUnlocked()
                    },
                    { err -> scannerViewModel.postMessage(err) }
                )
            },
            onSetNewPin = { pin -> scannerViewModel.saveScannerPin(pin) },
            onVerifyPin = { candidate -> scannerViewModel.verifyScannerPin(candidate) },
            onUnlocked = {
                pendingUnlockTarget = null
                onUnlocked()
            },
            onDismiss = { pendingUnlockTarget = null }
        )
    }
}
