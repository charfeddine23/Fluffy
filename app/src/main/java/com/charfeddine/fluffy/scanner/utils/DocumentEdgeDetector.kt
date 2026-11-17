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
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class EdgeDetectionFrameResult(
    val quad: DocumentQuad,
    val confidence: Float,
    val isLocked: Boolean = false,
    val frameAspectRatio: Float = 0.75f
)

data class TrackedEdgeState(
    val quad: DocumentQuad = DocumentQuad.FULL_FRAME,
    val confidence: Float = 0f,
    val isLocked: Boolean = false,
    val stabilityProgress: Float = 0f,
    val shouldAutoCapture: Boolean = false,
    val frameAspectRatio: Float = 0.75f
)

/**
 * 100% Offline, High-Accuracy Computer Vision Document Edge Detection & Temporal Stability Engine.
 *
 * Pipeline per frame (runs entirely off the UI thread with zero native OpenCV bloat):
 *  1. Rotation-aligned Grayscale luminance extraction.
 *  2. CLAHE (Contrast Limited Adaptive Histogram Equalization, 8x8 tiles + bilinear blend).
 *  3. Separable 5x5 Gaussian Blur (sigma ~1.1) to suppress paper/table grain.
 *  4. 3x3 Morphological Closing (Dilation -> Erosion) to bridge shadows/textures & keep outer edges solid.
 *  5. Sobel Gradient + Non-Maximum Suppression + Adaptive Dual-Threshold Canny Hysteresis.
 *  6. Connected-component boundary contour extraction -> Convex Hull -> Douglas-Peucker 4-point
 *     approximation (`approxPolyDP`) with area (>15%), convexity, aspect-ratio, and edge-support validation.
 */
object DocumentEdgeDetector {

    private const val ANALYZE_W = 144
    private const val ANALYZE_H = 192
    private const val CLAHE_TILES_X = 8
    private const val CLAHE_TILES_Y = 8
    private const val HIST_BINS = 64

    fun detectFromImageProxy(imageProxy: ImageProxy): EdgeDetectionFrameResult {
        val plane = imageProxy.planes.firstOrNull()
            ?: return EdgeDetectionFrameResult(DocumentQuad.FULL_FRAME, 0f, false)
        val srcW = imageProxy.width
        val srcH = imageProxy.height
        if (srcW < 32 || srcH < 32) {
            return EdgeDetectionFrameResult(DocumentQuad.FULL_FRAME, 0f, false)
        }

        val rotation = ((imageProxy.imageInfo.rotationDegrees % 360) + 360) % 360
        val rotAspect = if (rotation == 90 || rotation == 270) {
            srcH.toFloat() / srcW.toFloat()
        } else {
            srcW.toFloat() / srcH.toFloat()
        }

        val gridW: Int
        val gridH: Int
        if (rotAspect >= 1f) {
            gridW = ANALYZE_H
            gridH = ANALYZE_W
        } else {
            gridW = ANALYZE_W
            gridH = ANALYZE_H
        }

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

        val res = runPipeline(luma, gridW, gridH)
        return res.copy(frameAspectRatio = rotAspect)
    }

    fun detectFromBitmap(bitmap: Bitmap): DocumentQuad {
        val aspect = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1).toFloat()
        val gridW = if (aspect >= 1f) ANALYZE_H else ANALYZE_W
        val gridH = if (aspect >= 1f) ANALYZE_W else ANALYZE_H
        val scaled = Bitmap.createScaledBitmap(bitmap, gridW, gridH, true)
        val pixels = IntArray(gridW * gridH)
        scaled.getPixels(pixels, 0, gridW, 0, 0, gridW, gridH)
        if (scaled !== bitmap) scaled.recycle()

        val luma = IntArray(gridW * gridH) { i ->
            val c = pixels[i]
            (Color.red(c) * 77 + Color.green(c) * 150 + Color.blue(c) * 29) shr 8
        }
        val res = runPipeline(luma, gridW, gridH)
        return if (res.isLocked) res.quad else DocumentQuad.FULL_FRAME
    }

    private fun runPipeline(rawLuma: IntArray, w: Int, h: Int): EdgeDetectionFrameResult {
        // 1. CLAHE for robust separation on low-contrast or unevenly lit backgrounds
        val clahe = applyClahe(rawLuma, w, h)

        // 2. Separable 5x5 Gaussian Blur
        val blurred = applyGaussianBlur5x5(clahe, w, h)

        // 3. Morphological Closing (Dilation followed by Erosion) removes fabric/wood/text noise
        val closed = applyMorphologicalClosing3x3(blurred, w, h)

        // 4. Adaptive Canny Edge Detection (Sobel + NMS + Hysteresis)
        val edges = detectCannyEdges(closed, w, h)

        // 5. Contour extraction + ApproxPolyDP (or structured edge-supported fallback)
        return findBestDocumentPolygon(edges, w, h)
    }

    /**
     * Contrast Limited Adaptive Histogram Equalization (CLAHE) with bilinear tile interpolation.
     */
    private fun applyClahe(src: IntArray, w: Int, h: Int): IntArray {
        val tilesX = CLAHE_TILES_X
        val tilesY = CLAHE_TILES_Y
        val tileW = max(4f, w.toFloat() / tilesX)
        val tileH = max(4f, h.toFloat() / tilesY)

        // Build clipped CDF lookup tables per tile: [ty][tx][bin] -> mapped 0..255
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

        // Bilinear interpolation across neighboring tiles
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

        // Horizontal pass
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

        // Vertical pass
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
     * 3x3 Grayscale Morphological Closing (Dilation -> Erosion).
     */
    private fun applyMorphologicalClosing3x3(src: IntArray, w: Int, h: Int): IntArray {
        val dilated = IntArray(w * h)
        val closed = IntArray(w * h)

        // Dilation (local 3x3 max)
        for (y in 0 until h) {
            val y0 = max(0, y - 1)
            val y1 = min(h - 1, y + 1)
            for (x in 0 until w) {
                val x0 = max(0, x - 1)
                val x1 = min(w - 1, x + 1)
                var maxV = 0
                for (ny in y0..y1) {
                    val row = ny * w
                    for (nx in x0..x1) {
                        val v = src[row + nx]
                        if (v > maxV) maxV = v
                    }
                }
                dilated[y * w + x] = maxV
            }
        }

        // Erosion (local 3x3 min)
        for (y in 0 until h) {
            val y0 = max(0, y - 1)
            val y1 = min(h - 1, y + 1)
            for (x in 0 until w) {
                val x0 = max(0, x - 1)
                val x1 = min(w - 1, x + 1)
                var minV = 255
                for (ny in y0..y1) {
                    val row = ny * w
                    for (nx in x0..x1) {
                        val v = dilated[row + nx]
                        if (v < minV) minV = v
                    }
                }
                closed[y * w + x] = minV
            }
        }
        return closed
    }

    /**
     * Canny Edge Detection with Sobel gradients, Non-Maximum Suppression,
     * Adaptive Percentile Thresholding, and BFS Hysteresis Edge Tracking.
     */
    private fun detectCannyEdges(src: IntArray, w: Int, h: Int): BooleanArray {
        val n = w * h
        val mag = IntArray(n)
        val dir = ByteArray(n) // 0: 0deg, 1: 45deg, 2: 90deg, 3: 135deg

        for (y in 1 until h - 1) {
            val ym = (y - 1) * w
            val y0 = y * w
            val yp = (y + 1) * w
            for (x in 1 until w - 1) {
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

        // Non-Maximum Suppression
        val nms = IntArray(n)
        val histBins = IntArray(256)
        var nmsMax = 1
        var nonZeroCount = 0

        for (y in 2 until h - 2) {
            val y0 = y * w
            for (x in 2 until w - 2) {
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
                    if (m > nmsMax) nmsMax = m
                    histBins[min(255, m shr 2)]++
                    nonZeroCount++
                }
            }
        }

        if (nonZeroCount < 20) return BooleanArray(n)

        // Adaptive threshold at ~82nd percentile of NMS ridges
        val targetRank = (nonZeroCount * 0.82f).toInt()
        var acc = 0
        var highThresh = 75
        for (b in 0 until 256) {
            acc += histBins[b]
            if (acc >= targetRank) {
                highThresh = max(56, b shl 2)
                break
            }
        }
        val lowThresh = max(24, (highThresh * 0.42f).toInt())

        // Double thresholding + BFS Hysteresis
        val strong = BooleanArray(n)
        val weak = BooleanArray(n)
        val queue = IntArray(n)
        var head = 0
        var tail = 0

        for (y in 2 until h - 2) {
            val row = y * w
            for (x in 2 until w - 2) {
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
                if (ny !in 1 until h - 1) continue
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = cx + dx
                    if (nx !in 1 until w - 1) continue
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

    private data class PxPt(val x: Int, val y: Int)

    /**
     * Traces 8-connected edge components, builds convex hull + approxPolyDP,
     * and scores valid 4-point convex rectangles >15% of frame area.
     */
    private fun findBestDocumentPolygon(edges: BooleanArray, w: Int, h: Int): EdgeDetectionFrameResult {
        val visited = BooleanArray(w * h)
        val components = ArrayList<List<PxPt>>()
        val stack = IntArray(w * h)

        for (y in 2 until h - 2) {
            for (x in 2 until w - 2) {
                val startIdx = y * w + x
                if (!edges[startIdx] || visited[startIdx]) continue

                val pts = ArrayList<PxPt>(128)
                var sp = 0
                stack[sp++] = startIdx
                visited[startIdx] = true

                while (sp > 0) {
                    val cur = stack[--sp]
                    val cy = cur / w
                    val cx = cur - cy * w
                    pts.add(PxPt(cx, cy))

                    // Explore 2-pixel neighborhood to bridge tiny 1px edge breaks
                    for (dy in -1..1) {
                        val ny = cy + dy
                        if (ny !in 1 until h - 1) continue
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = cx + dx
                            if (nx !in 1 until w - 1) continue
                            val nIdx = ny * w + nx
                            if (edges[nIdx] && !visited[nIdx]) {
                                visited[nIdx] = true
                                stack[sp++] = nIdx
                            }
                        }
                    }
                }

                if (pts.size >= (w + h) / 3) {
                    components.add(pts)
                }
            }
        }

        val frameArea = (w * h).toFloat()
        var bestScore = -1f
        var bestQuad: DocumentQuad? = null

        // Evaluate top largest edge components individually + merged top-2 nearby strong components
        val sortedComponents = components.sortedByDescending { it.size }.take(8)
        for (comp in sortedComponents) {
            val hull = computeConvexHull(comp)
            if (hull.size < 4) continue

            val candidate = fitFourPointPolygon(hull, w, h) ?: continue
            val areaFrac = polygonAreaNorm(candidate)
            if (areaFrac < 0.15f || areaFrac > 0.96f) continue
            if (!isConvexAndValidAngles(candidate)) continue
            if (!hasValidAspectRatio(candidate)) continue

            val edgeSupport = measureEdgeSupport(edges, w, h, candidate)
            if (edgeSupport < 0.24f) continue

            val score = (areaFrac * 0.45f) + (edgeSupport * 0.55f)
            if (score > bestScore) {
                bestScore = score
                bestQuad = candidate
            }
        }

        // Fallback check: combine top 2-3 edge components if page shadow split one long border
        if (bestQuad == null && sortedComponents.size >= 2) {
            val merged = ArrayList<PxPt>(512)
            sortedComponents.take(3).forEach { merged.addAll(it) }
            val hull = computeConvexHull(merged)
            if (hull.size >= 4) {
                val candidate = fitFourPointPolygon(hull, w, h)
                if (candidate != null) {
                    val areaFrac = polygonAreaNorm(candidate)
                    if (areaFrac in 0.16f..0.95f &&
                        isConvexAndValidAngles(candidate) &&
                        hasValidAspectRatio(candidate)
                    ) {
                        val edgeSupport = measureEdgeSupport(edges, w, h, candidate)
                        if (edgeSupport >= 0.26f) {
                            bestQuad = candidate
                            bestScore = (areaFrac * 0.40f) + (edgeSupport * 0.60f)
                        }
                    }
                }
            }
        }

        return if (bestQuad != null && bestScore > 0.28f) {
            EdgeDetectionFrameResult(
                quad = bestQuad,
                confidence = bestScore.coerceIn(0.35f, 0.99f),
                isLocked = true
            )
        } else {
            EdgeDetectionFrameResult(
                quad = DocumentQuad.FULL_FRAME,
                confidence = 0f,
                isLocked = false
            )
        }
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

    /**
     * Adaptive Douglas-Peucker (`approxPolyDP`) targeting a 4-vertex polygon on a closed convex hull.
     */
    private fun fitFourPointPolygon(hull: List<PxPt>, w: Int, h: Int): DocumentQuad? {
        if (hull.size < 4) return null
        val perimeter = computeClosedPerimeter(hull)
        if (perimeter < 20f) return null

        // Sweep epsilon ratios to extract crisp 4-point polygon
        val epsFractions = floatArrayOf(0.02f, 0.03f, 0.045f, 0.06f, 0.08f, 0.11f)
        for (frac in epsFractions) {
            val approx = approxClosedPolyDP(hull, perimeter * frac)
            if (approx.size == 4) {
                return orderFourCorners(approx, w, h)
            }
        }

        // If polygon stays at 5..7 vertices due to beveled corners, fall back to extreme diagonal corners
        val approxLoose = approxClosedPolyDP(hull, perimeter * 0.035f)
        if (approxLoose.size in 4..8) {
            val tl = approxLoose.minByOrNull { it.x + it.y } ?: return null
            val br = approxLoose.maxByOrNull { it.x + it.y } ?: return null
            val tr = approxLoose.maxByOrNull { it.x - it.y } ?: return null
            val bl = approxLoose.minByOrNull { it.x - it.y } ?: return null
            val distinct = setOf(tl, tr, br, bl)
            if (distinct.size == 4) {
                return orderFourCorners(listOf(tl, tr, br, bl), w, h)
            }
        }
        return null
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
        // Split closed ring at opposite extremes for deterministic Douglas-Peucker
        var idxA = 0
        var idxB = hull.size / 2
        var maxD = -1f
        for (i in hull.indices) {
            val d = hypot((hull[i].x - hull[0].x).toFloat(), (hull[i].y - hull[0].y).toFloat())
            if (d > maxD) {
                maxD = d
                idxB = i
            }
        }
        val firstHalf = hull.subList(idxA, idxB + 1)
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

    private fun orderFourCorners(pts: List<PxPt>, w: Int, h: Int): DocumentQuad {
        val norm = pts.map {
            PointNorm(
                x = (it.x.toFloat() / (w - 1).coerceAtLeast(1)).coerceIn(0.01f, 0.99f),
                y = (it.y.toFloat() / (h - 1).coerceAtLeast(1)).coerceIn(0.01f, 0.99f)
            )
        }
        val tl = norm.minBy { it.x + it.y }
        val br = norm.maxBy { it.x + it.y }
        val remaining = norm.filter { it !== tl && it !== br }
        val tr = remaining.maxBy { it.x - it.y }
        val bl = remaining.minBy { it.x - it.y }
        return DocumentQuad(tl, tr, br, bl)
    }

    private fun polygonAreaNorm(q: DocumentQuad): Float {
        val xs = floatArrayOf(q.topLeft.x, q.topRight.x, q.bottomRight.x, q.bottomLeft.x)
        val ys = floatArrayOf(q.topLeft.y, q.topRight.y, q.bottomRight.y, q.bottomLeft.y)
        var sum = 0f
        for (i in 0 until 4) {
            val j = (i + 1) % 4
            sum += xs[i] * ys[j] - xs[j] * ys[i]
        }
        return abs(sum) * 0.5f
    }

    private fun isConvexAndValidAngles(q: DocumentQuad): Boolean {
        val pts = arrayOf(q.topLeft, q.topRight, q.bottomRight, q.bottomLeft)
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

            // Interior angle cosine (using vectors from vertex b: -v1 and v2)
            val dot = (-v1x * v2x) + (-v1y * v2y)
            val cosTheta = (dot / (len1 * len2)).coerceIn(-1f, 1f)
            val deg = Math.toDegrees(acos(cosTheta.toDouble()))
            if (deg < 40.0 || deg > 145.0) return false
        }
        return true
    }

    private fun hasValidAspectRatio(q: DocumentQuad): Boolean {
        val topW = hypot(q.topRight.x - q.topLeft.x, q.topRight.y - q.topLeft.y)
        val botW = hypot(q.bottomRight.x - q.bottomLeft.x, q.bottomRight.y - q.bottomLeft.y)
        val leftH = hypot(q.bottomLeft.x - q.topLeft.x, q.bottomLeft.y - q.topLeft.y)
        val rightH = hypot(q.bottomRight.x - q.topRight.x, q.bottomRight.y - q.topRight.y)
        val avgW = (topW + botW) * 0.5f
        val avgH = (leftH + rightH) * 0.5f
        if (avgW < 0.18f || avgH < 0.18f) return false
        val ratio = avgW / avgH
        return ratio in 0.35f..2.85f
    }

    private fun measureEdgeSupport(edges: BooleanArray, w: Int, h: Int, q: DocumentQuad): Float {
        val corners = arrayOf(q.topLeft, q.topRight, q.bottomRight, q.bottomLeft)
        var totalSamples = 0
        var hits = 0
        for (i in 0 until 4) {
            val a = corners[i]
            val b = corners[(i + 1) % 4]
            val steps = 20
            for (s in 0..steps) {
                val t = s.toFloat() / steps
                val px = ((a.x + (b.x - a.x) * t) * (w - 1)).roundToInt().coerceIn(2, w - 3)
                val py = ((a.y + (b.y - a.y) * t) * (h - 1)).roundToInt().coerceIn(2, h - 3)
                totalSamples++
                var found = false
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        if (edges[(py + dy) * w + (px + dx)]) {
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
 * Temporal Corner Smoothing Buffer (last 5 frames) + 1.2s Corner Stability Tracker.
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
                    quad = DocumentQuad.FULL_FRAME,
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
            val w = (idx + 1).toFloat() // More recent frames carry higher weight
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
