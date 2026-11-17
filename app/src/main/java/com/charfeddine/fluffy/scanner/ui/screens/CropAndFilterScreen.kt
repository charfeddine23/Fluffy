package com.charfeddine.fluffy.scanner.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import com.charfeddine.fluffy.scanner.ui.theme.scannerGlassSurface
import com.charfeddine.fluffy.scanner.utils.DocumentImageProcessor
import kotlinx.coroutines.Dispatchers
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

    var isCropPhase by remember(frame.id) { mutableStateOf(true) }
    var docTitle by remember { mutableStateOf("") }
    var showAdjustSliders by remember { mutableStateOf(false) }

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
                            .height(48.dp)
                            .scannerGlassSurface(RoundedCornerShape(14.dp))
                            .clickable {
                                rawBitmap?.let {
                                    val auto = DocumentImageProcessor.detectQuadFromBitmap(it)
                                    scannerViewModel.updateFrameQuad(activeIdx, auto)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Default.Crop, contentDescription = null, tint = ScannerGlassPalette.CyanGlow, modifier = Modifier.size(18.dp))
                            Text(if (isArabic) "تحديد تلقائي" else "Auto-Fit", color = ScannerGlassPalette.TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .scannerGlassSurface(RoundedCornerShape(14.dp))
                            .clickable { scannerViewModel.updateFrameQuad(activeIdx, DocumentQuad.FULL_FRAME) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isArabic) "كامل الإطار" else "Full Frame", color = ScannerGlassPalette.TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { isCropPhase = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
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
    var fingerOverlayPos by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
    ) {
        val bw = bitmap.width.toFloat().coerceAtLeast(1f)
        val bh = bitmap.height.toFloat().coerceAtLeast(1f)
        val cw = boxSize.width.toFloat().coerceAtLeast(1f)
        val ch = boxSize.height.toFloat().coerceAtLeast(1f)
        val scale = minOf(cw / bw, ch / bh)
        val drawW = bw * scale
        val drawH = bh * scale
        val offsetX = (cw - drawW) / 2f
        val offsetY = (ch - drawH) / 2f

        fun normToScreen(p: PointNorm) = Offset(offsetX + p.x * drawW, offsetY + p.y * drawH)
        fun screenToNorm(off: Offset) = PointNorm(
            x = ((off.x - offsetX) / drawW).coerceIn(0.02f, 0.98f),
            y = ((off.y - offsetY) / drawH).coerceIn(0.02f, 0.98f)
        )

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Crop preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(quad, boxSize) {
                    detectDragGestures(
                        onDragStart = { start ->
                            val tl = normToScreen(quad.topLeft)
                            val tr = normToScreen(quad.topRight)
                            val br = normToScreen(quad.bottomRight)
                            val bl = normToScreen(quad.bottomLeft)
                            val handles = listOf(
                                DragHandleTarget.TL to tl,
                                DragHandleTarget.TR to tr,
                                DragHandleTarget.BR to br,
                                DragHandleTarget.BL to bl,
                                DragHandleTarget.TOP_MID to Offset((tl.x + tr.x) / 2f, (tl.y + tr.y) / 2f),
                                DragHandleTarget.RIGHT_MID to Offset((tr.x + br.x) / 2f, (tr.y + br.y) / 2f),
                                DragHandleTarget.BOTTOM_MID to Offset((bl.x + br.x) / 2f, (bl.y + br.y) / 2f),
                                DragHandleTarget.LEFT_MID to Offset((tl.x + bl.x) / 2f, (tl.y + bl.y) / 2f)
                            )
                            val hitRadius = with(density) { 40.dp.toPx() }
                            val nearest = handles.minByOrNull { hypot((it.second.x - start.x).toDouble(), (it.second.y - start.y).toDouble()) }
                            if (nearest != null && hypot((nearest.second.x - start.x).toDouble(), (nearest.second.y - start.y).toDouble()) <= hitRadius) {
                                activeHandle = nearest.first
                                fingerOverlayPos = start
                            }
                        },
                        onDrag = { change, dragAmount ->
                            val target = activeHandle ?: return@detectDragGestures
                            change.consume()
                            val pt = screenToNorm(change.position)
                            val dx = dragAmount.x / drawW
                            val dy = dragAmount.y / drawH
                            fingerOverlayPos = change.position
                            val updated = when (target) {
                                DragHandleTarget.TL -> quad.copy(topLeft = pt)
                                DragHandleTarget.TR -> quad.copy(topRight = pt)
                                DragHandleTarget.BR -> quad.copy(bottomRight = pt)
                                DragHandleTarget.BL -> quad.copy(bottomLeft = pt)
                                DragHandleTarget.TOP_MID -> quad.copy(
                                    topLeft = quad.topLeft.copy(y = (quad.topLeft.y + dy).coerceIn(0.02f, 0.90f)),
                                    topRight = quad.topRight.copy(y = (quad.topRight.y + dy).coerceIn(0.02f, 0.90f))
                                )
                                DragHandleTarget.BOTTOM_MID -> quad.copy(
                                    bottomLeft = quad.bottomLeft.copy(y = (quad.bottomLeft.y + dy).coerceIn(0.10f, 0.98f)),
                                    bottomRight = quad.bottomRight.copy(y = (quad.bottomRight.y + dy).coerceIn(0.10f, 0.98f))
                                )
                                DragHandleTarget.LEFT_MID -> quad.copy(
                                    topLeft = quad.topLeft.copy(x = (quad.topLeft.x + dx).coerceIn(0.02f, 0.90f)),
                                    bottomLeft = quad.bottomLeft.copy(x = (quad.bottomLeft.x + dx).coerceIn(0.02f, 0.90f))
                                )
                                DragHandleTarget.RIGHT_MID -> quad.copy(
                                    topRight = quad.topRight.copy(x = (quad.topRight.x + dx).coerceIn(0.10f, 0.98f)),
                                    bottomRight = quad.bottomRight.copy(x = (quad.bottomRight.x + dx).coerceIn(0.10f, 0.98f))
                                )
                            }
                            onQuadChange(updated)
                        },
                        onDragEnd = {
                            activeHandle = null
                            fingerOverlayPos = null
                        },
                        onDragCancel = {
                            activeHandle = null
                            fingerOverlayPos = null
                        }
                    )
                }
        ) {
            val tl = normToScreen(quad.topLeft)
            val tr = normToScreen(quad.topRight)
            val br = normToScreen(quad.bottomRight)
            val bl = normToScreen(quad.bottomLeft)

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

            // Corner handles
            listOf(tl, tr, br, bl).forEach { pt ->
                drawCircle(ScannerGlassPalette.EdgeOverlayCyan.copy(alpha = 0.35f), radius = 14.dp.toPx(), center = pt)
                drawCircle(ScannerGlassPalette.EdgeOverlayCyan, radius = 9.5.dp.toPx(), center = pt)
                drawCircle(Color.White, radius = 4.5.dp.toPx(), center = pt)
            }
            // Edge mid-point handles
            listOf(
                Offset((tl.x + tr.x) / 2f, (tl.y + tr.y) / 2f),
                Offset((tr.x + br.x) / 2f, (tr.y + br.y) / 2f),
                Offset((bl.x + br.x) / 2f, (bl.y + br.y) / 2f),
                Offset((tl.x + bl.x) / 2f, (tl.y + bl.y) / 2f)
            ).forEach { mid ->
                drawCircle(ScannerGlassPalette.EdgeOverlayPurple, radius = 7.dp.toPx(), center = mid)
                drawCircle(Color.White, radius = 3.dp.toPx(), center = mid)
            }
        }

        // Real-Time Zoom Magnifier Loupe above user's finger during drag
        val activePos = fingerOverlayPos
        if (activeHandle != null && activePos != null) {
            val norm = screenToNorm(activePos)
            val loupePx = with(density) { 104.dp.toPx() }
            val cropRegionSize = (minOf(bitmap.width, bitmap.height) * 0.16f).roundToInt().coerceIn(60, 260)
            val cx = (norm.x * bitmap.width).roundToInt()
            val cy = (norm.y * bitmap.height).roundToInt()
            val srcLeft = (cx - cropRegionSize / 2).coerceIn(0, (bitmap.width - cropRegionSize).coerceAtLeast(0))
            val srcTop = (cy - cropRegionSize / 2).coerceIn(0, (bitmap.height - cropRegionSize).coerceAtLeast(0))
            val safeW = minOf(cropRegionSize, bitmap.width - srcLeft).coerceAtLeast(1)
            val safeH = minOf(cropRegionSize, bitmap.height - srcTop).coerceAtLeast(1)

            val loupeBmp = remember(srcLeft, srcTop, safeW, safeH, bitmap) {
                Bitmap.createBitmap(bitmap, srcLeft, srcTop, safeW, safeH)
            }
            val loupeX = (activePos.x - loupePx / 2f).coerceIn(8f, (boxSize.width - loupePx - 8f).coerceAtLeast(8f))
            val loupeY = (activePos.y - loupePx - with(density) { 36.dp.toPx() }).coerceAtLeast(8f)

            Box(
                modifier = Modifier
                    .offset { IntOffset(loupeX.roundToInt(), loupeY.roundToInt()) }
                    .size(104.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, ScannerGlassPalette.CyanGlow, CircleShape)
            ) {
                Image(
                    bitmap = loupeBmp.asImageBitmap(),
                    contentDescription = "Zoom Loupe",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        ScannerGlassPalette.CyanGlow.copy(alpha = 0.75f),
                        Offset(size.width / 2f, 0f),
                        Offset(size.width / 2f, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        ScannerGlassPalette.CyanGlow.copy(alpha = 0.75f),
                        Offset(0f, size.height / 2f),
                        Offset(size.width, size.height / 2f),
                        strokeWidth = 1.dp.toPx()
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
            bitmap = afterBitmap.asImageBitmap(),
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
                bitmap = beforeBitmap.asImageBitmap(),
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
