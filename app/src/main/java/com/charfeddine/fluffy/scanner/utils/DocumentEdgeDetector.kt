package com.charfeddine.fluffy.scanner.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageProxy
import com.charfeddine.fluffy.scanner.data.model.DocumentQuad
import com.charfeddine.fluffy.scanner.data.model.PointNorm
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class EdgeDetectionFrameResult(
    val quad: DocumentQuad,
    val confidence: Float,
    val isLocked: Boolean = false,
    val frameAspectRatio: Float = 0.75f,
    val frameWidth: Int = 0,
    val frameHeight: Int = 0
)

data class TrackedEdgeState(
    val quad: DocumentQuad = DocumentQuad.SAFE_DEFAULT_MARGIN,
    val confidence: Float = 0f,
    val isLocked: Boolean = false,
    val stabilityProgress: Float = 0f,
    val shouldAutoCapture: Boolean = false,
    val frameAspectRatio: Float = 0.75f,
    val frameWidth: Int = 0,
    val frameHeight: Int = 0
)

/**
 * 100% Offline, Multi-Stage Computer Vision Document Boundary & Line-Intersection Engine.
 *
 * Designed for BOTH:
 *  - Real-time CameraX ImageAnalysis frames (reusable-buffer CLAHE + Morphology + Line-Intersection + Temporal Smoothing).
 *  - High-accuracy EXIF-Normalized Static / Gallery Photos (`detectFromBitmap`):
 *      1. Temporary downscaled working copy (max dimension ~800px) with exact `scaleX` / `scaleY`
 *         coordinate mapping back to original full-resolution dimensions.
 *      2. Pass 1 (Standard Contrast): Grayscale -> CLAHE -> 5x5 Gaussian Blur ->
 *         Separable Morphological Closing -> Adaptive Edge Hysteresis.
 *      3. Pass 2 (White-on-Light / Low-Contrast Fallback): HSV Saturation/Value + Border Chroma
 *         contrast -> Integral-Image Adaptive Thresholding Mask Perimeter + Sensitive Hysteresis.
 *      4. Pure Line-Intersection Quadrilateral Reconstruction via trimmed 2x2 PCA orthogonal line
 *         regression, multi-angle + PCA-oriented corner seeding, and geometric line intersections
 *         sorted strictly `[Top-Left, Top-Right, Bottom-Right, Bottom-Left]`.
 *      5. Graceful 8% inset fallback ([DocumentQuad.SAFE_DEFAULT_MARGIN]) if no document is found.
 */
object DocumentEdgeDetector {

    private const val LIVE_ANALYZE_W = 240
    private const val LIVE_ANALYZE_H = 320
    private const val STATIC_WORK_MAX_DIM = 800
    private const val CLAHE_TILES_X = 8
    private const val CLAHE_TILES_Y = 8
    private const val HIST_BINS = 64

    private class LiveWorkspace(maxPixels: Int) {
        val luma = IntArray(maxPixels)
        val stretched = IntArray(maxPixels)
        val temp = IntArray(maxPixels)
        val blurred = IntArray(maxPixels)
        val closed = IntArray(maxPixels)
        val hDil = IntArray(maxPixels)
        val gx = IntArray(maxPixels)
        val gy = IntArray(maxPixels)
        val mag = IntArray(maxPixels)
        val dir = ByteArray(maxPixels)
        val nms = IntArray(maxPixels)
        val strong = BooleanArray(maxPixels)
        val weak = BooleanArray(maxPixels)
        val queue = IntArray(maxPixels)
        val visited = BooleanArray(maxPixels)
        val stack = IntArray(maxPixels)
        val histBins = IntArray(256)
        val claheHist = IntArray(HIST_BINS)
        val claheLuts = Array(CLAHE_TILES_Y) { Array(CLAHE_TILES_X) { IntArray(HIST_BINS) } }
    }

    private val liveWorkspaceLocal = object : ThreadLocal<LiveWorkspace>() {
        override fun initialValue(): LiveWorkspace = LiveWorkspace(LIVE_ANALYZE_W * LIVE_ANALYZE_H)
    }

    private data class PxPt(val x: Int, val y: Int)
    private data class FloatPt(val x: Float, val y: Float)

    /**
     * Orthogonal line representation: a*x + b*y + c = 0 with a^2 + b^2 = 1.
     */
    private data class OrthoLine(val a: Double, val b: Double, val c: Double)

    /**
     * Real-time zero-allocation CameraX Y-plane analyzer with upright axis normalization,
     * tile CLAHE local contrast enhancement, morphological text suppression, and line-intersection corners.
     */
    fun detectFromImageProxy(
        imageProxy: ImageProxy,
        isIdCardHint: Boolean = false
    ): EdgeDetectionFrameResult {
        val plane = imageProxy.planes.firstOrNull()
            ?: return EdgeDetectionFrameResult(DocumentQuad.SAFE_DEFAULT_MARGIN, 0f, false)
        val srcW = imageProxy.width
        val srcH = imageProxy.height
        if (srcW < 32 || srcH < 32) {
            return EdgeDetectionFrameResult(DocumentQuad.SAFE_DEFAULT_MARGIN, 0f, false)
        }

        val rotation = ((imageProxy.imageInfo.rotationDegrees % 360) + 360) % 360
        val rotW = if (rotation == 90 || rotation == 270) srcH else srcW
        val rotH = if (rotation == 90 || rotation == 270) srcW else srcH
        val rotAspect = rotW.toFloat() / rotH.toFloat()

        val gridW = if (rotAspect >= 1f) LIVE_ANALYZE_H else LIVE_ANALYZE_W
        val gridH = if (rotAspect >= 1f) LIVE_ANALYZE_W else LIVE_ANALYZE_H

        val ws = liveWorkspaceLocal.get() ?: LiveWorkspace(LIVE_ANALYZE_W * LIVE_ANALYZE_H)
        val buffer = plane.buffer
        buffer.rewind()
        val limit = buffer.limit()
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        val luma = ws.luma
        val wMinus1 = srcW - 1
        val hMinus1 = srcH - 1
        val gwMinus1 = (gridW - 1).coerceAtLeast(1).toFloat()
        val ghMinus1 = (gridH - 1).coerceAtLeast(1).toFloat()
        val sampleStepX = max(1, srcW / (gridW * 2))
        val sampleStepY = max(1, srcH / (gridH * 2))

        // Extract raw Y-plane luminance rotated into upright display axes with 2x2 noise-averaging
        for (gy in 0 until gridH) {
            val v = gy / ghMinus1
            val gridRow = gy * gridW
            for (gx in 0 until gridW) {
                val u = gx / gwMinus1
                val cx: Int
                val cy: Int
                when (rotation) {
                    90 -> {
                        cx = (v * wMinus1).toInt()
                        cy = ((1f - u) * hMinus1).toInt()
                    }
                    180 -> {
                        cx = ((1f - u) * wMinus1).toInt()
                        cy = ((1f - v) * hMinus1).toInt()
                    }
                    270 -> {
                        cx = ((1f - v) * wMinus1).toInt()
                        cy = (u * hMinus1).toInt()
                    }
                    else -> {
                        cx = (u * wMinus1).toInt()
                        cy = (v * hMinus1).toInt()
                    }
                }
                val x0 = cx.coerceIn(0, wMinus1)
                val y0 = cy.coerceIn(0, hMinus1)
                val x1 = (x0 + sampleStepX).coerceAtMost(wMinus1)
                val y1 = (y0 + sampleStepY).coerceAtMost(hMinus1)

                val i00 = y0 * rowStride + x0 * pixelStride
                val i10 = y0 * rowStride + x1 * pixelStride
                val i01 = y1 * rowStride + x0 * pixelStride
                val i11 = y1 * rowStride + x1 * pixelStride

                val y00 = if (i00 in 0 until limit) buffer.get(i00).toInt() and 0xFF else 128
                val y10 = if (i10 in 0 until limit) buffer.get(i10).toInt() and 0xFF else y00
                val y01 = if (i01 in 0 until limit) buffer.get(i01).toInt() and 0xFF else y00
                val y11 = if (i11 in 0 until limit) buffer.get(i11).toInt() and 0xFF else y00

                luma[gridRow + gx] = (y00 + y10 + y01 + y11) shr 2
            }
        }

        val res = runLiveFastPass(
            ws = ws,
            w = gridW,
            h = gridH,
            isIdCardHint = isIdCardHint
        )
        return res.copy(
            frameAspectRatio = rotAspect,
            frameWidth = rotW,
            frameHeight = rotH
        )
    }

    /**
     * Real-time zero-allocation live Y-plane pipeline:
     * Tile CLAHE -> 5x5 Gaussian -> 3x3 Morphological Close -> Edge Hysteresis -> Line-Intersection Quad.
     */
    private fun runLiveFastPass(
        ws: LiveWorkspace,
        w: Int,
        h: Int,
        isIdCardHint: Boolean = false
    ): EdgeDetectionFrameResult {
        val n = w * h
        val luma = ws.luma
        val stretched = ws.stretched

        // 1. Zero-allocation 8x8 tile CLAHE separates low-contrast documents & ID cards from busy desks
        applyClaheInPlace(
            src = luma,
            dst = stretched,
            w = w,
            h = h,
            hist = ws.claheHist,
            luts = ws.claheLuts
        )

        // 2. Separable 5x5 Gaussian Blur (zero-alloc into ws.temp -> ws.blurred)
        val temp = ws.temp
        val blurred = ws.blurred
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val xM2 = max(0, x - 2)
                val xM1 = max(0, x - 1)
                val xP1 = min(w - 1, x + 1)
                val xP2 = min(w - 1, x + 2)
                temp[row + x] = (
                    stretched[row + xM2] +
                        4 * stretched[row + xM1] +
                        6 * stretched[row + x] +
                        4 * stretched[row + xP1] +
                        stretched[row + xP2]
                    ) shr 4
            }
        }
        for (y in 0 until h) {
            val yM2 = max(0, y - 2) * w
            val yM1 = max(0, y - 1) * w
            val y0 = y * w
            val yP1 = min(h - 1, y + 1) * w
            val yP2 = min(h - 1, y + 2) * w
            for (x in 0 until w) {
                blurred[y0 + x] = (
                    temp[yM2 + x] +
                        4 * temp[yM1 + x] +
                        6 * temp[y0 + x] +
                        4 * temp[yP1 + x] +
                        temp[yP2 + x]
                    ) shr 4
            }
        }

        // 3. Fast 3x3 Separable Morphological Closing (radius = 1, reuses ws.hDil / ws.temp / ws.closed)
        val hDil = ws.hDil
        val vDil = ws.temp
        val closed = ws.closed
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val xm = max(0, x - 1)
                val xp = min(w - 1, x + 1)
                hDil[row + x] = max(blurred[row + xm], max(blurred[row + x], blurred[row + xp]))
            }
        }
        for (y in 0 until h) {
            val ym = max(0, y - 1) * w
            val y0 = y * w
            val yp = min(h - 1, y + 1) * w
            for (x in 0 until w) {
                vDil[y0 + x] = max(hDil[ym + x], max(hDil[y0 + x], hDil[yp + x]))
            }
        }
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val xm = max(0, x - 1)
                val xp = min(w - 1, x + 1)
                hDil[row + x] = min(vDil[row + xm], min(vDil[row + x], vDil[row + xp]))
            }
        }
        for (y in 0 until h) {
            val ym = max(0, y - 1) * w
            val y0 = y * w
            val yp = min(h - 1, y + 1) * w
            for (x in 0 until w) {
                closed[y0 + x] = min(hDil[ym + x], min(hDil[y0 + x], hDil[yp + x]))
            }
        }

        // 4. Zero-alloc Scharr/Sobel + NMS + BFS Canny Edge Detection
        val gxArr = ws.gx
        val gyArr = ws.gy
        val mag = ws.mag
        val dir = ws.dir
        val nms = ws.nms
        val strong = ws.strong
        val weak = ws.weak
        val queue = ws.queue
        val histBins = ws.histBins
        gxArr.fill(0, 0, n)
        gyArr.fill(0, 0, n)
        mag.fill(0, 0, n)
        nms.fill(0, 0, n)
        strong.fill(false, 0, n)
        weak.fill(false, 0, n)
        histBins.fill(0)

        for (y in 2 until h - 2) {
            val ym = (y - 1) * w
            val y0 = y * w
            val yp = (y + 1) * w
            for (x in 2 until w - 2) {
                val gx = if (isIdCardHint) {
                    // Scharr 3x3 for superior rotational invariance on tilted ID cards
                    (-3 * closed[ym + x - 1] + 3 * closed[ym + x + 1] -
                        10 * closed[y0 + x - 1] + 10 * closed[y0 + x + 1] -
                        3 * closed[yp + x - 1] + 3 * closed[yp + x + 1]) shr 2
                } else {
                    -closed[ym + x - 1] + closed[ym + x + 1] -
                        2 * closed[y0 + x - 1] + 2 * closed[y0 + x + 1] -
                        closed[yp + x - 1] + closed[yp + x + 1]
                }
                val gy = if (isIdCardHint) {
                    (-3 * closed[ym + x - 1] - 10 * closed[ym + x] - 3 * closed[ym + x + 1] +
                        3 * closed[yp + x - 1] + 10 * closed[yp + x] + 3 * closed[yp + x + 1]) shr 2
                } else {
                    -closed[ym + x - 1] - 2 * closed[ym + x] - closed[ym + x + 1] +
                        closed[yp + x - 1] + 2 * closed[yp + x] + closed[yp + x + 1]
                }
                val m = abs(gx) + abs(gy)
                val idx = y0 + x
                gxArr[idx] = gx
                gyArr[idx] = gy
                mag[idx] = m
                if (m > 10) {
                    val ax = abs(gx)
                    val ay = abs(gy)
                    // Fast octant quantization without costly Math.toDegrees(atan2)
                    dir[idx] = when {
                        ay * 2.414f < ax -> 0
                        ax * 2.414f < ay -> 2
                        (gx xor gy) >= 0 -> 3
                        else -> 1
                    }.toByte()
                }
            }
        }

        var nonZeroCount = 0
        for (y in 3 until h - 3) {
            val y0 = y * w
            for (x in 3 until w - 3) {
                val idx = y0 + x
                val m = mag[idx]
                if (m <= 12) continue
                val (n1, n2) = when (dir[idx].toInt()) {
                    0 -> mag[idx - 1] to mag[idx + 1]
                    1 -> mag[idx - w + 1] to mag[idx + w - 1]
                    2 -> mag[idx - w] to mag[idx + w]
                    else -> mag[idx - w - 1] to mag[idx + w + 1]
                }
                if (m >= n1 && m >= n2) {
                    nms[idx] = m
                    histBins[min(255, m shr 2)]++
                    nonZeroCount++
                }
            }
        }

        if (nonZeroCount < 24) {
            return EdgeDetectionFrameResult(DocumentQuad.SAFE_DEFAULT_MARGIN, 0f, false)
        }

        val targetRank = (nonZeroCount * 0.78f).toInt()
        var acc = 0
        var highThresh = 58
        for (b in 0 until 256) {
            acc += histBins[b]
            if (acc >= targetRank) {
                highThresh = max(42, b shl 2)
                break
            }
        }
        val lowThresh = max(14, (highThresh * 0.42f).toInt())

        var head = 0
        var tail = 0
        for (y in 3 until h - 3) {
            val row = y * w
            for (x in 3 until w - 3) {
                val idx = row + x
                val v = nms[idx]
                if (v >= highThresh) {
                    strong[idx] = true
                    queue[tail++] = idx
                } else if (v >= lowThresh) {
                    weak[idx] = true
                }
            }
        }

        while (head < tail) {
            val cur = queue[head++]
            val cy = cur / w
            val cx = cur - cy * w
            for (dy in -1..1) {
                val ny = cy + dy
                if (ny !in 2 until h - 2) continue
                val nyRow = ny * w
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = cx + dx
                    if (nx !in 2 until w - 2) continue
                    val nIdx = nyRow + nx
                    if (weak[nIdx] && !strong[nIdx]) {
                        strong[nIdx] = true
                        queue[tail++] = nIdx
                    }
                }
            }
        }

        val contourRes = findBestDocumentQuad(
            edges = strong,
            w = w,
            h = h,
            origW = w,
            origH = h,
            scaleX = 1f,
            scaleY = 1f,
            reusableVisited = ws.visited,
            reusableStack = ws.stack,
            isIdCardHint = isIdCardHint
        )
        if (!isIdCardHint || (contourRes.isLocked && contourRes.confidence >= 0.62f && !isBorderHuggingQuad(contourRes.quad))) {
            return contourRes
        }
        val houghRes = runHoughRansacRectanglePass(
            edges = strong,
            gradMag = mag,
            gxArr = gxArr,
            gyArr = gyArr,
            w = w,
            h = h,
            origW = w,
            origH = h,
            scaleX = 1f,
            scaleY = 1f,
            isIdCardHint = true
        )
        return when {
            houghRes.isLocked && contourRes.isLocked ->
                if (houghRes.confidence >= contourRes.confidence) houghRes else contourRes
            houghRes.isLocked -> houghRes
            else -> contourRes
        }
    }

    /**
     * Multi-scale Guided ROI + Full-Frame ID Card Detector:
     * Combines full-frame modern multi-cue detection with a cropped high-resolution ROI centered
     * around the viewfinder guide quad so printed edges, rounded corners, and tilted cards lock reliably
     * even when fingers hold the card or busy backgrounds surround it.
     */
    fun detectIdCardWithGuidance(bitmap: Bitmap, guideQuad: DocumentQuad? = null): DocumentQuad {
        val fullRes = detectFromBitmapInternal(bitmap, isIdCardHint = true)
        val effectiveGuide = guideQuad ?: DocumentQuad(
            topLeft = PointNorm(0.05f, 0.16f),
            topRight = PointNorm(0.95f, 0.16f),
            bottomRight = PointNorm(0.95f, 0.80f),
            bottomLeft = PointNorm(0.05f, 0.80f)
        )

        val gPts = effectiveGuide.asList()
        val gMinX = gPts.minOf { it.x }
        val gMaxX = gPts.maxOf { it.x }
        val gMinY = gPts.minOf { it.y }
        val gMaxY = gPts.maxOf { it.y }
        val gW = (gMaxX - gMinX).coerceAtLeast(0.15f)
        val gH = (gMaxY - gMinY).coerceAtLeast(0.12f)

        val roiMinX = (gMinX - gW * 0.28f).coerceIn(0f, 0.85f)
        val roiMaxX = (gMaxX + gW * 0.28f).coerceIn(roiMinX + 0.15f, 1f)
        val roiMinY = (gMinY - gH * 0.42f).coerceIn(0f, 0.85f)
        val roiMaxY = (gMaxY + gH * 0.42f).coerceIn(roiMinY + 0.15f, 1f)

        val cropX = (roiMinX * bitmap.width).roundToInt().coerceIn(0, (bitmap.width - 64).coerceAtLeast(0))
        val cropY = (roiMinY * bitmap.height).roundToInt().coerceIn(0, (bitmap.height - 64).coerceAtLeast(0))
        val cropW = ((roiMaxX - roiMinX) * bitmap.width).roundToInt().coerceIn(1, bitmap.width - cropX)
        val cropH = ((roiMaxY - roiMinY) * bitmap.height).roundToInt().coerceIn(1, bitmap.height - cropY)

        val shouldRunRoi = cropW >= 120 && cropH >= 90 &&
            (cropW < bitmap.width * 0.90f || cropH < bitmap.height * 0.90f)

        if (!shouldRunRoi) {
            return if (fullRes.isLocked) fullRes.quad else DocumentQuad.SAFE_DEFAULT_MARGIN
        }

        val roiBmp = runCatching { Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH) }.getOrNull()
            ?: return if (fullRes.isLocked) fullRes.quad else DocumentQuad.SAFE_DEFAULT_MARGIN

        val roiRes = try {
            detectFromBitmapInternal(roiBmp, isIdCardHint = true)
        } finally {
            if (roiBmp !== bitmap && !roiBmp.isRecycled) roiBmp.recycle()
        }

        if (!roiRes.isLocked || isBorderHuggingQuad(roiRes.quad)) {
            return if (fullRes.isLocked) fullRes.quad else DocumentQuad.SAFE_DEFAULT_MARGIN
        }

        fun mapPointFromRoi(p: PointNorm): PointNorm = PointNorm(
            x = ((cropX + p.x * cropW) / bitmap.width.toFloat()).coerceIn(0.01f, 0.99f),
            y = ((cropY + p.y * cropH) / bitmap.height.toFloat()).coerceIn(0.01f, 0.99f)
        )

        val mappedRoiQuad = DocumentQuad(
            topLeft = mapPointFromRoi(roiRes.quad.topLeft),
            topRight = mapPointFromRoi(roiRes.quad.topRight),
            bottomRight = mapPointFromRoi(roiRes.quad.bottomRight),
            bottomLeft = mapPointFromRoi(roiRes.quad.bottomLeft)
        )

        if (!fullRes.isLocked || isBorderHuggingQuad(fullRes.quad)) {
            return mappedRoiQuad
        }

        val fullAspectBonus = idAspectClosuresScore(fullRes.quad, bitmap.width, bitmap.height)
        val roiAspectBonus = idAspectClosuresScore(mappedRoiQuad, bitmap.width, bitmap.height)
        val fullTotal = fullRes.confidence + fullAspectBonus
        val roiTotal = roiRes.confidence + roiAspectBonus + 0.04f
        return if (roiTotal >= fullTotal) mappedRoiQuad else fullRes.quad
    }

    private fun idAspectClosuresScore(quad: DocumentQuad, w: Int, h: Int): Float {
        val topW = hypot(((quad.topRight.x - quad.topLeft.x) * w).toDouble(), ((quad.topRight.y - quad.topLeft.y) * h).toDouble())
        val botW = hypot(((quad.bottomRight.x - quad.bottomLeft.x) * w).toDouble(), ((quad.bottomRight.y - quad.bottomLeft.y) * h).toDouble())
        val leftH = hypot(((quad.bottomLeft.x - quad.topLeft.x) * w).toDouble(), ((quad.bottomLeft.y - quad.topLeft.y) * h).toDouble())
        val rightH = hypot(((quad.bottomRight.x - quad.topRight.x) * w).toDouble(), ((quad.bottomRight.y - quad.topRight.y) * h).toDouble())
        val avgW = (topW + botW) * 0.5
        val avgH = (leftH + rightH) * 0.5
        val ratio = (max(avgW, avgH) / min(avgW, avgH).coerceAtLeast(1.0)).toFloat()
        return (1f - (abs(ratio - 1.586f) / 0.70f)).coerceIn(0f, 1f) * 0.14f
    }

    /**
     * High-accuracy multi-stage document boundary detection for Gallery photos and captured Bitmaps.
     * Should be invoked on `Dispatchers.Default`.
     */
    fun detectFromBitmap(bitmap: Bitmap, isIdCardHint: Boolean = false): DocumentQuad =
        detectFromBitmapInternal(bitmap, isIdCardHint).let {
            if (it.isLocked) it.quad else DocumentQuad.SAFE_DEFAULT_MARGIN
        }

    private fun detectFromBitmapInternal(
        bitmap: Bitmap,
        isIdCardHint: Boolean = false
    ): EdgeDetectionFrameResult {
        val origW = bitmap.width
        val origH = bitmap.height
        if (origW < 32 || origH < 32) {
            return EdgeDetectionFrameResult(DocumentQuad.SAFE_DEFAULT_MARGIN, 0f, false)
        }

        // 1. Create temporary downscaled working copy (~800px max dimension) to suppress
        //    high-resolution inner text/paper grain noise while preserving outer boundaries.
        val maxOrig = max(origW, origH)
        val scale = if (maxOrig > STATIC_WORK_MAX_DIM) {
            STATIC_WORK_MAX_DIM.toFloat() / maxOrig.toFloat()
        } else {
            1f
        }
        val workW = (origW * scale).roundToInt().coerceAtLeast(64)
        val workH = (origH * scale).roundToInt().coerceAtLeast(64)
        val scaleX = workW.toFloat() / origW.toFloat()
        val scaleY = workH.toFloat() / origH.toFloat()

        val workBmp = if (workW != origW || workH != origH) {
            Bitmap.createScaledBitmap(bitmap, workW, workH, true)
        } else {
            bitmap
        }
        val pixels = IntArray(workW * workH)
        workBmp.getPixels(pixels, 0, workW, 0, 0, workW, workH)
        if (workBmp !== bitmap) {
            workBmp.recycle()
        }

        val rawLuma = IntArray(workW * workH) { i ->
            val c = pixels[i]
            (Color.red(c) * 77 + Color.green(c) * 150 + Color.blue(c) * 29) shr 8
        }

        val closingRadius = if (max(workW, workH) >= 420) 3 else 1

        // ID Card Mode: run CIE-Lab glare-suppressed isolation + multi-channel Scharr Hough-RANSAC passes
        val cardPass = if (isIdCardHint) {
            runIdCardIsolationPass(
                pixels = pixels,
                rawLuma = rawLuma,
                w = workW,
                h = workH,
                origW = origW,
                origH = origH,
                scaleX = scaleX,
                scaleY = scaleY,
                isIdCardHint = true
            )
        } else {
            null
        }
        val houghPass = if (isIdCardHint) {
            runMultiChannelHoughPass(
                pixels = pixels,
                rawLuma = rawLuma,
                w = workW,
                h = workH,
                closingRadius = closingRadius,
                origW = origW,
                origH = origH,
                scaleX = scaleX,
                scaleY = scaleY,
                isIdCardHint = true
            )
        } else {
            null
        }

        // 2. Pass 1: Standard Contrast (Grayscale -> CLAHE -> 5x5 Gaussian -> Morphological Closing -> Canny)
        val pass1 = runStandardPass(
            rawLuma = rawLuma,
            w = workW,
            h = workH,
            closingRadius = closingRadius,
            origW = origW,
            origH = origH,
            scaleX = scaleX,
            scaleY = scaleY,
            isIdCardHint = isIdCardHint
        )

        if (!isIdCardHint && pass1.isLocked && pass1.confidence >= 0.78f && !isBorderHuggingQuad(pass1.quad)) {
            return pass1
        }

        // 3. Pass 2: Fallback for White-on-Light / Low-Contrast / Colored Backgrounds
        val pass2 = runWhiteOnLightFallbackPass(
            pixels = pixels,
            rawLuma = rawLuma,
            w = workW,
            h = workH,
            closingRadius = closingRadius,
            origW = origW,
            origH = origH,
            scaleX = scaleX,
            scaleY = scaleY,
            isIdCardHint = isIdCardHint
        )

        val fallbackCardPass = cardPass ?: if (!pass1.isLocked || isBorderHuggingQuad(pass1.quad) || !pass2.isLocked) {
            runIdCardIsolationPass(
                pixels = pixels,
                rawLuma = rawLuma,
                w = workW,
                h = workH,
                origW = origW,
                origH = origH,
                scaleX = scaleX,
                scaleY = scaleY,
                isIdCardHint = isIdCardHint
            )
        } else {
            null
        }
        val fallbackHoughPass = houghPass ?: if (!pass1.isLocked || isBorderHuggingQuad(pass1.quad)) {
            runMultiChannelHoughPass(
                pixels = pixels,
                rawLuma = rawLuma,
                w = workW,
                h = workH,
                closingRadius = closingRadius,
                origW = origW,
                origH = origH,
                scaleX = scaleX,
                scaleY = scaleY,
                isIdCardHint = isIdCardHint
            )
        } else {
            null
        }

        val lockedCandidates = listOfNotNull(cardPass, houghPass, pass1, pass2, fallbackCardPass, fallbackHoughPass)
            .filter { it.isLocked }
        val nonBorderWinner = lockedCandidates
            .filterNot { isBorderHuggingQuad(it.quad) }
            .maxByOrNull { candidate ->
                val aspectBoost = if (isIdCardHint) idAspectClosuresScore(candidate.quad, origW, origH) else 0f
                candidate.confidence + aspectBoost
            }
        val winner = nonBorderWinner ?: lockedCandidates.maxByOrNull { it.confidence }

        return winner ?: EdgeDetectionFrameResult(DocumentQuad.SAFE_DEFAULT_MARGIN, 0f, false)
    }

    private fun runStandardPass(
        rawLuma: IntArray,
        w: Int,
        h: Int,
        closingRadius: Int,
        origW: Int,
        origH: Int,
        scaleX: Float,
        scaleY: Float,
        isIdCardHint: Boolean = false
    ): EdgeDetectionFrameResult {
        val clahe = applyClahe(rawLuma, w, h)
        val blurred = applyGaussianBlur5x5(clahe, w, h)
        val closed = applySeparableMorphClosing(blurred, w, h, closingRadius)
        val edges = detectCannyEdges(
            src = closed,
            w = w,
            h = h,
            percentile = 0.80f,
            minHighThresh = 46,
            minNmsFloor = 10
        )
        return findBestDocumentQuad(
            edges = edges,
            w = w,
            h = h,
            origW = origW,
            origH = origH,
            scaleX = scaleX,
            scaleY = scaleY,
            isIdCardHint = isIdCardHint
        )
    }

    /**
     * Dedicated ID Card & Small-Card Isolation Pass (Modernized CIE-Lab + 4-Edge Background Model):
     * Samples 4 independent border zones (top, bottom, left, right) in perceptual CIE-Lab space so
     * fingers/hands on one edge don't corrupt background estimation, suppresses PVC specular glare hotspots,
     * fills interior holes (portraits/chips/text) via outside-in flood fill, and fits sharp 4-line intersections.
     */
    private fun runIdCardIsolationPass(
        pixels: IntArray,
        rawLuma: IntArray,
        w: Int,
        h: Int,
        origW: Int,
        origH: Int,
        scaleX: Float,
        scaleY: Float,
        isIdCardHint: Boolean
    ): EdgeDetectionFrameResult {
        val n = w * h
        val labL = FloatArray(n)
        val labA = FloatArray(n)
        val labB = FloatArray(n)
        computeFastLab(pixels, labL, labA, labB)

        val borderBand = max(4, min(w, h) / 24)
        // Compute independent median CIE-Lab across top, bottom, left, right outer border strips
        fun stripMedian(xRange: IntRange, yRange: IntRange): Triple<Float, Float, Float> {
            val lArr = ArrayList<Float>(128)
            val aArr = ArrayList<Float>(128)
            val bArr = ArrayList<Float>(128)
            for (y in yRange) {
                val row = y * w
                for (x in xRange step 2) {
                    val idx = row + x
                    lArr.add(labL[idx])
                    aArr.add(labA[idx])
                    bArr.add(labB[idx])
                }
            }
            if (lArr.isEmpty()) return Triple(50f, 0f, 0f)
            lArr.sort(); aArr.sort(); bArr.sort()
            val mid = lArr.size / 2
            return Triple(lArr[mid], aArr[mid], bArr[mid])
        }

        val topBg = stripMedian(0 until w, 0 until borderBand)
        val botBg = stripMedian(0 until w, max(0, h - borderBand) until h)
        val leftBg = stripMedian(0 until borderBand, 0 until h)
        val rightBg = stripMedian(max(0, w - borderBand) until w, 0 until h)
        val bgRefs = listOf(topBg, botBg, leftBg, rightBg)

        val signal = IntArray(n)
        for (i in 0 until n) {
            val l = labL[i]
            val a = labA[i]
            val b = labB[i]
            // Compare against closest 2 border strips (if a finger covers 1 strip, other strips stay accurate)
            var minD = Float.MAX_VALUE
            var secondD = Float.MAX_VALUE
            for (ref in bgRefs) {
                val d = hypot((l - ref.first) * 0.9f, hypot(a - ref.second, b - ref.third) * 1.45f)
                if (d < minD) {
                    secondD = minD
                    minD = d
                } else if (d < secondD) {
                    secondD = d
                }
            }
            val deltaE = (minD * 0.55f + secondD * 0.45f)
            val chroma = hypot(a, b)
            // Specular PVC glare boost keeps bright shiny lamination inside the card silhouette
            val glarePreserve = if (l > 86f && chroma < 10f) 18f else 0f
            val neutralBrightBoost = if (l > 62f && chroma < 14f) (l - 62f) * 0.35f else 0f
            signal[i] = (deltaE * 2.4f + glarePreserve + neutralBrightBoost).roundToInt().coerceIn(0, 255)
        }

        // Smooth out leather/fabric/wood texture speckles before thresholding
        val smoothed1 = applyGaussianBlur5x5(signal, w, h)
        val smoothed = applyGaussianBlur5x5(smoothed1, w, h)

        // Otsu threshold on smoothed CIE-Lab foreground distance signal
        val hist = IntArray(256)
        for (v in smoothed) hist[v.coerceIn(0, 255)]++
        var sumAll = 0.0
        for (t in 0 until 256) sumAll += t * hist[t]
        var sumB = 0.0
        var wB = 0
        var maxVar = -1.0
        var otsuThresh = 42
        for (t in 0 until 256) {
            wB += hist[t]
            if (wB == 0) continue
            val wF = n - wB
            if (wF == 0) break
            sumB += t * hist[t]
            val mB = sumB / wB
            val mF = (sumAll - sumB) / wF
            val between = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)
            if (between > maxVar) {
                maxVar = between
                otsuThresh = t
            }
        }
        val thresh = otsuThresh.coerceIn(22, 175)

        val mask = BooleanArray(n) { smoothed[it] >= thresh }
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (x < 2 || x >= w - 2 || y < 2 || y >= h - 2) {
                    mask[y * w + x] = false
                }
            }
        }

        val closedMask = morphCloseBinary(mask, w, h, radius = 3)
        // Outside-in flood fill on !closedMask -> everything NOT reachable from border is solid inside card
        val isOutside = BooleanArray(n)
        val q = IntArray(n)
        var head = 0
        var tail = 0
        for (x in 0 until w) {
            if (!closedMask[x]) { isOutside[x] = true; q[tail++] = x }
            val bot = (h - 1) * w + x
            if (!closedMask[bot] && !isOutside[bot]) { isOutside[bot] = true; q[tail++] = bot }
        }
        for (y in 1 until h - 1) {
            val lIdx = y * w
            if (!closedMask[lIdx] && !isOutside[lIdx]) { isOutside[lIdx] = true; q[tail++] = lIdx }
            val rIdx = y * w + (w - 1)
            if (!closedMask[rIdx] && !isOutside[rIdx]) { isOutside[rIdx] = true; q[tail++] = rIdx }
        }
        while (head < tail) {
            val cur = q[head++]
            val cy = cur / w
            val cx = cur - cy * w
            if (cx > 0) {
                val idx = cur - 1
                if (!isOutside[idx] && !closedMask[idx]) { isOutside[idx] = true; q[tail++] = idx }
            }
            if (cx < w - 1) {
                val idx = cur + 1
                if (!isOutside[idx] && !closedMask[idx]) { isOutside[idx] = true; q[tail++] = idx }
            }
            if (cy > 0) {
                val idx = cur - w
                if (!isOutside[idx] && !closedMask[idx]) { isOutside[idx] = true; q[tail++] = idx }
            }
            if (cy < h - 1) {
                val idx = cur + w
                if (!isOutside[idx] && !closedMask[idx]) { isOutside[idx] = true; q[tail++] = idx }
            }
        }
        val solidMask = BooleanArray(n) { !isOutside[it] }
        val perimeterEdges = extractMaskBoundaryEdges(solidMask, w, h)

        return findBestDocumentQuad(
            edges = perimeterEdges,
            w = w,
            h = h,
            origW = origW,
            origH = origH,
            scaleX = scaleX,
            scaleY = scaleY,
            isIdCardHint = isIdCardHint
        )
    }

    /**
     * Multi-Channel Scharr 3x3 + Oriented Hough-Peak + RANSAC 4-Line Rectangle Pass:
     * Combines perceptual CIE-Lab chroma + CLAHE luminance Scharr gradients so tilted, glare-lit,
     * partially occluded (by fingers), or low-luminance-contrast cards form sharp 4-line intersections.
     */
    private fun runMultiChannelHoughPass(
        pixels: IntArray,
        rawLuma: IntArray,
        w: Int,
        h: Int,
        closingRadius: Int,
        origW: Int,
        origH: Int,
        scaleX: Float,
        scaleY: Float,
        isIdCardHint: Boolean
    ): EdgeDetectionFrameResult {
        val n = w * h
        val labL = FloatArray(n)
        val labA = FloatArray(n)
        val labB = FloatArray(n)
        computeFastLab(pixels, labL, labA, labB)

        val lumaClahe = applyClahe(rawLuma, w, h)
        val lumaSmooth = applySeparableMorphClosing(applyGaussianBlur5x5(lumaClahe, w, h), w, h, max(1, closingRadius - 1))
        val aInt = applyGaussianBlur5x5(IntArray(n) { (labA[it] + 128f).roundToInt().coerceIn(0, 255) }, w, h)
        val bInt = applyGaussianBlur5x5(IntArray(n) { (labB[it] + 128f).roundToInt().coerceIn(0, 255) }, w, h)

        val gxArr = IntArray(n)
        val gyArr = IntArray(n)
        val mag = IntArray(n)
        val dir = ByteArray(n)

        for (y in 2 until h - 2) {
            val ym = (y - 1) * w
            val y0 = y * w
            val yp = (y + 1) * w
            for (x in 2 until w - 2) {
                fun scharrX(arr: IntArray): Int =
                    (-3 * arr[ym + x - 1] + 3 * arr[ym + x + 1] -
                        10 * arr[y0 + x - 1] + 10 * arr[y0 + x + 1] -
                        3 * arr[yp + x - 1] + 3 * arr[yp + x + 1]) shr 2

                fun scharrY(arr: IntArray): Int =
                    (-3 * arr[ym + x - 1] - 10 * arr[ym + x] - 3 * arr[ym + x + 1] +
                        3 * arr[yp + x - 1] + 10 * arr[yp + x] + 3 * arr[yp + x + 1]) shr 2

                val gxl = scharrX(lumaSmooth)
                val gyl = scharrY(lumaSmooth)
                val gxa = (scharrX(aInt) * 1.45f).roundToInt()
                val gya = (scharrY(aInt) * 1.45f).roundToInt()
                val gxb = (scharrX(bInt) * 1.45f).roundToInt()
                val gyb = (scharrY(bInt) * 1.45f).roundToInt()

                val ml = abs(gxl) + abs(gyl)
                val ma = abs(gxa) + abs(gya)
                val mb = abs(gxb) + abs(gyb)

                val domGx: Int
                val domGy: Int
                val domM: Int
                when {
                    ma >= ml && ma >= mb -> { domGx = gxa; domGy = gya; domM = ma }
                    mb >= ml && mb >= ma -> { domGx = gxb; domGy = gyb; domM = mb }
                    else -> { domGx = gxl; domGy = gyl; domM = ml }
                }

                val idx = y0 + x
                gxArr[idx] = domGx
                gyArr[idx] = domGy
                mag[idx] = domM
                if (domM > 12) {
                    val ax = abs(domGx)
                    val ay = abs(domGy)
                    dir[idx] = when {
                        ay * 2.414f < ax -> 0
                        ax * 2.414f < ay -> 2
                        (domGx xor domGy) >= 0 -> 3
                        else -> 1
                    }.toByte()
                }
            }
        }

        val nms = IntArray(n)
        val histBins = IntArray(256)
        var nonZero = 0
        for (y in 3 until h - 3) {
            val y0 = y * w
            for (x in 3 until w - 3) {
                val idx = y0 + x
                val m = mag[idx]
                if (m <= 14) continue
                val (n1, n2) = when (dir[idx].toInt()) {
                    0 -> mag[idx - 1] to mag[idx + 1]
                    1 -> mag[idx - w + 1] to mag[idx + w - 1]
                    2 -> mag[idx - w] to mag[idx + w]
                    else -> mag[idx - w - 1] to mag[idx + w + 1]
                }
                if (m >= n1 && m >= n2) {
                    nms[idx] = m
                    histBins[min(255, m shr 2)]++
                    nonZero++
                }
            }
        }
        if (nonZero < 30) return EdgeDetectionFrameResult(DocumentQuad.SAFE_DEFAULT_MARGIN, 0f, false)

        val targetRank = (nonZero * 0.72f).toInt()
        var acc = 0
        var highThresh = 46
        for (b in 0 until 256) {
            acc += histBins[b]
            if (acc >= targetRank) {
                highThresh = max(34, b shl 2)
                break
            }
        }
        val lowThresh = max(14, (highThresh * 0.42f).toInt())
        val edges = BooleanArray(n)
        val weak = BooleanArray(n)
        val queue = IntArray(n)
        var head = 0
        var tail = 0
        for (y in 3 until h - 3) {
            val row = y * w
            for (x in 3 until w - 3) {
                val idx = row + x
                val v = nms[idx]
                if (v >= highThresh) {
                    edges[idx] = true
                    queue[tail++] = idx
                } else if (v >= lowThresh) {
                    weak[idx] = true
                }
            }
        }
        while (head < tail) {
            val cur = queue[head++]
            val cy = cur / w
            val cx = cur - cy * w
            for (dy in -1..1) {
                val ny = cy + dy
                if (ny !in 2 until h - 2) continue
                val row = ny * w
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = cx + dx
                    if (nx !in 2 until w - 2) continue
                    val nIdx = row + nx
                    if (weak[nIdx] && !edges[nIdx]) {
                        edges[nIdx] = true
                        queue[tail++] = nIdx
                    }
                }
            }
        }

        val houghCandidate = runHoughRansacRectanglePass(
            edges = edges,
            gradMag = nms,
            gxArr = gxArr,
            gyArr = gyArr,
            w = w,
            h = h,
            origW = origW,
            origH = origH,
            scaleX = scaleX,
            scaleY = scaleY,
            isIdCardHint = isIdCardHint
        )
        val contourCandidate = findBestDocumentQuad(
            edges = edges,
            w = w,
            h = h,
            origW = origW,
            origH = origH,
            scaleX = scaleX,
            scaleY = scaleY,
            isIdCardHint = isIdCardHint
        )
        return when {
            houghCandidate.isLocked && contourCandidate.isLocked ->
                if (houghCandidate.confidence >= contourCandidate.confidence) houghCandidate else contourCandidate
            houghCandidate.isLocked -> houghCandidate
            else -> contourCandidate
        }
    }

    /**
     * Oriented Hough-Space + RANSAC 4-Line Rectangle Detector:
     * Maps NMS gradient edge points (weighted by magnitude and aligned with normal vector atan2(gy, gx))
     * into 2-degree (theta, rho) accumulator space, extracts top parallel/tapered line families
     * separated by >= 72 deg, fits outlier-resistant orthogonal lines, and validates closed quads.
     */
    private fun runHoughRansacRectanglePass(
        edges: BooleanArray,
        gradMag: IntArray,
        gxArr: IntArray,
        gyArr: IntArray,
        w: Int,
        h: Int,
        origW: Int,
        origH: Int,
        scaleX: Float,
        scaleY: Float,
        isIdCardHint: Boolean
    ): EdgeDetectionFrameResult {
        val thetaBins = 90 // 0..178 deg in 2-deg steps
        val diag = hypot(w.toDouble(), h.toDouble()).toFloat()
        val rhoStep = max(2f, diag / 160f)
        val numRho = ((2f * diag) / rhoStep).toInt() + 4
        val rhoCenterOffset = numRho / 2

        val cosTab = FloatArray(thetaBins)
        val sinTab = FloatArray(thetaBins)
        for (t in 0 until thetaBins) {
            val rad = Math.toRadians((t * 2).toDouble())
            cosTab[t] = cos(rad).toFloat()
            sinTab[t] = sin(rad).toFloat()
        }

        val acc = FloatArray(thetaBins * numRho)
        val thetaPower = FloatArray(thetaBins)
        val edgePts = ArrayList<FloatPt>(1200)
        val edgeGx = ArrayList<Int>(1200)
        val edgeGy = ArrayList<Int>(1200)

        val step = if (max(w, h) > 420) 2 else 1
        for (y in 4 until h - 4 step step) {
            val row = y * w
            for (x in 4 until w - 4 step step) {
                val idx = row + x
                if (!edges[idx]) continue
                val m = max(16, gradMag[idx]).toFloat()
                val gx = gxArr[idx]
                val gy = gyArr[idx]
                edgePts.add(FloatPt(x.toFloat(), y.toFloat()))
                edgeGx.add(gx)
                edgeGy.add(gy)

                var deg = Math.toDegrees(atan2(gy.toDouble(), gx.toDouble()))
                if (deg < 0.0) deg += 180.0
                if (deg >= 180.0) deg -= 180.0
                val centerBin = ((deg / 2.0).roundToInt()) % thetaBins

                for (dt in -2..2) {
                    val tBin = (centerBin + dt + thetaBins) % thetaBins
                    val wgt = m * when (abs(dt)) {
                        0 -> 1.0f
                        1 -> 0.72f
                        else -> 0.40f
                    }
                    val rho = x * cosTab[tBin] + y * sinTab[tBin]
                    val rBin = (rho / rhoStep).roundToInt() + rhoCenterOffset
                    if (rBin in 1 until numRho - 1) {
                        val binIdx = tBin * numRho + rBin
                        acc[binIdx] += wgt
                        acc[binIdx - 1] += wgt * 0.35f
                        acc[binIdx + 1] += wgt * 0.35f
                        thetaPower[tBin] += wgt
                    }
                }
            }
        }
        if (edgePts.size < 36) return EdgeDetectionFrameResult(DocumentQuad.SAFE_DEFAULT_MARGIN, 0f, false)

        // Find dominant angle peaks for family 1 and nearly-perpendicular family 2
        val sortedThetas = (0 until thetaBins).sortedByDescending { thetaPower[it] }
        val angleSeeds1 = ArrayList<Int>(3)
        for (tb in sortedThetas) {
            val minDiff = angleSeeds1.minOfOrNull { prev ->
                val d = abs(tb - prev)
                min(d, thetaBins - d)
            } ?: thetaBins
            if (minDiff >= 6) {
                angleSeeds1.add(tb)
                if (angleSeeds1.size >= 3) break
            }
        }

        val minSepRhoBins = max(6, (min(w, h) * (if (isIdCardHint) 0.06f else 0.12f) / rhoStep).roundToInt())
        fun extractPeakLinesNear(targetTheta: Int): List<OrthoLine> {
            // Collect local maxima over [targetTheta - 3 .. targetTheta + 3] x rho
            data class HoughPeak(val t: Int, val r: Int, val score: Float)
            val peaks = ArrayList<HoughPeak>(16)
            for (dt in -3..3) {
                val tb = (targetTheta + dt + thetaBins) % thetaBins
                val base = tb * numRho
                for (r in 3 until numRho - 3) {
                    val v = acc[base + r]
                    if (v > acc[base + r - 1] && v >= acc[base + r + 1] && v > 120f) {
                        peaks.add(HoughPeak(tb, r, v))
                    }
                }
            }
            peaks.sortByDescending { it.score }
            val chosen = ArrayList<HoughPeak>(4)
            for (p in peaks) {
                val rhoSigned = (p.r - rhoCenterOffset) * rhoStep
                val tooClose = chosen.any { c ->
                    val prevRho = (c.r - rhoCenterOffset) * rhoStep
                    abs(abs(rhoSigned) - abs(prevRho)) < minSepRhoBins * rhoStep &&
                        (rhoSigned * prevRho > 0f || abs(rhoSigned - prevRho) < minSepRhoBins * rhoStep)
                }
                if (!tooClose) {
                    chosen.add(p)
                    if (chosen.size >= 4) break
                }
            }

            // Refine each Hough line against raw edge points matching its normal orientation (RANSAC / PCA trim)
            val tol = max(4.0, min(w, h) * 0.016)
            return chosen.mapNotNull { pk ->
                val ca = cosTab[pk.t].toDouble()
                val cb = sinTab[pk.t].toDouble()
                val rho = ((pk.r - rhoCenterOffset) * rhoStep).toDouble()
                val cc = -rho
                val inliers = ArrayList<FloatPt>(64)
                for (i in edgePts.indices) {
                    val pt = edgePts[i]
                    val dist = abs(ca * pt.x + cb * pt.y + cc)
                    if (dist <= tol) {
                        val gNorm = hypot(edgeGx[i].toDouble(), edgeGy[i].toDouble())
                        if (gNorm > 1.0) {
                            val cosAlign = abs((edgeGx[i] * ca + edgeGy[i] * cb) / gNorm)
                            if (cosAlign >= 0.72) inliers.add(pt)
                        } else {
                            inliers.add(pt)
                        }
                    }
                }
                if (inliers.size >= 8) fitRobustOrthogonalLine(inliers) ?: OrthoLine(ca, cb, cc)
                else null
            }
        }

        var bestScore = -1f
        var bestCorners: List<FloatPt>? = null

        for (t1 in angleSeeds1) {
            val orthoCenter = (t1 + 45) % thetaBins // +90 deg
            var bestT2 = orthoCenter
            var bestT2Pow = -1f
            for (dt in -9..9) { // +/- 18 deg perspective skew allowance
                val candT = (orthoCenter + dt + thetaBins) % thetaBins
                if (thetaPower[candT] > bestT2Pow) {
                    bestT2Pow = thetaPower[candT]
                    bestT2 = candT
                }
            }

            val family1 = extractPeakLinesNear(t1)
            val family2 = extractPeakLinesNear(bestT2)
            if (family1.size < 2 || family2.size < 2) continue

            for (i in 0 until family1.size) {
                for (j in i + 1 until family1.size) {
                    val lA = family1[i]
                    val lB = family1[j]
                    for (m in 0 until family2.size) {
                        for (k in m + 1 until family2.size) {
                            val lC = family2[m]
                            val lD = family2[k]
                            val p1 = intersectLines(lA, lC) ?: continue
                            val p2 = intersectLines(lA, lD) ?: continue
                            val p3 = intersectLines(lB, lD) ?: continue
                            val p4 = intersectLines(lB, lC) ?: continue
                            val raw = listOf(p1, p2, p3, p4)
                            val marginX = w * 0.03f
                            val marginY = h * 0.03f
                            if (raw.any { it.x < -marginX || it.x > w + marginX || it.y < -marginY || it.y > h + marginY }) {
                                continue
                            }
                            val ordered = orderFloatCornersClockwise(
                                raw.map {
                                    FloatPt(
                                        x = it.x.coerceIn(1f, (w - 2).toFloat()),
                                        y = it.y.coerceIn(1f, (h - 2).toFloat())
                                    )
                                }
                            )
                            val score = evaluateCandidateWorkQuad(ordered, edges, w, h, isIdCardHint)
                            if (score > bestScore) {
                                bestScore = score
                                bestCorners = ordered
                            }
                        }
                    }
                }
            }
        }

        return if (bestCorners != null && bestScore > 0.34f) {
            EdgeDetectionFrameResult(
                quad = mapWorkCornersToOriginalQuad(bestCorners, origW, origH, scaleX, scaleY),
                confidence = bestScore.coerceIn(0.35f, 0.99f),
                isLocked = true
            )
        } else {
            EdgeDetectionFrameResult(DocumentQuad.SAFE_DEFAULT_MARGIN, 0f, false)
        }
    }

    /**
     * Converts sRGB pixels into perceptual CIE-Lab (L* 0..100, a* -128..127, b* -128..127).
     */
    private fun computeFastLab(pixels: IntArray, outL: FloatArray, outA: FloatArray, outB: FloatArray) {
        val inv255 = 1f / 255f
        fun fLab(t: Double): Float {
            return if (t > 0.008856) Math.cbrt(t).toFloat() else (7.787 * t + 16.0 / 116.0).toFloat()
        }
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = ((c ushr 16) and 0xFF) * inv255
            val g = ((c ushr 8) and 0xFF) * inv255
            val b = (c and 0xFF) * inv255
            val x = (r * 0.4124564 + g * 0.3575761 + b * 0.1804375) / 0.95047
            val y = (r * 0.2126729 + g * 0.7151522 + b * 0.0721750)
            val z = (r * 0.0193339 + g * 0.1191920 + b * 0.9503041) / 1.08883
            val fx = fLab(x)
            val fy = fLab(y)
            val fz = fLab(z)
            outL[i] = 116f * fy - 16f
            outA[i] = 500f * (fx - fy)
            outB[i] = 200f * (fy - fz)
        }
    }

    /**
     * Pass 2: Isolates white/colored paper from bright desks, sheets, or textured surfaces
     * via HSV Value/Saturation separation, border chroma distance, Adaptive Thresholding,
     * and sensitive Canny boundary extraction.
     */
    private fun runWhiteOnLightFallbackPass(
        pixels: IntArray,
        rawLuma: IntArray,
        w: Int,
        h: Int,
        closingRadius: Int,
        origW: Int,
        origH: Int,
        scaleX: Float,
        scaleY: Float,
        isIdCardHint: Boolean = false
    ): EdgeDetectionFrameResult {
        val n = w * h

        // Sample outer 3% border frame to estimate background RGB/Value/Saturation
        var bgRSum = 0L
        var bgGSum = 0L
        var bgBSum = 0L
        var bgCount = 0
        val borderBand = max(3, min(w, h) / 30)
        for (y in 0 until h) {
            val isEdgeY = y < borderBand || y >= h - borderBand
            val row = y * w
            for (x in 0 until w) {
                if (isEdgeY || x < borderBand || x >= w - borderBand) {
                    val c = pixels[row + x]
                    bgRSum += Color.red(c)
                    bgGSum += Color.green(c)
                    bgBSum += Color.blue(c)
                    bgCount++
                }
            }
        }
        val bgR = if (bgCount > 0) (bgRSum / bgCount).toInt() else 128
        val bgG = if (bgCount > 0) (bgGSum / bgCount).toInt() else 128
        val bgB = if (bgCount > 0) (bgBSum / bgCount).toInt() else 128

        // Extract HSV Saturation/Value + Border Chroma Distance channel
        val hsvPaperMap = IntArray(n)
        val satMap = IntArray(n)
        for (i in 0 until n) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val v = maxC
            val s = if (maxC == 0) 0 else ((maxC - minC) * 255) / maxC
            satMap[i] = s

            val chromaDist = (abs(r - bgR) + abs(g - bgG) + abs(b - bgB)) / 3
            // Bright + low-saturation paper or distinctly colored card relative to desk border
            val score = (v * 0.65f - s * 0.45f + chromaDist * 0.85f + 40f).roundToInt()
            hsvPaperMap[i] = score.coerceIn(0, 255)
        }

        // Sub-pipeline 2A: Sensitive Canny on stretched HSV paper channel
        val stretchedHsv = applyClahe(hsvPaperMap, w, h)
        val blurredHsv = applyGaussianBlur5x5(stretchedHsv, w, h)
        val closedHsv = applySeparableMorphClosing(blurredHsv, w, h, closingRadius)
        val edgesHsv = detectCannyEdges(
            src = closedHsv,
            w = w,
            h = h,
            percentile = 0.72f,
            minHighThresh = 32,
            minNmsFloor = 7
        )
        val hsvCandidate = findBestDocumentQuad(
            edges = edgesHsv,
            w = w,
            h = h,
            origW = origW,
            origH = origH,
            scaleX = scaleX,
            scaleY = scaleY,
            isIdCardHint = isIdCardHint
        )

        // Sub-pipeline 2B: Integral-Image Adaptive Thresholding foreground silhouette -> perimeter
        val adaptiveMask = buildAdaptiveDocumentMask(hsvPaperMap, rawLuma, w, h)
        val maskPerimeter = extractMaskBoundaryEdges(adaptiveMask, w, h)
        val maskCandidate = findBestDocumentQuad(
            edges = maskPerimeter,
            w = w,
            h = h,
            origW = origW,
            origH = origH,
            scaleX = scaleX,
            scaleY = scaleY,
            isIdCardHint = isIdCardHint
        )

        return when {
            hsvCandidate.isLocked && maskCandidate.isLocked ->
                if (hsvCandidate.confidence >= maskCandidate.confidence) hsvCandidate else maskCandidate
            hsvCandidate.isLocked -> hsvCandidate
            maskCandidate.isLocked -> maskCandidate
            else -> EdgeDetectionFrameResult(DocumentQuad.SAFE_DEFAULT_MARGIN, 0f, false)
        }
    }

    /**
     * Integral-image adaptive foreground thresholding + morphological cleanup to isolate
     * bright/distinct document surfaces even over light backgrounds.
     */
    private fun buildAdaptiveDocumentMask(
        hsvPaperMap: IntArray,
        rawLuma: IntArray,
        w: Int,
        h: Int
    ): BooleanArray {
        val n = w * h
        val combined = IntArray(n) { i -> ((hsvPaperMap[i] * 3 + rawLuma[i] * 2) / 5).coerceIn(0, 255) }
        val integral = LongArray(n)
        for (y in 0 until h) {
            var rowSum = 0L
            val rowOff = y * w
            for (x in 0 until w) {
                rowSum += combined[rowOff + x]
                integral[rowOff + x] = rowSum + if (y > 0) integral[(y - 1) * w + x] else 0L
            }
        }

        val rad = max(14, min(w, h) / 10)
        val mask = BooleanArray(n)
        for (y in 2 until h - 2) {
            val y1 = max(0, y - rad)
            val y2 = min(h - 1, y + rad)
            val rowOff = y * w
            for (x in 2 until w - 2) {
                val x1 = max(0, x - rad)
                val x2 = min(w - 1, x + rad)
                val cnt = (x2 - x1 + 1) * (y2 - y1 + 1)
                var sum = integral[y2 * w + x2]
                if (y1 > 0) sum -= integral[(y1 - 1) * w + x2]
                if (x1 > 0) sum -= integral[y2 * w + (x1 - 1)]
                if (y1 > 0 && x1 > 0) sum += integral[(y1 - 1) * w + (x1 - 1)]

                val localMean = sum.toFloat() / cnt.coerceAtLeast(1)
                val v = combined[rowOff + x].toFloat()
                // Pixel belongs to foreground document if noticeably above local background threshold
                mask[rowOff + x] = v > localMean * 1.035f
            }
        }

        // Fill small internal holes & erase small noise speckles
        return morphCloseBinary(mask, w, h, radius = 2)
    }

    private fun morphCloseBinary(src: BooleanArray, w: Int, h: Int, radius: Int): BooleanArray {
        val dilated = BooleanArray(w * h)
        for (y in radius until h - radius) {
            val row = y * w
            for (x in radius until w - radius) {
                var hit = false
                for (dy in -radius..radius) {
                    val nyRow = (y + dy) * w
                    for (dx in -radius..radius) {
                        if (src[nyRow + x + dx]) {
                            hit = true
                            break
                        }
                    }
                    if (hit) break
                }
                dilated[row + x] = hit
            }
        }
        val eroded = BooleanArray(w * h)
        for (y in radius until h - radius) {
            val row = y * w
            for (x in radius until w - radius) {
                var all = true
                for (dy in -radius..radius) {
                    val nyRow = (y + dy) * w
                    for (dx in -radius..radius) {
                        if (!dilated[nyRow + x + dx]) {
                            all = false
                            break
                        }
                    }
                    if (!all) break
                }
                eroded[row + x] = all
            }
        }
        return eroded
    }

    private fun extractMaskBoundaryEdges(mask: BooleanArray, w: Int, h: Int): BooleanArray {
        val edges = BooleanArray(w * h)
        for (y in 2 until h - 2) {
            val row = y * w
            for (x in 2 until w - 2) {
                val idx = row + x
                if (!mask[idx]) continue
                if (!mask[idx - 1] || !mask[idx + 1] || !mask[idx - w] || !mask[idx + w]) {
                    edges[idx] = true
                }
            }
        }
        return edges
    }

    /**
     * Contrast Limited Adaptive Histogram Equalization (CLAHE) with bilinear tile interpolation.
     */
    private fun applyClahe(src: IntArray, w: Int, h: Int): IntArray {
        val out = IntArray(w * h)
        val hist = IntArray(HIST_BINS)
        val luts = Array(CLAHE_TILES_Y) { Array(CLAHE_TILES_X) { IntArray(HIST_BINS) } }
        applyClaheInPlace(src, out, w, h, hist, luts)
        return out
    }

    private fun applyClaheInPlace(
        src: IntArray,
        dst: IntArray,
        w: Int,
        h: Int,
        hist: IntArray,
        luts: Array<Array<IntArray>>
    ) {
        val tilesX = CLAHE_TILES_X
        val tilesY = CLAHE_TILES_Y
        val tileW = max(4f, w.toFloat() / tilesX)
        val tileH = max(4f, h.toFloat() / tilesY)

        for (ty in 0 until tilesY) {
            val yStart = (ty * tileH).toInt().coerceIn(0, h - 1)
            val yEnd = ((ty + 1) * tileH).toInt().coerceIn(yStart + 1, h)
            for (tx in 0 until tilesX) {
                val xStart = (tx * tileW).toInt().coerceIn(0, w - 1)
                val xEnd = ((tx + 1) * tileW).toInt().coerceIn(xStart + 1, w)
                hist.fill(0)
                var count = 0
                for (y in yStart until yEnd) {
                    val rowOff = y * w
                    for (x in xStart until xEnd) {
                        val bin = (src[rowOff + x] * HIST_BINS) shr 8
                        hist[bin.coerceIn(0, HIST_BINS - 1)]++
                        count++
                    }
                }
                if (count <= 0) continue

                val clipLimit = max(1, ((count.toFloat() / HIST_BINS) * 2.6f).roundToInt())
                var excess = 0
                for (b in 0 until HIST_BINS) {
                    if (hist[b] > clipLimit) {
                        excess += hist[b] - clipLimit
                        hist[b] = clipLimit
                    }
                }
                val redist = excess / HIST_BINS
                val rem = excess % HIST_BINS
                for (b in 0 until HIST_BINS) {
                    hist[b] += redist + if (b < rem) 1 else 0
                }

                var cdf = 0
                val lut = luts[ty][tx]
                for (b in 0 until HIST_BINS) {
                    cdf += hist[b]
                    lut[b] = ((cdf * 255f) / count).roundToInt().coerceIn(0, 255)
                }
            }
        }

        for (y in 0 until h) {
            val fty = (y / tileH) - 0.5f
            val ty0 = fty.toInt().coerceIn(0, tilesY - 1)
            val ty1 = (ty0 + 1).coerceIn(0, tilesY - 1)
            val wy = (fty - ty0).coerceIn(0f, 1f)
            val rowOff = y * w
            for (x in 0 until w) {
                val ftx = (x / tileW) - 0.5f
                val tx0 = ftx.toInt().coerceIn(0, tilesX - 1)
                val tx1 = (tx0 + 1).coerceIn(0, tilesX - 1)
                val wx = (ftx - tx0).coerceIn(0f, 1f)

                val bin = ((src[rowOff + x] * HIST_BINS) shr 8).coerceIn(0, HIST_BINS - 1)
                val v00 = luts[ty0][tx0][bin]
                val v10 = luts[ty0][tx1][bin]
                val v01 = luts[ty1][tx0][bin]
                val v11 = luts[ty1][tx1][bin]

                val top = v00 + (v10 - v00) * wx
                val bot = v01 + (v11 - v01) * wx
                dst[rowOff + x] = (top + (bot - top) * wy).roundToInt().coerceIn(0, 255)
            }
        }
    }

    /**
     * Separable 5x5 Gaussian kernel [1, 4, 6, 4, 1] / 16.
     */
    private fun applyGaussianBlur5x5(src: IntArray, w: Int, h: Int): IntArray {
        val temp = IntArray(w * h)
        val out = IntArray(w * h)

        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val xM2 = max(0, x - 2)
                val xM1 = max(0, x - 1)
                val xP1 = min(w - 1, x + 1)
                val xP2 = min(w - 1, x + 2)
                temp[row + x] = (
                    src[row + xM2] +
                        4 * src[row + xM1] +
                        6 * src[row + x] +
                        4 * src[row + xP1] +
                        src[row + xP2]
                    ) shr 4
            }
        }

        for (y in 0 until h) {
            val yM2 = max(0, y - 2) * w
            val yM1 = max(0, y - 1) * w
            val y0 = y * w
            val yP1 = min(h - 1, y + 1) * w
            val yP2 = min(h - 1, y + 2) * w
            for (x in 0 until w) {
                out[y0 + x] = (
                    temp[yM2 + x] +
                        4 * temp[yM1 + x] +
                        6 * temp[y0 + x] +
                        4 * temp[yP1 + x] +
                        temp[yP2 + x]
                    ) shr 4
            }
        }
        return out
    }

    /**
     * Fast separable Morphological Closing (Dilation -> Erosion) with configurable radius.
     * Erases inner printed text lines & surface texture noise while keeping outer edges intact.
     */
    private fun applySeparableMorphClosing(src: IntArray, w: Int, h: Int, radius: Int): IntArray {
        val n = w * h
        val hDil = IntArray(n)
        val vDil = IntArray(n)
        val hErode = IntArray(n)
        val out = IntArray(n)

        // Dilation: horizontal max then vertical max
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val x0 = max(0, x - radius)
                val x1 = min(w - 1, x + radius)
                var mx = 0
                for (nx in x0..x1) {
                    val v = src[row + nx]
                    if (v > mx) mx = v
                }
                hDil[row + x] = mx
            }
        }
        for (y in 0 until h) {
            val y0 = max(0, y - radius)
            val y1 = min(h - 1, y + radius)
            val row = y * w
            for (x in 0 until w) {
                var mx = 0
                for (ny in y0..y1) {
                    val v = hDil[ny * w + x]
                    if (v > mx) mx = v
                }
                vDil[row + x] = mx
            }
        }

        // Erosion: horizontal min then vertical min
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val x0 = max(0, x - radius)
                val x1 = min(w - 1, x + radius)
                var mn = 255
                for (nx in x0..x1) {
                    val v = vDil[row + nx]
                    if (v < mn) mn = v
                }
                hErode[row + x] = mn
            }
        }
        for (y in 0 until h) {
            val y0 = max(0, y - radius)
            val y1 = min(h - 1, y + radius)
            val row = y * w
            for (x in 0 until w) {
                var mn = 255
                for (ny in y0..y1) {
                    val v = hErode[ny * w + x]
                    if (v < mn) mn = v
                }
                out[row + x] = mn
            }
        }
        return out
    }

    /**
     * Canny Edge Detection with Sobel gradients, Non-Maximum Suppression,
     * Adaptive Percentile Thresholding, and BFS Hysteresis Edge Tracking.
     */
    private fun detectCannyEdges(
        src: IntArray,
        w: Int,
        h: Int,
        percentile: Float,
        minHighThresh: Int,
        minNmsFloor: Int
    ): BooleanArray {
        val n = w * h
        val mag = IntArray(n)
        val dir = ByteArray(n)

        for (y in 2 until h - 2) {
            val ym = (y - 1) * w
            val y0 = y * w
            val yp = (y + 1) * w
            for (x in 2 until w - 2) {
                val gx = -src[ym + x - 1] + src[ym + x + 1] -
                    2 * src[y0 + x - 1] + 2 * src[y0 + x + 1] -
                    src[yp + x - 1] + src[yp + x + 1]
                val gy = -src[ym + x - 1] - 2 * src[ym + x] - src[ym + x + 1] +
                    src[yp + x - 1] + 2 * src[yp + x] + src[yp + x + 1]

                val m = abs(gx) + abs(gy)
                val idx = y0 + x
                mag[idx] = m

                if (m > 0) {
                    var angle = Math.toDegrees(atan2(gy.toDouble(), gx.toDouble()))
                    if (angle < 0) angle += 180.0
                    dir[idx] = when {
                        angle < 22.5 || angle >= 157.5 -> 0
                        angle < 67.5 -> 1
                        angle < 112.5 -> 2
                        else -> 3
                    }.toByte()
                }
            }
        }

        val nms = IntArray(n)
        val histBins = IntArray(256)
        var nonZeroCount = 0

        for (y in 3 until h - 3) {
            val y0 = y * w
            for (x in 3 until w - 3) {
                val idx = y0 + x
                val m = mag[idx]
                if (m <= minNmsFloor) continue
                val (n1, n2) = when (dir[idx].toInt()) {
                    0 -> mag[idx - 1] to mag[idx + 1]
                    1 -> mag[idx - w + 1] to mag[idx + w - 1]
                    2 -> mag[idx - w] to mag[idx + w]
                    else -> mag[idx - w - 1] to mag[idx + w + 1]
                }
                if (m >= n1 && m >= n2) {
                    nms[idx] = m
                    histBins[min(255, m shr 2)]++
                    nonZeroCount++
                }
            }
        }

        if (nonZeroCount < 24) return BooleanArray(n)

        val targetRank = (nonZeroCount * percentile).toInt()
        var acc = 0
        var highThresh = minHighThresh + 20
        for (b in 0 until 256) {
            acc += histBins[b]
            if (acc >= targetRank) {
                highThresh = max(minHighThresh, b shl 2)
                break
            }
        }
        val lowThresh = max(16, (highThresh * 0.42f).toInt())

        val strong = BooleanArray(n)
        val weak = BooleanArray(n)
        val queue = IntArray(n)
        var head = 0
        var tail = 0

        for (y in 3 until h - 3) {
            val row = y * w
            for (x in 3 until w - 3) {
                val idx = row + x
                val v = nms[idx]
                if (v >= highThresh) {
                    strong[idx] = true
                    queue[tail++] = idx
                } else if (v >= lowThresh) {
                    weak[idx] = true
                }
            }
        }

        while (head < tail) {
            val cur = queue[head++]
            val cy = cur / w
            val cx = cur - cy * w
            for (dy in -1..1) {
                val ny = cy + dy
                if (ny !in 2 until h - 2) continue
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = cx + dx
                    if (nx !in 2 until w - 2) continue
                    val nIdx = ny * w + nx
                    if (weak[nIdx] && !strong[nIdx]) {
                        strong[nIdx] = true
                        queue[tail++] = nIdx
                    }
                }
            }
        }
        return strong
    }

    /**
     * Bridges 1px edge breaks purely for connected-component grouping while keeping raw thin
     * edges intact for line fitting and edge-support verification.
     */
    private fun dilateBinary1Px(edges: BooleanArray, w: Int, h: Int): BooleanArray {
        val out = BooleanArray(w * h)
        for (y in 2 until h - 2) {
            val row = y * w
            for (x in 2 until w - 2) {
                if (edges[row + x]) {
                    out[row - w + x - 1] = true
                    out[row - w + x] = true
                    out[row - w + x + 1] = true
                    out[row + x - 1] = true
                    out[row + x] = true
                    out[row + x + 1] = true
                    out[row + w + x - 1] = true
                    out[row + w + x] = true
                    out[row + w + x + 1] = true
                }
            }
        }
        return out
    }

    private fun findBestDocumentQuad(
        edges: BooleanArray,
        w: Int,
        h: Int,
        origW: Int,
        origH: Int,
        scaleX: Float,
        scaleY: Float,
        reusableVisited: BooleanArray? = null,
        reusableStack: IntArray? = null,
        isIdCardHint: Boolean = false
    ): EdgeDetectionFrameResult {
        val isHighResWork = max(w, h) >= 360
        val clusterEdges = if (isHighResWork) dilateBinary1Px(edges, w, h) else edges

        val n = w * h
        val visited = if (reusableVisited != null && reusableVisited.size >= n) {
            reusableVisited.also { it.fill(false, 0, n) }
        } else {
            BooleanArray(n)
        }
        val components = ArrayList<List<PxPt>>()
        val stack = if (reusableStack != null && reusableStack.size >= n) {
            reusableStack
        } else {
            IntArray(n)
        }
        val minComponentPts = max(24, (w + h) / 4)

        for (y in 3 until h - 3) {
            for (x in 3 until w - 3) {
                val startIdx = y * w + x
                if (!clusterEdges[startIdx] || visited[startIdx]) continue

                val pts = ArrayList<PxPt>(256)
                var sp = 0
                stack[sp++] = startIdx
                visited[startIdx] = true

                while (sp > 0) {
                    val cur = stack[--sp]
                    val cy = cur / w
                    val cx = cur - cy * w
                    if (edges[cur]) {
                        pts.add(PxPt(cx, cy))
                    }

                    for (dy in -1..1) {
                        val ny = cy + dy
                        if (ny !in 2 until h - 2) continue
                        val nyRow = ny * w
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = cx + dx
                            if (nx !in 2 until w - 2) continue
                            val nIdx = nyRow + nx
                            if (clusterEdges[nIdx] && !visited[nIdx]) {
                                visited[nIdx] = true
                                stack[sp++] = nIdx
                            }
                        }
                    }
                }

                if (pts.size >= minComponentPts) {
                    components.add(pts)
                }
            }
        }

        if (components.isEmpty()) {
            return EdgeDetectionFrameResult(DocumentQuad.SAFE_DEFAULT_MARGIN, 0f, false)
        }

        var bestScore = -1f
        var bestWorkCorners: List<FloatPt>? = null

        val sortedComponents = components.sortedByDescending { it.size }.take(10)

        // Build candidate point clouds: individual top components first, then proximity-compatible merged borders
        val candidateClouds = ArrayList<Pair<List<PxPt>, Boolean>>(sortedComponents.size + 2)
        sortedComponents.forEach { candidateClouds.add(it to false) }
        if (sortedComponents.size >= 2) {
            val merged2 = ArrayList<PxPt>(sortedComponents[0].size + sortedComponents[1].size)
            merged2.addAll(sortedComponents[0])
            merged2.addAll(sortedComponents[1])
            candidateClouds.add(merged2 to true)
        }
        if (sortedComponents.size >= 3) {
            val merged3 = ArrayList<PxPt>(1024)
            sortedComponents.take(3).forEach { merged3.addAll(it) }
            candidateClouds.add(merged3 to true)
        }

        for ((cloud, isMergedCloud) in candidateClouds) {
            val hull = computeConvexHull(cloud)
            if (hull.size < 4) continue
            val mergePenalty = if (isMergedCloud) 0.06f else 0f

            // Line-Intersection Quadrilateral Reconstruction (handles rounded ID cards, skewed pages, and bent edges)
            val lineCorners = reconstructQuadByLineIntersections(hull, cloud, edges, w, h, isIdCardHint)
            if (lineCorners != null) {
                val score = evaluateCandidateWorkQuad(lineCorners, edges, w, h, isIdCardHint) - mergePenalty
                if (score > bestScore) {
                    bestScore = score
                    bestWorkCorners = lineCorners
                }
            }
        }

        return if (bestWorkCorners != null && bestScore > 0.34f) {
            val mappedQuad = mapWorkCornersToOriginalQuad(
                corners = bestWorkCorners,
                origW = origW,
                origH = origH,
                scaleX = scaleX,
                scaleY = scaleY
            )
            EdgeDetectionFrameResult(
                quad = mappedQuad,
                confidence = bestScore.coerceIn(0.35f, 0.99f),
                isLocked = true
            )
        } else {
            EdgeDetectionFrameResult(
                quad = DocumentQuad.SAFE_DEFAULT_MARGIN,
                confidence = 0f,
                isLocked = false
            )
        }
    }

    /**
     * Reconstructs 4 sharp corner intersections by extracting the 4 dominant outer side
     * edge point sets (excluding corner-turn arcs where rounded or folded corners happen),
     * fitting outlier-resistant orthogonal PCA lines (`a*x + b*y + c = 0`), intersecting adjacent lines,
     * and running 1 refinement pass on raw thin edge pixels sorted `[TL, TR, BR, BL]`.
     */
    private fun reconstructQuadByLineIntersections(
        hull: List<PxPt>,
        cloud: List<PxPt>,
        edges: BooleanArray,
        w: Int,
        h: Int,
        isIdCardHint: Boolean = false
    ): List<FloatPt>? {
        if (hull.size < 4) return null

        val pcaRad = estimatePrincipalAxisRad(hull)
        val seedRadians = buildList {
            add(0.0)
            add(Math.toRadians(15.0))
            add(Math.toRadians(30.0))
            if (pcaRad != null) add(pcaRad)
        }
        var bestReconstructed: List<FloatPt>? = null
        var bestSupport = -1f

        val distTol = max(3.5f, min(w, h) * 0.015f)
        val minSideLenPx = min(w, h) * if (isIdCardHint) 0.06f else 0.14f

        for (rad in seedRadians) {
            val cosA = cos(rad)
            val sinA = sin(rad)

            var idxTL = 0; var minSum = Double.MAX_VALUE
            var idxBR = 0; var maxSum = -Double.MAX_VALUE
            var idxTR = 0; var maxDiff = -Double.MAX_VALUE
            var idxBL = 0; var minDiff = Double.MAX_VALUE

            for (i in hull.indices) {
                val p = hull[i]
                val rx = p.x * cosA - p.y * sinA
                val ry = p.x * sinA + p.y * cosA
                val s = rx + ry
                val d = rx - ry
                if (s < minSum) { minSum = s; idxTL = i }
                if (s > maxSum) { maxSum = s; idxBR = i }
                if (d > maxDiff) { maxDiff = d; idxTR = i }
                if (d < minDiff) { minDiff = d; idxBL = i }
            }

            if (setOf(idxTL, idxTR, idxBR, idxBL).size < 4) continue

            // Seed ordered corners: TL -> TR -> BR -> BL
            val seedCorners = listOf(
                FloatPt(hull[idxTL].x.toFloat(), hull[idxTL].y.toFloat()),
                FloatPt(hull[idxTR].x.toFloat(), hull[idxTR].y.toFloat()),
                FloatPt(hull[idxBR].x.toFloat(), hull[idxBR].y.toFloat()),
                FloatPt(hull[idxBL].x.toFloat(), hull[idxBL].y.toFloat())
            )

            val lines = ArrayList<OrthoLine>(4)
            var validAllSides = true

            for (side in 0 until 4) {
                val a = seedCorners[side]
                val b = seedCorners[(side + 1) % 4]
                val segLen = hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
                if (segLen < minSideLenPx) {
                    validAllSides = false
                    break
                }

                // Gather interior points along this side (t in 0.12..0.88) to ignore rounded/folded corners
                val sideInliers = ArrayList<FloatPt>(64)
                val ux = (b.x - a.x) / segLen
                val uy = (b.y - a.y) / segLen
                val nx = -uy
                val ny = ux

                for (pt in cloud) {
                    val px = pt.x.toFloat() - a.x
                    val py = pt.y.toFloat() - a.y
                    val projT = (px * ux + py * uy) / segLen
                    if (projT in 0.12f..0.88f) {
                        val perpDist = abs(px * nx + py * ny)
                        if (perpDist <= distTol) {
                            sideInliers.add(FloatPt(pt.x.toFloat(), pt.y.toFloat()))
                        }
                    }
                }

                val fitted = if (sideInliers.size >= 6) {
                    fitRobustOrthogonalLine(sideInliers)
                } else {
                    lineThroughTwoPoints(a, b)
                }
                if (fitted == null) {
                    validAllSides = false
                    break
                }
                lines.add(fitted)
            }

            if (!validAllSides) continue

            val interCorners = intersectClosedQuadLines(lines, w, h) ?: continue

            // Refinement Pass: re-sample thin edges near the intersected side interiors for razor precision
            val refinedLines = ArrayList<OrthoLine>(4)
            val refineTol = max(3f, min(w, h) * 0.008f)
            for (side in 0 until 4) {
                val a = interCorners[side]
                val b = interCorners[(side + 1) % 4]
                val segLen = hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat().coerceAtLeast(1f)
                val ux = (b.x - a.x) / segLen
                val uy = (b.y - a.y) / segLen

                val refinedPts = ArrayList<FloatPt>(64)
                val steps = max(24, (segLen / 3f).roundToInt())
                for (s in 0..steps) {
                    val t = 0.10f + 0.80f * (s.toFloat() / steps)
                    val sx = (a.x + (b.x - a.x) * t).roundToInt()
                    val sy = (a.y + (b.y - a.y) * t).roundToInt()
                    val searchR = refineTol.roundToInt().coerceIn(2, 5)
                    for (oy in -searchR..searchR) {
                        val py = sy + oy
                        if (py !in 2 until h - 2) continue
                        val row = py * w
                        for (ox in -searchR..searchR) {
                            val px = sx + ox
                            if (px !in 2 until w - 2) continue
                            if (edges[row + px]) {
                                val d = abs((px - a.x) * (-uy) + (py - a.y) * ux)
                                if (d <= refineTol) {
                                    refinedPts.add(FloatPt(px.toFloat(), py.toFloat()))
                                }
                            }
                        }
                    }
                }
                val rLine = if (refinedPts.size >= 8) fitRobustOrthogonalLine(refinedPts) else lines[side]
                refinedLines.add(rLine ?: lines[side])
            }

            val finalCorners = intersectClosedQuadLines(refinedLines, w, h) ?: interCorners
            val sup = measureEdgeSupportPx(edges, w, h, finalCorners)
            if (sup > bestSupport) {
                bestSupport = sup
                bestReconstructed = finalCorners
            }
        }

        return bestReconstructed
    }

    private fun estimatePrincipalAxisRad(pts: List<PxPt>): Double? {
        if (pts.size < 4) return null
        var mx = 0.0
        var my = 0.0
        for (p in pts) {
            mx += p.x
            my += p.y
        }
        mx /= pts.size
        my /= pts.size
        var sxx = 0.0
        var syy = 0.0
        var sxy = 0.0
        for (p in pts) {
            val dx = p.x - mx
            val dy = p.y - my
            sxx += dx * dx
            syy += dy * dy
            sxy += dx * dy
        }
        if (sxx + syy < 1e-4) return null
        return -0.5 * atan2(2.0 * sxy, sxx - syy)
    }

    /**
     * Two-pass outlier-trimmed 2x2 PCA Orthogonal Line Regression (`a*x + b*y + c = 0`, `a^2 + b^2 = 1`).
     * Filters background speckles or rounded-corner curvature residuals before computing the final line.
     */
    private fun fitRobustOrthogonalLine(pts: List<FloatPt>): OrthoLine? {
        val initial = fitOrthogonalLine(pts) ?: return null
        if (pts.size < 10) return initial
        val dists = DoubleArray(pts.size) { i ->
            abs(initial.a * pts[i].x + initial.b * pts[i].y + initial.c)
        }
        val sortedDists = dists.sortedArray()
        val medianErr = sortedDists[sortedDists.size / 2]
        val cut = max(1.6, medianErr * 2.2)
        val filtered = ArrayList<FloatPt>(pts.size)
        for (i in pts.indices) {
            if (dists[i] <= cut) filtered.add(pts[i])
        }
        return if (filtered.size >= 6) fitOrthogonalLine(filtered) ?: initial else initial
    }

    /**
     * Orthogonal Total-Least-Squares (2x2 PCA) line fit: minimizes perpendicular distance
     * to `a*x + b*y + c = 0` (`a^2 + b^2 = 1`) — singularity-free for vertical & angled lines.
     */
    private fun fitOrthogonalLine(pts: List<FloatPt>): OrthoLine? {
        if (pts.size < 2) return null
        var meanX = 0.0
        var meanY = 0.0
        for (p in pts) {
            meanX += p.x
            meanY += p.y
        }
        meanX /= pts.size
        meanY /= pts.size

        var sxx = 0.0
        var syy = 0.0
        var sxy = 0.0
        for (p in pts) {
            val dx = p.x - meanX
            val dy = p.y - meanY
            sxx += dx * dx
            syy += dy * dy
            sxy += dx * dy
        }
        if (sxx + syy < 1e-4) return null

        // Normal direction corresponds to eigenvector of smallest eigenvalue
        val trace = sxx + syy
        val diff = sxx - syy
        val disc = sqrt(diff * diff + 4.0 * sxy * sxy)
        val lambdaMin = (trace - disc) * 0.5

        var nx = sxy
        var ny = lambdaMin - sxx
        if (abs(nx) < 1e-9 && abs(ny) < 1e-9) {
            nx = lambdaMin - syy
            ny = sxy
        }
        val norm = hypot(nx, ny)
        if (norm < 1e-9) return null
        val a = nx / norm
        val b = ny / norm
        val c = -(a * meanX + b * meanY)
        return OrthoLine(a, b, c)
    }

    private fun lineThroughTwoPoints(p1: FloatPt, p2: FloatPt): OrthoLine? {
        val dx = (p2.x - p1.x).toDouble()
        val dy = (p2.y - p1.y).toDouble()
        val len = hypot(dx, dy)
        if (len < 1e-4) return null
        val a = -dy / len
        val b = dx / len
        val c = -(a * p1.x + b * p1.y)
        return OrthoLine(a, b, c)
    }

    private fun intersectLines(l1: OrthoLine, l2: OrthoLine): FloatPt? {
        val det = l1.a * l2.b - l2.a * l1.b
        // Reject nearly parallel lines (< ~14 deg intersection angle)
        if (abs(det) < 0.24) return null
        val x = (l1.b * l2.c - l2.b * l1.c) / det
        val y = (l1.c * l2.a - l2.c * l1.a) / det
        if (!x.isFinite() || !y.isFinite()) return null
        return FloatPt(x.toFloat(), y.toFloat())
    }

    private fun intersectClosedQuadLines(lines: List<OrthoLine>, w: Int, h: Int): List<FloatPt>? {
        if (lines.size != 4) return null
        val tl = intersectLines(lines[3], lines[0]) ?: return null
        val tr = intersectLines(lines[0], lines[1]) ?: return null
        val br = intersectLines(lines[1], lines[2]) ?: return null
        val bl = intersectLines(lines[2], lines[3]) ?: return null

        val raw = listOf(tl, tr, br, bl)
        val marginX = w * 0.03f
        val marginY = h * 0.03f
        if (raw.any {
                it.x < -marginX || it.x > w + marginX ||
                    it.y < -marginY || it.y > h + marginY
            }
        ) {
            return null
        }

        // Clamp cleanly inside image bounds and ensure consistent clockwise ordering
        val clamped = raw.map {
            FloatPt(
                x = it.x.coerceIn(1f, (w - 2).toFloat()),
                y = it.y.coerceIn(1f, (h - 2).toFloat())
            )
        }
        return orderFloatCornersClockwise(clamped)
    }

    private fun orderFloatCornersClockwise(pts: List<FloatPt>): List<FloatPt> {
        val tl = pts.minBy { it.x + it.y }
        val br = pts.maxBy { it.x + it.y }
        val remaining = pts.filter { it !== tl && it !== br }
        if (remaining.size < 2) return pts
        val tr = remaining.maxBy { it.x - it.y }
        val bl = remaining.minBy { it.x - it.y }
        return listOf(tl, tr, br, bl)
    }

    /**
     * Maps detected corner coordinates from the downscaled working image back to the
     * original full-resolution bitmap dimensions using `scaleX` and `scaleY` ratios,
     * then normalizes into [0..1] [DocumentQuad].
     */
    private fun mapWorkCornersToOriginalQuad(
        corners: List<FloatPt>,
        origW: Int,
        origH: Int,
        scaleX: Float,
        scaleY: Float
    ): DocumentQuad {
        val normPts = corners.map { pt ->
            val origX = pt.x / scaleX
            val origY = pt.y / scaleY
            PointNorm(
                x = (origX / origW.toFloat()).coerceIn(0.01f, 0.99f),
                y = (origY / origH.toFloat()).coerceIn(0.01f, 0.99f)
            )
        }
        val tl = normPts.minBy { it.x + it.y }
        val br = normPts.maxBy { it.x + it.y }
        val remaining = normPts.filter { it !== tl && it !== br }
        val tr = remaining.maxByOrNull { it.x - it.y } ?: normPts[1]
        val bl = remaining.minByOrNull { it.x - it.y } ?: normPts[3]
        return DocumentQuad(tl, tr, br, bl)
    }

    private fun evaluateCandidateWorkQuad(
        corners: List<FloatPt>,
        edges: BooleanArray,
        w: Int,
        h: Int,
        isIdCardHint: Boolean = false
    ): Float {
        if (corners.size != 4) return -1f
        val wf = w.toFloat().coerceAtLeast(1f)
        val hf = h.toFloat().coerceAtLeast(1f)
        val norm = corners.map { FloatPt(it.x / wf, it.y / hf) }

        val areaFrac = polygonArea(norm)
        val minArea = if (isIdCardHint) 0.022f else 0.09f
        if (areaFrac < minArea || areaFrac > 0.96f) return -1f
        if (!isConvexAndValidAnglesPx(norm, isIdCardHint)) return -1f
        if (!hasValidAspectRatioPx(norm, isIdCardHint)) return -1f

        val edgeSupport = measureEdgeSupportPx(edges, w, h, corners)
        if (edgeSupport < 0.28f) return -1f

        // Penalize quads whose corners hug the outer 2% frame border (often spurious border/shadow clamps)
        val borderCornerCount = norm.count { pt ->
            pt.x <= 0.02f || pt.x >= 0.98f || pt.y <= 0.02f || pt.y >= 0.98f
        }
        val borderPenalty = borderCornerCount * 0.045f

        // Opposite-edge length consistency bonus (rectangles have similar opposite side lengths)
        val topL = hypot(norm[1].x - norm[0].x, norm[1].y - norm[0].y)
        val botL = hypot(norm[2].x - norm[3].x, norm[2].y - norm[3].y)
        val leftL = hypot(norm[3].x - norm[0].x, norm[3].y - norm[0].y)
        val rightL = hypot(norm[2].x - norm[1].x, norm[2].y - norm[1].y)
        val ratioW = min(topL, botL) / max(topL, botL).coerceAtLeast(1e-4f)
        val ratioH = min(leftL, rightL) / max(leftL, rightL).coerceAtLeast(1e-4f)
        val rectScore = ((ratioW + ratioH) * 0.5f).coerceIn(0f, 1f)

        val idAspectBonus = if (isIdCardHint) {
            val pxAvgW = ((topL + botL) * 0.5f) * wf
            val pxAvgH = ((leftL + rightL) * 0.5f) * hf
            val pxAspect = max(pxAvgW, pxAvgH) / min(pxAvgW, pxAvgH).coerceAtLeast(1f)
            // Standard ISO ID-1 card ratio is ~1.586
            val distFromIso = abs(pxAspect - 1.586f)
            (1f - (distFromIso / 0.65f)).coerceIn(0f, 1f) * 0.14f
        } else {
            0f
        }

        val cappedAreaScore = (areaFrac / 0.70f).coerceIn(0f, 1f)
        return (edgeSupport * 0.56f) + (rectScore * 0.24f) + (cappedAreaScore * 0.20f) + idAspectBonus - borderPenalty
    }

    private fun isBorderHuggingQuad(quad: DocumentQuad): Boolean {
        val pts = listOf(quad.topLeft, quad.topRight, quad.bottomRight, quad.bottomLeft)
        return pts.count { it.x <= 0.025f || it.x >= 0.975f || it.y <= 0.025f || it.y >= 0.975f } >= 2
    }

    /**
     * Andrew's Monotone Chain O(n log n) Convex Hull.
     */
    private fun computeConvexHull(points: List<PxPt>): List<PxPt> {
        if (points.size <= 3) return points
        val sorted = points.sortedWith(compareBy<PxPt> { it.x }.thenBy { it.y })
        val lower = ArrayList<PxPt>()
        for (p in sorted) {
            while (lower.size >= 2 && cross(lower[lower.size - 2], lower[lower.size - 1], p) <= 0L) {
                lower.removeAt(lower.lastIndex)
            }
            lower.add(p)
        }
        val upper = ArrayList<PxPt>()
        for (i in sorted.lastIndex downTo 0) {
            val p = sorted[i]
            while (upper.size >= 2 && cross(upper[upper.size - 2], upper[upper.size - 1], p) <= 0L) {
                upper.removeAt(upper.lastIndex)
            }
            upper.add(p)
        }
        lower.removeAt(lower.lastIndex)
        upper.removeAt(upper.lastIndex)
        lower.addAll(upper)
        return lower
    }

    private fun cross(a: PxPt, b: PxPt, c: PxPt): Long {
        return (b.x - a.x).toLong() * (c.y - a.y).toLong() -
            (b.y - a.y).toLong() * (c.x - a.x).toLong()
    }

    private fun polygonArea(pts: List<FloatPt>): Float {
        var sum = 0f
        for (i in 0 until 4) {
            val j = (i + 1) % 4
            sum += pts[i].x * pts[j].y - pts[j].x * pts[i].y
        }
        return abs(sum) * 0.5f
    }

    private fun isConvexAndValidAnglesPx(pts: List<FloatPt>, isIdCardHint: Boolean = false): Boolean {
        val minSide = if (isIdCardHint) 0.055f else 0.08f
        var sign = 0
        for (i in 0 until 4) {
            val a = pts[i]
            val b = pts[(i + 1) % 4]
            val c = pts[(i + 2) % 4]
            val v1x = b.x - a.x
            val v1y = b.y - a.y
            val v2x = c.x - b.x
            val v2y = c.y - b.y
            val cp = v1x * v2y - v1y * v2x
            val curSign = if (cp > 0f) 1 else -1
            if (sign == 0) sign = curSign else if (sign != curSign) return false

            val len1 = hypot(v1x, v1y)
            val len2 = hypot(v2x, v2y)
            if (len1 < minSide || len2 < minSide) return false

            val dot = (-v1x * v2x) + (-v1y * v2y)
            val cosTheta = (dot / (len1 * len2)).coerceIn(-1f, 1f)
            val deg = Math.toDegrees(acos(cosTheta.toDouble()))
            if (deg < 40.0 || deg > 145.0) return false
        }
        return true
    }

    private fun hasValidAspectRatioPx(pts: List<FloatPt>, isIdCardHint: Boolean = false): Boolean {
        val topW = hypot(pts[1].x - pts[0].x, pts[1].y - pts[0].y)
        val botW = hypot(pts[2].x - pts[3].x, pts[2].y - pts[3].y)
        val leftH = hypot(pts[3].x - pts[0].x, pts[3].y - pts[0].y)
        val rightH = hypot(pts[2].x - pts[1].x, pts[2].y - pts[1].y)
        val avgW = (topW + botW) * 0.5f
        val avgH = (leftH + rightH) * 0.5f
        val minDim = if (isIdCardHint) 0.08f else 0.12f
        if (avgW < minDim || avgH < minDim) return false
        val ratio = avgW / avgH
        return ratio in 0.30f..3.30f
    }

    private fun measureEdgeSupportPx(
        edges: BooleanArray,
        w: Int,
        h: Int,
        corners: List<FloatPt>
    ): Float {
        var totalSamples = 0
        var hits = 0
        val searchR = if (max(w, h) >= 360) 4 else 2
        for (i in 0 until 4) {
            val a = corners[i]
            val b = corners[(i + 1) % 4]
            val steps = 24
            for (s in 1 until steps) {
                val t = s.toFloat() / steps
                val px = (a.x + (b.x - a.x) * t).roundToInt().coerceIn(searchR, w - searchR - 1)
                val py = (a.y + (b.y - a.y) * t).roundToInt().coerceIn(searchR, h - searchR - 1)
                totalSamples++
                var found = false
                for (dy in -searchR..searchR) {
                    val row = (py + dy) * w
                    for (dx in -searchR..searchR) {
                        if (edges[row + px + dx]) {
                            found = true
                            break
                        }
                    }
                    if (found) break
                }
                if (found) hits++
            }
        }
        return if (totalSamples > 0) hits.toFloat() / totalSamples else 0f
    }
}

/**
 * Temporal Corner Smoothing Buffer (last 4 frames) + 600ms Tight Corner Stability Tracker.
 * Eliminates overlay jitter and triggers near-instantaneous Auto-Capture once corners lock.
 */
class CornerStabilityTracker(
    private val windowSize: Int = 4,
    private val steadyDurationMs: Long = 600L,
    private val jitterThresholdNorm: Float = 0.018f
) {
    private val buffer = ArrayDeque<DocumentQuad>(windowSize)
    private var lastSmoothed: DocumentQuad? = null
    private var missedStreak = 0
    private var jumpStreak = 0
    private var steadyStartMs = 0L
    private var lastCaptureMs = 0L

    fun update(
        detection: EdgeDetectionFrameResult,
        nowMs: Long = System.currentTimeMillis(),
        autoCaptureEnabled: Boolean = true
    ): TrackedEdgeState {
        if (!detection.isLocked) {
            missedStreak++
            jumpStreak = 0
            if (missedStreak >= 4) {
                buffer.clear()
                lastSmoothed = null
                steadyStartMs = 0L
                return TrackedEdgeState(
                    quad = DocumentQuad.SAFE_DEFAULT_MARGIN,
                    confidence = 0f,
                    isLocked = false,
                    stabilityProgress = 0f,
                    shouldAutoCapture = false,
                    frameAspectRatio = detection.frameAspectRatio,
                    frameWidth = detection.frameWidth,
                    frameHeight = detection.frameHeight
                )
            }
            val held = lastSmoothed
            return if (held != null) {
                TrackedEdgeState(
                    quad = held,
                    confidence = detection.confidence * 0.75f,
                    isLocked = true,
                    stabilityProgress = computeProgress(nowMs),
                    shouldAutoCapture = false,
                    frameAspectRatio = detection.frameAspectRatio,
                    frameWidth = detection.frameWidth,
                    frameHeight = detection.frameHeight
                )
            } else {
                TrackedEdgeState(
                    frameAspectRatio = detection.frameAspectRatio,
                    frameWidth = detection.frameWidth,
                    frameHeight = detection.frameHeight
                )
            }
        }

        missedStreak = 0
        val prevHeld = lastSmoothed
        if (prevHeld != null && maxCornerMovement(prevHeld, detection.quad) > 0.24f) {
            // Suppress single-frame outlier jumps; snap cleanly if the camera genuinely moved to a new doc
            jumpStreak++
            if (jumpStreak < 2) {
                return TrackedEdgeState(
                    quad = prevHeld,
                    confidence = detection.confidence * 0.85f,
                    isLocked = true,
                    stabilityProgress = 0f,
                    shouldAutoCapture = false,
                    frameAspectRatio = detection.frameAspectRatio,
                    frameWidth = detection.frameWidth,
                    frameHeight = detection.frameHeight
                )
            }
            buffer.clear()
            jumpStreak = 0
            steadyStartMs = nowMs
        } else {
            jumpStreak = 0
        }

        if (buffer.size >= windowSize) {
            buffer.removeFirst()
        }
        buffer.addLast(detection.quad)

        val smoothed = computeWeightedAverageQuad()
        lastSmoothed = smoothed

        val maxDelta = if (prevHeld != null) maxCornerMovement(prevHeld, smoothed) else 1f
        if (maxDelta <= jitterThresholdNorm) {
            if (steadyStartMs == 0L) steadyStartMs = nowMs
        } else {
            steadyStartMs = nowMs
        }

        val progress = computeProgress(nowMs)
        val cooldownReady = (nowMs - lastCaptureMs) >= 1800L
        val triggerCapture = autoCaptureEnabled && cooldownReady && progress >= 1f
        if (triggerCapture) {
            lastCaptureMs = nowMs
            steadyStartMs = 0L
        }

        return TrackedEdgeState(
            quad = smoothed,
            confidence = detection.confidence,
            isLocked = true,
            stabilityProgress = progress,
            shouldAutoCapture = triggerCapture,
            frameAspectRatio = detection.frameAspectRatio,
            frameWidth = detection.frameWidth,
            frameHeight = detection.frameHeight
        )
    }

    fun markCaptured(nowMs: Long = System.currentTimeMillis()) {
        lastCaptureMs = nowMs
        steadyStartMs = 0L
    }

    fun reset() {
        buffer.clear()
        lastSmoothed = null
        missedStreak = 0
        jumpStreak = 0
        steadyStartMs = 0L
    }

    private fun computeProgress(nowMs: Long): Float {
        if (steadyStartMs == 0L) return 0f
        return ((nowMs - steadyStartMs).toFloat() / steadyDurationMs.toFloat()).coerceIn(0f, 1f)
    }

    private fun computeWeightedAverageQuad(): DocumentQuad {
        var tlX = 0f; var tlY = 0f
        var trX = 0f; var trY = 0f
        var brX = 0f; var brY = 0f
        var blX = 0f; var blY = 0f
        var totalWeight = 0f

        buffer.forEachIndexed { idx, q ->
            val w = (idx + 1).toFloat()
            tlX += q.topLeft.x * w; tlY += q.topLeft.y * w
            trX += q.topRight.x * w; trY += q.topRight.y * w
            brX += q.bottomRight.x * w; brY += q.bottomRight.y * w
            blX += q.bottomLeft.x * w; blY += q.bottomLeft.y * w
            totalWeight += w
        }
        val inv = if (totalWeight > 0f) 1f / totalWeight else 1f
        return DocumentQuad(
            topLeft = PointNorm(tlX * inv, tlY * inv),
            topRight = PointNorm(trX * inv, trY * inv),
            bottomRight = PointNorm(brX * inv, brY * inv),
            bottomLeft = PointNorm(blX * inv, blY * inv)
        )
    }

    private fun maxCornerMovement(a: DocumentQuad, b: DocumentQuad): Float {
        val d1 = hypot(a.topLeft.x - b.topLeft.x, a.topLeft.y - b.topLeft.y)
        val d2 = hypot(a.topRight.x - b.topRight.x, a.topRight.y - b.topRight.y)
        val d3 = hypot(a.bottomRight.x - b.bottomRight.x, a.bottomRight.y - b.bottomRight.y)
        val d4 = hypot(a.bottomLeft.x - b.bottomLeft.x, a.bottomLeft.y - b.bottomLeft.y)
        return max(max(d1, d2), max(d3, d4))
    }
}
