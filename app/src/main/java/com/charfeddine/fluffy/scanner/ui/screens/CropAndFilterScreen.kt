package com.charfeddine.fluffy.scanner.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charfeddine.fluffy.scanner.data.model.DocumentQuad
import com.charfeddine.fluffy.scanner.data.model.PointNorm
import com.charfeddine.fluffy.scanner.data.model.ScannerFilterType
import com.charfeddine.fluffy.scanner.domain.ScannerViewModel
import com.charfeddine.fluffy.scanner.ui.theme.ScannerDarkLiquidBackdrop
import com.charfeddine.fluffy.scanner.ui.theme.ScannerGlassPalette
import com.charfeddine.fluffy.scanner.ui.theme.iosLiquidGlass
import com.charfeddine.fluffy.scanner.ui.theme.scannerGlassSurface
import com.charfeddine.fluffy.scanner.utils.DocumentImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.hypot
import kotlin.math.roundToInt

private enum class DragHandleTarget {
    TL, TR, BR, BL, TOP_MID, RIGHT_MID, BOTTOM_MID, LEFT_MID
}

@Composable
fun CropAndFilterScreen(
    scannerViewModel: ScannerViewModel,
    appendToDocId: String?,
    isArabic: Boolean,
    onBackToCamera: () -> Unit
) {
    BackHandler { onBackToCamera() }

    val batchFrames by scannerViewModel.batchFrames.collectAsState()
    val activeIdx by scannerViewModel.activeCropIndex.collectAsState()
    val isBusy by scannerViewModel.isBusy.collectAsState()

    val frame = batchFrames.getOrNull(activeIdx)
    if (frame == null) {
        LaunchedEffect(Unit) { onBackToCamera() }
        return
    }

    val uiScope = rememberCoroutineScope()
    var isCropPhase by remember(frame.id) { mutableStateOf(true) }
    var docTitle by remember { mutableStateOf("") }
    var showAdjustSliders by remember { mutableStateOf(false) }
    var isAutoFitting by remember(frame.id) { mutableStateOf(false) }

    var rawBitmap by remember(frame.id) { mutableStateOf<Bitmap?>(null) }
    var warpedBitmap by remember(frame.id) { mutableStateOf<Bitmap?>(null) }
    var enhancedBitmap by remember(frame.id) { mutableStateOf<Bitmap?>(null) }

    // Before/After split comparison fraction (0.0 .. 1.0)
    var splitComparisonX by remember { mutableFloatStateOf(0.55f) }

    LaunchedEffect(frame.id, frame.rawImagePath) {
        rawBitmap = withContext(Dispatchers.IO) {
            DocumentImageProcessor.decodeSampledBitmap(File(frame.rawImagePath), 1400)
        }
    }

    // Recompute warped + enhanced preview when moving into Enhance phase or tweaking filters
    LaunchedEffect(
        isCropPhase,
        rawBitmap,
        frame.detectedQuad,
        frame.filter,
        frame.brightness,
        frame.contrast,
        frame.sharpness,
        frame.rotationDeg
    ) {
        val raw = rawBitmap ?: return@LaunchedEffect
        if (!isCropPhase) {
            withContext(Dispatchers.Default) {
                val warped = DocumentImageProcessor.warpPerspective(raw, frame.detectedQuad, 1200)
                val enhanced = DocumentImageProcessor.applyEnhancements(
                    source = warped,
                    filter = frame.filter,
                    brightness = frame.brightness,
                    contrast = frame.contrast,
                    sharpness = frame.sharpness,
                    rotationDeg = frame.rotationDeg
                )
                val rotatedBefore = if (frame.rotationDeg % 360 != 0) {
                    DocumentImageProcessor.rotateBitmap(warped, frame.rotationDeg.toFloat())
                } else {
                    warped
                }
                withContext(Dispatchers.Main) {
                    warpedBitmap = rotatedBefore
                    enhancedBitmap = enhanced
                }
            }
        }
    }

    ScannerDarkLiquidBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToCamera,
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

                // Crop <-> Filters phase switcher pill
                Row(
                    modifier = Modifier
                        .scannerGlassSurface(CircleShape, glowBorder = true)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isCropPhase) ScannerGlassPalette.CyanGlow else Color.Transparent)
                            .clickable { isCropPhase = true }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (isArabic) "1. القص والمنظور" else "1. Crop",
                            color = if (isCropPhase) Color(0xFF04101B) else ScannerGlassPalette.TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (!isCropPhase) ScannerGlassPalette.CyanGlow else Color.Transparent)
                            .clickable { isCropPhase = false }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (isArabic) "2. الفلاتر والضبط" else "2. Enhance",
                            color = if (!isCropPhase) Color(0xFF04101B) else ScannerGlassPalette.TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = { scannerViewModel.removeBatchFrame(activeIdx) },
                    modifier = Modifier
                        .size(48.dp)
                        .scannerGlassSurface(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = if (isArabic) "حذف الصفحة" else "Remove Page",
                        tint = ScannerGlassPalette.RoseGlow
                    )
                }
            }

            // Batch page selector strip (when scanning multiple pages)
            if (batchFrames.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(batchFrames) { idx, _ ->
                        val selected = idx == activeIdx
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .scannerGlassSurface(CircleShape, glowBorder = selected)
                                .clickable { scannerViewModel.activeCropIndex.value = idx }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isArabic) "صفحة ${idx + 1}" else "Page ${idx + 1}",
                                color = if (selected) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Main Interactive Canvas Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .scannerGlassSurface(RoundedCornerShape(22.dp), glowBorder = true)
                    .clipToBounds()
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = rawBitmap
                if (bmp == null) {
                    CircularProgressIndicator(color = ScannerGlassPalette.CyanGlow)
                } else if (isCropPhase) {
                    InteractiveCropWithMagnifier(
                        bitmap = bmp,
                        quad = frame.detectedQuad,
                        onQuadChange = { newQuad -> scannerViewModel.updateFrameQuad(activeIdx, newQuad) }
                    )
                } else {
                    val beforeBmp = warpedBitmap
                    val afterBmp = enhancedBitmap
                    if (beforeBmp == null || afterBmp == null) {
                        CircularProgressIndicator(color = ScannerGlassPalette.CyanGlow)
                    } else {
                        BeforeAfterComparisonViewport(
                            beforeBitmap = beforeBmp,
                            afterBitmap = afterBmp,
                            splitFraction = splitComparisonX,
                            isArabic = isArabic,
                            onSplitFractionChange = { splitComparisonX = it }
                        )
                    }
                }
            }

            // Bottom Controls Panel depending on Crop vs Enhance Phase
            if (isCropPhase) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("crop_auto_fit_btn")
                            .iosLiquidGlass(
                                shape = RoundedCornerShape(16.dp),
                                glowAccent = true,
                                fillAlpha = 0.72f
                            )
                            .clickable(enabled = !isAutoFitting) {
                                val bmp = rawBitmap ?: return@clickable
                                isAutoFitting = true
                                uiScope.launch {
                                    try {
                                        val auto = withContext(Dispatchers.Default) {
                                            DocumentImageProcessor.detectQuadFromBitmap(bmp)
                                        }
                                        scannerViewModel.updateFrameQuad(activeIdx, auto)
                                    } finally {
                                        isAutoFitting = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isAutoFitting) {
                                CircularProgressIndicator(
                                    color = ScannerGlassPalette.CyanGlow,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Crop,
                                    contentDescription = if (isArabic) "تحديد تلقائي" else "Auto-Fit",
                                    tint = ScannerGlassPalette.CyanGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = if (isArabic) "تحديد تلقائي" else "Auto-Fit",
                                color = ScannerGlassPalette.TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("crop_full_frame_btn")
                            .iosLiquidGlass(RoundedCornerShape(16.dp), fillAlpha = 0.62f)
                            .clickable { scannerViewModel.updateFrameQuad(activeIdx, DocumentQuad.FULL_FRAME) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "كامل الإطار" else "Full Frame",
                            color = ScannerGlassPalette.TextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = { isCropPhase = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("crop_apply_next_btn")
                    ) {
                        Text(
                            text = if (isArabic) "تصحيح المنظور →" else "Warp & Filter →",
                            color = Color(0xFF04101B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                // Enhance & Filter Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scannerGlassSurface(RoundedCornerShape(22.dp), glowBorder = true)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Filter Preset Pills
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ScannerFilterType.entries) { filter ->
                            val selected = frame.filter == filter
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("filter_chip_${filter.id}")
                                    .scannerGlassSurface(
                                        RoundedCornerShape(14.dp),
                                        glowBorder = selected,
                                        fillAlpha = if (selected) 0.92f else 0.50f
                                    )
                                    .clickable {
                                        scannerViewModel.updateFrameEnhancement(
                                            activeIdx, filter, frame.brightness, frame.contrast, frame.sharpness, frame.rotationDeg
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isArabic) filter.labelAr else filter.labelEn,
                                    color = if (selected) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextPrimary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Rotate 90 L/R, Auto-Deskew & Sliders toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    scannerViewModel.updateFrameEnhancement(
                                        activeIdx, frame.filter, frame.brightness, frame.contrast, frame.sharpness, (frame.rotationDeg - 90) % 360
                                    )
                                },
                                modifier = Modifier.size(44.dp).scannerGlassSurface(CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate Left", tint = ScannerGlassPalette.TextPrimary)
                            }
                            IconButton(
                                onClick = {
                                    scannerViewModel.updateFrameEnhancement(
                                        activeIdx, frame.filter, frame.brightness, frame.contrast, frame.sharpness, (frame.rotationDeg + 90) % 360
                                    )
                                },
                                modifier = Modifier.size(44.dp).scannerGlassSurface(CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate Right", tint = ScannerGlassPalette.TextPrimary)
                            }
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .scannerGlassSurface(CircleShape)
                                    .clickable {
                                        warpedBitmap?.let { wb ->
                                            val angle = DocumentImageProcessor.estimateAutoDeskewAngle(wb)
                                            scannerViewModel.updateFrameEnhancement(
                                                activeIdx,
                                                frame.filter,
                                                frame.brightness,
                                                frame.contrast,
                                                frame.sharpness,
                                                frame.rotationDeg + angle.roundToInt()
                                            )
                                        }
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = ScannerGlassPalette.VioletGlow, modifier = Modifier.size(16.dp))
                                    Text(if (isArabic) "استقامة" else "Deskew", color = ScannerGlassPalette.TextPrimary, fontSize = 11.5.sp)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .scannerGlassSurface(CircleShape, glowBorder = showAdjustSliders)
                                .clickable { showAdjustSliders = !showAdjustSliders }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = ScannerGlassPalette.CyanGlow, modifier = Modifier.size(16.dp))
                                Text(if (isArabic) "السطوع والتباين" else "Adjust", color = ScannerGlassPalette.TextPrimary, fontSize = 11.5.sp)
                            }
                        }
                    }

                    if (showAdjustSliders) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isArabic) "السطوع: ${frame.brightness.toInt()}" else "Brightness: ${frame.brightness.toInt()}",
                                color = ScannerGlassPalette.TextSecondary, fontSize = 11.sp
                            )
                            Slider(
                                value = frame.brightness,
                                onValueChange = {
                                    scannerViewModel.updateFrameEnhancement(activeIdx, frame.filter, it, frame.contrast, frame.sharpness, frame.rotationDeg)
                                },
                                valueRange = -60f..60f,
                                colors = SliderDefaults.colors(thumbColor = ScannerGlassPalette.CyanGlow, activeTrackColor = ScannerGlassPalette.CyanGlow)
                            )
                            Text(
                                text = if (isArabic) "التباين: ${String.format("%.2f", frame.contrast)} • الحدة: ${(frame.sharpness * 100).toInt()}%" else "Contrast: ${String.format("%.2f", frame.contrast)} • Sharpness: ${(frame.sharpness * 100).toInt()}%",
                                color = ScannerGlassPalette.TextSecondary, fontSize = 11.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Slider(
                                    value = frame.contrast,
                                    onValueChange = {
                                        scannerViewModel.updateFrameEnhancement(activeIdx, frame.filter, frame.brightness, it, frame.sharpness, frame.rotationDeg)
                                    },
                                    valueRange = 0.6f..1.8f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(thumbColor = ScannerGlassPalette.VioletGlow, activeTrackColor = ScannerGlassPalette.VioletGlow)
                                )
                                Slider(
                                    value = frame.sharpness,
                                    onValueChange = {
                                        scannerViewModel.updateFrameEnhancement(activeIdx, frame.filter, frame.brightness, frame.contrast, it, frame.rotationDeg)
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(thumbColor = ScannerGlassPalette.EmeraldGlow, activeTrackColor = ScannerGlassPalette.EmeraldGlow)
                                )
                            }
                        }
                    }

                    // Document Title + Save Document CTA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (appendToDocId == null) {
                            OutlinedTextField(
                                value = docTitle,
                                onValueChange = { docTitle = it },
                                placeholder = {
                                    Text(
                                        if (isArabic) "اسم المستند..." else "Document title...",
                                        fontSize = 13.sp,
                                        color = ScannerGlassPalette.TextSecondary
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ScannerGlassPalette.CyanGlow,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                                    focusedTextColor = ScannerGlassPalette.TextPrimary,
                                    unfocusedTextColor = ScannerGlassPalette.TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Button(
                            onClick = { scannerViewModel.commitBatchToDocument(docTitle, appendToDocId, isArabic) },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("crop_save_document_btn")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(color = Color(0xFF04101B), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF04101B))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "حفظ (${batchFrames.size})" else "Save (${batchFrames.size})",
                                    color = Color(0xFF04101B),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveCropWithMagnifier(
    bitmap: Bitmap,
    quad: DocumentQuad,
    onQuadChange: (DocumentQuad) -> Unit
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var activeHandle by remember { mutableStateOf<DragHandleTarget?>(null) }
    var activeAnchorScreenPos by remember { mutableStateOf<Offset?>(null) }
    var localQuad by remember(bitmap) { mutableStateOf(quad) }
    val density = LocalDensity.current

    // Sync external quad updates (e.g., Auto-Fit or Full Frame) only when not actively dragging
    LaunchedEffect(quad) {
        if (activeHandle == null) {
            localQuad = quad
        }
    }

    // Smoothly animate 4 corners when Auto-Fit / Full Frame runs, while snapping with 0 lag during drag
    val cornerAnimSpec: AnimationSpec<Offset> = if (activeHandle != null) {
        snap()
    } else {
        spring(dampingRatio = 0.78f, stiffness = 360f)
    }
    val animTl by animateOffsetAsState(Offset(localQuad.topLeft.x, localQuad.topLeft.y), cornerAnimSpec, label = "tl")
    val animTr by animateOffsetAsState(Offset(localQuad.topRight.x, localQuad.topRight.y), cornerAnimSpec, label = "tr")
    val animBr by animateOffsetAsState(Offset(localQuad.bottomRight.x, localQuad.bottomRight.y), cornerAnimSpec, label = "br")
    val animBl by animateOffsetAsState(Offset(localQuad.bottomLeft.x, localQuad.bottomLeft.y), cornerAnimSpec, label = "bl")

    val displayedQuad = DocumentQuad(
        topLeft = PointNorm(animTl.x, animTl.y),
        topRight = PointNorm(animTr.x, animTr.y),
        bottomRight = PointNorm(animBr.x, animBr.y),
        bottomLeft = PointNorm(animBl.x, animBl.y)
    )

    val latestOnQuadCommit by rememberUpdatedState(onQuadChange)
    val latestLocalQuad by rememberUpdatedState(localQuad)
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    val edgePadPx = with(density) { 14.dp.toPx() }
    val bw = bitmap.width.toFloat().coerceAtLeast(1f)
    val bh = bitmap.height.toFloat().coerceAtLeast(1f)
    val cw = boxSize.width.toFloat().coerceAtLeast(1f)
    val ch = boxSize.height.toFloat().coerceAtLeast(1f)
    val availW = (cw - edgePadPx * 2f).coerceAtLeast(1f)
    val availH = (ch - edgePadPx * 2f).coerceAtLeast(1f)
    val scale = minOf(availW / bw, availH / bh)
    val drawW = bw * scale
    val drawH = bh * scale
    val offsetX = (cw - drawW) / 2f
    val offsetY = (ch - drawH) / 2f

    fun normToScreen(p: PointNorm): Offset = Offset(
        x = offsetX + p.x * drawW,
        y = offsetY + p.y * drawH
    )

    fun screenToNorm(off: Offset): PointNorm = PointNorm(
        x = ((off.x - offsetX) / drawW).coerceIn(0f, 1f),
        y = ((off.y - offsetY) / drawH).coerceIn(0f, 1f)
    )

    fun screenToNormUnclamped(off: Offset): PointNorm = PointNorm(
        x = (off.x - offsetX) / drawW,
        y = (off.y - offsetY) / drawH
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Crop preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmap, boxSize) {
                    if (boxSize.width <= 0 || boxSize.height <= 0) return@pointerInput
                    val cornerHitRadius = with(density) { 52.dp.toPx() }
                    val midHitRadius = with(density) { 44.dp.toPx() }
                    val minGap = 0.04f

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val q0 = latestLocalQuad
                        val tl0 = normToScreen(q0.topLeft)
                        val tr0 = normToScreen(q0.topRight)
                        val br0 = normToScreen(q0.bottomRight)
                        val bl0 = normToScreen(q0.bottomLeft)

                        val cornerHandles = listOf(
                            DragHandleTarget.TL to tl0,
                            DragHandleTarget.TR to tr0,
                            DragHandleTarget.BR to br0,
                            DragHandleTarget.BL to bl0
                        )
                        val midHandles = listOf(
                            DragHandleTarget.TOP_MID to Offset((tl0.x + tr0.x) / 2f, (tl0.y + tr0.y) / 2f),
                            DragHandleTarget.RIGHT_MID to Offset((tr0.x + br0.x) / 2f, (tr0.y + br0.y) / 2f),
                            DragHandleTarget.BOTTOM_MID to Offset((bl0.x + br0.x) / 2f, (bl0.y + br0.y) / 2f),
                            DragHandleTarget.LEFT_MID to Offset((tl0.x + bl0.x) / 2f, (tl0.y + bl0.y) / 2f)
                        )

                        val bestCorner = cornerHandles.minByOrNull { (_, p) ->
                            hypot((p.x - down.position.x).toDouble(), (p.y - down.position.y).toDouble())
                        }
                        val bestCornerDist = bestCorner?.second?.let { p ->
                            hypot((p.x - down.position.x).toDouble(), (p.y - down.position.y).toDouble())
                        } ?: Double.MAX_VALUE

                        val bestMid = midHandles.minByOrNull { (_, p) ->
                            hypot((p.x - down.position.x).toDouble(), (p.y - down.position.y).toDouble())
                        }
                        val bestMidDist = bestMid?.second?.let { p ->
                            hypot((p.x - down.position.x).toDouble(), (p.y - down.position.y).toDouble())
                        } ?: Double.MAX_VALUE

                        // Pick midpoint if directly targeted; otherwise prioritize corners
                        val picked: Pair<DragHandleTarget, Offset> = when {
                            bestMid != null && bestMidDist <= midHitRadius && bestMidDist < bestCornerDist * 0.65 -> bestMid
                            bestCorner != null && bestCornerDist <= cornerHitRadius -> bestCorner
                            bestMid != null && bestMidDist <= midHitRadius -> bestMid
                            else -> return@awaitEachGesture
                        }

                        down.consume()
                        val target = picked.first
                        val grabOffset = picked.second - down.position
                        val startNorm = screenToNormUnclamped(down.position)

                        activeHandle = target
                        activeAnchorScreenPos = picked.second

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: event.changes.firstOrNull()
                                    ?: break

                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }

                                change.consume()
                                val movedPos = change.position + grabOffset
                                val targetNorm = screenToNorm(movedPos)
                                val currUnclamped = screenToNormUnclamped(change.position)
                                val totalDx = currUnclamped.x - startNorm.x
                                val totalDy = currUnclamped.y - startNorm.y

                                val updated = when (target) {
                                    DragHandleTarget.TL -> {
                                        val maxX = (q0.topRight.x - minGap).coerceAtLeast(0f)
                                        val maxY = (q0.bottomLeft.y - minGap).coerceAtLeast(0f)
                                        val pt = PointNorm(
                                            x = targetNorm.x.coerceIn(0f, maxX),
                                            y = targetNorm.y.coerceIn(0f, maxY)
                                        )
                                        q0.copy(topLeft = pt)
                                    }

                                    DragHandleTarget.TR -> {
                                        val minX = (q0.topLeft.x + minGap).coerceAtMost(1f)
                                        val maxY = (q0.bottomRight.y - minGap).coerceAtLeast(0f)
                                        val pt = PointNorm(
                                            x = targetNorm.x.coerceIn(minX, 1f),
                                            y = targetNorm.y.coerceIn(0f, maxY)
                                        )
                                        q0.copy(topRight = pt)
                                    }

                                    DragHandleTarget.BR -> {
                                        val minX = (q0.bottomLeft.x + minGap).coerceAtMost(1f)
                                        val minY = (q0.topRight.y + minGap).coerceAtMost(1f)
                                        val pt = PointNorm(
                                            x = targetNorm.x.coerceIn(minX, 1f),
                                            y = targetNorm.y.coerceIn(minY, 1f)
                                        )
                                        q0.copy(bottomRight = pt)
                                    }

                                    DragHandleTarget.BL -> {
                                        val maxX = (q0.bottomRight.x - minGap).coerceAtLeast(0f)
                                        val minY = (q0.topLeft.y + minGap).coerceAtMost(1f)
                                        val pt = PointNorm(
                                            x = targetNorm.x.coerceIn(0f, maxX),
                                            y = targetNorm.y.coerceIn(minY, 1f)
                                        )
                                        q0.copy(bottomLeft = pt)
                                    }

                                    DragHandleTarget.TOP_MID -> {
                                        val minDy = maxOf(-q0.topLeft.y, -q0.topRight.y)
                                        val maxDy = minOf(
                                            q0.bottomLeft.y - minGap - q0.topLeft.y,
                                            q0.bottomRight.y - minGap - q0.topRight.y
                                        )
                                        val dy = totalDy.coerceIn(minOf(minDy, maxDy), maxOf(minDy, maxDy))
                                        q0.copy(
                                            topLeft = q0.topLeft.copy(y = (q0.topLeft.y + dy).coerceIn(0f, 1f)),
                                            topRight = q0.topRight.copy(y = (q0.topRight.y + dy).coerceIn(0f, 1f))
                                        )
                                    }

                                    DragHandleTarget.BOTTOM_MID -> {
                                        val minDy = maxOf(
                                            q0.topLeft.y + minGap - q0.bottomLeft.y,
                                            q0.topRight.y + minGap - q0.bottomRight.y
                                        )
                                        val maxDy = minOf(1f - q0.bottomLeft.y, 1f - q0.bottomRight.y)
                                        val dy = totalDy.coerceIn(minOf(minDy, maxDy), maxOf(minDy, maxDy))
                                        q0.copy(
                                            bottomLeft = q0.bottomLeft.copy(y = (q0.bottomLeft.y + dy).coerceIn(0f, 1f)),
                                            bottomRight = q0.bottomRight.copy(y = (q0.bottomRight.y + dy).coerceIn(0f, 1f))
                                        )
                                    }

                                    DragHandleTarget.LEFT_MID -> {
                                        val minDx = maxOf(-q0.topLeft.x, -q0.bottomLeft.x)
                                        val maxDx = minOf(
                                            q0.topRight.x - minGap - q0.topLeft.x,
                                            q0.bottomRight.x - minGap - q0.bottomLeft.x
                                        )
                                        val dx = totalDx.coerceIn(minOf(minDx, maxDx), maxOf(minDx, maxDx))
                                        q0.copy(
                                            topLeft = q0.topLeft.copy(x = (q0.topLeft.x + dx).coerceIn(0f, 1f)),
                                            bottomLeft = q0.bottomLeft.copy(x = (q0.bottomLeft.x + dx).coerceIn(0f, 1f))
                                        )
                                    }

                                    DragHandleTarget.RIGHT_MID -> {
                                        val minDx = maxOf(
                                            q0.topLeft.x + minGap - q0.topRight.x,
                                            q0.bottomLeft.x + minGap - q0.bottomRight.x
                                        )
                                        val maxDx = minOf(1f - q0.topRight.x, 1f - q0.bottomRight.x)
                                        val dx = totalDx.coerceIn(minOf(minDx, maxDx), maxOf(minDx, maxDx))
                                        q0.copy(
                                            topRight = q0.topRight.copy(x = (q0.topRight.x + dx).coerceIn(0f, 1f)),
                                            bottomRight = q0.bottomRight.copy(x = (q0.bottomRight.x + dx).coerceIn(0f, 1f))
                                        )
                                    }
                                }

                                localQuad = updated
                                activeAnchorScreenPos = when (target) {
                                    DragHandleTarget.TL -> normToScreen(updated.topLeft)
                                    DragHandleTarget.TR -> normToScreen(updated.topRight)
                                    DragHandleTarget.BR -> normToScreen(updated.bottomRight)
                                    DragHandleTarget.BL -> normToScreen(updated.bottomLeft)
                                    DragHandleTarget.TOP_MID -> {
                                        val a = normToScreen(updated.topLeft)
                                        val b = normToScreen(updated.topRight)
                                        Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                                    }
                                    DragHandleTarget.RIGHT_MID -> {
                                        val a = normToScreen(updated.topRight)
                                        val b = normToScreen(updated.bottomRight)
                                        Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                                    }
                                    DragHandleTarget.BOTTOM_MID -> {
                                        val a = normToScreen(updated.bottomLeft)
                                        val b = normToScreen(updated.bottomRight)
                                        Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                                    }
                                    DragHandleTarget.LEFT_MID -> {
                                        val a = normToScreen(updated.topLeft)
                                        val b = normToScreen(updated.bottomLeft)
                                        Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                                    }
                                }
                            }
                        } finally {
                            activeHandle = null
                            activeAnchorScreenPos = null
                            latestOnQuadCommit(latestLocalQuad)
                        }
                    }
                }
        ) {
            val tl = normToScreen(displayedQuad.topLeft)
            val tr = normToScreen(displayedQuad.topRight)
            val br = normToScreen(displayedQuad.bottomRight)
            val bl = normToScreen(displayedQuad.bottomLeft)

            val path = Path().apply {
                moveTo(tl.x, tl.y)
                lineTo(tr.x, tr.y)
                lineTo(br.x, br.y)
                lineTo(bl.x, bl.y)
                close()
            }
            drawPath(path, ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.12f))
            drawPath(
                path,
                brush = Brush.linearGradient(
                    listOf(ScannerGlassPalette.EdgeOverlayCyan, ScannerGlassPalette.EdgeOverlayPurple)
                ),
                style = Stroke(width = 2.6.dp.toPx())
            )

            // Corner handles with expanded active glow
            listOf(
                DragHandleTarget.TL to tl,
                DragHandleTarget.TR to tr,
                DragHandleTarget.BR to br,
                DragHandleTarget.BL to bl
            ).forEach { (hTarget, pt) ->
                val isSelected = activeHandle == hTarget
                drawCircle(
                    color = ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = if (isSelected) 0.55f else 0.30f),
                    radius = (if (isSelected) 20.dp else 14.dp).toPx(),
                    center = pt
                )
                drawCircle(
                    color = ScannerGlassPalette.EdgeOverlayCyan,
                    radius = (if (isSelected) 12.dp else 9.5.dp).toPx(),
                    center = pt
                )
                drawCircle(Color.White, radius = (if (isSelected) 5.5.dp else 4.5.dp).toPx(), center = pt)
            }

            // Edge mid-point handles
            listOf(
                DragHandleTarget.TOP_MID to Offset((tl.x + tr.x) / 2f, (tl.y + tr.y) / 2f),
                DragHandleTarget.RIGHT_MID to Offset((tr.x + br.x) / 2f, (tr.y + br.y) / 2f),
                DragHandleTarget.BOTTOM_MID to Offset((bl.x + br.x) / 2f, (bl.y + br.y) / 2f),
                DragHandleTarget.LEFT_MID to Offset((tl.x + bl.x) / 2f, (tl.y + bl.y) / 2f)
            ).forEach { (hTarget, mid) ->
                val isSelected = activeHandle == hTarget
                drawCircle(
                    color = ScannerGlassPalette.EdgeOverlayPurple,
                    radius = (if (isSelected) 9.5.dp else 7.dp).toPx(),
                    center = mid
                )
                drawCircle(Color.White, radius = (if (isSelected) 4.dp else 3.dp).toPx(), center = mid)
            }
        }

        // Zero-Allocation Precision Magnifier Loupe (tracks all 4 corners + 4 edge mid-handles)
        val activePos = activeAnchorScreenPos
        if (activeHandle != null && activePos != null && boxSize.width > 0 && boxSize.height > 0) {
            val norm = screenToNorm(activePos)
            val loupeSizeDp = 112.dp
            val loupePx = with(density) { loupeSizeDp.toPx() }
            val cropRegionSize = (minOf(bitmap.width, bitmap.height) * 0.14f)
                .roundToInt()
                .coerceIn(56, 260)
                .toFloat()
            val halfCrop = cropRegionSize * 0.5f

            val cx = norm.x * bitmap.width
            val cy = norm.y * bitmap.height
            val desiredLeft = cx - halfCrop
            val desiredTop = cy - halfCrop

            val srcLeft = desiredLeft.roundToInt().coerceIn(0, (bitmap.width - 1).coerceAtLeast(0))
            val srcTop = desiredTop.roundToInt().coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
            val srcRight = (cx + halfCrop).roundToInt().coerceIn(srcLeft + 1, bitmap.width)
            val srcBottom = (cy + halfCrop).roundToInt().coerceIn(srcTop + 1, bitmap.height)
            val srcW = (srcRight - srcLeft).coerceAtLeast(1)
            val srcH = (srcBottom - srcTop).coerceAtLeast(1)

            val gapAbove = with(density) { 44.dp.toPx() }
            val preferAboveY = activePos.y - loupePx - gapAbove
            val loupeY = if (preferAboveY >= 8f) {
                preferAboveY
            } else {
                (activePos.y + gapAbove).coerceAtMost((boxSize.height - loupePx - 8f).coerceAtLeast(8f))
            }
            val loupeX = (activePos.x - loupePx / 2f).coerceIn(8f, (boxSize.width - loupePx - 8f).coerceAtLeast(8f))

            Box(
                modifier = Modifier
                    .offset { IntOffset(loupeX.roundToInt(), loupeY.roundToInt()) }
                    .size(loupeSizeDp)
                    .shadow(16.dp, CircleShape, ambientColor = ScannerGlassPalette.CyanGlow, spotColor = ScannerGlassPalette.VioletGlow)
                    .clip(CircleShape)
                    .background(Color(0xFF060A14))
                    .border(
                        width = 2.5.dp,
                        brush = Brush.linearGradient(
                            listOf(ScannerGlassPalette.EdgeOverlayCyan, ScannerGlassPalette.EdgeOverlayPurple)
                        ),
                        shape = CircleShape
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val zoomScaleX = size.width / cropRegionSize
                    val zoomScaleY = size.height / cropRegionSize
                    val dstLeft = ((srcLeft - desiredLeft) * zoomScaleX).roundToInt()
                    val dstTop = ((srcTop - desiredTop) * zoomScaleY).roundToInt()
                    val dstW = (srcW * zoomScaleX).roundToInt().coerceAtLeast(1)
                    val dstH = (srcH * zoomScaleY).roundToInt().coerceAtLeast(1)

                    drawImage(
                        image = imageBitmap,
                        srcOffset = IntOffset(srcLeft, srcTop),
                        srcSize = IntSize(srcW, srcH),
                        dstOffset = IntOffset(dstLeft, dstTop),
                        dstSize = IntSize(dstW, dstH)
                    )
                    drawLine(
                        ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.85f),
                        Offset(size.width / 2f, 0f),
                        Offset(size.width / 2f, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.85f),
                        Offset(0f, size.height / 2f),
                        Offset(size.width, size.height / 2f),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.5.dp.toPx(),
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = ScannerGlassPalette.EdgeOverlayCyan,
                        radius = 2.5.dp.toPx(),
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "2.5x",
                        color = ScannerGlassPalette.EdgeOverlayCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BeforeAfterComparisonViewport(
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap,
    splitFraction: Float,
    isArabic: Boolean,
    onSplitFractionChange: (Float) -> Unit
) {
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val beforeImg = remember(beforeBitmap) { beforeBitmap.asImageBitmap() }
    val afterImg = remember(afterBitmap) { afterBitmap.asImageBitmap() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .pointerInput(viewportSize) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val w = viewportSize.width.coerceAtLeast(1).toFloat()
                    onSplitFractionChange((change.position.x / w).coerceIn(0.08f, 0.92f))
                }
            }
    ) {
        // Right side: Enhanced (After)
        Image(
            bitmap = afterImg,
            contentDescription = "Enhanced preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Left clipped region: Original (Before)
        val clipWidthDp = with(density) { (viewportSize.width * splitFraction).toDp() }
        Box(
            modifier = Modifier
                .width(clipWidthDp)
                .fillMaxSize()
                .clipToBounds()
        ) {
            Image(
                bitmap = beforeImg,
                contentDescription = "Original preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(with(density) { viewportSize.width.toDp() })
                    .fillMaxSize()
            )
        }

        // Split Divider Line + Before/After labels
        Canvas(modifier = Modifier.fillMaxSize()) {
            val splitX = size.width * splitFraction
            drawLine(
                color = ScannerGlassPalette.CyanGlow,
                start = Offset(splitX, 0f),
                end = Offset(splitX, size.height),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(ScannerGlassPalette.CyanGlow, radius = 14.dp.toPx(), center = Offset(splitX, size.height / 2f))
            drawCircle(Color(0xFF04101B), radius = 6.dp.toPx(), center = Offset(splitX, size.height / 2f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(if (isArabic) "قبل" else "Before", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ScannerGlassPalette.CyanGlow.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(if (isArabic) "بعد" else "After", color = Color(0xFF04101B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
