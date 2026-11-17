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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.charfeddine.fluffy.scanner.utils.DocumentImageProcessor
import com.charfeddine.fluffy.scanner.utils.OfflineOcrAndBarcodeEngine
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.abs

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

    var flashMode by remember { mutableIntStateOf(FLASH_MODE_OFF) }
    var showGrid by remember { mutableStateOf(true) }
    var autoCaptureEnabled by remember { mutableStateOf(true) }
    var detectedQuad by remember { mutableStateOf(DocumentQuad.FULL_FRAME) }
    var edgeConfidence by remember { mutableFloatStateOf(0f) }
    var barcodeResult by remember { mutableStateOf<BarcodeScanResult?>(null) }

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
    var lastAnalysisMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    LaunchedEffect(flashMode, boundCamera) {
        imageCapture?.flashMode = flashMode
        boundCamera?.cameraControl?.enableTorch(flashMode == FLASH_MODE_ON)
    }

    fun takePhoto(autoGoCrop: Boolean) {
        val cap = imageCapture ?: return
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

    // Auto-capture countdown when edge detection confidence stays >= 0.72 in Document modes
    var autoCountdownSec by remember { mutableIntStateOf(0) }
    LaunchedEffect(autoCaptureEnabled, edgeConfidence, activeMode, isBusy, hasCameraPermission) {
        if (!hasCameraPermission || !autoCaptureEnabled || activeMode == ScanMode.QR_BARCODE || isBusy) {
            autoCountdownSec = 0
            return@LaunchedEffect
        }
        if (edgeConfidence >= 0.72f) {
            for (sec in 3 downTo 1) {
                autoCountdownSec = sec
                delay(750L)
                if (edgeConfidence < 0.65f || !autoCaptureEnabled) {
                    autoCountdownSec = 0
                    return@LaunchedEffect
                }
            }
            autoCountdownSec = 0
            takePhoto(autoGoCrop = false)
        } else {
            autoCountdownSec = 0
        }
    }

    val animTlX by animateFloatAsState(detectedQuad.topLeft.x, spring(stiffness = 300f), label = "tlx")
    val animTlY by animateFloatAsState(detectedQuad.topLeft.y, spring(stiffness = 300f), label = "tly")
    val animTrX by animateFloatAsState(detectedQuad.topRight.x, spring(stiffness = 300f), label = "trx")
    val animTrY by animateFloatAsState(detectedQuad.topRight.y, spring(stiffness = 300f), label = "try")
    val animBrX by animateFloatAsState(detectedQuad.bottomRight.x, spring(stiffness = 300f), label = "brx")
    val animBrY by animateFloatAsState(detectedQuad.bottomRight.y, spring(stiffness = 300f), label = "bry")
    val animBlX by animateFloatAsState(detectedQuad.bottomLeft.x, spring(stiffness = 300f), label = "blx")
    val animBlY by animateFloatAsState(detectedQuad.bottomLeft.y, spring(stiffness = 300f), label = "bly")

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
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val now = System.currentTimeMillis()
                            if (now - lastAnalysisMs < 130L) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            lastAnalysisMs = now

                            if (activeMode == ScanMode.QR_BARCODE) {
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
                                val edgeRes = DocumentImageProcessor.detectDocumentQuadFromImageProxy(imageProxy)
                                detectedQuad = edgeRes.quad
                                edgeConfidence = edgeRes.confidence
                                imageProxy.close()
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

            // Overlays: 3x3 Grid, Neon 4-Corner Polygon, Book Spine Divider, ID Card Frame, QR Target
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                if (showGrid) {
                    val gridColor = Color.White.copy(alpha = 0.20f)
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
                            brush = ScannerGlassPalette.NeonBorderBrush,
                            topLeft = Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(boxSize, boxSize),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    ScanMode.BOOK -> {
                        // Center Spine Guide
                        drawLine(
                            brush = Brush.verticalGradient(
                                listOf(Color.Transparent, ScannerGlassPalette.VioletGlow, Color.Transparent)
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
                            color = ScannerGlassPalette.CyanGlow.copy(alpha = 0.85f),
                            topLeft = Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(cardW, cardH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }

                    ScanMode.SINGLE -> {
                        // Real-time Detected Neon Quad Overlay
                        val tl = Offset(animTlX * w, animTlY * h)
                        val tr = Offset(animTrX * w, animTrY * h)
                        val br = Offset(animBrX * w, animBrY * h)
                        val bl = Offset(animBlX * w, animBlY * h)
                        val polyPath = Path().apply {
                            moveTo(tl.x, tl.y)
                            lineTo(tr.x, tr.y)
                            lineTo(br.x, br.y)
                            lineTo(bl.x, bl.y)
                            close()
                        }
                        val fillAlpha = if (edgeConfidence >= 0.65f) 0.16f else 0.06f
                        drawPath(
                            path = polyPath,
                            color = ScannerGlassPalette.CyanGlow.copy(alpha = fillAlpha)
                        )
                        drawPath(
                            path = polyPath,
                            brush = Brush.linearGradient(
                                listOf(ScannerGlassPalette.CyanGlow, ScannerGlassPalette.VioletGlow)
                            ),
                            style = Stroke(width = 2.6.dp.toPx())
                        )
                        listOf(tl, tr, br, bl).forEach { pt ->
                            drawCircle(ScannerGlassPalette.CyanGlow, radius = 7.dp.toPx(), center = pt)
                            drawCircle(Color.White, radius = 3.dp.toPx(), center = pt)
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
                // Top Glass HUD
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                        // Leveler / Tilt + Auto-Capture Indicator Pill
                        Row(
                            modifier = Modifier
                                .scannerGlassSurface(CircleShape, glowBorder = isLevelSteady)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isLevelSteady) ScannerGlassPalette.EmeraldGlow
                                        else ScannerGlassPalette.AmberGlow
                                    )
                            )
                            Text(
                                text = when {
                                    autoCountdownSec > 0 ->
                                        if (isArabic) "التقاط تلقائي $autoCountdownSec..." else "Auto-Capture $autoCountdownSec..."
                                    activeMode == ScanMode.ID_CARD && idCardFrontPath == null ->
                                        if (isArabic) "صوّر الوجه الأمامي للبطاقة" else "Capture Front of ID"
                                    activeMode == ScanMode.ID_CARD && idCardFrontPath != null ->
                                        if (isArabic) "صوّر الوجه الخلفي للبطاقة" else "Capture Back of ID"
                                    isLevelSteady ->
                                        if (isArabic) "متوازن ✓" else "Level ✓"
                                    else ->
                                        if (isArabic) "ثبّت الهاتف" else "Hold Steady"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ScannerGlassPalette.TextPrimary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

                    // Auto-capture quick toggle pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .scannerGlassSurface(CircleShape, glowBorder = autoCaptureEnabled)
                                .clickable { autoCaptureEnabled = !autoCaptureEnabled }
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (autoCaptureEnabled) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isArabic) {
                                    if (autoCaptureEnabled) "التقاط تلقائي: مفعّل" else "التقاط تلقائي: يدوي"
                                } else {
                                    if (autoCaptureEnabled) "Auto-Capture: ON" else "Auto-Capture: OFF"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = ScannerGlassPalette.TextPrimary
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
                    // Mode Switcher Strip
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                .clickable(enabled = !isBusy && activeMode != ScanMode.QR_BARCODE) {
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
                                        painter = rememberAsyncImagePainter(File(batchFrames.last().rawImagePath)),
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
