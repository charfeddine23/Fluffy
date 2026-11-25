package com.charfeddine.fluffy.scanner.ui.screens

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charfeddine.fluffy.scanner.data.model.DocumentQuad
import com.charfeddine.fluffy.scanner.data.model.IdCardPrintMode
import com.charfeddine.fluffy.scanner.data.model.IdCardSide
import com.charfeddine.fluffy.scanner.data.model.PointNorm
import com.charfeddine.fluffy.scanner.data.model.ScannerFilterType
import com.charfeddine.fluffy.scanner.domain.ScannerSubRoute
import com.charfeddine.fluffy.scanner.domain.ScannerViewModel
import com.charfeddine.fluffy.scanner.ui.theme.ScannerDarkLiquidBackdrop
import com.charfeddine.fluffy.scanner.ui.theme.ScannerGlassPalette
import com.charfeddine.fluffy.scanner.ui.theme.iosLiquidGlass
import com.charfeddine.fluffy.scanner.utils.DocumentImageProcessor
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class DragHandleTarget {
    TL, TR, BR, BL, TOP_MID, RIGHT_MID, BOTTOM_MID, LEFT_MID
}

@Composable
fun CropAndFilterScreen(
    scannerViewModel: ScannerViewModel,
    appendToDocId: String?,
    editDocumentId: String? = null,
    editPageId: String? = null,
    isArabic: Boolean,
    onBackToCamera: () -> Unit
) {
    BackHandler { onBackToCamera() }

    val isEditExistingMode = editDocumentId != null && editPageId != null
    val batchFrames by scannerViewModel.batchFrames.collectAsState()
    val activeIdx by scannerViewModel.activeCropIndex.collectAsState()
    val currentRoute by scannerViewModel.currentRoute.collectAsState()
    val isBusy by scannerViewModel.isBusy.collectAsState()

    val frame = batchFrames.getOrNull(activeIdx)
    if (frame == null) {
        LaunchedEffect(currentRoute) {
            if (currentRoute is ScannerSubRoute.CropAndFilter) {
                onBackToCamera()
            }
        }
        return
    }

    val uiScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pageStripState = rememberLazyListState()
    var isCropPhase by rememberSaveable(frame.id, editPageId) { mutableStateOf(true) }
    var docTitle by remember { mutableStateOf("") }
    var showAdjustSliders by remember { mutableStateOf(false) }
    var activeToneMetric by remember { mutableIntStateOf(0) } // 0=Brightness, 1=Contrast, 2=Sharpness
    var showSplitCompare by remember { mutableStateOf(true) }
    var isAutoFitting by remember(frame.id) { mutableStateOf(false) }

    val rawBitmapCache = remember { mutableStateMapOf<String, Bitmap>() }
    val rawBitmap = rawBitmapCache[frame.id]
    var warpedBitmap by remember(frame.id) { mutableStateOf<Bitmap?>(null) }
    var enhancedBitmap by remember(frame.id) { mutableStateOf<Bitmap?>(null) }
    var filterThumbnails by remember(frame.id) { mutableStateOf<Map<ScannerFilterType, Bitmap>>(emptyMap()) }
    var isRenderingPreview by remember(frame.id) { mutableStateOf(false) }

    val stageDragAnim = remember { Animatable(0f) }
    var lastNavDirection by remember { mutableIntStateOf(0) }
    val swipeThresholdPx = with(density) { 52.dp.toPx() }

    LaunchedEffect(activeIdx, batchFrames.size) {
        if (batchFrames.size > 1 && activeIdx in batchFrames.indices) {
            pageStripState.animateScrollToItem(activeIdx)
        }
    }

    fun navigateBatchPageByDelta(delta: Int) {
        val target = activeIdx + delta
        if (target in batchFrames.indices) {
            lastNavDirection = delta
            scannerViewModel.activeCropIndex.value = target
        }
    }

    // Before/After split comparison fraction (0.0 .. 1.0)
    var splitComparisonX by remember { mutableFloatStateOf(0.54f) }

    val pairedPartner = remember(batchFrames, frame.id, frame.idCardGroupId) {
        val gid = frame.idCardGroupId
        if (gid == null) null
        else batchFrames.firstOrNull { it.id != frame.id && it.idCardGroupId == gid }
    }

    // Decode active frame immediately and pre-cache adjacent frames for instant swipe transitions
    LaunchedEffect(frame.id, frame.rawImagePath, batchFrames, activeIdx) {
        if (rawBitmapCache[frame.id] == null) {
            val decoded = withContext(Dispatchers.IO) {
                scannerViewModel.repository.decodeSampledPageBitmap(frame.rawImagePath, 1280)
            }
            if (decoded != null) {
                rawBitmapCache[frame.id] = decoded
            }
        }
        val activeIds = batchFrames.map { it.id }.toSet()
        rawBitmapCache.keys.toList().filter { it !in activeIds }.forEach { rawBitmapCache.remove(it) }
        val neighbors = listOfNotNull(
            batchFrames.getOrNull(activeIdx + 1),
            batchFrames.getOrNull(activeIdx - 1)
        )
        for (neighbor in neighbors) {
            if (rawBitmapCache[neighbor.id] == null) {
                val nBmp = withContext(Dispatchers.IO) {
                    scannerViewModel.repository.decodeSampledPageBitmap(neighbor.rawImagePath, 1280)
                }
                if (nBmp != null) {
                    rawBitmapCache[neighbor.id] = nBmp
                }
            }
        }
        if (rawBitmapCache.size > 5) {
            val keepIds = setOfNotNull(
                frame.id,
                batchFrames.getOrNull(activeIdx - 1)?.id,
                batchFrames.getOrNull(activeIdx + 1)?.id,
                batchFrames.getOrNull(activeIdx - 2)?.id,
                batchFrames.getOrNull(activeIdx + 2)?.id
            )
            rawBitmapCache.keys.toList().filter { it !in keepIds }.forEach { rawBitmapCache.remove(it) }
        }
    }

    // Generate crisp miniature previews for all 5 filters once per warp/rotation
    val effectiveIdPrintMode = remember(frame.idCardPrintMode, pairedPartner?.idCardPrintMode) {
        if (frame.idCardPrintMode == IdCardPrintMode.RECTO_VERSO ||
            pairedPartner?.idCardPrintMode == IdCardPrintMode.RECTO_VERSO
        ) {
            IdCardPrintMode.RECTO_VERSO
        } else {
            IdCardPrintMode.RECTO
        }
    }

    LaunchedEffect(
        isCropPhase,
        rawBitmap,
        frame.detectedQuad,
        frame.rotationDeg,
        effectiveIdPrintMode,
        pairedPartner?.id,
        pairedPartner?.detectedQuad
    ) {
        val raw = rawBitmap ?: return@LaunchedEffect
        if (!isCropPhase) {
            val thumbs = withContext(Dispatchers.Default) {
                val baseThumbSource = if (pairedPartner != null) {
                    if (effectiveIdPrintMode == IdCardPrintMode.RECTO_VERSO) {
                        val miniWarped = DocumentImageProcessor.warpPerspective(raw, frame.detectedQuad, 180)
                        val sheet = DocumentImageProcessor.createIdCardSingleFaceSheet(miniWarped)
                        if (miniWarped !== raw && !miniWarped.isRecycled) miniWarped.recycle()
                        val scaled = Bitmap.createScaledBitmap(sheet, 120, 170, true)
                        if (scaled !== sheet) sheet.recycle()
                        if (frame.rotationDeg % 360 != 0) {
                            val rot = DocumentImageProcessor.rotateBitmap(scaled, frame.rotationDeg.toFloat())
                            if (rot !== scaled) scaled.recycle()
                            rot
                        } else {
                            scaled
                        }
                    } else {
                        val partnerRaw = withContext(Dispatchers.IO) {
                            scannerViewModel.repository.decodeSampledPageBitmap(pairedPartner.rawImagePath, 700)
                        }
                        if (partnerRaw != null) {
                            val isFrameFront = frame.idCardSide == IdCardSide.FRONT || pairedPartner.idCardSide == IdCardSide.BACK
                            val frontBmp = if (isFrameFront) raw else partnerRaw
                            val backBmp = if (isFrameFront) partnerRaw else raw
                            val frontQuad = if (isFrameFront) frame.detectedQuad else pairedPartner.detectedQuad
                            val backQuad = if (isFrameFront) pairedPartner.detectedQuad else frame.detectedQuad

                            val fMini = DocumentImageProcessor.warpPerspective(frontBmp, frontQuad, 180)
                            val bMini = DocumentImageProcessor.warpPerspective(backBmp, backQuad, 180)
                            partnerRaw.recycle()
                            val merged = DocumentImageProcessor.mergeIdCardFrontAndBack(fMini, bMini)
                            if (fMini !== frontBmp) fMini.recycle()
                            if (bMini !== backBmp) bMini.recycle()
                            val scaled = Bitmap.createScaledBitmap(merged, 120, 170, true)
                            if (scaled !== merged) merged.recycle()
                            if (frame.rotationDeg % 360 != 0) {
                                val rot = DocumentImageProcessor.rotateBitmap(scaled, frame.rotationDeg.toFloat())
                                if (rot !== scaled) scaled.recycle()
                                rot
                            } else {
                                scaled
                            }
                        } else {
                            val miniWarped = DocumentImageProcessor.warpPerspective(raw, frame.detectedQuad, 150)
                            if (frame.rotationDeg % 360 != 0) {
                                DocumentImageProcessor.rotateBitmap(miniWarped, frame.rotationDeg.toFloat())
                            } else {
                                miniWarped
                            }
                        }
                    }
                } else {
                    val miniWarped = DocumentImageProcessor.warpPerspective(raw, frame.detectedQuad, 150)
                    if (frame.rotationDeg % 360 != 0) {
                        DocumentImageProcessor.rotateBitmap(miniWarped, frame.rotationDeg.toFloat())
                    } else {
                        miniWarped
                    }
                }
                ScannerFilterType.entries.associateWith { filter ->
                    DocumentImageProcessor.applyEnhancements(
                        source = baseThumbSource,
                        filter = filter,
                        brightness = 0f,
                        contrast = 1f,
                        sharpness = 0f,
                        rotationDeg = 0,
                        enableSuperRes = false
                    )
                }
            }
            filterThumbnails = thumbs
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
        frame.rotationDeg,
        effectiveIdPrintMode,
        pairedPartner?.id,
        pairedPartner?.detectedQuad
    ) {
        val raw = rawBitmap ?: return@LaunchedEffect
        if (!isCropPhase) {
            isRenderingPreview = true
            try {
                withContext(Dispatchers.Default) {
                    if (pairedPartner != null && effectiveIdPrintMode == IdCardPrintMode.RECTO_VERSO) {
                        val sideWarped = DocumentImageProcessor.warpPerspective(raw, frame.detectedQuad, 960)
                        val sideBeforeRaw = DocumentImageProcessor.createIdCardSingleFaceSheet(sideWarped)
                        val sideAuto = DocumentImageProcessor.autoEnhanceCapturedDocument(sideWarped, isIdCardMode = true)
                        if (sideWarped !== sideAuto && !sideWarped.isRecycled) sideWarped.recycle()

                        val sideEnh = DocumentImageProcessor.applyEnhancements(
                            source = sideAuto,
                            filter = frame.filter,
                            brightness = frame.brightness,
                            contrast = frame.contrast,
                            sharpness = frame.sharpness,
                            rotationDeg = 0,
                            enableSuperRes = false
                        )
                        if (sideAuto !== sideEnh && !sideAuto.isRecycled) sideAuto.recycle()

                        val sideAfterRaw = DocumentImageProcessor.createIdCardSingleFaceSheet(sideEnh)
                        if (!sideEnh.isRecycled) sideEnh.recycle()

                        val beforeSheet = if (frame.rotationDeg % 360 != 0) {
                            val rot = DocumentImageProcessor.rotateBitmap(sideBeforeRaw, frame.rotationDeg.toFloat())
                            if (rot !== sideBeforeRaw && !sideBeforeRaw.isRecycled) sideBeforeRaw.recycle()
                            rot
                        } else {
                            sideBeforeRaw
                        }
                        val afterSheet = if (frame.rotationDeg % 360 != 0) {
                            val rot = DocumentImageProcessor.rotateBitmap(sideAfterRaw, frame.rotationDeg.toFloat())
                            if (rot !== sideAfterRaw && !sideAfterRaw.isRecycled) sideAfterRaw.recycle()
                            rot
                        } else {
                            sideAfterRaw
                        }

                        withContext(Dispatchers.Main) {
                            warpedBitmap = beforeSheet
                            enhancedBitmap = afterSheet
                        }
                        return@withContext
                    }

                    if (pairedPartner != null) {
                        val partnerRaw = withContext(Dispatchers.IO) {
                            scannerViewModel.repository.decodeSampledPageBitmap(pairedPartner.rawImagePath, 1280)
                        }
                        if (partnerRaw != null) {
                            val isFrameFront = frame.idCardSide == IdCardSide.FRONT || pairedPartner.idCardSide == IdCardSide.BACK
                            val frontBmp = if (isFrameFront) raw else partnerRaw
                            val backBmp = if (isFrameFront) partnerRaw else raw
                            val frontQuad = if (isFrameFront) frame.detectedQuad else pairedPartner.detectedQuad
                            val backQuad = if (isFrameFront) pairedPartner.detectedQuad else frame.detectedQuad

                            val fWarped = DocumentImageProcessor.warpPerspective(frontBmp, frontQuad, 960)
                            val bWarped = DocumentImageProcessor.warpPerspective(backBmp, backQuad, 960)
                            partnerRaw.recycle()

                            val mergedBeforeRaw = DocumentImageProcessor.mergeIdCardFrontAndBack(fWarped, bWarped)
                            val fAuto = DocumentImageProcessor.autoEnhanceCapturedDocument(fWarped, isIdCardMode = true)
                            if (fWarped !== fAuto && !fWarped.isRecycled) fWarped.recycle()
                            val bAuto = DocumentImageProcessor.autoEnhanceCapturedDocument(bWarped, isIdCardMode = true)
                            if (bWarped !== bAuto && !bWarped.isRecycled) bWarped.recycle()

                            val fEnh = DocumentImageProcessor.applyEnhancements(
                                source = fAuto,
                                filter = frame.filter,
                                brightness = frame.brightness,
                                contrast = frame.contrast,
                                sharpness = frame.sharpness,
                                rotationDeg = 0,
                                enableSuperRes = false
                            )
                            if (fAuto !== fEnh && !fAuto.isRecycled) fAuto.recycle()

                            val bEnh = DocumentImageProcessor.applyEnhancements(
                                source = bAuto,
                                filter = frame.filter,
                                brightness = frame.brightness,
                                contrast = frame.contrast,
                                sharpness = frame.sharpness,
                                rotationDeg = 0,
                                enableSuperRes = false
                            )
                            if (bAuto !== bEnh && !bAuto.isRecycled) bAuto.recycle()

                            val mergedAfterRaw = DocumentImageProcessor.mergeIdCardFrontAndBack(fEnh, bEnh)
                            if (!fEnh.isRecycled) fEnh.recycle()
                            if (!bEnh.isRecycled) bEnh.recycle()

                            val beforeSheet = if (frame.rotationDeg % 360 != 0) {
                                val rot = DocumentImageProcessor.rotateBitmap(mergedBeforeRaw, frame.rotationDeg.toFloat())
                                if (rot !== mergedBeforeRaw && !mergedBeforeRaw.isRecycled) mergedBeforeRaw.recycle()
                                rot
                            } else {
                                mergedBeforeRaw
                            }
                            val afterSheet = if (frame.rotationDeg % 360 != 0) {
                                val rot = DocumentImageProcessor.rotateBitmap(mergedAfterRaw, frame.rotationDeg.toFloat())
                                if (rot !== mergedAfterRaw && !mergedAfterRaw.isRecycled) mergedAfterRaw.recycle()
                                rot
                            } else {
                                mergedAfterRaw
                            }

                            withContext(Dispatchers.Main) {
                                warpedBitmap = beforeSheet
                                enhancedBitmap = afterSheet
                            }
                            return@withContext
                        }
                    }

                    val warped = DocumentImageProcessor.warpPerspective(raw, frame.detectedQuad, 960)
                    val prepared = if (frame.idCardSide != IdCardSide.NONE) {
                        DocumentImageProcessor.autoEnhanceCapturedDocument(warped, isIdCardMode = true)
                    } else {
                        warped
                    }
                    val enhanced = DocumentImageProcessor.applyEnhancements(
                        source = prepared,
                        filter = frame.filter,
                        brightness = frame.brightness,
                        contrast = frame.contrast,
                        sharpness = frame.sharpness,
                        rotationDeg = frame.rotationDeg,
                        enableSuperRes = false
                    )
                    if (prepared !== warped && !prepared.isRecycled) prepared.recycle()
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
            } finally {
                isRenderingPreview = false
            }
        }
    }

    ScannerDarkLiquidBackdrop {
        // Subtle ambient glow accents tuned to the active app theme preset
        val phaseGlowColor = if (isCropPhase) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.VioletGlow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        phaseGlowColor.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.14f),
                    radius = size.width * 0.72f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. iOS 27 Floating Liquid Glass Top Header + Phase Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .minimumInteractiveComponentSize()
                        .iosLiquidGlass(shape = CircleShape, glowAccent = false, fillAlpha = 0.78f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true)
                        ) { onBackToCamera() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isArabic) "رجوع" else "Back",
                        tint = ScannerGlassPalette.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Segmented iOS 27 Glass Phase Switcher
                Row(
                    modifier = Modifier
                        .iosLiquidGlass(shape = CircleShape, glowAccent = true, fillAlpha = 0.78f)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isCropPhase) {
                                    Brush.horizontalGradient(
                                        listOf(ScannerGlassPalette.EdgeOverlayCyan, ScannerGlassPalette.CyanGlow)
                                    )
                                } else {
                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable { isCropPhase = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isArabic) "1. القص" else "1. Crop",
                            color = if (isCropPhase) ScannerGlassPalette.OnAccent else ScannerGlassPalette.TextSecondary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (!isCropPhase) {
                                    Brush.horizontalGradient(
                                        listOf(ScannerGlassPalette.EdgeOverlayCyan, ScannerGlassPalette.CyanGlow)
                                    )
                                } else {
                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable { isCropPhase = false }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isArabic) "2. التحسين" else "2. Enhance",
                            color = if (!isCropPhase) ScannerGlassPalette.OnAccent else ScannerGlassPalette.TextSecondary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }

                if (isEditExistingMode) {
                    Spacer(modifier = Modifier.size(46.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .minimumInteractiveComponentSize()
                            .iosLiquidGlass(shape = CircleShape, glowAccent = false, fillAlpha = 0.78f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true)
                            ) { scannerViewModel.removeBatchFrame(activeIdx) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = if (isArabic) "حذف الصفحة" else "Remove Page",
                            tint = ScannerGlassPalette.RoseGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Batch page selector strip (when scanning or importing multiple pages)
            if (batchFrames.size > 1) {
                LazyRow(
                    state = pageStripState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(batchFrames) { idx, item ->
                        val selected = idx == activeIdx
                        val pillLabel = when (item.idCardSide) {
                            IdCardSide.FRONT -> if (isArabic) "الوجه الأمامي" else "ID Front"
                            IdCardSide.BACK -> if (isArabic) "الوجه الخلفي" else "ID Back"
                            IdCardSide.NONE -> if (isArabic) "صفحة ${idx + 1}" else "Page ${idx + 1}"
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .iosLiquidGlass(CircleShape, glowAccent = selected, fillAlpha = if (selected) 0.86f else 0.56f)
                                .clickable {
                                    lastNavDirection = if (idx > activeIdx) 1 else if (idx < activeIdx) -1 else 0
                                    scannerViewModel.activeCropIndex.value = idx
                                }
                                .padding(horizontal = 13.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = pillLabel,
                                color = if (selected) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 2. Main Interactive Document Stage
            val canSwipePages = batchFrames.size > 1
            val onStageSwipeDragDelta: (Float) -> Unit = { totalDx ->
                val isNextDrag = if (isArabic) totalDx > 0f else totalDx < 0f
                val canMoveInDir = if (isNextDrag) activeIdx < batchFrames.lastIndex else activeIdx > 0
                val targetDx = if (canMoveInDir) totalDx * 0.58f else totalDx * 0.16f
                uiScope.launch { stageDragAnim.snapTo(targetDx) }
            }
            val onStageSwipeRelease: (Float, Float) -> Unit = { totalDx, totalDy ->
                uiScope.launch {
                    stageDragAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.78f, stiffness = 440f)
                    )
                }
                if (abs(totalDx) >= swipeThresholdPx && abs(totalDx) > abs(totalDy) * 1.15f) {
                    val delta = if (isArabic) {
                        if (totalDx > 0f) 1 else -1
                    } else {
                        if (totalDx < 0f) 1 else -1
                    }
                    navigateBatchPageByDelta(delta)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .iosLiquidGlass(RoundedCornerShape(26.dp), glowAccent = true, fillAlpha = 0.66f)
                    .clipToBounds()
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = frame.id,
                    transitionSpec = {
                        val visualDir = if (lastNavDirection == 0) 0 else if (isArabic) -lastNavDirection else lastNavDirection
                        if (visualDir == 0) {
                            (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
                                .togetherWith(fadeOut(tween(180)))
                        } else {
                            (slideInHorizontally(
                                animationSpec = tween(260),
                                initialOffsetX = { fullWidth -> (visualDir * fullWidth * 0.42f).roundToInt() }
                            ) + fadeIn(tween(230)) + scaleIn(initialScale = 0.95f, animationSpec = tween(260)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(240),
                                        targetOffsetX = { fullWidth -> (-visualDir * fullWidth * 0.35f).roundToInt() }
                                    ) + fadeOut(tween(190)) + scaleOut(targetScale = 0.94f, animationSpec = tween(240))
                                )
                        }
                    },
                    label = "crop_stage_page_transition",
                    modifier = Modifier.fillMaxSize()
                ) { targetFrameId ->
                    val targetFrame = batchFrames.firstOrNull { it.id == targetFrameId } ?: frame
                    val bmp = rawBitmapCache[targetFrameId]
                    if (bmp == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ScannerGlassPalette.CyanGlow)
                        }
                    } else {
                        val dragPx = stageDragAnim.value
                        val dragFraction = (abs(dragPx) / 900f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = dragPx
                                    val parallaxScale = 1f - dragFraction * 0.045f
                                    scaleX = parallaxScale
                                    scaleY = parallaxScale
                                    alpha = 1f - dragFraction * 0.14f
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCropPhase) {
                                InteractiveCropWithMagnifier(
                                    frameId = targetFrame.id,
                                    bitmap = bmp,
                                    quad = targetFrame.detectedQuad,
                                    canSwipePages = canSwipePages,
                                    onQuadChange = { committedFrameId, newQuad ->
                                        scannerViewModel.updateFrameQuadById(committedFrameId, newQuad)
                                    },
                                    onSwipeDragDelta = onStageSwipeDragDelta,
                                    onSwipeRelease = onStageSwipeRelease
                                )
                            } else {
                                val beforeBmp = warpedBitmap
                                val afterBmp = enhancedBitmap
                                if (targetFrame.id != frame.id || beforeBmp == null || afterBmp == null) {
                                    CircularProgressIndicator(color = ScannerGlassPalette.CyanGlow)
                                } else {
                                    BeforeAfterComparisonViewport(
                                        frameId = targetFrame.id,
                                        beforeBitmap = beforeBmp,
                                        afterBitmap = afterBmp,
                                        splitFraction = splitComparisonX,
                                        showSplitCompare = showSplitCompare,
                                        isRenderingPreview = isRenderingPreview,
                                        isArabic = isArabic,
                                        canSwipePages = canSwipePages,
                                        onToggleCompareMode = { showSplitCompare = !showSplitCompare },
                                        onSplitFractionChange = { splitComparisonX = it },
                                        onSwipeDragDelta = onStageSwipeDragDelta,
                                        onSwipeRelease = onStageSwipeRelease
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Bottom Studio Controls depending on Crop vs Enhance Phase
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
                                            if (frame.idCardSide != IdCardSide.NONE) {
                                                DocumentImageProcessor.detectIdCardQuad(bmp)
                                            } else {
                                                DocumentImageProcessor.detectQuadFromBitmap(
                                                    bitmap = bmp,
                                                    isIdCardHint = false
                                                )
                                            }
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
                            color = ScannerGlassPalette.OnAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                // Redesigned iOS 27 Dark Liquid Glass Enhance Studio Controls
                val hasToneModifications = abs(frame.brightness) > 0.5f ||
                    abs(frame.contrast - 1f) > 0.02f ||
                    frame.sharpness > 0.05f ||
                    frame.rotationDeg % 360 != 0

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // A. Live Thumbnail Filter Strip
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ScannerFilterType.entries) { filter ->
                            val selected = frame.filter == filter
                            val thumb = filterThumbnails[filter]
                            val thumbImg = remember(thumb) { thumb?.asImageBitmap() }

                            Column(
                                modifier = Modifier
                                    .width(74.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("filter_chip_${filter.id}")
                                    .iosLiquidGlass(
                                        shape = RoundedCornerShape(18.dp),
                                        glowAccent = selected,
                                        fillAlpha = if (selected) 0.88f else 0.56f
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true)
                                    ) {
                                        scannerViewModel.updateFrameEnhancement(
                                            activeIdx,
                                            filter,
                                            frame.brightness,
                                            frame.contrast,
                                            frame.sharpness,
                                            frame.rotationDeg
                                        )
                                    }
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ScannerGlassPalette.ThumbnailFrame)
                                        .border(
                                            width = if (selected) 1.3.dp else 0.7.dp,
                                            color = if (selected) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.SubtleStroke,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (thumbImg != null) {
                                        Image(
                                            bitmap = thumbImg,
                                            contentDescription = if (isArabic) filter.labelAr else filter.labelEn,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (selected) ScannerGlassPalette.CyanGlow.copy(alpha = 0.45f)
                                                    else ScannerGlassPalette.SubtleChipBg
                                                )
                                        )
                                    }

                                    if (selected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(ScannerGlassPalette.CyanGlow),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = ScannerGlassPalette.OnAccent,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (isArabic) filter.labelAr else filter.labelEn,
                                    color = if (selected) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextPrimary,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // B. Sleek Quick Action Liquid Glass Pill Row (Rotate, Deskew, Tune, Reset)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .minimumInteractiveComponentSize()
                                    .iosLiquidGlass(CircleShape, fillAlpha = 0.72f)
                                    .clickable {
                                        scannerViewModel.updateFrameEnhancement(
                                            activeIdx,
                                            frame.filter,
                                            frame.brightness,
                                            frame.contrast,
                                            frame.sharpness,
                                            (frame.rotationDeg - 90) % 360
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                                    contentDescription = if (isArabic) "تدوير يسار" else "Rotate Left",
                                    tint = ScannerGlassPalette.TextPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .minimumInteractiveComponentSize()
                                    .iosLiquidGlass(CircleShape, fillAlpha = 0.72f)
                                    .clickable {
                                        scannerViewModel.updateFrameEnhancement(
                                            activeIdx,
                                            frame.filter,
                                            frame.brightness,
                                            frame.contrast,
                                            frame.sharpness,
                                            (frame.rotationDeg + 90) % 360
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                    contentDescription = if (isArabic) "تدوير يمين" else "Rotate Right",
                                    tint = ScannerGlassPalette.TextPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .minimumInteractiveComponentSize()
                                    .iosLiquidGlass(CircleShape, fillAlpha = 0.72f)
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
                                    .padding(horizontal = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        tint = ScannerGlassPalette.VioletGlow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "استقامة" else "Deskew",
                                        color = ScannerGlassPalette.TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            AnimatedVisibility(visible = hasToneModifications) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .minimumInteractiveComponentSize()
                                        .iosLiquidGlass(CircleShape, fillAlpha = 0.72f)
                                        .clickable {
                                            scannerViewModel.updateFrameEnhancement(
                                                activeIdx,
                                                frame.filter,
                                                0f,
                                                1f,
                                                0f,
                                                0
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = if (isArabic) "إعادة الضبط" else "Reset Adjustments",
                                        tint = ScannerGlassPalette.AmberGlow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .minimumInteractiveComponentSize()
                                    .iosLiquidGlass(
                                        shape = CircleShape,
                                        glowAccent = showAdjustSliders,
                                        fillAlpha = if (showAdjustSliders) 0.88f else 0.72f
                                    )
                                    .clickable { showAdjustSliders = !showAdjustSliders }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = ScannerGlassPalette.CyanGlow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "الضبط" else "Adjust",
                                        color = if (showAdjustSliders) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // C. Single-Metric Tabbed iOS 27 Liquid Glass Tuner Card
                    AnimatedVisibility(
                        visible = showAdjustSliders,
                        enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
                    ) {
                        val activeAccent = when (activeToneMetric) {
                            0 -> ScannerGlassPalette.CyanGlow
                            1 -> ScannerGlassPalette.VioletGlow
                            else -> ScannerGlassPalette.EmeraldGlow
                        }
                        val metricValueLabel = when (activeToneMetric) {
                            0 -> {
                                val v = frame.brightness.roundToInt()
                                if (v > 0) "+$v" else "$v"
                            }
                            1 -> String.format(Locale.US, "%.2fx", frame.contrast)
                            else -> "${(frame.sharpness * 100).roundToInt()}%"
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .iosLiquidGlass(RoundedCornerShape(22.dp), glowAccent = true, fillAlpha = 0.82f)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val metricTabs = listOf(
                                        Triple(0, if (isArabic) "السطوع" else "Brightness", ScannerGlassPalette.CyanGlow),
                                        Triple(1, if (isArabic) "التباين" else "Contrast", ScannerGlassPalette.VioletGlow),
                                        Triple(2, if (isArabic) "الحدة" else "Sharpness", ScannerGlassPalette.EmeraldGlow)
                                    )
                                    metricTabs.forEach { (idx, label, accent) ->
                                        val isSel = activeToneMetric == idx
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (isSel) accent.copy(alpha = 0.22f) else Color.Transparent)
                                                .border(
                                                    width = if (isSel) 0.9.dp else 0.dp,
                                                    color = if (isSel) accent.copy(alpha = 0.65f) else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { activeToneMetric = idx }
                                                .padding(horizontal = 11.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSel) accent else ScannerGlassPalette.TextSecondary,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(activeAccent.copy(alpha = 0.16f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = metricValueLabel,
                                        color = activeAccent,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            when (activeToneMetric) {
                                0 -> Slider(
                                    value = frame.brightness,
                                    onValueChange = {
                                        scannerViewModel.updateFrameEnhancement(
                                            activeIdx, frame.filter, it, frame.contrast, frame.sharpness, frame.rotationDeg
                                        )
                                    },
                                    valueRange = -60f..60f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ScannerGlassPalette.CyanGlow,
                                        activeTrackColor = ScannerGlassPalette.CyanGlow,
                                        inactiveTrackColor = ScannerGlassPalette.SubtleStroke
                                    )
                                )
                                1 -> Slider(
                                    value = frame.contrast,
                                    onValueChange = {
                                        scannerViewModel.updateFrameEnhancement(
                                            activeIdx, frame.filter, frame.brightness, it, frame.sharpness, frame.rotationDeg
                                        )
                                    },
                                    valueRange = 0.6f..1.8f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ScannerGlassPalette.VioletGlow,
                                        activeTrackColor = ScannerGlassPalette.VioletGlow,
                                        inactiveTrackColor = ScannerGlassPalette.SubtleStroke
                                    )
                                )
                                else -> Slider(
                                    value = frame.sharpness,
                                    onValueChange = {
                                        scannerViewModel.updateFrameEnhancement(
                                            activeIdx, frame.filter, frame.brightness, frame.contrast, it, frame.rotationDeg
                                        )
                                    },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ScannerGlassPalette.EmeraldGlow,
                                        activeTrackColor = ScannerGlassPalette.EmeraldGlow,
                                        inactiveTrackColor = ScannerGlassPalette.SubtleStroke
                                    )
                                )
                            }
                        }
                    }

                    // D. Docked Frosted Liquid Glass Document Title & Save Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .iosLiquidGlass(RoundedCornerShape(22.dp), glowAccent = true, fillAlpha = 0.84f)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditExistingMode) {
                            Text(
                                text = if (isArabic) "تعديل حواف وتحسين الصفحة" else "Edit Page Edges & Filter",
                                color = ScannerGlassPalette.TextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        } else if (appendToDocId == null) {
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
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ScannerGlassPalette.CyanGlow,
                                    unfocusedBorderColor = ScannerGlassPalette.SubtleStroke,
                                    focusedContainerColor = ScannerGlassPalette.InputContainer,
                                    unfocusedContainerColor = ScannerGlassPalette.InputContainer.copy(alpha = 0.78f),
                                    focusedTextColor = ScannerGlassPalette.TextPrimary,
                                    unfocusedTextColor = ScannerGlassPalette.TextPrimary,
                                    cursorColor = ScannerGlassPalette.CyanGlow
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Text(
                                text = if (isArabic) "إضافة إلى المستند الحالي" else "Append to current doc",
                                color = ScannerGlassPalette.TextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val outputPageCount = remember(batchFrames) {
                            val seenGroups = mutableSetOf<String>()
                            var count = 0
                            batchFrames.forEach { item ->
                                val gid = item.idCardGroupId
                                val hasPartner = gid != null && batchFrames.count { it.idCardGroupId == gid } >= 2
                                if (hasPartner && gid != null) {
                                    if (seenGroups.add(gid)) {
                                        val groupVerso = batchFrames.any {
                                            it.idCardGroupId == gid && it.idCardPrintMode == IdCardPrintMode.RECTO_VERSO
                                        }
                                        count += if (groupVerso) 2 else 1
                                    }
                                } else {
                                    count += 1
                                }
                            }
                            count
                        }

                        Button(
                            onClick = {
                                val docId = editDocumentId
                                val pageId = editPageId
                                if (isEditExistingMode && docId != null && pageId != null) {
                                    scannerViewModel.saveEditedPageFrame(
                                        docId = docId,
                                        pageId = pageId,
                                        isArabic = isArabic
                                    )
                                } else if (!isEditExistingMode) {
                                    scannerViewModel.commitBatchToDocument(docTitle, appendToDocId, isArabic)
                                }
                            },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ScannerGlassPalette.CyanGlow),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("crop_save_document_btn")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(color = ScannerGlassPalette.OnAccent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ScannerGlassPalette.OnAccent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEditExistingMode) {
                                        if (isArabic) "حفظ التعديل" else "Save"
                                    } else {
                                        if (isArabic) "حفظ ($outputPageCount)" else "Save ($outputPageCount)"
                                    },
                                    color = ScannerGlassPalette.OnAccent,
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
    frameId: String,
    bitmap: Bitmap,
    quad: DocumentQuad,
    canSwipePages: Boolean,
    onQuadChange: (String, DocumentQuad) -> Unit,
    onSwipeDragDelta: (Float) -> Unit,
    onSwipeRelease: (Float, Float) -> Unit
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var activeHandle by remember { mutableStateOf<DragHandleTarget?>(null) }
    var activeAnchorScreenPos by remember { mutableStateOf<Offset?>(null) }
    var localQuad by remember(frameId, bitmap) { mutableStateOf(quad) }
    var zoomScale by remember(frameId, bitmap) { mutableFloatStateOf(1f) }
    var panOffset by remember(frameId, bitmap) { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    // Sync external quad updates (e.g., Auto-Fit or Full Frame) only when not actively dragging
    LaunchedEffect(frameId, quad) {
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
    val latestZoomScale by rememberUpdatedState(zoomScale)
    val latestPanOffset by rememberUpdatedState(panOffset)
    val latestCanSwipePages by rememberUpdatedState(canSwipePages)
    val latestOnSwipeDragDelta by rememberUpdatedState(onSwipeDragDelta)
    val latestOnSwipeRelease by rememberUpdatedState(onSwipeRelease)
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    val edgePadPx = with(density) { 14.dp.toPx() }
    val bw = bitmap.width.toFloat().coerceAtLeast(1f)
    val bh = bitmap.height.toFloat().coerceAtLeast(1f)
    val cw = boxSize.width.toFloat().coerceAtLeast(1f)
    val ch = boxSize.height.toFloat().coerceAtLeast(1f)
    val availW = (cw - edgePadPx * 2f).coerceAtLeast(1f)
    val availH = (ch - edgePadPx * 2f).coerceAtLeast(1f)
    val fitScale = minOf(availW / bw, availH / bh)
    val drawW = bw * fitScale
    val drawH = bh * fitScale
    val offsetX = (cw - drawW) * 0.5f
    val offsetY = (ch - drawH) * 0.5f
    val displayScaleX = drawW / bw
    val displayScaleY = drawH / bh

    fun clampCropPan(raw: Offset, scale: Float): Offset {
        if (scale <= 1.001f || boxSize.width <= 0 || boxSize.height <= 0) return Offset.Zero
        val minX = cw * (1f - scale)
        val minY = ch * (1f - scale)
        return Offset(
            x = raw.x.coerceIn(minX, 0f),
            y = raw.y.coerceIn(minY, 0f)
        )
    }

    fun applyCropZoomAt(targetScale: Float, focalScreen: Offset) {
        val clampedScale = targetScale.coerceIn(1f, 5f)
        val ratio = clampedScale / zoomScale
        val rawPan = focalScreen - (focalScreen - panOffset) * ratio
        zoomScale = clampedScale
        panOffset = clampCropPan(rawPan, clampedScale)
    }

    fun bitmapPointToScreenAt(pt: PointF, scale: Float, pan: Offset): Offset = Offset(
        x = (offsetX + pt.x * displayScaleX) * scale + pan.x,
        y = (offsetY + pt.y * displayScaleY) * scale + pan.y
    )

    fun normToScreenAt(p: PointNorm, scale: Float, pan: Offset): Offset = bitmapPointToScreenAt(
        PointF(p.x * bw, p.y * bh),
        scale,
        pan
    )

    fun screenToNormAt(off: Offset, scale: Float, pan: Offset): PointNorm {
        val baseX = (off.x - pan.x) / scale
        val baseY = (off.y - pan.y) / scale
        return PointNorm(
            x = (((baseX - offsetX) / displayScaleX) / bw).coerceIn(0f, 1f),
            y = (((baseY - offsetY) / displayScaleY) / bh).coerceIn(0f, 1f)
        )
    }

    fun screenToNormUnclampedAt(off: Offset, scale: Float, pan: Offset): PointNorm {
        val baseX = (off.x - pan.x) / scale
        val baseY = (off.y - pan.y) / scale
        return PointNorm(
            x = ((baseX - offsetX) / displayScaleX) / bw,
            y = ((baseY - offsetY) / displayScaleY) / bh
        )
    }

    val cropEdgeCyan = ScannerGlassPalette.EdgeOverlayCyan
    val cropEdgePurple = ScannerGlassPalette.EdgeOverlayPurple

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .onSizeChanged { boxSize = it }
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Crop preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
                    .padding(14.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(frameId, bitmap, boxSize) {
                        if (boxSize.width <= 0 || boxSize.height <= 0) return@pointerInput
                        val cornerHitRadius = with(density) { 52.dp.toPx() }
                        val midHitRadius = with(density) { 44.dp.toPx() }
                        val doubleTapDistPx = with(density) { 36.dp.toPx() }
                        val tapSlopPx = with(density) { 12.dp.toPx() }
                        val minGap = 0.04f
                        var lastTapTimeMs = 0L
                        var lastTapPos = Offset.Zero

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val s0 = latestZoomScale
                            val p0 = latestPanOffset
                            val q0 = latestLocalQuad
                            val tl0 = normToScreenAt(q0.topLeft, s0, p0)
                            val tr0 = normToScreenAt(q0.topRight, s0, p0)
                            val br0 = normToScreenAt(q0.bottomRight, s0, p0)
                            val bl0 = normToScreenAt(q0.bottomLeft, s0, p0)

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

                            val bestCorner = cornerHandles.minByOrNull { (_, pt) ->
                                hypot((pt.x - down.position.x).toDouble(), (pt.y - down.position.y).toDouble())
                            }
                            val bestCornerDist = bestCorner?.second?.let { pt ->
                                hypot((pt.x - down.position.x).toDouble(), (pt.y - down.position.y).toDouble())
                            } ?: Double.MAX_VALUE

                            val bestMid = midHandles.minByOrNull { (_, pt) ->
                                hypot((pt.x - down.position.x).toDouble(), (pt.y - down.position.y).toDouble())
                            }
                            val bestMidDist = bestMid?.second?.let { pt ->
                                hypot((pt.x - down.position.x).toDouble(), (pt.y - down.position.y).toDouble())
                            } ?: Double.MAX_VALUE

                            val picked: Pair<DragHandleTarget, Offset>? = when {
                                bestMid != null && bestMidDist <= midHitRadius && bestMidDist < bestCornerDist * 0.65 -> bestMid
                                bestCorner != null && bestCornerDist <= cornerHitRadius -> bestCorner
                                bestMid != null && bestMidDist <= midHitRadius -> bestMid
                                else -> null
                            }

                            var isDraggingHandle = picked != null
                            var isPinching = false
                            var prevCentroid = Offset.Zero
                            var prevSpan = 0f

                            if (picked != null) {
                                down.consume()
                                val target = picked.first
                                val grabOffset = picked.second - down.position
                                val startNorm = screenToNormUnclampedAt(down.position, s0, p0)

                                activeHandle = target
                                activeAnchorScreenPos = picked.second

                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pressed = event.changes.filter { it.pressed }
                                        if (pressed.size >= 2) {
                                            // Second finger joined -> switch smoothly to 2-finger centroid zoom
                                            isDraggingHandle = false
                                            isPinching = true
                                            activeHandle = null
                                            activeAnchorScreenPos = null
                                            latestOnQuadCommit(frameId, latestLocalQuad)
                                            val pA = pressed[0].position
                                            val pB = pressed[1].position
                                            prevCentroid = Offset((pA.x + pB.x) * 0.5f, (pA.y + pB.y) * 0.5f)
                                            prevSpan = hypot(pA.x - pB.x, pA.y - pB.y).coerceAtLeast(1f)
                                            pressed.forEach { it.consume() }
                                            break
                                        }

                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: event.changes.firstOrNull()
                                            ?: break

                                        if (!change.pressed) {
                                            change.consume()
                                            break
                                        }

                                        change.consume()
                                        val curScale = latestZoomScale
                                        val curPan = latestPanOffset
                                        val movedPos = change.position + grabOffset
                                        val targetNorm = screenToNormAt(movedPos, curScale, curPan)
                                        val currUnclamped = screenToNormUnclampedAt(change.position, curScale, curPan)
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
                                            DragHandleTarget.TL -> normToScreenAt(updated.topLeft, curScale, curPan)
                                            DragHandleTarget.TR -> normToScreenAt(updated.topRight, curScale, curPan)
                                            DragHandleTarget.BR -> normToScreenAt(updated.bottomRight, curScale, curPan)
                                            DragHandleTarget.BL -> normToScreenAt(updated.bottomLeft, curScale, curPan)
                                            DragHandleTarget.TOP_MID -> {
                                                val a = normToScreenAt(updated.topLeft, curScale, curPan)
                                                val b = normToScreenAt(updated.topRight, curScale, curPan)
                                                Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                                            }
                                            DragHandleTarget.RIGHT_MID -> {
                                                val a = normToScreenAt(updated.topRight, curScale, curPan)
                                                val b = normToScreenAt(updated.bottomRight, curScale, curPan)
                                                Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                                            }
                                            DragHandleTarget.BOTTOM_MID -> {
                                                val a = normToScreenAt(updated.bottomLeft, curScale, curPan)
                                                val b = normToScreenAt(updated.bottomRight, curScale, curPan)
                                                Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                                            }
                                            DragHandleTarget.LEFT_MID -> {
                                                val a = normToScreenAt(updated.topLeft, curScale, curPan)
                                                val b = normToScreenAt(updated.bottomLeft, curScale, curPan)
                                                Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                                            }
                                        }
                                    }
                                } finally {
                                    if (isDraggingHandle) {
                                        activeHandle = null
                                        activeAnchorScreenPos = null
                                        latestOnQuadCommit(frameId, latestLocalQuad)
                                    }
                                }
                            }

                            if (!isDraggingHandle) {
                                val downTimeMs = System.currentTimeMillis()
                                var maxMovedPx = 0f
                                var isSwipingPage = false
                                var swipeTotalDx = 0f
                                var swipeTotalDy = 0f
                                var wasMultiTouch = false
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pressed = event.changes.filter { it.pressed }
                                        if (pressed.isEmpty()) break

                                        if (pressed.size >= 2) {
                                            wasMultiTouch = true
                                            if (isSwipingPage) {
                                                isSwipingPage = false
                                                latestOnSwipeDragDelta(0f)
                                            }
                                            val pA = pressed[0].position
                                            val pB = pressed[1].position
                                            val currCentroid = Offset((pA.x + pB.x) * 0.5f, (pA.y + pB.y) * 0.5f)
                                            val currSpan = hypot(pA.x - pB.x, pA.y - pB.y).coerceAtLeast(1f)
                                            if (!isPinching) {
                                                isPinching = true
                                                prevCentroid = currCentroid
                                                prevSpan = currSpan
                                            } else {
                                                val zoomFactor = currSpan / prevSpan.coerceAtLeast(1f)
                                                val curScale = latestZoomScale
                                                val curPan = latestPanOffset
                                                val newScale = (curScale * zoomFactor).coerceIn(1f, 5f)
                                                val ratio = newScale / curScale
                                                val rawPan = currCentroid - (prevCentroid - curPan) * ratio
                                                zoomScale = newScale
                                                panOffset = clampCropPan(rawPan, newScale)
                                                prevCentroid = currCentroid
                                                prevSpan = currSpan
                                            }
                                            maxMovedPx = tapSlopPx + 10f
                                            pressed.forEach { it.consume() }
                                        } else {
                                            if (isPinching) {
                                                // One finger lifted after pinch
                                                isPinching = false
                                            } else {
                                                val c = pressed[0]
                                                val distFromDown = hypot(
                                                    c.position.x - down.position.x,
                                                    c.position.y - down.position.y
                                                )
                                                if (distFromDown > maxMovedPx) maxMovedPx = distFromDown
                                                if (latestZoomScale > 1.02f) {
                                                    val delta = c.position - c.previousPosition
                                                    if (delta != Offset.Zero) {
                                                        panOffset = clampCropPan(latestPanOffset + delta, latestZoomScale)
                                                        c.consume()
                                                    }
                                                } else if (latestCanSwipePages && !wasMultiTouch) {
                                                    val dx = c.position.x - down.position.x
                                                    val dy = c.position.y - down.position.y
                                                    if (!isSwipingPage && abs(dx) > tapSlopPx && abs(dx) > abs(dy) * 1.15f) {
                                                        isSwipingPage = true
                                                    }
                                                    if (isSwipingPage) {
                                                        swipeTotalDx = dx
                                                        swipeTotalDy = dy
                                                        latestOnSwipeDragDelta(dx)
                                                        c.consume()
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (isSwipingPage) {
                                        latestOnSwipeRelease(swipeTotalDx, swipeTotalDy)
                                        isSwipingPage = false
                                    } else if (maxMovedPx < tapSlopPx && !isPinching && !wasMultiTouch) {
                                        // Double-tap on image toggles 1x <-> 2.2x centered on tap point
                                        val nowMs = System.currentTimeMillis()
                                        val tapDist = hypot(
                                            down.position.x - lastTapPos.x,
                                            down.position.y - lastTapPos.y
                                        )
                                        if (nowMs - lastTapTimeMs < 290L && tapDist < doubleTapDistPx) {
                                            if (latestZoomScale > 1.05f) {
                                                zoomScale = 1f
                                                panOffset = Offset.Zero
                                            } else {
                                                applyCropZoomAt(2.2f, down.position)
                                            }
                                            lastTapTimeMs = 0L
                                        } else if (nowMs - downTimeMs < 260L) {
                                            lastTapTimeMs = nowMs
                                            lastTapPos = down.position
                                        }
                                    }
                                } finally {
                                    if (isSwipingPage) {
                                        latestOnSwipeDragDelta(0f)
                                    }
                                }
                            }
                        }
                    }
            ) {
                if (boxSize.width <= 0 || boxSize.height <= 0) return@Canvas
                val bitmapPts = DocumentImageProcessor.denormalizeQuadToBitmapPoints(
                    quad = displayedQuad,
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height
                )
                val tl = bitmapPointToScreenAt(bitmapPts[0], zoomScale, panOffset)
                val tr = bitmapPointToScreenAt(bitmapPts[1], zoomScale, panOffset)
                val br = bitmapPointToScreenAt(bitmapPts[2], zoomScale, panOffset)
                val bl = bitmapPointToScreenAt(bitmapPts[3], zoomScale, panOffset)

                val path = Path().apply {
                    moveTo(tl.x, tl.y)
                    lineTo(tr.x, tr.y)
                    lineTo(br.x, br.y)
                    lineTo(bl.x, bl.y)
                    close()
                }
                drawPath(path, cropEdgeCyan.copy(alpha = 0.12f))
                drawPath(
                    path,
                    brush = Brush.linearGradient(
                        listOf(cropEdgeCyan, cropEdgePurple)
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
                        color = cropEdgeCyan.copy(alpha = if (isSelected) 0.55f else 0.30f),
                        radius = (if (isSelected) 20.dp else 14.dp).toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = cropEdgeCyan,
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
                        color = cropEdgePurple,
                        radius = (if (isSelected) 9.5.dp else 7.dp).toPx(),
                        center = mid
                    )
                    drawCircle(Color.White, radius = (if (isSelected) 4.dp else 3.dp).toPx(), center = mid)
                }
            }

            // Compact Frosted Liquid Glass Zoom Pill (allows single-tap & reset alongside 2-finger pinch)
            if (boxSize.width > 0 && boxSize.height > 0 && activeHandle == null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .iosLiquidGlass(CircleShape, glowAccent = zoomScale > 1.03f, fillAlpha = 0.78f)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .clickable(enabled = zoomScale > 1.02f) {
                                applyCropZoomAt((zoomScale - 0.5f).coerceAtLeast(1f), Offset(cw * 0.5f, ch * 0.5f))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "−",
                            color = if (zoomScale > 1.02f) ScannerGlassPalette.TextPrimary else ScannerGlassPalette.TextSecondary.copy(alpha = 0.45f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.1fx", zoomScale),
                        color = if (zoomScale > 1.03f) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(enabled = zoomScale > 1.03f) {
                                zoomScale = 1f
                                panOffset = Offset.Zero
                            }
                            .padding(horizontal = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .clickable(enabled = zoomScale < 4.95f) {
                                applyCropZoomAt((zoomScale + 0.5f).coerceAtMost(5f), Offset(cw * 0.5f, ch * 0.5f))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            color = if (zoomScale < 4.95f) ScannerGlassPalette.TextPrimary else ScannerGlassPalette.TextSecondary.copy(alpha = 0.45f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Zero-Allocation Precision Magnifier Loupe (tracks all 4 corners + 4 edge mid-handles)
            val activePos = activeAnchorScreenPos
            if (activeHandle != null && activePos != null && boxSize.width > 0 && boxSize.height > 0) {
                val norm = screenToNormAt(activePos, zoomScale, panOffset)
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
                        .background(ScannerGlassPalette.DeepObsidian)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(
                                listOf(cropEdgeCyan, cropEdgePurple)
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
                            cropEdgeCyan.copy(alpha = 0.85f),
                            Offset(size.width / 2f, 0f),
                            Offset(size.width / 2f, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            cropEdgeCyan.copy(alpha = 0.85f),
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
                            color = cropEdgeCyan,
                            radius = 2.5.dp.toPx(),
                            center = Offset(size.width / 2f, size.height / 2f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .clip(CircleShape)
                            .background(ScannerGlassPalette.FrostedElevated)
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
}

@Composable
private fun BeforeAfterComparisonViewport(
    frameId: String,
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap,
    splitFraction: Float,
    showSplitCompare: Boolean,
    isRenderingPreview: Boolean,
    isArabic: Boolean,
    canSwipePages: Boolean,
    onToggleCompareMode: () -> Unit,
    onSplitFractionChange: (Float) -> Unit,
    onSwipeDragDelta: (Float) -> Unit,
    onSwipeRelease: (Float, Float) -> Unit
) {
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var zoomScale by remember(frameId) { mutableFloatStateOf(1f) }
    var panOffset by remember(frameId) { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val beforeImg = remember(beforeBitmap) { beforeBitmap.asImageBitmap() }
    val afterImg = remember(afterBitmap) { afterBitmap.asImageBitmap() }

    val latestZoomScale by rememberUpdatedState(zoomScale)
    val latestPanOffset by rememberUpdatedState(panOffset)
    val latestSplitFraction by rememberUpdatedState(splitFraction)
    val latestCanSwipePages by rememberUpdatedState(canSwipePages)
    val latestOnSwipeDragDelta by rememberUpdatedState(onSwipeDragDelta)
    val latestOnSwipeRelease by rememberUpdatedState(onSwipeRelease)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Non-overlapping Top Controls Strip (Compare Pill + Render Indicator + Compact Zoom Pill)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .iosLiquidGlass(CircleShape, glowAccent = showSplitCompare, fillAlpha = 0.82f)
                    .clickable { onToggleCompareMode() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isRenderingPreview) {
                    CircularProgressIndicator(
                        color = ScannerGlassPalette.CyanGlow,
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (showSplitCompare) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.EmeraldGlow)
                    )
                }
                Text(
                    text = if (showSplitCompare) {
                        if (isArabic) "مقارنة قبل / بعد" else "Split Compare"
                    } else {
                        if (isArabic) "معاينة كاملة" else "Full Preview"
                    },
                    color = ScannerGlassPalette.TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val stageCw = viewportSize.width.toFloat().coerceAtLeast(1f)
            val stageCh = viewportSize.height.toFloat().coerceAtLeast(1f)
            fun zoomAtCenter(nextScale: Float) {
                val clampedScale = nextScale.coerceIn(1f, 5f)
                if (clampedScale <= 1.001f) {
                    zoomScale = 1f
                    panOffset = Offset.Zero
                } else {
                    val focal = Offset(stageCw * 0.5f, stageCh * 0.5f)
                    val ratio = clampedScale / zoomScale
                    val rawPan = focal - (focal - panOffset) * ratio
                    val minX = stageCw * (1f - clampedScale)
                    val minY = stageCh * (1f - clampedScale)
                    zoomScale = clampedScale
                    panOffset = Offset(rawPan.x.coerceIn(minX, 0f), rawPan.y.coerceIn(minY, 0f))
                }
            }

            Row(
                modifier = Modifier
                    .iosLiquidGlass(CircleShape, glowAccent = zoomScale > 1.03f, fillAlpha = 0.78f)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(enabled = zoomScale > 1.02f) {
                            zoomAtCenter((zoomScale - 0.5f).coerceAtLeast(1f))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "−",
                        color = if (zoomScale > 1.02f) ScannerGlassPalette.TextPrimary else ScannerGlassPalette.TextSecondary.copy(alpha = 0.45f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = String.format(Locale.US, "%.1fx", zoomScale),
                    color = if (zoomScale > 1.03f) ScannerGlassPalette.CyanGlow else ScannerGlassPalette.TextSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = zoomScale > 1.03f) {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        }
                        .padding(horizontal = 3.dp)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(enabled = zoomScale < 4.95f) {
                            zoomAtCenter((zoomScale + 0.5f).coerceAtMost(5f))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = if (zoomScale < 4.95f) ScannerGlassPalette.TextPrimary else ScannerGlassPalette.TextSecondary.copy(alpha = 0.45f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Force physical LTR coordinate space for the Before/After wipe & natural-dimension preview
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .onSizeChanged { viewportSize = it },
                contentAlignment = Alignment.Center
            ) {
                val imgAspect = (afterBitmap.width.toFloat() / afterBitmap.height.coerceAtLeast(1).toFloat())
                    .coerceIn(0.18f, 5.5f)
                val padEdge = 6.dp
                val availW = (maxWidth - padEdge * 2).coerceAtLeast(1.dp)
                val availH = (maxHeight - padEdge * 2).coerceAtLeast(1.dp)
                val containerAspect = availW / availH
                val (paperW, paperH) = if (containerAspect > imgAspect) {
                    (availH * imgAspect) to availH
                } else {
                    availW to (availW / imgAspect)
                }

                val paperWpx = with(density) { paperW.toPx() }.coerceAtLeast(1f)
                val cw = viewportSize.width.toFloat().coerceAtLeast(1f)
                val ch = viewportSize.height.toFloat().coerceAtLeast(1f)
                val paperLeftPx = (cw - paperWpx) * 0.5f

                fun clampEnhancePan(raw: Offset, scale: Float): Offset {
                    if (scale <= 1.001f || viewportSize.width <= 0 || viewportSize.height <= 0) return Offset.Zero
                    val minX = cw * (1f - scale)
                    val minY = ch * (1f - scale)
                    return Offset(
                        x = raw.x.coerceIn(minX, 0f),
                        y = raw.y.coerceIn(minY, 0f)
                    )
                }

                fun applyEnhanceZoomAt(targetScale: Float, focalScreen: Offset) {
                    val clampedScale = targetScale.coerceIn(1f, 5f)
                    val ratio = clampedScale / zoomScale
                    val rawPan = focalScreen - (focalScreen - panOffset) * ratio
                    zoomScale = clampedScale
                    panOffset = clampEnhancePan(rawPan, clampedScale)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(frameId, viewportSize, showSplitCompare, paperWpx) {
                            if (viewportSize.width <= 0 || viewportSize.height <= 0) return@pointerInput
                            val dividerGrabPx = with(density) { 42.dp.toPx() }
                            val tapSlopPx = with(density) { 12.dp.toPx() }
                            val doubleTapDistPx = with(density) { 36.dp.toPx() }
                            var lastTapTimeMs = 0L
                            var lastTapPos = Offset.Zero

                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val downTimeMs = System.currentTimeMillis()
                                val s0 = latestZoomScale
                                val p0 = latestPanOffset
                                val splitScreenX = (paperLeftPx + paperWpx * latestSplitFraction) * s0 + p0.x
                                val hitSplitBeam = abs(down.position.x - splitScreenX) <= dividerGrabPx
                                var draggingDivider = showSplitCompare &&
                                    (hitSplitBeam || (!latestCanSwipePages && s0 <= 1.02f))
                                var isPinching = false
                                var isSwipingPage = false
                                var swipeTotalDx = 0f
                                var swipeTotalDy = 0f
                                var wasMultiTouch = false
                                var prevCentroid = Offset.Zero
                                var prevSpan = 0f
                                var maxMovedPx = 0f

                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pressed = event.changes.filter { it.pressed }
                                        if (pressed.isEmpty()) break

                                        if (pressed.size >= 2) {
                                            draggingDivider = false
                                            wasMultiTouch = true
                                            if (isSwipingPage) {
                                                isSwipingPage = false
                                                latestOnSwipeDragDelta(0f)
                                            }
                                            val pA = pressed[0].position
                                            val pB = pressed[1].position
                                            val currCentroid = Offset((pA.x + pB.x) * 0.5f, (pA.y + pB.y) * 0.5f)
                                            val currSpan = hypot(pA.x - pB.x, pA.y - pB.y).coerceAtLeast(1f)
                                            if (!isPinching) {
                                                isPinching = true
                                                prevCentroid = currCentroid
                                                prevSpan = currSpan
                                            } else {
                                                val zoomFactor = currSpan / prevSpan.coerceAtLeast(1f)
                                                val curScale = latestZoomScale
                                                val curPan = latestPanOffset
                                                val newScale = (curScale * zoomFactor).coerceIn(1f, 5f)
                                                val ratio = newScale / curScale
                                                val rawPan = currCentroid - (prevCentroid - curPan) * ratio
                                                zoomScale = newScale
                                                panOffset = clampEnhancePan(rawPan, newScale)
                                                prevCentroid = currCentroid
                                                prevSpan = currSpan
                                            }
                                            maxMovedPx = tapSlopPx + 10f
                                            pressed.forEach { it.consume() }
                                        } else {
                                            if (isPinching) {
                                                isPinching = false
                                            } else {
                                                val c = pressed[0]
                                                val distFromDown = hypot(
                                                    c.position.x - down.position.x,
                                                    c.position.y - down.position.y
                                                )
                                                if (distFromDown > maxMovedPx) maxMovedPx = distFromDown

                                                if (draggingDivider && showSplitCompare) {
                                                    val contentX = (c.position.x - latestPanOffset.x) / latestZoomScale
                                                    val frac = ((contentX - paperLeftPx) / paperWpx).coerceIn(0.05f, 0.95f)
                                                    onSplitFractionChange(frac)
                                                    c.consume()
                                                } else if (latestZoomScale > 1.02f) {
                                                    val delta = c.position - c.previousPosition
                                                    if (delta != Offset.Zero) {
                                                        panOffset = clampEnhancePan(latestPanOffset + delta, latestZoomScale)
                                                        c.consume()
                                                    }
                                                } else if (latestCanSwipePages && !wasMultiTouch) {
                                                    val dx = c.position.x - down.position.x
                                                    val dy = c.position.y - down.position.y
                                                    if (!isSwipingPage && abs(dx) > tapSlopPx && abs(dx) > abs(dy) * 1.15f) {
                                                        isSwipingPage = true
                                                    }
                                                    if (isSwipingPage) {
                                                        swipeTotalDx = dx
                                                        swipeTotalDy = dy
                                                        latestOnSwipeDragDelta(dx)
                                                        c.consume()
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (isSwipingPage) {
                                        latestOnSwipeRelease(swipeTotalDx, swipeTotalDy)
                                        isSwipingPage = false
                                    } else if (maxMovedPx < tapSlopPx && !isPinching && !wasMultiTouch) {
                                        val nowMs = System.currentTimeMillis()
                                        val tapDist = hypot(
                                            down.position.x - lastTapPos.x,
                                            down.position.y - lastTapPos.y
                                        )
                                        if (nowMs - lastTapTimeMs < 290L && tapDist < doubleTapDistPx) {
                                            if (latestZoomScale > 1.05f) {
                                                zoomScale = 1f
                                                panOffset = Offset.Zero
                                            } else {
                                                applyEnhanceZoomAt(2.2f, down.position)
                                            }
                                            lastTapTimeMs = 0L
                                        } else if (nowMs - downTimeMs < 260L) {
                                            lastTapTimeMs = nowMs
                                            lastTapPos = down.position
                                        }
                                    }
                                } finally {
                                    if (isSwipingPage) {
                                        latestOnSwipeDragDelta(0f)
                                    }
                                }
                            }
                        }
                ) {
                    // Zoomable / Pannable Layer displaying the document in its true natural aspect ratio
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0f, 0f)
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(paperW, paperH)
                                .shadow(14.dp, RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Full Enhanced (After) Layer at true dimensions
                            Image(
                                bitmap = afterImg,
                                contentDescription = "Enhanced preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (showSplitCompare) {
                                // Clipped Original (Before) Layer on the physical Left side
                                Image(
                                    bitmap = beforeImg,
                                    contentDescription = "Original preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .drawWithContent {
                                            clipRect(
                                                left = 0f,
                                                top = 0f,
                                                right = size.width * splitFraction,
                                                bottom = size.height
                                            ) {
                                                this@drawWithContent.drawContent()
                                            }
                                        }
                                )
                            }
                        }
                    }

                    if (showSplitCompare && viewportSize.width > 0 && viewportSize.height > 0) {
                        // Glowing Split Divider Beam & Frosted Handle in screen space
                        val beamCyan = ScannerGlassPalette.EdgeOverlayCyan
                        val beamPurple = ScannerGlassPalette.EdgeOverlayPurple
                        val beamHandleFill = ScannerGlassPalette.FrostedElevated
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val paperTopPx = (size.height - paperH.toPx()) * 0.5f
                            val paperBotPx = (size.height + paperH.toPx()) * 0.5f
                            val splitContentX = paperLeftPx + paperWpx * splitFraction
                            val splitX = splitContentX * zoomScale + panOffset.x
                            val lineTop = (paperTopPx * zoomScale + panOffset.y).coerceIn(0f, size.height)
                            val lineBot = (paperBotPx * zoomScale + panOffset.y).coerceIn(0f, size.height)

                            drawLine(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        beamCyan,
                                        beamPurple,
                                        beamCyan
                                    )
                                ),
                                start = Offset(splitX, lineTop),
                                end = Offset(splitX, lineBot),
                                strokeWidth = 2.2.dp.toPx()
                            )
                            val cy = ((lineTop + lineBot) * 0.5f).coerceIn(20.dp.toPx(), size.height - 20.dp.toPx())
                            drawCircle(
                                color = beamHandleFill,
                                radius = 17.dp.toPx(),
                                center = Offset(splitX, cy)
                            )
                            drawCircle(
                                brush = Brush.linearGradient(
                                    listOf(beamCyan, beamPurple)
                                ),
                                radius = 17.dp.toPx(),
                                center = Offset(splitX, cy),
                                style = Stroke(width = 1.8.dp.toPx())
                            )
                            val barHalfH = 5.dp.toPx()
                            val gap = 3.5.dp.toPx()
                            drawLine(
                                color = beamCyan,
                                start = Offset(splitX - gap, cy - barHalfH),
                                end = Offset(splitX - gap, cy + barHalfH),
                                strokeWidth = 1.6.dp.toPx()
                            )
                            drawLine(
                                color = beamCyan,
                                start = Offset(splitX + gap, cy - barHalfH),
                                end = Offset(splitX + gap, cy + barHalfH),
                                strokeWidth = 1.6.dp.toPx()
                            )
                        }

                        // Before / After Liquid Glass Badges
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .iosLiquidGlass(CircleShape, fillAlpha = 0.80f)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "قبل" else "Before",
                                    color = ScannerGlassPalette.TextPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(ScannerGlassPalette.CyanGlow.copy(alpha = 0.90f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "بعد" else "After",
                                    color = ScannerGlassPalette.OnAccent,
                                    fontSize = 10.5.sp,
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
