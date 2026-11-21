package com.charfeddine.fluffy.scanner.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PointF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Size as AndroidSize
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.charfeddine.fluffy.scanner.data.model.BarcodeScanResult
import com.charfeddine.fluffy.scanner.data.model.ScanMode
import com.charfeddine.fluffy.scanner.domain.ScannerViewModel
import com.charfeddine.fluffy.scanner.ui.theme.ScannerDarkLiquidBackdrop
import com.charfeddine.fluffy.scanner.ui.theme.ScannerGlassPalette
import com.charfeddine.fluffy.scanner.ui.theme.iosLiquidGlass
import com.charfeddine.fluffy.scanner.ui.theme.scannerGlassSurface
import com.charfeddine.fluffy.scanner.utils.CornerStabilityTracker
import com.charfeddine.fluffy.scanner.utils.DocumentImageProcessor
import com.charfeddine.fluffy.scanner.utils.OfflineOcrAndBarcodeEngine
import com.charfeddine.fluffy.scanner.utils.TrackedEdgeState
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScannerScreen(
    scannerViewModel: ScannerViewModel,
    appendToDocId: String?,
    isArabic: Boolean,
    onSaveQrAsNote: ((String, String) -> Unit)? = null,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val activeMode by scannerViewModel.activeScanMode.collectAsState()
    val batchFrames by scannerViewModel.batchFrames.collectAsState()
    val idCardFrontPath by scannerViewModel.idCardFrontRawPath.collectAsState()
    val isBusy by scannerViewModel.isBusy.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scannerViewModel.importDeviceFiles(uris, appendToDocId, isArabic)
        }
    }

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val idGuideTopOffsetPx = remember(density) { with(density) { 20.dp.toPx() } }
    var flashMode by remember { mutableIntStateOf(FLASH_MODE_OFF) }
    var showGrid by remember { mutableStateOf(true) }
    var autoCaptureEnabled by remember { mutableStateOf(false) }
    var edgeState by remember { mutableStateOf(TrackedEdgeState()) }
    var barcodeResult by remember { mutableStateOf<BarcodeScanResult?>(null) }
    var showCapturedBanner by remember { mutableStateOf(false) }
    var showCaptureFlash by remember { mutableStateOf(false) }
    val stabilityTracker = remember {
        CornerStabilityTracker(
            windowSize = 4,
            steadyDurationMs = 600L,
            jitterThresholdNorm = 0.018f
        )
    }

    // Tilt / Leveler accelerometer state
    var tiltRollDeg by remember { mutableFloatStateOf(0f) }
    var tiltPitchDeg by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorMgr = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sensorMgr?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val ax = event.values.getOrNull(0) ?: 0f
                val ay = event.values.getOrNull(1) ?: 0f
                tiltRollDeg = (ax * 9f).coerceIn(-45f, 45f)
                tiltPitchDeg = (ay * 9f).coerceIn(-45f, 45f)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (accel != null) {
            sensorMgr.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorMgr?.unregisterListener(listener) }
    }

    val isLevelSteady = abs(tiltRollDeg) < 12f && (abs(tiltPitchDeg) < 16f || abs(tiltPitchDeg - 45f) < 35f)

    // Dedicated high-priority single-thread executor so ImageAnalysis never queues or lags UI
    val cameraExecutor = remember {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "FluffyCameraAnalyzer").apply {
                priority = Thread.MAX_PRIORITY
                isDaemon = true
            }
        }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    val isDisposed = remember { AtomicBoolean(false) }
    val isCaptureInFlight = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        onDispose {
            isDisposed.set(true)
            runCatching { cameraProviderRef?.unbindAll() }
            runCatching { cameraExecutor.shutdownNow() }
            stabilityTracker.reset()
        }
    }

    LaunchedEffect(flashMode, boundCamera) {
        imageCapture?.flashMode = flashMode
        boundCamera?.cameraControl?.enableTorch(flashMode == FLASH_MODE_ON)
    }

    LaunchedEffect(focusTapOffset) {
        if (focusTapOffset != null) {
            delay(950L)
            focusTapOffset = null
        }
    }

    LaunchedEffect(showCapturedBanner) {
        if (showCapturedBanner) {
            delay(1100L)
            showCapturedBanner = false
        }
    }

    LaunchedEffect(showCaptureFlash) {
        if (showCaptureFlash) {
            delay(150L)
            showCaptureFlash = false
        }
    }

    fun takePhoto(autoGoCrop: Boolean) {
        if (isDisposed.get()) return
        val cap = imageCapture ?: return
        if (!isCaptureInFlight.compareAndSet(false, true)) return
        stabilityTracker.markCaptured()
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        showCaptureFlash = true
        showCapturedBanner = true
        val lockedQuadSnapshot = if (edgeState.isLocked) edgeState.quad else null
        val analyzerW = edgeState.frameWidth
        val analyzerH = edgeState.frameHeight
        val frameAspect = edgeState.frameAspectRatio
        val pvSnapshot = previewViewRef
        val pvWidthSnapshot = pvSnapshot?.width ?: 0
        val pvHeightSnapshot = pvSnapshot?.height ?: 0
        val previewCornersSnapshot: Array<PointF>? = if (lockedQuadSnapshot != null && pvWidthSnapshot > 0 && pvHeightSnapshot > 0) {
            val pw = pvWidthSnapshot.toFloat()
            val ph = pvHeightSnapshot.toFloat()
            val camAspect = frameAspect.coerceIn(0.4f, 2.5f)
            val fillScale = max(pw / camAspect, ph)
            val renderedW = camAspect * fillScale
            val renderedH = fillScale
            val offX = (pw - renderedW) * 0.5f
            val offY = (ph - renderedH) * 0.5f
            fun toPreviewPoint(nx: Float, ny: Float) = PointF(
                offX + nx * renderedW,
                offY + ny * renderedH
            )
            arrayOf(
                toPreviewPoint(lockedQuadSnapshot.topLeft.x, lockedQuadSnapshot.topLeft.y),
                toPreviewPoint(lockedQuadSnapshot.topRight.x, lockedQuadSnapshot.topRight.y),
                toPreviewPoint(lockedQuadSnapshot.bottomRight.x, lockedQuadSnapshot.bottomRight.y),
                toPreviewPoint(lockedQuadSnapshot.bottomLeft.x, lockedQuadSnapshot.bottomLeft.y)
            )
        } else {
            null
        }

        val idGuidePreviewCornersSnapshot: Array<PointF>? =
            if (activeMode == ScanMode.ID_CARD && pvWidthSnapshot > 0 && pvHeightSnapshot > 0) {
                val pw = pvWidthSnapshot.toFloat()
                val ph = pvHeightSnapshot.toFloat()
                val cardW = pw * 0.86f
                val cardH = cardW / 1.586f
                val left = (pw - cardW) * 0.5f
                val top = (ph - cardH) * 0.5f - idGuideTopOffsetPx
                arrayOf(
                    PointF(left, top),
                    PointF(left + cardW, top),
                    PointF(left + cardW, top + cardH),
                    PointF(left, top + cardH)
                )
            } else {
                null
            }

        val outFile = File(scannerViewModel.repository.docsRootDir, "raw_${UUID.randomUUID()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val shutterFired = AtomicBoolean(false)
        val mainHandler = Handler(Looper.getMainLooper())

        val executeShutter = Runnable {
            if (isDisposed.get()) {
                isCaptureInFlight.set(false)
                return@Runnable
            }
            if (!shutterFired.compareAndSet(false, true)) return@Runnable
            runCatching {
                cap.takePicture(
                    outputOptions,
                    mainExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            isCaptureInFlight.set(false)
                            if (isDisposed.get()) return
                            scannerViewModel.processCapturedFile(
                                file = outFile,
                                appendToDocId = appendToDocId,
                                isArabic = isArabic,
                                capturedQuad = lockedQuadSnapshot,
                                capturedPreviewCorners = previewCornersSnapshot,
                                idGuidePreviewCorners = idGuidePreviewCornersSnapshot,
                                previewWidth = pvWidthSnapshot,
                                previewHeight = pvHeightSnapshot,
                                analyzerWidth = analyzerW,
                                analyzerHeight = analyzerH,
                                autoNavigateToCrop = autoGoCrop
                            )
                        }

                        override fun onError(exception: ImageCaptureException) {
                            isCaptureInFlight.set(false)
                            if (isDisposed.get()) return
                            Toast.makeText(
                                context,
                                if (isArabic) "تعذر التقاط الصورة: ${exception.message}" else "Capture failed: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }.onFailure {
                isCaptureInFlight.set(false)
            }
        }

        // Pre-capture FocusMeteringAction locked onto detected document bounding box or frame center
        val pv = previewViewRef
        val cam = boundCamera
        if (pv != null && cam != null && pv.width > 0 && pv.height > 0) {
            val pvW = pv.width.toFloat()
            val pvH = pv.height.toFloat()
            val (focusX, focusY) = if (lockedQuadSnapshot != null) {
                val cx = (lockedQuadSnapshot.topLeft.x + lockedQuadSnapshot.topRight.x +
                    lockedQuadSnapshot.bottomRight.x + lockedQuadSnapshot.bottomLeft.x) * 0.25f
                val cy = (lockedQuadSnapshot.topLeft.y + lockedQuadSnapshot.topRight.y +
                    lockedQuadSnapshot.bottomRight.y + lockedQuadSnapshot.bottomLeft.y) * 0.25f
                val camAspect = frameAspect.coerceIn(0.4f, 2.5f)
                val scale = max(pvW / camAspect, pvH)
                val renderedW = camAspect * scale
                val renderedH = scale
                val offX = (pvW - renderedW) * 0.5f
                val offY = (pvH - renderedH) * 0.5f
                (offX + cx * renderedW).coerceIn(12f, pvW - 12f) to
                    (offY + cy * renderedH).coerceIn(12f, pvH - 12f)
            } else {
                (pvW * 0.5f) to (pvH * 0.5f)
            }

            runCatching {
                val pt = pv.meteringPointFactory.createPoint(focusX, focusY, 0.22f)
                val action = FocusMeteringAction.Builder(
                    pt,
                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
                ).setAutoCancelDuration(2, TimeUnit.SECONDS).build()
                mainHandler.postDelayed(executeShutter, 260L)
                cam.cameraControl.startFocusAndMetering(action).addListener(
                    {
                        mainHandler.removeCallbacks(executeShutter)
                        executeShutter.run()
                    },
                    mainExecutor
                )
            }.onFailure {
                mainHandler.removeCallbacks(executeShutter)
                executeShutter.run()
            }
        } else {
            executeShutter.run()
        }
    }

    val latestActiveMode by rememberUpdatedState(activeMode)
    val latestAutoCapture by rememberUpdatedState(autoCaptureEnabled)
    val latestIsBusy by rememberUpdatedState(isBusy)
    val latestTakePhoto by rememberUpdatedState<(Boolean) -> Unit>({ goCrop -> takePhoto(goCrop) })

    val animStability by animateFloatAsState(edgeState.stabilityProgress, spring(stiffness = 240f), label = "stability_ring")

    val infiniteTransition = rememberInfiniteTransition(label = "scanner_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulse_alpha"
    )
    val scanSweepFraction by infiniteTransition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Reverse),
        label = "laser_sweep"
    )

    if (!hasCameraPermission) {
        ScannerDarkLiquidBackdrop {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scannerGlassSurface(RoundedCornerShape(28.dp), glowBorder = true)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = ScannerGlassPalette.CyanGlow,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = if (isArabic) "إذن الكاميرا لتصوير المستندات" else "Camera Permission Needed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScannerGlassPalette.TextPrimary
                    )
                    Text(
                        text = if (isArabic) {
                            "يحتاج الماسح الضوئي إلى الكاميرا لاكتشاف حواف المستندات محلياً 100% بدون إنترنت."
                        } else {
                            "Fluffy Doc Scanner processes documents 100% offline. Grant camera access or import photos directly from your gallery."
                        },
                        fontSize = 13.sp,
                        color = ScannerGlassPalette.TextSecondary
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (isArabic) "تفعيل الكاميرا" else "Grant Camera Access",
                            color = ScannerGlassPalette.DeepObsidian,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .scannerGlassSurface(RoundedCornerShape(14.dp))
                                .clickable {
                                    galleryPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "استيراد ملفات" else "Import Files",
                                color = ScannerGlassPalette.CyanGlow,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .scannerGlassSurface(RoundedCornerShape(14.dp))
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "رجوع" else "Back",
                                color = ScannerGlassPalette.TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
        return
    }

    // Full-Bleed Edge-to-Edge Live Camera Experience
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    focusTapOffset = tapOffset
                    val pv = previewViewRef
                    val cam = boundCamera
                    if (pv != null && cam != null) {
                        runCatching {
                            val pt = pv.meteringPointFactory.createPoint(tapOffset.x, tapOffset.y)
                            val action = FocusMeteringAction.Builder(
                                pt,
                                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                            ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                            cam.cameraControl.startFocusAndMetering(action)
                        }
                    }
                }
            }
    ) {
        // 1. Live Full-Screen CameraX Viewfinder
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                previewViewRef = previewView
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    if (isDisposed.get()) return@addListener
                    cameraProviderRef = provider
                    val previewResolutionSelector = ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .build()
                    val preview = Preview.Builder()
                        .setResolutionSelector(previewResolutionSelector)
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                    val captureResolutionSelector = ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                        .build()
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setJpegQuality(95)
                        .setResolutionSelector(captureResolutionSelector)
                        .setFlashMode(flashMode)
                        .build()
                    imageCapture = capture

                    val analysisResolutionSelector = ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                AndroidSize(640, 480),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setResolutionSelector(analysisResolutionSelector)
                        .build()
                    val mainExec = ContextCompat.getMainExecutor(ctx)
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (isDisposed.get()) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val now = System.currentTimeMillis()
                        val modeNow = latestActiveMode
                        if (modeNow == ScanMode.QR_BARCODE) {
                            val mediaImg = imageProxy.image
                            if (mediaImg != null) {
                                val inputImg = InputImage.fromMediaImage(mediaImg, imageProxy.imageInfo.rotationDegrees)
                                OfflineOcrAndBarcodeEngine.scanBarcodeFromInputImage(
                                    image = inputImg,
                                    onDetected = { res -> if (res != null && !isDisposed.get()) barcodeResult = res },
                                    onComplete = { imageProxy.close() }
                                )
                            } else {
                                imageProxy.close()
                            }
                        } else {
                            val rawRes = runCatching {
                                DocumentImageProcessor.detectDocumentQuadFromImageProxy(imageProxy)
                            }.getOrNull()
                            imageProxy.close()
                            if (rawRes != null && !isDisposed.get()) {
                                val tracked = stabilityTracker.update(
                                    detection = rawRes,
                                    nowMs = now,
                                    autoCaptureEnabled = latestAutoCapture && !latestIsBusy && modeNow != ScanMode.ID_CARD
                                )
                                mainExec.execute {
                                    if (!isDisposed.get()) {
                                        edgeState = tracked
                                        if (tracked.shouldAutoCapture && !latestIsBusy) {
                                            latestTakePhoto(false)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    runCatching {
                        provider.unbindAll()
                        val cam = provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture,
                            analysis
                        )
                        boundCamera = cam
                        // Engage Continuous Auto-Focus & Metering on center plane immediately
                        previewView.post {
                            if (previewView.width > 0 && previewView.height > 0) {
                                runCatching {
                                    val centerPt = previewView.meteringPointFactory.createPoint(
                                        previewView.width * 0.5f,
                                        previewView.height * 0.5f,
                                        0.25f
                                    )
                                    val cafAction = FocusMeteringAction.Builder(
                                        centerPt,
                                        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                    ).setAutoCancelDuration(5, TimeUnit.SECONDS).build()
                                    cam.cameraControl.startFocusAndMetering(cafAction)
                                }
                            }
                        }
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Subtle Edge-to-Edge Atmospheric Dark Scrims (Preserves Full Camera Brightness in Center)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ScannerGlassPalette.DeepObsidian.copy(alpha = 0.78f),
                            ScannerGlassPalette.DeepObsidian.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(235.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ScannerGlassPalette.DeepObsidian.copy(alpha = 0.45f),
                            ScannerGlassPalette.DeepObsidian.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // 3. Precision Architectural Overlays: Grid, Real-Time Polygon, Book Spine, ID Frame, QR Reticle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            if (showGrid) {
                val gridColor = Color.White.copy(alpha = 0.13f)
                val sw = 0.75.dp.toPx()
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = sw)
                drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = sw)
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = sw)
                drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = sw)
            }

            when (activeMode) {
                ScanMode.QR_BARCODE -> {
                    val boxSize = w * 0.64f
                    val left = (w - boxSize) / 2f
                    val top = (h - boxSize) / 2f
                    val arm = 28.dp.toPx()
                    val strokeW = 3.2.dp.toPx()
                    val qrBrush = Brush.linearGradient(
                        listOf(ScannerGlassPalette.EdgeOverlayCyan, ScannerGlassPalette.EdgeOverlayPurple)
                    )
                    // 4 Corner Brackets + Laser Scan Line
                    drawLine(qrBrush, Offset(left, top + arm), Offset(left, top), strokeW, StrokeCap.Round)
                    drawLine(qrBrush, Offset(left, top), Offset(left + arm, top), strokeW, StrokeCap.Round)
                    drawLine(qrBrush, Offset(left + boxSize - arm, top), Offset(left + boxSize, top), strokeW, StrokeCap.Round)
                    drawLine(qrBrush, Offset(left + boxSize, top), Offset(left + boxSize, top + arm), strokeW, StrokeCap.Round)
                    drawLine(qrBrush, Offset(left, top + boxSize - arm), Offset(left, top + boxSize), strokeW, StrokeCap.Round)
                    drawLine(qrBrush, Offset(left, top + boxSize), Offset(left + arm, top + boxSize), strokeW, StrokeCap.Round)
                    drawLine(qrBrush, Offset(left + boxSize - arm, top + boxSize), Offset(left + boxSize, top + boxSize), strokeW, StrokeCap.Round)
                    drawLine(qrBrush, Offset(left + boxSize, top + boxSize - arm), Offset(left + boxSize, top + boxSize), strokeW, StrokeCap.Round)

                    val laserY = top + boxSize * ((scanSweepFraction - 0.24f) / 0.48f).coerceIn(0.08f, 0.92f)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, ScannerGlassPalette.EdgeOverlayCyan, ScannerGlassPalette.EdgeOverlayPurple, Color.Transparent)
                        ),
                        start = Offset(left + 14.dp.toPx(), laserY),
                        end = Offset(left + boxSize - 14.dp.toPx(), laserY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                ScanMode.BOOK -> {
                    val topY = h * 0.16f
                    val botY = h * 0.74f
                    drawLine(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                ScannerGlassPalette.EdgeOverlayCyan,
                                ScannerGlassPalette.EdgeOverlayPurple,
                                Color.Transparent
                            )
                        ),
                        start = Offset(w / 2f, topY),
                        end = Offset(w / 2f, botY),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.22f),
                        topLeft = Offset(w * 0.06f, topY),
                        size = Size(w * 0.88f, botY - topY),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }

                ScanMode.ID_CARD -> {
                    val cardW = w * 0.86f
                    val cardH = cardW / 1.586f
                    val left = (w - cardW) / 2f
                    val top = (h - cardH) / 2f - 20.dp.toPx()
                    drawRoundRect(
                        color = ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.08f),
                        topLeft = Offset(left, top),
                        size = Size(cardW, cardH),
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(ScannerGlassPalette.EdgeOverlayCyan, ScannerGlassPalette.EdgeOverlayPurple)
                        ),
                        topLeft = Offset(left, top),
                        size = Size(cardW, cardH),
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }

            // Overhead Top-Down Horizon Bubble Leveler (visible in Book Split mode)
            if (activeMode == ScanMode.BOOK) {
                val center = Offset(w * 0.5f, h * 0.46f)
                val ringRadius = 18.dp.toPx()
                val maxBubbleShift = 15.dp.toPx()
                val rawShiftX = (tiltRollDeg * 0.55f).dp.toPx()
                val rawShiftY = (tiltPitchDeg * 0.55f).dp.toPx()
                val shiftMag = hypot(rawShiftX, rawShiftY)
                val clampedShiftX = if (shiftMag > maxBubbleShift && shiftMag > 0.01f) {
                    rawShiftX * (maxBubbleShift / shiftMag)
                } else {
                    rawShiftX
                }
                val clampedShiftY = if (shiftMag > maxBubbleShift && shiftMag > 0.01f) {
                    rawShiftY * (maxBubbleShift / shiftMag)
                } else {
                    rawShiftY
                }
                val isOverheadAligned = abs(tiltRollDeg) < 6.5f && abs(tiltPitchDeg) < 6.5f
                val levelColor = if (isOverheadAligned) {
                    ScannerGlassPalette.EdgeOverlayCyan
                } else {
                    Color.White.copy(alpha = 0.48f)
                }

                drawCircle(
                    color = levelColor.copy(alpha = if (isOverheadAligned) 0.70f else 0.25f),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 1.25.dp.toPx())
                )
                drawLine(
                    color = levelColor.copy(alpha = 0.40f),
                    start = Offset(center.x - 5.dp.toPx(), center.y),
                    end = Offset(center.x + 5.dp.toPx(), center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = levelColor.copy(alpha = 0.40f),
                    start = Offset(center.x, center.y - 5.dp.toPx()),
                    end = Offset(center.x, center.y + 5.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
                drawCircle(
                    color = levelColor.copy(alpha = if (isOverheadAligned) 0.90f else 0.65f),
                    radius = 4.2.dp.toPx(),
                    center = Offset(center.x + clampedShiftX, center.y + clampedShiftY)
                )
            }

            // Tap-to-Focus glowing reticle
            focusTapOffset?.let { pt ->
                drawCircle(
                    color = ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.25f),
                    radius = 28.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = ScannerGlassPalette.EdgeOverlayCyan,
                    radius = 28.dp.toPx(),
                    center = pt,
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = pt
                )
            }
        }

        // 4. Visual Shutter Flash Feedback
        AnimatedVisibility(
            visible = showCaptureFlash,
            enter = fadeIn(tween(60)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.55f))
            )
        }

        // 5. Floating Luxury HUD Overlays (Inside Safe Insets, Never Constraining Viewfinder)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP CLUSTER: Back + Dynamic Status Pill + Unified Quick-Settings Glass Capsule
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clean Frosted iOS Liquid Glass Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(12.dp, CircleShape)
                            .iosLiquidGlass(CircleShape, fillAlpha = 0.70f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back",
                            tint = ScannerGlassPalette.TextPrimary
                        )
                    }

                    // Unified Frosted Liquid Glass Utility Capsule (Auto-Capture, Flash, Grid)
                    Row(
                        modifier = Modifier
                            .shadow(12.dp, CircleShape)
                            .iosLiquidGlass(CircleShape, fillAlpha = 0.70f)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { autoCaptureEnabled = !autoCaptureEnabled },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = if (isArabic) "التقاط تلقائي" else "Auto Capture",
                                tint = if (autoCaptureEnabled) ScannerGlassPalette.EdgeOverlayCyan else ScannerGlassPalette.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(0.8.dp)
                                .height(18.dp)
                                .background(Color.White.copy(alpha = 0.14f))
                        )

                        IconButton(
                            onClick = {
                                flashMode = when (flashMode) {
                                    FLASH_MODE_OFF -> FLASH_MODE_ON
                                    FLASH_MODE_ON -> FLASH_MODE_AUTO
                                    else -> FLASH_MODE_OFF
                                }
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = when (flashMode) {
                                    FLASH_MODE_ON -> Icons.Default.FlashOn
                                    FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                    else -> Icons.Default.FlashOff
                                },
                                contentDescription = if (isArabic) "الفلاش" else "Flash",
                                tint = if (flashMode == FLASH_MODE_OFF) ScannerGlassPalette.TextPrimary else ScannerGlassPalette.AmberGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(0.8.dp)
                                .height(18.dp)
                                .background(Color.White.copy(alpha = 0.14f))
                        )

                        IconButton(
                            onClick = { showGrid = !showGrid },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                                contentDescription = if (isArabic) "الشبكة" else "Grid",
                                tint = if (showGrid) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Centered Sleek Real-Time Status Guidance Pill
                val isHoldingSteady = edgeState.isLocked && !showCapturedBanner &&
                    activeMode == ScanMode.BOOK
                Row(
                    modifier = Modifier
                        .shadow(10.dp, CircleShape)
                        .iosLiquidGlass(
                            shape = CircleShape,
                            glowAccent = isHoldingSteady || showCapturedBanner,
                            fillAlpha = 0.72f
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        showCapturedBanner -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ScannerGlassPalette.EdgeOverlayCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        isHoldingSteady && autoCaptureEnabled -> {
                            CircularProgressIndicator(
                                progress = { animStability.coerceIn(0.08f, 1f) },
                                color = ScannerGlassPalette.EdgeOverlayCyan,
                                trackColor = Color.White.copy(alpha = 0.18f),
                                strokeWidth = 2.4.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        else -> {
                            val dotColor by animateColorAsState(
                                targetValue = when {
                                    edgeState.isLocked -> ScannerGlassPalette.EdgeOverlayCyan
                                    isLevelSteady -> ScannerGlassPalette.AmberGlow
                                    else -> ScannerGlassPalette.TextSecondary
                                },
                                label = "status_dot"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor.copy(alpha = if (!edgeState.isLocked) pulseAlpha else 1f))
                            )
                        }
                    }

                    Text(
                        text = when {
                            showCapturedBanner ->
                                if (isArabic) "تم الالتقاط!" else "Captured!"
                            activeMode == ScanMode.ID_CARD && idCardFrontPath == null ->
                                if (isArabic) "صوّر الوجه الأمامي للبطاقة" else "Scan Front of ID"
                            activeMode == ScanMode.ID_CARD && idCardFrontPath != null ->
                                if (isArabic) "صوّر الوجه الخلفي للبطاقة" else "Scan Back of ID"
                            activeMode == ScanMode.QR_BARCODE ->
                                if (isArabic) "وجّه الكاميرا نحو الرمز" else "Point at QR / Code"
                            edgeState.isLocked ->
                                if (isArabic) "ثبّت الهاتف..." else "Hold Steady..."
                            else ->
                                if (isArabic) "جارٍ البحث عن مستند..." else "Searching for document..."
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (showCapturedBanner) ScannerGlassPalette.EdgeOverlayCyan else ScannerGlassPalette.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // QR / Barcode Live Detection Floating Glass Card
            AnimatedVisibility(visible = activeMode == ScanMode.QR_BARCODE && barcodeResult != null) {
                barcodeResult?.let { qr ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scannerGlassSurface(RoundedCornerShape(22.dp), glowBorder = true, fillAlpha = 0.84f)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${qr.formatLabel} • ${qr.displayTitle}",
                            color = ScannerGlassPalette.CyanGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = qr.rawValue,
                            color = ScannerGlassPalette.TextPrimary,
                            fontSize = 14.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .scannerGlassSurface(RoundedCornerShape(14.dp))
                                    .clickable {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Scanned Code", qr.rawValue))
                                        Toast.makeText(context, if (isArabic) "تم النسخ ✓" else "Copied ✓", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ScannerGlassPalette.CyanGlow, modifier = Modifier.size(16.dp))
                                    Text(if (isArabic) "نسخ" else "Copy", color = ScannerGlassPalette.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (qr.isUrl) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .scannerGlassSurface(RoundedCornerShape(14.dp), glowBorder = true)
                                        .clickable {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(qr.rawValue)))
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = ScannerGlassPalette.EmeraldGlow, modifier = Modifier.size(16.dp))
                                        Text(if (isArabic) "فتح" else "Open", color = ScannerGlassPalette.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            if (onSaveQrAsNote != null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .scannerGlassSurface(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onSaveQrAsNote(qr.displayTitle, qr.rawValue)
                                            Toast.makeText(context, if (isArabic) "تم الحفظ في Fluffy ✓" else "Saved to Fluffy ✓", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (isArabic) "حفظ كملاحظة" else "Save Note", color = ScannerGlassPalette.VioletGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM CLUSTER: Sleek Segmented Mode Capsule + Symmetric Luxury Shutter Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Unified Segmented Frosted Liquid Glass Mode Pill Track
                Box(
                    modifier = Modifier
                        .shadow(14.dp, CircleShape)
                        .iosLiquidGlass(CircleShape, fillAlpha = 0.72f)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(ScanMode.entries) { mode ->
                            val selected = activeMode == mode
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("scan_mode_${mode.id}")
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.24f),
                                                    ScannerGlassPalette.EdgeOverlayPurple.copy(alpha = 0.24f)
                                                )
                                            )
                                        } else {
                                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                        }
                                    )
                                    .then(
                                        if (selected) {
                                            Modifier.border(
                                                0.9.dp,
                                                ScannerGlassPalette.NeonBorderBrush,
                                                CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        scannerViewModel.activeScanMode.value = mode
                                        if (mode != ScanMode.QR_BARCODE) barcodeResult = null
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isArabic) mode.labelAr else mode.labelEn,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) ScannerGlassPalette.EdgeOverlayCyan else ScannerGlassPalette.TextSecondary
                                )
                            }
                        }
                    }
                }

                // Floating Symmetrical Shutter Row (No heavy container box — pure floating glass!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Floating File Import
                    Box(modifier = Modifier.width(96.dp), contentAlignment = Alignment.CenterStart) {
                        IconButton(
                            onClick = {
                                galleryPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .shadow(12.dp, CircleShape)
                                .testTag("camera_gallery_import_btn")
                                .iosLiquidGlass(CircleShape, fillAlpha = 0.72f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = if (isArabic) "استيراد من المعرض" else "Import from Gallery",
                                tint = ScannerGlassPalette.TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Center: Flagship Luminous Shutter Button with Stability Progress Arc
                    val shutterInteraction = remember { MutableInteractionSource() }
                    val shutterPressed by shutterInteraction.collectIsPressedAsState()
                    val shutterScale by animateFloatAsState(
                        targetValue = if (shutterPressed) 0.91f else 1f,
                        animationSpec = spring(stiffness = 500f),
                        label = "shutter_scale"
                    )
                    val isDocumentActive = activeMode != ScanMode.QR_BARCODE
                    val showSteadyRing = edgeState.isLocked && autoCaptureEnabled &&
                        activeMode == ScanMode.BOOK

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(shutterScale)
                            .minimumInteractiveComponentSize()
                            .testTag("camera_shutter_btn")
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable(
                                interactionSource = shutterInteraction,
                                indication = null,
                                enabled = isDocumentActive
                            ) {
                                takePhoto(autoGoCrop = false)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Neon Ring + Live Stability Progress Arc
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 3.2.dp.toPx()
                            drawCircle(
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = if (isDocumentActive) 0.45f else 0.16f),
                                        ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = if (isDocumentActive) 0.55f else 0.16f)
                                    )
                                ),
                                style = Stroke(width = stroke)
                            )
                            if (showSteadyRing) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            ScannerGlassPalette.EdgeOverlayCyan,
                                            ScannerGlassPalette.EdgeOverlayPurple,
                                            ScannerGlassPalette.EdgeOverlayCyan
                                        )
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = 360f * animStability.coerceIn(0f, 1f),
                                    useCenter = false,
                                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        if (isBusy) {
                            CircularProgressIndicator(
                                color = ScannerGlassPalette.EdgeOverlayCyan,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.6.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = if (isDocumentActive) {
                                                listOf(Color.White, Color(0xFFE2E8F0))
                                            } else {
                                                listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.20f))
                                            }
                                        )
                                    )
                            )
                        }
                    }

                    // Right: Sleek Stacked Batch Preview Capsule (Symmetrical Width)
                    Box(modifier = Modifier.width(96.dp), contentAlignment = Alignment.CenterEnd) {
                        if (batchFrames.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .shadow(14.dp, RoundedCornerShape(18.dp))
                                    .testTag("camera_batch_done_btn")
                                    .iosLiquidGlass(RoundedCornerShape(18.dp), glowAccent = true, fillAlpha = 0.78f)
                                    .clickable { scannerViewModel.proceedToCropReview(appendToDocId) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box {
                                        Image(
                                            painter = rememberAsyncImagePainter(File(batchFrames.last().processedImagePath)),
                                            contentDescription = if (isArabic) "معاينة الصفحات" else "Pages preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-4).dp)
                                                .clip(CircleShape)
                                                .background(ScannerGlassPalette.EdgeOverlayCyan)
                                                .padding(horizontal = 5.dp, vertical = 1.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${batchFrames.size}",
                                                color = ScannerGlassPalette.DeepObsidian,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = if (isArabic) "متابعة" else "Continue",
                                        tint = ScannerGlassPalette.EdgeOverlayCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
