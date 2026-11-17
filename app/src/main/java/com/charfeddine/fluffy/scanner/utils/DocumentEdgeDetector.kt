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
    val frameAspectRatio: Float = 0.75f
)

data class TrackedEdgeState(
    val quad: DocumentQuad = DocumentQuad.SAFE_DEFAULT_MARGIN,
    val confidence: Float = 0f,
    val isLocked: Boolean = false,
    val stabilityProgress: Float = 0f,
    val shouldAutoCapture: Boolean = false,
    val frameAspectRatio: Float = 0.75f
)

/**
 * 100% Offline, Multi-Stage Computer Vision Document Boundary & Line-Intersection Engine.
 *
 * Designed for BOTH:
 *  - Real-time CameraX ImageAnalysis frames (fast ~192px path + Temporal Stability Tracker).
 *  - High-accuracy Static / Gallery Photos (`detectFromBitmap`):
 *      1. Temporary downscaled working copy (max dimension ~800px) with exact `scaleX` / `scaleY`
 *         coordinate mapping back to original full-resolution dimensions.
 *      2. Pass 1 (Standard Contrast): Grayscale -> CLAHE -> 5x5 Gaussian Blur ->
 *         Separable Morphological Closing -> Adaptive Canny Edge Detection.
 *      3. Pass 2 (White-on-Light / Low-Contrast Fallback): HSV Saturation/Value + Border Chroma
 *         contrast -> Integral-Image Adaptive Thresholding Mask Perimeter + Sensitive Canny.
 *      4. Smart Quadrilateral Fitting via PCA Orthogonal Line Regression & Geometric Intersections
 *         (reconstructs sharp 4 corners even on rounded ID cards or folded document corners).
 *      5. Graceful 8% inset fallback ([DocumentQuad.SAFE_DEFAULT_MARGIN]) if no document is found.
 */
object DocumentEdgeDetector {

    private const val LIVE_ANALYZE_W = 180
    private const val LIVE_ANALYZE_H = 240
    private const val STATIC_WORK_MAX_DIM = 800
    private const val CLAHE_TILES_X = 8
    private const val CLAHE_TILES_Y = 8
    private const val HIST_BINS = 64

    private data class PxPt(val x: Int, val y: Int)
    private data class FloatPt(val x: Float, val y: Float)

    /**
     * Orthogonal line representation: a*x + b*y + c = 0 with a^2 + b^2 = 1.
     */
    private data class OrthoLine(val a: Double, val b: Double, val c: Double)

    /**
     * Real-time CameraX frame analyzer (runs on ImageAnalysis background executor).
     */
    fun detectFromImageProxy(imageProxy: ImageProxy): EdgeDetectionFrameResult {
        val plane = imageProxy.planes.firstOrNull()
            ?: return EdgeDetectionFrameResult(DocumentQuad.SAFE_DEFAULT_MARGIN, 0f, false)
        val srcW = imageProxy.width
        val srcH = imageProxy.height
        if (srcW < 32 || srcH < 32) {
            return EdgeDetectionFrameResult(DocumentQuad.SAFE_DEFAULT_MARGIN, 0f, false)
        }

        val rotation = ((imageProxy.imageInfo.rotationDegrees % 360) + 360) % 360
        val rotAspect = if (rotation == 90 || rotation == 270) {
            srcH.toFloat() / srcW.toFloat()
        } else {
            srcW.toFloat() / srcH.toFloat()
        }

        val gridW = if (rotAspect >= 1f) LIVE_ANALYZE_H else LIVE_ANALYZE_W
        val gridH = if (rotAspect >= 1f) LIVE_ANALYZE_W else LIVE_ANALYZE_H

        val buffer = plane.buffer
        buffer.rewind()
        val limit = buffer.limit()
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        val luma = IntArray(gridW * gridH)
        val wMinus1 = srcW - 1
        val hMinus1 = srcH - 1
        val gwMinus1 = (gridW - 1).coerceAtLeast(1).toFloat()
        val ghMinus1 = (gridH - 1).coerceAtLeast(1).toFloat()

        for (gy in 0 until gridH) {
            val v = gy / ghMinus1
            for (gx in 0 until gridW) {
                val u = gx / gwMinus1
                val srcX: Int
                val srcY: Int
                when (rotation) {
                    90 -> {
                        srcX = (v * wMinus1).toInt()
                        srcY = ((1f - u) * hMinus1).toInt()
                    }
                    180 -> {
                        srcX = ((1f - u) * wMinus1).toInt()
                        srcY = ((1f - v) * hMinus1).toInt()
                    }
                    270 -> {
                        srcX = ((1f - v) * wMinus1).toInt()
                        srcY = (u * hMinus1).toInt()
                    }
                    else -> {
                        srcX = (u * wMinus1).toInt()
                        srcY = (v * hMinus1).toInt()
                    }
                }
                val idx = srcY.coerceIn(0, hMinus1) * rowStride + srcX.coerceIn(0, wMinus1) * pixelStride
                luma[gy * gridW + gx] = if (idx in 0 until limit) {
                    buffer.get(idx).toInt() and 0xFF
                } else {
                    128
                }
            }
        }

        val res = runStandardPass(
            rawLuma = luma,
            w = gridW,
            h = gridH,
            closingRadius = 1,
            origW = gridW,
            origH = gridH,
            scaleX = 1f,
            scaleY = 1f
        )
        return res.copy(frameAspectRatio = rotAspect)
    }

    /**
     * High-accuracy multi-stage document boundary detection for Gallery photos and captured Bitmaps.
     * Should be invoked on `Dispatchers.Default`.
     */
    fun detectFromBitmap(bitmap: Bitmap): DocumentQuad {
        val origW = bitmap.width
        val origH = bitmap.height
        if (origW < 32 || origH < 32) return DocumentQuad.SAFE_DEFAULT_MARGIN

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

        // 2. Pass 1: Standard Contrast (Grayscale -> CLAHE -> 5x5 Gaussian -> Morphological Closing -> Canny)
        val closingRadius = if (max(workW, workH) >= 420) 3 else 1
        val pass1 = runStandardPass(
            rawLuma = rawLuma,
            w = workW,
            h = workH,
            closingRadius = closingRadius,
            origW = origW,
            origH = origH,
            scaleX = scaleX,
            scaleY = scaleY
        )

        if (pass1.isLocked && pass1.confidence >= 0.64f) {
            return pass1.quad
        }

        // 3. Pass 2: Fallback for White-on-Light / Low-Contrast / Colored Backgrounds
        //    Uses HSV Saturation/Value extraction + Border Chroma contrast + Adaptive Thresholding
        val pass2 = runWhiteOnLightFallbackPass(
            pixels = pixels,
            rawLuma = rawLuma,
            w = workW,
            h = workH,
            closingRadius = closingRadius,
            origW = origW,
            origH = origH,
            scaleX = scaleX,
            scaleY = scaleY
        )

        val winner = when {
            pass1.isLocked && pass2.isLocked ->
                if (pass2.confidence > pass1.confidence + 0.04f) pass2 else pass1
            pass1.isLocked -> pass1
            pass2.isLocked -> pass2
            else -> null
        }

        // 4. Graceful Safe Default Margin (8% inset rectangle; never 0,0 full frame)
        return winner?.quad ?: DocumentQuad.SAFE_DEFAULT_MARGIN
    }

    private fun runStandardPass(
        rawLuma: IntArray,
        w: Int,
        h: Int,
        closingRadius: Int,
        origW: Int,
        origH: Int,
        scaleX: Float,
        scaleY: Float
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
            scaleY = scaleY
        )
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
        scaleY: Float
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
            scaleY = scaleY
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
            scaleY = scaleY
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
        val tilesX = CLAHE_TILES_X
        val tilesY = CLAHE_TILES_Y
        val tileW = max(4f, w.toFloat() / tilesX)
        val tileH = max(4f, h.toFloat() / tilesY)

        val luts = Array(tilesY) { Array(tilesX) { IntArray(HIST_BINS) } }
        val hist = IntArray(HIST_BINS)

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

        val out = IntArray(w * h)
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
                out[rowOff + x] = (top + (bot - top) * wy).roundToInt().coerceIn(0, 255)
            }
        }
        return out
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
        scaleY: Float
    ): EdgeDetectionFrameResult {
        val isHighResWork = max(w, h) >= 360
        val clusterEdges = if (isHighResWork) dilateBinary1Px(edges, w, h) else edges

        val visited = BooleanArray(w * h)
        val components = ArrayList<List<PxPt>>()
        val stack = IntArray(w * h)
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

        // Build candidate point clouds: individual top components + merged top-2/top-3 large borders
        val candidateClouds = ArrayList<List<PxPt>>(sortedComponents.size + 2)
        candidateClouds.addAll(sortedComponents)
        if (sortedComponents.size >= 2) {
            val merged2 = ArrayList<PxPt>(sortedComponents[0].size + sortedComponents[1].size)
            merged2.addAll(sortedComponents[0])
            merged2.addAll(sortedComponents[1])
            candidateClouds.add(merged2)
        }
        if (sortedComponents.size >= 3) {
            val merged3 = ArrayList<PxPt>(1024)
            sortedComponents.take(3).forEach { merged3.addAll(it) }
            candidateClouds.add(merged3)
        }

        for (cloud in candidateClouds) {
            val hull = computeConvexHull(cloud)
            if (hull.size < 4) continue

            // Candidate A: Line-Intersection Quadrilateral Reconstruction (handles rounded / folded corners)
            val lineCorners = reconstructQuadByLineIntersections(hull, cloud, edges, w, h)
            if (lineCorners != null) {
                val score = evaluateCandidateWorkQuad(lineCorners, edges, w, h)
                if (score > bestScore) {
                    bestScore = score
                    bestWorkCorners = lineCorners
                }
            }

            // Candidate B: Adaptive Douglas-Peucker cross-check
            val polyCorners = fitFourPointPolygonPx(hull)
            if (polyCorners != null) {
                val floatPoly = polyCorners.map { FloatPt(it.x.toFloat(), it.y.toFloat()) }
                val score = evaluateCandidateWorkQuad(floatPoly, edges, w, h)
                if (score > bestScore) {
                    bestScore = score
                    bestWorkCorners = floatPoly
                }
            }
        }

        return if (bestWorkCorners != null && bestScore > 0.30f) {
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
     * fitting orthogonal PCA lines (`a*x + b*y + c = 0`), intersecting adjacent lines,
     * and running 1 refinement pass on raw thin edge pixels.
     */
    private fun reconstructQuadByLineIntersections(
        hull: List<PxPt>,
        cloud: List<PxPt>,
        edges: BooleanArray,
        w: Int,
        h: Int
    ): List<FloatPt>? {
        if (hull.size < 4) return null

        // Try standard diagonal extrema + 22.5-degree rotated extrema seeds so angled documents work cleanly
        val seedAnglesDeg = floatArrayOf(0f, 22.5f)
        var bestReconstructed: List<FloatPt>? = null
        var bestSupport = -1f

        val distTol = max(3.5f, min(w, h) * 0.015f)

        for (deg in seedAnglesDeg) {
            val rad = Math.toRadians(deg.toDouble())
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
                if (segLen < min(w, h) * 0.14f) {
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
                    fitOrthogonalLine(sideInliers)
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
                                // Only keep points close perpendicular to line
                                val d = abs((px - a.x) * (-uy) + (py - a.y) * ux)
                                if (d <= refineTol) {
                                    refinedPts.add(FloatPt(px.toFloat(), py.toFloat()))
                                }
                            }
                        }
                    }
                }
                val rLine = if (refinedPts.size >= 8) fitOrthogonalLine(refinedPts) else lines[side]
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
        val marginX = w * 0.08f
        val marginY = h * 0.08f
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

    private fun evaluateCandidateWorkQuad(corners: List<FloatPt>, edges: BooleanArray, w: Int, h: Int): Float {
        if (corners.size != 4) return -1f
        val wf = w.toFloat().coerceAtLeast(1f)
        val hf = h.toFloat().coerceAtLeast(1f)
        val norm = corners.map { FloatPt(it.x / wf, it.y / hf) }

        val areaFrac = polygonArea(norm)
        if (areaFrac < 0.15f || areaFrac > 0.96f) return -1f
        if (!isConvexAndValidAnglesPx(norm)) return -1f
        if (!hasValidAspectRatioPx(norm)) return -1f

        val edgeSupport = measureEdgeSupportPx(edges, w, h, corners)
        if (edgeSupport < 0.24f) return -1f

        // Opposite-edge length consistency bonus (rectangles have similar opposite side lengths)
        val topL = hypot(norm[1].x - norm[0].x, norm[1].y - norm[0].y)
        val botL = hypot(norm[2].x - norm[3].x, norm[2].y - norm[3].y)
        val leftL = hypot(norm[3].x - norm[0].x, norm[3].y - norm[0].y)
        val rightL = hypot(norm[2].x - norm[1].x, norm[2].y - norm[1].y)
        val ratioW = min(topL, botL) / max(topL, botL).coerceAtLeast(1e-4f)
        val ratioH = min(leftL, rightL) / max(leftL, rightL).coerceAtLeast(1e-4f)
        val rectScore = ((ratioW + ratioH) * 0.5f).coerceIn(0f, 1f)

        return (edgeSupport * 0.56f) + (areaFrac * 0.28f) + (rectScore * 0.16f)
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

    private fun fitFourPointPolygonPx(hull: List<PxPt>): List<PxPt>? {
        if (hull.size < 4) return null
        val perimeter = computeClosedPerimeter(hull)
        if (perimeter < 20f) return null

        val epsFractions = floatArrayOf(0.02f, 0.03f, 0.045f, 0.06f, 0.08f, 0.11f)
        for (frac in epsFractions) {
            val approx = approxClosedPolyDP(hull, perimeter * frac)
            if (approx.size == 4) {
                return orderFourPxCornersClockwise(approx)
            }
        }
        return null
    }

    private fun orderFourPxCornersClockwise(pts: List<PxPt>): List<PxPt> {
        val tl = pts.minBy { it.x + it.y }
        val br = pts.maxBy { it.x + it.y }
        val remaining = pts.filter { it !== tl && it !== br }
        if (remaining.size < 2) return pts
        val tr = remaining.maxBy { it.x - it.y }
        val bl = remaining.minBy { it.x - it.y }
        return listOf(tl, tr, br, bl)
    }

    private fun computeClosedPerimeter(pts: List<PxPt>): Float {
        var sum = 0f
        for (i in pts.indices) {
            val a = pts[i]
            val b = pts[(i + 1) % pts.size]
            sum += hypot((b.x - a.x).toFloat(), (b.y - a.y).toFloat())
        }
        return sum
    }

    private fun approxClosedPolyDP(hull: List<PxPt>, epsilon: Float): List<PxPt> {
        if (hull.size <= 4) return hull
        var idxB = hull.size / 2
        var maxD = -1f
        for (i in hull.indices) {
            val d = hypot((hull[i].x - hull[0].x).toFloat(), (hull[i].y - hull[0].y).toFloat())
            if (d > maxD) {
                maxD = d
                idxB = i
            }
        }
        val firstHalf = hull.subList(0, idxB + 1)
        val secondHalf = hull.subList(idxB, hull.size) + hull[0]

        val simp1 = douglasPeuckerOpen(firstHalf, epsilon)
        val simp2 = douglasPeuckerOpen(secondHalf, epsilon)

        val combined = ArrayList<PxPt>(simp1.size + simp2.size)
        combined.addAll(simp1.dropLast(1))
        combined.addAll(simp2.dropLast(1))
        return combined
    }

    private fun douglasPeuckerOpen(pts: List<PxPt>, epsilon: Float): List<PxPt> {
        if (pts.size < 3) return pts
        val start = pts.first()
        val end = pts.last()
        var maxDist = 0f
        var maxIdx = 0
        for (i in 1 until pts.lastIndex) {
            val d = perpDistance(pts[i], start, end)
            if (d > maxDist) {
                maxDist = d
                maxIdx = i
            }
        }
        return if (maxDist > epsilon) {
            val left = douglasPeuckerOpen(pts.subList(0, maxIdx + 1), epsilon)
            val right = douglasPeuckerOpen(pts.subList(maxIdx, pts.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(start, end)
        }
    }

    private fun perpDistance(p: PxPt, a: PxPt, b: PxPt): Float {
        val dx = (b.x - a.x).toFloat()
        val dy = (b.y - a.y).toFloat()
        val len = hypot(dx, dy)
        if (len < 1e-4f) return hypot((p.x - a.x).toFloat(), (p.y - a.y).toFloat())
        return abs(dy * p.x - dx * p.y + b.x * a.y - b.y * a.x) / len
    }

    private fun polygonArea(pts: List<FloatPt>): Float {
        var sum = 0f
        for (i in 0 until 4) {
            val j = (i + 1) % 4
            sum += pts[i].x * pts[j].y - pts[j].x * pts[i].y
        }
        return abs(sum) * 0.5f
    }

    private fun isConvexAndValidAnglesPx(pts: List<FloatPt>): Boolean {
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
            if (len1 < 0.08f || len2 < 0.08f) return false

            val dot = (-v1x * v2x) + (-v1y * v2y)
            val cosTheta = (dot / (len1 * len2)).coerceIn(-1f, 1f)
            val deg = Math.toDegrees(acos(cosTheta.toDouble()))
            if (deg < 40.0 || deg > 145.0) return false
        }
        return true
    }

    private fun hasValidAspectRatioPx(pts: List<FloatPt>): Boolean {
        val topW = hypot(pts[1].x - pts[0].x, pts[1].y - pts[0].y)
        val botW = hypot(pts[2].x - pts[3].x, pts[2].y - pts[3].y)
        val leftH = hypot(pts[3].x - pts[0].x, pts[3].y - pts[0].y)
        val rightH = hypot(pts[2].x - pts[1].x, pts[2].y - pts[1].y)
        val avgW = (topW + botW) * 0.5f
        val avgH = (leftH + rightH) * 0.5f
        if (avgW < 0.16f || avgH < 0.16f) return false
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
 * Temporal Corner Smoothing Buffer (last 5 frames) + 1.0s Corner Stability Tracker.
 * Eliminates overlay jitter and reliably triggers Auto-Capture once corners hold steady.
 */
class CornerStabilityTracker(
    private val windowSize: Int = 5,
    private val steadyDurationMs: Long = 1000L,
    private val jitterThresholdNorm: Float = 0.022f
) {
    private val buffer = ArrayDeque<DocumentQuad>(windowSize)
    private var lastSmoothed: DocumentQuad? = null
    private var missedStreak = 0
    private var steadyStartMs = 0L
    private var lastCaptureMs = 0L

    fun update(
        detection: EdgeDetectionFrameResult,
        nowMs: Long = System.currentTimeMillis(),
        autoCaptureEnabled: Boolean = true
    ): TrackedEdgeState {
        if (!detection.isLocked) {
            missedStreak++
            if (missedStreak >= 3) {
                buffer.clear()
                lastSmoothed = null
                steadyStartMs = 0L
                return TrackedEdgeState(
                    quad = DocumentQuad.SAFE_DEFAULT_MARGIN,
                    confidence = 0f,
                    isLocked = false,
                    stabilityProgress = 0f,
                    shouldAutoCapture = false,
                    frameAspectRatio = detection.frameAspectRatio
                )
            }
            val held = lastSmoothed
            return if (held != null) {
                TrackedEdgeState(
                    quad = held,
                    confidence = detection.confidence * 0.7f,
                    isLocked = true,
                    stabilityProgress = computeProgress(nowMs),
                    shouldAutoCapture = false,
                    frameAspectRatio = detection.frameAspectRatio
                )
            } else {
                TrackedEdgeState(frameAspectRatio = detection.frameAspectRatio)
            }
        }

        missedStreak = 0
        if (buffer.size >= windowSize) {
            buffer.removeFirst()
        }
        buffer.addLast(detection.quad)

        val smoothed = computeWeightedAverageQuad()
        val prev = lastSmoothed
        lastSmoothed = smoothed

        val maxDelta = if (prev != null) maxCornerMovement(prev, smoothed) else 1f
        if (maxDelta <= jitterThresholdNorm) {
            if (steadyStartMs == 0L) steadyStartMs = nowMs
        } else {
            steadyStartMs = nowMs
        }

        val progress = computeProgress(nowMs)
        val cooldownReady = (nowMs - lastCaptureMs) >= 2200L
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
            frameAspectRatio = detection.frameAspectRatio
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
