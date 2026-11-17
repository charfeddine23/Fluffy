package com.charfeddine.fluffy.scanner.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.charfeddine.fluffy.scanner.data.model.DocumentQuad
import com.charfeddine.fluffy.scanner.data.model.ScanMode
import com.charfeddine.fluffy.scanner.domain.ScannerViewModel
import com.charfeddine.fluffy.scanner.ui.theme.ScannerDarkLiquidBackdrop
import com.charfeddine.fluffy.scanner.ui.theme.ScannerGlassPalette
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
import kotlin.math.abs
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
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scannerViewModel.importGalleryUris(uris, appendToDocId, isArabic)
        }
    }

    val haptics = LocalHapticFeedback.current
    var flashMode by remember { mutableIntStateOf(FLASH_MODE_OFF) }
    var showGrid by remember { mutableStateOf(true) }
    var autoCaptureEnabled by remember { mutableStateOf(true) }
    var edgeState by remember { mutableStateOf(TrackedEdgeState()) }
    var barcodeResult by remember { mutableStateOf<BarcodeScanResult?>(null) }
    var showCapturedBanner by remember { mutableStateOf(false) }
    var showCaptureFlash by remember { mutableStateOf(false) }
    val stabilityTracker = remember { CornerStabilityTracker(windowSize = 5, steadyDurationMs = 1200L) }

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

    // CameraX handles
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var lastAnalysisMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { cameraProviderRef?.unbindAll() }
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(flashMode, boundCamera) {
        imageCapture?.flashMode = flashMode
        boundCamera?.cameraControl?.enableTorch(flashMode == FLASH_MODE_ON)
    }

    LaunchedEffect(showCapturedBanner) {
        if (showCapturedBanner) {
            delay(1100L)
            showCapturedBanner = false
        }
    }

    LaunchedEffect(showCaptureFlash) {
        if (showCaptureFlash) {
            delay(160L)
            showCaptureFlash = false
        }
    }

    fun takePhoto(autoGoCrop: Boolean) {
        val cap = imageCapture ?: return
        stabilityTracker.markCaptured()
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        showCaptureFlash = true
        showCapturedBanner = true
        val lockedQuadSnapshot = if (edgeState.isLocked) edgeState.quad else null

        val outFile = File(scannerViewModel.repository.docsRootDir, "raw_${UUID.randomUUID()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()
        cap.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    scannerViewModel.processCapturedFile(
                        file = outFile,
                        appendToDocId = appendToDocId,
                        isArabic = isArabic,
                        capturedQuad = lockedQuadSnapshot,
                        autoNavigateToCrop = autoGoCrop
                    )
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        context,
                        if (isArabic) "تعذر التقاط الصورة: ${exception.message}" else "Capture failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    val latestActiveMode by rememberUpdatedState(activeMode)
    val latestAutoCapture by rememberUpdatedState(autoCaptureEnabled)
    val latestIsBusy by rememberUpdatedState(isBusy)
    val latestTakePhoto by rememberUpdatedState<(Boolean) -> Unit>({ goCrop -> takePhoto(goCrop) })

    val animTlX by animateFloatAsState(edgeState.quad.topLeft.x, spring(stiffness = 260f, dampingRatio = 0.85f), label = "tlx")
    val animTlY by animateFloatAsState(edgeState.quad.topLeft.y, spring(stiffness = 260f, dampingRatio = 0.85f), label = "tly")
    val animTrX by animateFloatAsState(edgeState.quad.topRight.x, spring(stiffness = 260f, dampingRatio = 0.85f), label = "trx")
    val animTrY by animateFloatAsState(edgeState.quad.topRight.y, spring(stiffness = 260f, dampingRatio = 0.85f), label = "try")
    val animBrX by animateFloatAsState(edgeState.quad.bottomRight.x, spring(stiffness = 260f, dampingRatio = 0.85f), label = "brx")
    val animBrY by animateFloatAsState(edgeState.quad.bottomRight.y, spring(stiffness = 260f, dampingRatio = 0.85f), label = "bry")
    val animBlX by animateFloatAsState(edgeState.quad.bottomLeft.x, spring(stiffness = 260f, dampingRatio = 0.85f), label = "blx")
    val animBlY by animateFloatAsState(edgeState.quad.bottomLeft.y, spring(stiffness = 260f, dampingRatio = 0.85f), label = "bly")
    val animStability by animateFloatAsState(edgeState.stabilityProgress, spring(stiffness = 240f), label = "stability_ring")

    ScannerDarkLiquidBackdrop {
        if (!hasCameraPermission) {
            // Permission Request / Gallery Fallback UI
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
                            color = Color(0xFF04101B),
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
                                    galleryPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "استيراد من المعرض" else "Import Gallery",
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
        } else {
            // Live CameraX Preview + Real-time Edge Overlay
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        cameraProviderRef = provider
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .setFlashMode(flashMode)
                            .build()
                        imageCapture = capture

                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        val mainExec = ContextCompat.getMainExecutor(ctx)
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val now = System.currentTimeMillis()
                            if (now - lastAnalysisMs < 85L) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            lastAnalysisMs = now

                            val modeNow = latestActiveMode
                            if (modeNow == ScanMode.QR_BARCODE) {
                                val mediaImg = imageProxy.image
                                if (mediaImg != null) {
                                    val inputImg = InputImage.fromMediaImage(mediaImg, imageProxy.imageInfo.rotationDegrees)
                                    OfflineOcrAndBarcodeEngine.scanBarcodeFromInputImage(
                                        image = inputImg,
                                        onDetected = { res -> if (res != null) barcodeResult = res },
                                        onComplete = { imageProxy.close() }
                                    )
                                } else {
                                    imageProxy.close()
                                }
                            } else {
                                val rawRes = DocumentImageProcessor.detectDocumentQuadFromImageProxy(imageProxy)
                                val tracked = stabilityTracker.update(
                                    detection = rawRes,
                                    nowMs = now,
                                    autoCaptureEnabled = latestAutoCapture && !latestIsBusy
                                )
                                imageProxy.close()
                                mainExec.execute {
                                    edgeState = tracked
                                    if (tracked.shouldAutoCapture && !latestIsBusy) {
                                        latestTakePhoto(false)
                                    }
                                }
                            }
                        }

                        runCatching {
                            provider.unbindAll()
                            boundCamera = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture,
                                analysis
                            )
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Visual Shutter Flash Overlay on Auto/Manual Capture
            AnimatedVisibility(
                visible = showCaptureFlash,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.58f))
                )
            }

            // Overlays: 3x3 Grid, Real-Time Glowing Neon Quad (#00F5D4 -> #9B5DE5), Book Spine Divider, ID Card Frame, QR Target
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                if (showGrid) {
                    val gridColor = Color.White.copy(alpha = 0.16f)
                    drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.dp.toPx())
                    drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = 1.dp.toPx())
                    drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.dp.toPx())
                    drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = 1.dp.toPx())
                }

                when (activeMode) {
                    ScanMode.QR_BARCODE -> {
                        val boxSize = w * 0.66f
                        val left = (w - boxSize) / 2f
                        val top = (h - boxSize) / 2f
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                listOf(ScannerGlassPalette.EdgeOverlayCyan, ScannerGlassPalette.EdgeOverlayPurple)
                            ),
                            topLeft = Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(boxSize, boxSize),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    ScanMode.BOOK -> {
                        drawLine(
                            brush = Brush.verticalGradient(
                                listOf(Color.Transparent, ScannerGlassPalette.EdgeOverlayPurple, Color.Transparent)
                            ),
                            start = Offset(w / 2f, h * 0.16f),
                            end = Offset(w / 2f, h * 0.78f),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    ScanMode.ID_CARD -> {
                        val cardW = w * 0.84f
                        val cardH = cardW / 1.586f
                        val left = (w - cardW) / 2f
                        val top = (h - cardH) / 2f
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                listOf(ScannerGlassPalette.EdgeOverlayCyan, ScannerGlassPalette.EdgeOverlayPurple)
                            ),
                            topLeft = Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(cardW, cardH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }

                    ScanMode.SINGLE -> {
                        if (edgeState.isLocked) {
                            // FILL_CENTER exact projection from rotated camera frame -> PreviewView Canvas
                            val camAspect = edgeState.frameAspectRatio.coerceIn(0.4f, 2.5f)
                            val camH = 1f
                            val camW = camAspect
                            val scale = max(w / camW, h / camH)
                            val renderedW = camW * scale
                            val renderedH = camH * scale
                            val offX = (w - renderedW) / 2f
                            val offY = (h - renderedH) / 2f

                            fun project(nx: Float, ny: Float) = Offset(
                                (offX + nx * renderedW).coerceIn(6f, w - 6f),
                                (offY + ny * renderedH).coerceIn(6f, h - 6f)
                            )

                            val tl = project(animTlX, animTlY)
                            val tr = project(animTrX, animTrY)
                            val br = project(animBrX, animBrY)
                            val bl = project(animBlX, animBlY)

                            val polyPath = Path().apply {
                                moveTo(tl.x, tl.y)
                                lineTo(tr.x, tr.y)
                                lineTo(br.x, br.y)
                                lineTo(bl.x, bl.y)
                                close()
                            }

                            val neonBrush = Brush.linearGradient(
                                colors = listOf(
                                    ScannerGlassPalette.EdgeOverlayCyan,
                                    ScannerGlassPalette.EdgeOverlayPurple
                                )
                            )
                            val fillAlpha = if (edgeState.stabilityProgress > 0.25f) 0.18f else 0.10f
                            drawPath(
                                path = polyPath,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = fillAlpha),
                                        ScannerGlassPalette.EdgeOverlayPurple.copy(alpha = fillAlpha)
                                    )
                                )
                            )
                            // Soft outer glow halo stroke + sharp inner neon stroke
                            drawPath(
                                path = polyPath,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.35f),
                                        ScannerGlassPalette.EdgeOverlayPurple.copy(alpha = 0.35f)
                                    )
                                ),
                                style = Stroke(width = 8.dp.toPx(), join = StrokeJoin.Round)
                            )
                            drawPath(
                                path = polyPath,
                                brush = neonBrush,
                                style = Stroke(width = 2.8.dp.toPx(), join = StrokeJoin.Round)
                            )

                            listOf(tl, tr, br, bl).forEach { pt ->
                                drawCircle(ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.40f), radius = 11.dp.toPx(), center = pt)
                                drawCircle(ScannerGlassPalette.EdgeOverlayCyan, radius = 6.5.dp.toPx(), center = pt)
                                drawCircle(Color.White, radius = 3.dp.toPx(), center = pt)
                            }
                        } else {
                            // Subtle corner search reticles while searching
                            val marginX = w * 0.11f
                            val topY = h * 0.20f
                            val botY = h * 0.76f
                            val len = 26.dp.toPx()
                            val reticleColor = ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.42f)
                            val st = 2.2.dp.toPx()
                            drawLine(reticleColor, Offset(marginX, topY + len), Offset(marginX, topY), st, StrokeCap.Round)
                            drawLine(reticleColor, Offset(marginX, topY), Offset(marginX + len, topY), st, StrokeCap.Round)
                            drawLine(reticleColor, Offset(w - marginX - len, topY), Offset(w - marginX, topY), st, StrokeCap.Round)
                            drawLine(reticleColor, Offset(w - marginX, topY), Offset(w - marginX, topY + len), st, StrokeCap.Round)
                            drawLine(reticleColor, Offset(marginX, botY - len), Offset(marginX, botY), st, StrokeCap.Round)
                            drawLine(reticleColor, Offset(marginX, botY), Offset(marginX + len, botY), st, StrokeCap.Round)
                            drawLine(reticleColor, Offset(w - marginX - len, botY), Offset(w - marginX, botY), st, StrokeCap.Round)
                            drawLine(reticleColor, Offset(w - marginX, botY), Offset(w - marginX, botY - len), st, StrokeCap.Round)
                        }
                    }
                }
            }

            // HUD Controls (Top Bar + Leveler + Mode Switcher + Bottom Capture Dock)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Single Sleek Top Glass HUD Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .scannerGlassSurface(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back",
                            tint = ScannerGlassPalette.TextPrimary
                        )
                    }

                    // Dynamic Detection Guidance Pill with Circular Stability Progress Ring
                    val isHoldingSteady = edgeState.isLocked && !showCapturedBanner &&
                        (activeMode == ScanMode.SINGLE || activeMode == ScanMode.BOOK)
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(horizontal = 6.dp)
                            .scannerGlassSurface(CircleShape, glowBorder = isHoldingSteady || showCapturedBanner)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
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
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                edgeState.isLocked -> ScannerGlassPalette.EdgeOverlayCyan
                                                isLevelSteady -> ScannerGlassPalette.AmberGlow
                                                else -> ScannerGlassPalette.TextSecondary
                                            }
                                        )
                                )
                            }
                        }

                        Text(
                            text = when {
                                showCapturedBanner ->
                                    if (isArabic) "تم الالتقاط!" else "Captured!"
                                activeMode == ScanMode.ID_CARD && idCardFrontPath == null ->
                                    if (isArabic) "وجه البطاقة الأمامي" else "Front of ID"
                                activeMode == ScanMode.ID_CARD && idCardFrontPath != null ->
                                    if (isArabic) "وجه البطاقة الخلفي" else "Back of ID"
                                activeMode == ScanMode.QR_BARCODE ->
                                    if (isArabic) "وجّه نحو الرمز" else "Point at QR / Code"
                                edgeState.isLocked ->
                                    if (isArabic) "ثبّت الهاتف..." else "Hold Steady..."
                                else ->
                                    if (isArabic) "جارٍ البحث عن مستند..." else "Searching for document..."
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showCapturedBanner) ScannerGlassPalette.EdgeOverlayCyan else ScannerGlassPalette.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { autoCaptureEnabled = !autoCaptureEnabled },
                            modifier = Modifier
                                .size(48.dp)
                                .scannerGlassSurface(CircleShape, glowBorder = autoCaptureEnabled)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = if (isArabic) "التقاط تلقائي" else "Auto Capture",
                                tint = if (autoCaptureEnabled) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                flashMode = when (flashMode) {
                                    FLASH_MODE_OFF -> FLASH_MODE_ON
                                    FLASH_MODE_ON -> FLASH_MODE_AUTO
                                    else -> FLASH_MODE_OFF
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .scannerGlassSurface(CircleShape, glowBorder = flashMode != FLASH_MODE_OFF)
                        ) {
                            Icon(
                                imageVector = when (flashMode) {
                                    FLASH_MODE_ON -> Icons.Default.FlashOn
                                    FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                    else -> Icons.Default.FlashOff
                                },
                                contentDescription = if (isArabic) "الفلاش" else "Flash",
                                tint = if (flashMode == FLASH_MODE_OFF) ScannerGlassPalette.TextPrimary else ScannerGlassPalette.AmberGlow
                            )
                        }
                        IconButton(
                            onClick = { showGrid = !showGrid },
                            modifier = Modifier
                                .size(48.dp)
                                .scannerGlassSurface(CircleShape)
                        ) {
                            Icon(
                                imageVector = if (showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                                contentDescription = if (isArabic) "الشبكة" else "Grid",
                                tint = if (showGrid) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary
                            )
                        }
                    }
                }

                // QR / Barcode Live Detection Result Card
                AnimatedVisibility(visible = activeMode == ScanMode.QR_BARCODE && barcodeResult != null) {
                    barcodeResult?.let { qr ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scannerGlassSurface(RoundedCornerShape(20.dp), glowBorder = true)
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
                                        .height(44.dp)
                                        .scannerGlassSurface(RoundedCornerShape(12.dp))
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
                                            .height(44.dp)
                                            .scannerGlassSurface(RoundedCornerShape(12.dp), glowBorder = true)
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
                                            .height(44.dp)
                                            .scannerGlassSurface(RoundedCornerShape(12.dp))
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

                // Bottom Camera Mode Switcher + Batch Thumbnails + Shutter Bar
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Centered Mode Switcher Strip
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        items(ScanMode.entries) { mode ->
                            val selected = activeMode == mode
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("scan_mode_${mode.id}")
                                    .scannerGlassSurface(
                                        shape = CircleShape,
                                        glowBorder = selected,
                                        fillAlpha = if (selected) 0.90f else 0.56f
                                    )
                                    .clickable {
                                        scannerViewModel.activeScanMode.value = mode
                                        if (mode != ScanMode.QR_BARCODE) barcodeResult = null
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isArabic) mode.labelAr else mode.labelEn,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextPrimary
                                )
                            }
                        }
                    }

                    // Bottom Shutter + Gallery + Batch Done Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scannerGlassSurface(RoundedCornerShape(26.dp), glowBorder = true)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery Import
                        IconButton(
                            onClick = {
                                galleryPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("camera_gallery_import_btn")
                                .scannerGlassSurface(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = if (isArabic) "استيراد من المعرض" else "Import from Gallery",
                                tint = ScannerGlassPalette.TextPrimary
                            )
                        }

                        // Neon Shutter Button
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("camera_shutter_btn")
                                .clip(CircleShape)
                                .border(3.dp, ScannerGlassPalette.NeonBorderBrush, CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .clickable(enabled = activeMode != ScanMode.QR_BARCODE) {
                                    takePhoto(autoGoCrop = false)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    color = ScannerGlassPalette.CyanGlow,
                                    modifier = Modifier.size(30.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }

                        // Batch Thumbnail Counter -> Proceeds to Crop & Filters
                        if (batchFrames.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("camera_batch_done_btn")
                                    .scannerGlassSurface(RoundedCornerShape(16.dp), glowBorder = true)
                                    .clickable { scannerViewModel.proceedToCropReview(appendToDocId) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(File(batchFrames.last().processedImagePath)),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Text(
                                        text = "${batchFrames.size} ✓",
                                        color = ScannerGlassPalette.CyanGlow,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = if (isArabic) "متابعة" else "Continue",
                                        tint = ScannerGlassPalette.EmeraldGlow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(50.dp))
                        }
                    }
                }
            }
        }
    }
}
