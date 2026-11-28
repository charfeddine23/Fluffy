package com.charfeddine.fluffy.scanner.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.charfeddine.fluffy.scanner.data.model.DocumentQuad
import com.charfeddine.fluffy.scanner.data.model.PageAnnotations
import com.charfeddine.fluffy.scanner.data.model.PointNorm
import com.charfeddine.fluffy.scanner.data.model.ScannerFilterType
import com.charfeddine.fluffy.scanner.data.model.SignaturePlacement
import com.charfeddine.fluffy.scanner.data.model.WatermarkConfig
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

object DocumentImageProcessor {

    /**
     * Delegates real-time 100% offline 4-corner detection to [DocumentEdgeDetector].
     */
    fun detectDocumentQuadFromImageProxy(
        imageProxy: ImageProxy,
        isIdCardHint: Boolean = false
    ): EdgeDetectionFrameResult =
        DocumentEdgeDetector.detectFromImageProxy(imageProxy, isIdCardHint)

    fun detectQuadFromBitmap(bitmap: Bitmap, isIdCardHint: Boolean = false): DocumentQuad =
        DocumentEdgeDetector.detectFromBitmap(bitmap, isIdCardHint)

    fun detectIdCardQuad(bitmap: Bitmap, guideQuad: DocumentQuad? = null): DocumentQuad =
        DocumentEdgeDetector.detectIdCardWithGuidance(bitmap, guideQuad)

    /**
     * Maps 4 corner [PointF] coordinates from [PreviewView] space (`previewWidth x previewHeight`)
     * to exact upright full-resolution [Bitmap] coordinates (`bitmapWidth x bitmapHeight`) using
     * explicit `scaleX = bitmapWidth / previewWidth` and `scaleY = bitmapHeight / previewHeight`
     * while accounting for `PreviewView.ScaleType.FILL_CENTER` aspect-ratio center-cropping.
     */
    fun mapPreviewCornersToBitmapQuad(
        previewCorners: Array<PointF>,
        previewWidth: Int,
        previewHeight: Int,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): DocumentQuad {
        if (previewCorners.size < 4 || previewWidth <= 0 || previewHeight <= 0 || bitmapWidth <= 0 || bitmapHeight <= 0) {
            return DocumentQuad.SAFE_DEFAULT_MARGIN
        }

        val scaleX = bitmapWidth.toFloat() / previewWidth.toFloat()
        val scaleY = bitmapHeight.toFloat() / previewHeight.toFloat()
        // Under FILL_CENTER, the sensor bitmap fills the larger scale on preview (i.e. smaller inverse ratio)
        val uniformScale = min(scaleX, scaleY)
        val renderedWInPreview = bitmapWidth.toFloat() / uniformScale
        val renderedHInPreview = bitmapHeight.toFloat() / uniformScale
        val offsetXInPreview = (previewWidth.toFloat() - renderedWInPreview) * 0.5f
        val offsetYInPreview = (previewHeight.toFloat() - renderedHInPreview) * 0.5f

        val mapped = previewCorners.take(4).map { pt ->
            val bitmapPxX = if (abs(scaleX - scaleY) < 0.015f) {
                pt.x * scaleX
            } else {
                (pt.x - offsetXInPreview) * uniformScale
            }
            val bitmapPxY = if (abs(scaleX - scaleY) < 0.015f) {
                pt.y * scaleY
            } else {
                (pt.y - offsetYInPreview) * uniformScale
            }
            PointNorm(
                x = (bitmapPxX / bitmapWidth.toFloat()).coerceIn(0.01f, 0.99f),
                y = (bitmapPxY / bitmapHeight.toFloat()).coerceIn(0.01f, 0.99f)
            )
        }
        return DocumentQuad(
            topLeft = mapped[0],
            topRight = mapped[1],
            bottomRight = mapped[2],
            bottomLeft = mapped[3]
        )
    }

    /**
     * Crops [source] from sensor dimensions to match the exact center-cropped aspect ratio shown
     * inside [PreviewView] (`FILL_CENTER` with dimensions `previewWidth x previewHeight`).
     * Eliminates zoom-out FOV mismatch ("يبعد الصورة") between live camera preview and `1. Crop`.
     */
    fun cropBitmapToPreviewViewport(
        source: Bitmap,
        previewWidth: Int,
        previewHeight: Int
    ): Bitmap {
        if (previewWidth <= 0 || previewHeight <= 0 || source.width <= 0 || source.height <= 0) {
            return source
        }
        val targetAspect = previewWidth.toFloat() / previewHeight.toFloat()
        val srcAspect = source.width.toFloat() / source.height.toFloat()
        if (abs(targetAspect - srcAspect) < 0.02f) {
            return source
        }

        val cropW: Int
        val cropH: Int
        val cropX: Int
        val cropY: Int
        if (srcAspect > targetAspect) {
            // Source is wider than preview -> crop horizontally centered
            cropH = source.height
            cropW = (source.height * targetAspect).roundToInt().coerceIn(32, source.width)
            cropX = ((source.width - cropW) * 0.5f).roundToInt().coerceIn(0, source.width - cropW)
            cropY = 0
        } else {
            // Source is taller than preview -> crop vertically centered
            cropW = source.width
            cropH = (source.width / targetAspect).roundToInt().coerceIn(32, source.height)
            cropX = 0
            cropY = ((source.height - cropH) * 0.5f).roundToInt().coerceIn(0, source.height - cropH)
        }
        return runCatching {
            Bitmap.createBitmap(source, cropX, cropY, cropW, cropH)
        }.getOrDefault(source)
    }

    /**
     * Returns true when [quad] is a real non-fallback convex document boundary.
     */
    fun isValidDocumentQuad(quad: DocumentQuad, isIdCardHint: Boolean = false): Boolean {
        if (quad == DocumentQuad.SAFE_DEFAULT_MARGIN || quad == DocumentQuad.FULL_FRAME) {
            return false
        }
        val pts = quad.asList()
        if (pts.any { it.x.isNaN() || it.y.isNaN() }) return false
        var area2 = 0f
        for (i in 0 until 4) {
            val a = pts[i]
            val b = pts[(i + 1) % 4]
            area2 += a.x * b.y - b.x * a.y
        }
        val area = abs(area2) * 0.5f
        val minArea = if (isIdCardHint) 0.020f else 0.075f
        if (area < minArea || area > 0.97f) return false
        val topW = hypot((quad.topRight.x - quad.topLeft.x).toDouble(), (quad.topRight.y - quad.topLeft.y).toDouble())
        val botW = hypot((quad.bottomRight.x - quad.bottomLeft.x).toDouble(), (quad.bottomRight.y - quad.bottomLeft.y).toDouble())
        val leftH = hypot((quad.bottomLeft.x - quad.topLeft.x).toDouble(), (quad.bottomLeft.y - quad.topLeft.y).toDouble())
        val rightH = hypot((quad.bottomRight.x - quad.topRight.x).toDouble(), (quad.bottomRight.y - quad.topRight.y).toDouble())
        val minW = if (isIdCardHint) 0.075 else 0.14
        val minH = if (isIdCardHint) 0.055 else 0.14
        return min(topW, botW) > minW && min(leftH, rightH) > minH
    }

    /**
     * Multiplies normalized [0..1] corner coordinates by the actual [Bitmap] pixel dimensions
     * (`bitmapWidth x bitmapHeight`) to yield exact pixel-space [PointF] coordinates `[TL, TR, BR, BL]`.
     */
    fun denormalizeQuadToBitmapPoints(
        quad: DocumentQuad,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): Array<PointF> {
        val bw = bitmapWidth.toFloat().coerceAtLeast(1f)
        val bh = bitmapHeight.toFloat().coerceAtLeast(1f)
        return arrayOf(
            PointF(quad.topLeft.x * bw, quad.topLeft.y * bh),
            PointF(quad.topRight.x * bw, quad.topRight.y * bh),
            PointF(quad.bottomRight.x * bw, quad.bottomRight.y * bh),
            PointF(quad.bottomLeft.x * bw, quad.bottomLeft.y * bh)
        )
    }

    /**
     * Maps normalized [0..1] corner coordinates from the fast 640x480 rotated ImageAnalysis stream
     * onto the full-resolution ImageCapture sensor bitmap dimensions, accounting for center-cropped FOV
     * aspect-ratio differences when sensor capture & analysis aspect ratios differ.
     */
    fun mapAnalyzerQuadToCaptureDimensions(
        quad: DocumentQuad,
        analyzerWidth: Int,
        analyzerHeight: Int,
        captureWidth: Int,
        captureHeight: Int
    ): DocumentQuad {
        if (analyzerWidth <= 0 || analyzerHeight <= 0 || captureWidth <= 0 || captureHeight <= 0) {
            return quad
        }
        val aLong = max(analyzerWidth, analyzerHeight).toFloat()
        val aShort = min(analyzerWidth, analyzerHeight).toFloat().coerceAtLeast(1f)
        val cLong = max(captureWidth, captureHeight).toFloat()
        val cShort = min(captureWidth, captureHeight).toFloat().coerceAtLeast(1f)

        val aRatio = aLong / aShort
        val cRatio = cLong / cShort
        if (abs(aRatio - cRatio) < 0.015f) {
            return quad
        }

        val fovScale = cRatio / aRatio
        val isCaptureLandscape = captureWidth >= captureHeight

        fun mapPoint(p: PointNorm): PointNorm {
            val mx: Float
            val my: Float
            if (isCaptureLandscape) {
                mx = p.x
                my = 0.5f + (p.y - 0.5f) * fovScale
            } else {
                mx = 0.5f + (p.x - 0.5f) * fovScale
                my = p.y
            }
            return PointNorm(
                x = mx.coerceIn(0.01f, 0.99f),
                y = my.coerceIn(0.01f, 0.99f)
            )
        }

        return DocumentQuad(
            topLeft = mapPoint(quad.topLeft),
            topRight = mapPoint(quad.topRight),
            bottomRight = mapPoint(quad.bottomRight),
            bottomLeft = mapPoint(quad.bottomLeft)
        )
    }

    /**
     * Perspective warp transformation using Matrix.setPolyToPoly (homography warp)
     * to flatten skewed pages into an upright rectangular document bitmap.
     */
    fun warpPerspective(source: Bitmap, quad: DocumentQuad, maxSidePx: Int = 2400): Bitmap {
        val w = source.width.toFloat()
        val h = source.height.toFloat()
        val tlX = quad.topLeft.x * w
        val tlY = quad.topLeft.y * h
        val trX = quad.topRight.x * w
        val trY = quad.topRight.y * h
        val brX = quad.bottomRight.x * w
        val brY = quad.bottomRight.y * h
        val blX = quad.bottomLeft.x * w
        val blY = quad.bottomLeft.y * h

        val topW = hypot((trX - tlX).toDouble(), (trY - tlY).toDouble())
        val botW = hypot((brX - blX).toDouble(), (brY - blY).toDouble())
        val leftH = hypot((blX - tlX).toDouble(), (blY - tlY).toDouble())
        val rightH = hypot((brX - trX).toDouble(), (brY - trY).toDouble())

        var dstW = max(topW, botW).roundToInt().coerceAtLeast(64)
        var dstH = max(leftH, rightH).roundToInt().coerceAtLeast(64)

        val initialLargest = max(dstW, dstH)
        if (initialLargest in 1 until 480) {
            val upScale = 480f / initialLargest.toFloat()
            dstW = (dstW * upScale).roundToInt().coerceAtLeast(64)
            dstH = (dstH * upScale).roundToInt().coerceAtLeast(64)
        }

        val largest = max(dstW, dstH)
        if (largest > maxSidePx) {
            val scale = maxSidePx.toFloat() / largest
            dstW = (dstW * scale).roundToInt().coerceAtLeast(64)
            dstH = (dstH * scale).roundToInt().coerceAtLeast(64)
        }

        val srcPoly = floatArrayOf(tlX, tlY, trX, trY, brX, brY, blX, blY)
        val dstPoly = floatArrayOf(0f, 0f, dstW.toFloat(), 0f, dstW.toFloat(), dstH.toFloat(), 0f, dstH.toFloat())

        val matrix = Matrix()
        val mapped = matrix.setPolyToPoly(srcPoly, 0, dstPoly, 0, 4)
        if (!mapped) {
            return Bitmap.createScaledBitmap(source, dstW, dstH, true)
        }

        val output = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (abs(degrees % 360f) < 0.1f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Estimates subtle document skew angle (-7°..+7°) from horizontal edge alignment for Auto-Deskew.
     */
    fun estimateAutoDeskewAngle(bitmap: Bitmap): Float {
        val sampleW = 160
        val sampleH = 220
        val small = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)
        val px = IntArray(sampleW * sampleH)
        small.getPixels(px, 0, sampleW, 0, 0, sampleW, sampleH)
        if (small !== bitmap) small.recycle()

        val luma = IntArray(sampleW * sampleH) { i ->
            val c = px[i]
            (Color.red(c) * 77 + Color.green(c) * 150 + Color.blue(c) * 29) shr 8
        }

        var bestAngle = 0f
        var bestVariance = -1.0
        for (step in -14..14) {
            val angle = step * 0.5f
            val tanA = sin(Math.toRadians(angle.toDouble())) / cos(Math.toRadians(angle.toDouble()))
            val rowSums = DoubleArray(sampleH)
            for (y in 8 until sampleH - 8) {
                var sumDark = 0.0
                for (x in 12 until sampleW - 12 step 2) {
                    val sy = (y + (x - sampleW / 2) * tanA).roundToInt()
                    if (sy in 0 until sampleH) {
                        val lum = luma[sy * sampleW + x]
                        if (lum < 145) sumDark += (145 - lum)
                    }
                }
                rowSums[y] = sumDark
            }
            val mean = rowSums.average()
            val variance = rowSums.sumOf { (it - mean) * (it - mean) }
            if (variance > bestVariance) {
                bestVariance = variance
                bestAngle = -angle
            }
        }
        return bestAngle.coerceIn(-7f, 7f)
    }

    /**
     * Applies full offline enhancement pipeline:
     * Filter preset + Shadows (-1..1) + Highlights (-1..1) + Saturation (0..2) +
     * Temperature (-1..1) + Brightness (-100..100) + Contrast (0.5..2.0) + Sharpness (0..1) + Rotation.
     */
    fun applyEnhancements(
        source: Bitmap,
        filter: ScannerFilterType,
        brightness: Float = 0f,
        contrast: Float = 1f,
        sharpness: Float = 0f,
        rotationDeg: Int = 0,
        enableSuperRes: Boolean = true,
        shadows: Float = 0f,
        highlights: Float = 0f,
        saturation: Float = 1f,
        temperature: Float = 0f
    ): Bitmap {
        val rotated = if (rotationDeg % 360 != 0) rotateBitmap(source, rotationDeg.toFloat()) else source

        val filtered = when (filter) {
            ScannerFilterType.ORIGINAL -> rotated.copy(Bitmap.Config.ARGB_8888, true)
            ScannerFilterType.GRAYSCALE -> applyGrayscaleFilter(rotated)
            ScannerFilterType.MAGIC_COLOR -> applyMagicColorFilter(rotated, enableSuperRes = enableSuperRes)
            ScannerFilterType.BW_DOCUMENT -> applyAdaptiveBwThreshold(rotated)
            ScannerFilterType.SHADOW_GLARE_REMOVAL -> applyShadowGlareRemoval(rotated)
            ScannerFilterType.SUPER_CLEAR -> applySuperClearFilter(rotated, enableSuperRes = enableSuperRes)
            ScannerFilterType.DEEP_INK -> applyDeepInkFilter(rotated)
            ScannerFilterType.CLEAN_PAPER -> applyCleanPaperFilter(rotated)
        }
        if (rotated !== source && !rotated.isRecycled) rotated.recycle()

        val toneRecovered = if (abs(shadows) > 0.01f || abs(highlights) > 0.01f) {
            applyShadowsHighlights(filtered, shadows, highlights).also {
                if (it !== filtered && !filtered.isRecycled) filtered.recycle()
            }
        } else {
            filtered
        }

        val hasColorMatrixAdj = abs(brightness) > 0.5f ||
            abs(contrast - 1f) > 0.02f ||
            abs(saturation - 1f) > 0.01f ||
            abs(temperature) > 0.01f

        val adjusted = if (hasColorMatrixAdj) {
            applyColorAndToneMatrix(
                source = toneRecovered,
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                temperature = temperature
            ).also {
                if (it !== toneRecovered && !toneRecovered.isRecycled) toneRecovered.recycle()
            }
        } else {
            toneRecovered
        }

        return if (sharpness > 0.05f) {
            applySharpen(adjusted, sharpness.coerceIn(0f, 1f)).also {
                if (it !== adjusted && !adjusted.isRecycled) adjusted.recycle()
            }
        } else {
            adjusted
        }
    }

    private fun applyShadowsHighlights(
        source: Bitmap,
        shadows: Float,
        highlights: Float
    ): Bitmap {
        val w = source.width
        val h = source.height
        val n = w * h
        val sVal = shadows.coerceIn(-1f, 1f)
        val hVal = highlights.coerceIn(-1f, 1f)

        // Precompute luminance gain & offset LUT for 0..255 so inner pixel loop has zero pow() calls
        val scaleLut = FloatArray(256)
        val zeroLift = if (sVal > 0f) (sVal * 42f).roundToInt() else 0
        for (l in 0..255) {
            val ln = l / 255f
            val wShadow = (1f - ln) * (1f - ln)
            val wHigh = ln * ln
            val shadowDelta = if (sVal >= 0f) sVal * wShadow * 96f else sVal * wShadow * 76f
            val highDelta = if (hVal >= 0f) hVal * wHigh * 84f else hVal * wHigh * 96f
            val targetL = (l + shadowDelta + highDelta).coerceIn(0f, 255f)
            scaleLut[l] = if (l == 0) 1f else targetL / l.toFloat()
        }

        val px = IntArray(n)
        source.getPixels(px, 0, w, 0, 0, w, h)
        for (i in 0 until n) {
            val c = px[i]
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val lum = ((r * 77 + g * 150 + b * 29) shr 8).coerceIn(0, 255)
            val nr: Int
            val ng: Int
            val nb: Int
            if (lum == 0 && zeroLift > 0) {
                nr = zeroLift
                ng = zeroLift
                nb = zeroLift
            } else {
                val scale = scaleLut[lum]
                nr = (r * scale).roundToInt().coerceIn(0, 255)
                ng = (g * scale).roundToInt().coerceIn(0, 255)
                nb = (b * scale).roundToInt().coerceIn(0, 255)
            }
            px[i] = (c and -0x1000000) or (nr shl 16) or (ng shl 8) or nb
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun applyColorAndToneMatrix(
        source: Bitmap,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        temperature: Float
    ): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val combined = ColorMatrix()

        val sat = saturation.coerceIn(0f, 2f)
        if (abs(sat - 1f) > 0.01f) {
            combined.postConcat(ColorMatrix().apply { setSaturation(sat) })
        }

        val temp = temperature.coerceIn(-1f, 1f)
        if (abs(temp) > 0.01f) {
            val rScale = (1f + temp * 0.22f).coerceIn(0.72f, 1.28f)
            val gScale = (1f + temp * 0.03f).coerceIn(0.94f, 1.06f)
            val bScale = (1f - temp * 0.24f).coerceIn(0.72f, 1.28f)
            val shift = temp * 10f
            combined.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        rScale, 0f, 0f, 0f, shift,
                        0f, gScale, 0f, 0f, 0f,
                        0f, 0f, bScale, 0f, -shift,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        if (abs(brightness) > 0.5f || abs(contrast - 1f) > 0.02f) {
            val translate = (-0.5f * contrast + 0.5f) * 255f + brightness
            combined.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(combined) }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }

    private fun applyGrayscaleFilter(source: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val satMatrix = ColorMatrix().apply { setSaturation(0f) }
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                1.22f, 0f, 0f, 0f, -18f,
                0f, 1.22f, 0f, 0f, -18f,
                0f, 0f, 1.22f, 0f, -18f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        satMatrix.postConcat(contrastMatrix)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(satMatrix) }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }

    /**
     * Automatic AI Post-Capture Document / ID Card Enhancement Pipeline (runs on Dispatchers.Default):
     *  1. Adaptive Super-Resolution (Lanczos4 upscaling to >= 2400px longest edge if smaller)
     *  2. Illumination Division Normalization & Shadow/Yellow-Tint Removal
     *     (Dedicated ID Card mode preserves natural skin tones, portraits, and holographic contrast)
     *  3. Multi-Scale Edge-Preserving Unsharp Masking (razor-sharp text/MRZ without background grain)
     */
    fun autoEnhanceCapturedDocument(
        warped: Bitmap,
        isIdCardMode: Boolean = false
    ): Bitmap {
        val superRes = upscaleLanczos4IfNeeded(
            source = warped,
            minLongestEdge = 2000,
            maxScale = 2.2f
        )
        val normalized = applyIlluminationDivisionNormalization(
            source = superRes,
            preserveSkinAndPortrait = isIdCardMode
        )
        if (superRes !== warped && !superRes.isRecycled) {
            superRes.recycle()
        }
        val sharpened = applyMultiScaleUnsharpMask(
            source = normalized,
            strength = if (isIdCardMode) 0.72f else 0.85f,
            noiseThreshold = if (isIdCardMode) 6.0f else 5.0f
        )
        if (sharpened !== normalized && !normalized.isRecycled) {
            normalized.recycle()
        }
        return sharpened
    }

    /**
     * Separable Lanczos4 (a = 4) adaptive super-resolution upscaler.
     * Enforces an upper pixel budget (~3.8 MP) to guarantee zero OOM crashes on mobile devices.
     */
    fun upscaleLanczos4IfNeeded(
        source: Bitmap,
        minLongestEdge: Int = 2000,
        maxScale: Float = 2.2f
    ): Bitmap {
        val srcW = source.width
        val srcH = source.height
        val longest = max(srcW, srcH)
        if (longest <= 0 || longest >= minLongestEdge || longest < 280) {
            return source
        }

        var desiredScale = (minLongestEdge.toFloat() / longest.toFloat()).coerceIn(1f, maxScale)
        if (desiredScale <= 1.03f) return source

        var dstW = (srcW * desiredScale).roundToInt().coerceAtLeast(srcW + 1)
        var dstH = (srcH * desiredScale).roundToInt().coerceAtLeast(srcH + 1)
        val maxTotalPixels = 3_800_000L
        val targetPixels = dstW.toLong() * dstH.toLong()
        if (targetPixels > maxTotalPixels) {
            val downRatio = kotlin.math.sqrt(maxTotalPixels.toDouble() / targetPixels.toDouble()).toFloat()
            desiredScale *= downRatio
            if (desiredScale <= 1.03f) return source
            dstW = (srcW * desiredScale).roundToInt().coerceAtLeast(srcW + 1)
            dstH = (srcH * desiredScale).roundToInt().coerceAtLeast(srcH + 1)
        }

        // Precompute 8-tap Lanczos4 weights for each destination column & row (no sin() in pixel loops)
        val a = 4
        val tapCount = a * 2 // 8 taps
        val xStart = IntArray(dstW)
        val xWeights = FloatArray(dstW * tapCount)
        val scaleX = srcW.toFloat() / dstW.toFloat()
        for (dx in 0 until dstW) {
            val center = (dx + 0.5f) * scaleX - 0.5f
            val base = floor(center).toInt() - a + 1
            xStart[dx] = base
            var wSum = 0f
            val rowOff = dx * tapCount
            for (k in 0 until tapCount) {
                val dist = center - (base + k)
                val w = lanczosKernel(dist, a)
                xWeights[rowOff + k] = w
                wSum += w
            }
            val inv = if (abs(wSum) > 1e-6f) 1f / wSum else 1f
            for (k in 0 until tapCount) xWeights[rowOff + k] *= inv
        }

        val yStart = IntArray(dstH)
        val yWeights = FloatArray(dstH * tapCount)
        val scaleY = srcH.toFloat() / dstH.toFloat()
        for (dy in 0 until dstH) {
            val center = (dy + 0.5f) * scaleY - 0.5f
            val base = floor(center).toInt() - a + 1
            yStart[dy] = base
            var wSum = 0f
            val rowOff = dy * tapCount
            for (k in 0 until tapCount) {
                val dist = center - (base + k)
                val w = lanczosKernel(dist, a)
                yWeights[rowOff + k] = w
                wSum += w
            }
            val inv = if (abs(wSum) > 1e-6f) 1f / wSum else 1f
            for (k in 0 until tapCount) yWeights[rowOff + k] *= inv
        }

        val srcPx = IntArray(srcW * srcH)
        source.getPixels(srcPx, 0, srcW, 0, 0, srcW, srcH)

        // Horizontal separable pass: srcW x srcH -> dstW x srcH (packed ARGB)
        val tempPx = IntArray(dstW * srcH)
        val wMinus1 = srcW - 1
        for (sy in 0 until srcH) {
            val srcRow = sy * srcW
            val tmpRow = sy * dstW
            for (dx in 0 until dstW) {
                val base = xStart[dx]
                val wOff = dx * tapCount
                var rAcc = 0f
                var gAcc = 0f
                var bAcc = 0f
                for (k in 0 until tapCount) {
                    val sx = (base + k).coerceIn(0, wMinus1)
                    val c = srcPx[srcRow + sx]
                    val w = xWeights[wOff + k]
                    rAcc += ((c ushr 16) and 0xFF) * w
                    gAcc += ((c ushr 8) and 0xFF) * w
                    bAcc += (c and 0xFF) * w
                }
                val r = rAcc.roundToInt().coerceIn(0, 255)
                val g = gAcc.roundToInt().coerceIn(0, 255)
                val b = bAcc.roundToInt().coerceIn(0, 255)
                tempPx[tmpRow + dx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        // Vertical separable pass: dstW x srcH -> dstW x dstH
        val dstPx = IntArray(dstW * dstH)
        val hMinus1 = srcH - 1
        for (dy in 0 until dstH) {
            val base = yStart[dy]
            val wOff = dy * tapCount
            val dstRow = dy * dstW
            for (dx in 0 until dstW) {
                var rAcc = 0f
                var gAcc = 0f
                var bAcc = 0f
                for (k in 0 until tapCount) {
                    val sy = (base + k).coerceIn(0, hMinus1)
                    val c = tempPx[sy * dstW + dx]
                    val w = yWeights[wOff + k]
                    rAcc += ((c ushr 16) and 0xFF) * w
                    gAcc += ((c ushr 8) and 0xFF) * w
                    bAcc += (c and 0xFF) * w
                }
                val r = rAcc.roundToInt().coerceIn(0, 255)
                val g = gAcc.roundToInt().coerceIn(0, 255)
                val b = bAcc.roundToInt().coerceIn(0, 255)
                dstPx[dstRow + dx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        return Bitmap.createBitmap(dstPx, dstW, dstH, Bitmap.Config.ARGB_8888)
    }

    private fun lanczosKernel(x: Float, a: Int): Float {
        val ax = abs(x)
        if (ax < 1e-5f) return 1f
        if (ax >= a) return 0f
        val piX = PI * x
        val piXOverA = piX / a
        return ((sin(piX) / piX) * (sin(piXOverA) / piXOverA)).toFloat()
    }

    /**
     * Illumination Normalization & Shadow Removal via Division Normalization:
     * Divides pixel luminance by a heavily blurred local background surface estimate to erase
     * uneven lighting, shadows, and warm yellow paper tint.
     * When [preserveSkinAndPortrait] is true (ID Card Mode), blends gently to preserve natural
     * skin tones and portrait/hologram contrast.
     */
    fun applyIlluminationDivisionNormalization(
        source: Bitmap,
        preserveSkinAndPortrait: Boolean = false
    ): Bitmap {
        val w = source.width
        val h = source.height
        val bgW = (w / 14).coerceIn(18, 96)
        val bgH = (h / 14).coerceIn(18, 96)
        val tiny = Bitmap.createScaledBitmap(source, bgW, bgH, true)
        val blurredBg = boxBlurBitmap(tiny, radius = 4)
        val fullBg = Bitmap.createScaledBitmap(blurredBg, w, h, true)
        tiny.recycle()
        blurredBg.recycle()

        val n = w * h
        val srcPx = IntArray(n)
        val bgPx = IntArray(n)
        source.getPixels(srcPx, 0, w, 0, 0, w, h)
        fullBg.getPixels(bgPx, 0, w, 0, 0, w, h)
        fullBg.recycle()

        if (preserveSkinAndPortrait) {
            // Dedicated ID Card illumination leveling: preserves natural skin tones & holographic contrast
            for (i in 0 until n) {
                val s = srcPx[i]
                val b = bgPx[i]
                val sr = (s ushr 16) and 0xFF
                val sg = (s ushr 8) and 0xFF
                val sb = s and 0xFF

                val bgLum = max(38, (((b ushr 16) and 0xFF) * 77 + ((b ushr 8) and 0xFF) * 150 + (b and 0xFF) * 29) shr 8)
                val targetGain = (224f / bgLum.toFloat()).coerceIn(0.88f, 1.85f)
                val gain = 1f + (targetGain - 1f) * 0.45f

                val nr = (sr * gain).roundToInt().coerceIn(0, 255)
                val ng = (sg * gain).roundToInt().coerceIn(0, 255)
                val nb = (sb * gain).roundToInt().coerceIn(0, 255)
                srcPx[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        } else {
            // Studio-lit Document normalization: luminance + white-balance division erases shadows & yellow cast
            for (i in 0 until n) {
                val s = srcPx[i]
                val b = bgPx[i]
                val sr = (s ushr 16) and 0xFF
                val sg = (s ushr 8) and 0xFF
                val sb = s and 0xFF

                val br = max(36, (b ushr 16) and 0xFF)
                val bg = max(36, (b ushr 8) and 0xFF)
                val bb = max(36, b and 0xFF)

                val sLum = max(1, (sr * 77 + sg * 150 + sb * 29) shr 8)
                val bgLum = max(36, (br * 77 + bg * 150 + bb * 29) shr 8)

                // Luminance division gain + per-channel tint neutralization
                val lumScale = (246f / bgLum.toFloat()).coerceIn(0.85f, 3.2f)
                val cr = (sr * 0.65f * lumScale) + (sr * 0.35f * (246f / br.toFloat()))
                val cg = (sg * 0.65f * lumScale) + (sg * 0.35f * (246f / bg.toFloat()))
                val cb = (sb * 0.65f * lumScale) + (sb * 0.35f * (246f / bb.toFloat()))

                var nr = cr.toInt().coerceIn(0, 255)
                var ng = cg.toInt().coerceIn(0, 255)
                var nb = cb.toInt().coerceIn(0, 255)

                // Clean studio-white paper ceiling & crisp dark ink anchoring
                val outLum = (nr * 77 + ng * 150 + nb * 29) shr 8
                if (outLum > 218) {
                    val push = ((outLum - 218) / 37f).coerceIn(0f, 1f) * 0.65f
                    nr = (nr + (255 - nr) * push).toInt().coerceIn(0, 255)
                    ng = (ng + (255 - ng) * push).toInt().coerceIn(0, 255)
                    nb = (nb + (255 - nb) * push).toInt().coerceIn(0, 255)
                } else if (outLum < 150 && sLum < 165) {
                    nr = (nr * 0.88f).toInt().coerceIn(0, 255)
                    ng = (ng * 0.88f).toInt().coerceIn(0, 255)
                    nb = (nb * 0.88f).toInt().coerceIn(0, 255)
                }
                srcPx[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }
        return Bitmap.createBitmap(srcPx, w, h, Bitmap.Config.ARGB_8888)
    }

    /**
     * Multi-Scale Edge-Preserving Unsharp Masking:
     * Combines fine (3x3) and medium (5x5) luminance detail scales gated by a noise floor so
     * fine printed text & MRZ micro-characters become razor crisp without amplifying paper grain.
     */
    fun applyMultiScaleUnsharpMask(
        source: Bitmap,
        strength: Float = 0.85f,
        noiseThreshold: Float = 5.5f
    ): Bitmap {
        val w = source.width
        val h = source.height
        val n = w * h
        if (w < 8 || h < 8) return source.copy(Bitmap.Config.ARGB_8888, true)

        val srcPx = IntArray(n)
        source.getPixels(srcPx, 0, w, 0, 0, w, h)

        val luma = IntArray(n)
        for (i in 0 until n) {
            val c = srcPx[i]
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            luma[i] = (r * 77 + g * 150 + b * 29) shr 8
        }

        // Fine scale: separable 3x3 Gaussian [1, 2, 1] / 4
        val temp = IntArray(n)
        val fineBlur = IntArray(n)
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val xm = max(0, x - 1)
                val xp = min(w - 1, x + 1)
                temp[row + x] = (luma[row + xm] + 2 * luma[row + x] + luma[row + xp]) shr 2
            }
        }
        for (y in 0 until h) {
            val ym = max(0, y - 1) * w
            val y0 = y * w
            val yp = min(h - 1, y + 1) * w
            for (x in 0 until w) {
                fineBlur[y0 + x] = (temp[ym + x] + 2 * temp[y0 + x] + temp[yp + x]) shr 2
            }
        }

        // Medium scale: separable 5x5 Gaussian [1, 4, 6, 4, 1] / 16
        val medBlur = IntArray(n)
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val xm2 = max(0, x - 2)
                val xm1 = max(0, x - 1)
                val xp1 = min(w - 1, x + 1)
                val xp2 = min(w - 1, x + 2)
                temp[row + x] = (luma[row + xm2] + 4 * luma[row + xm1] + 6 * luma[row + x] + 4 * luma[row + xp1] + luma[row + xp2]) shr 4
            }
        }
        for (y in 0 until h) {
            val ym2 = max(0, y - 2) * w
            val ym1 = max(0, y - 1) * w
            val y0 = y * w
            val yp1 = min(h - 1, y + 1) * w
            val yp2 = min(h - 1, y + 2) * w
            for (x in 0 until w) {
                medBlur[y0 + x] = (temp[ym2 + x] + 4 * temp[ym1 + x] + 6 * temp[y0 + x] + 4 * temp[yp1 + x] + temp[yp2 + x]) shr 4
            }
        }

        for (i in 0 until n) {
            val lum = luma[i]
            val detailFine = (lum - fineBlur[i]).toFloat()
            val detailMed = (lum - medBlur[i]).toFloat()
            val combinedDetail = 0.62f * detailFine + 0.38f * detailMed
            val absD = abs(combinedDetail)
            val delta = if (absD > noiseThreshold) {
                val excess = absD - noiseThreshold
                val signed = if (combinedDetail >= 0f) excess else -excess
                (signed * strength).coerceIn(-68f, 68f).roundToInt()
            } else {
                0
            }

            if (delta != 0) {
                val c = srcPx[i]
                val r = (((c ushr 16) and 0xFF) + delta).coerceIn(0, 255)
                val g = (((c ushr 8) and 0xFF) + delta).coerceIn(0, 255)
                val b = ((c and 0xFF) + delta).coerceIn(0, 255)
                srcPx[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return Bitmap.createBitmap(srcPx, w, h, Bitmap.Config.ARGB_8888)
    }

    /**
     * Magic Color: local illumination background division removes shadows and yellow cast
     * while enriching ink clarity and crispness.
     */
    private fun applyMagicColorFilter(source: Bitmap, enableSuperRes: Boolean = true): Bitmap {
        val superRes = if (enableSuperRes) {
            upscaleLanczos4IfNeeded(source, minLongestEdge = 2000, maxScale = 2.0f)
        } else {
            source
        }
        val leveled = applyIlluminationDivisionNormalization(superRes, preserveSkinAndPortrait = false)
        if (superRes !== source && !superRes.isRecycled) superRes.recycle()
        return leveled
    }

    /**
     * Adaptive Bradley-Roth Integral Image Thresholding for crisp B&W Documents.
     */
    private fun applyAdaptiveBwThreshold(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val px = IntArray(w * h)
        source.getPixels(px, 0, w, 0, 0, w, h)

        val luma = IntArray(w * h)
        val integral = LongArray(w * h)
        for (y in 0 until h) {
            var rowSum = 0L
            for (x in 0 until w) {
                val idx = y * w + x
                val c = px[idx]
                val lum = (Color.red(c) * 77 + Color.green(c) * 150 + Color.blue(c) * 29) shr 8
                luma[idx] = lum
                rowSum += lum
                integral[idx] = rowSum + if (y > 0) integral[(y - 1) * w + x] else 0L
            }
        }

        val s2 = (min(w, h) / 16).coerceIn(8, 38)
        val tFactor = 0.86f // Pixels < 86% of local mean become ink black

        for (y in 0 until h) {
            val y1 = max(0, y - s2)
            val y2 = min(h - 1, y + s2)
            for (x in 0 until w) {
                val x1 = max(0, x - s2)
                val x2 = min(w - 1, x + s2)
                val count = (x2 - x1 + 1) * (y2 - y1 + 1)
                var sum = integral[y2 * w + x2]
                if (y1 > 0) sum -= integral[(y1 - 1) * w + x2]
                if (x1 > 0) sum -= integral[y2 * w + (x1 - 1)]
                if (y1 > 0 && x1 > 0) sum += integral[(y1 - 1) * w + (x1 - 1)]

                val idx = y * w + x
                val isInk = (luma[idx] * count) < (sum * tFactor)
                px[idx] = if (isInk) Color.rgb(18, 22, 28) else Color.WHITE
            }
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    /**
     * Pre-OCR Arabic & Bilingual Binarization Filter:
     * Upscales smaller cards/documents via Lanczos4 so Arabic diacritics & dots separate cleanly,
     * levels uneven shadows with division normalization, and applies adaptive Bradley-Roth
     * thresholding for ultra-crisp ink-on-white input to Tesseract4Android (`ara.traineddata`).
     */
    fun prepareBitmapForArabicOcr(source: Bitmap): Bitmap {
        val upscaled = upscaleLanczos4IfNeeded(
            source = source,
            minLongestEdge = 2000,
            maxScale = 2.0f
        )
        val leveled = applyIlluminationDivisionNormalization(
            source = upscaled,
            preserveSkinAndPortrait = false
        )
        if (upscaled !== source && !upscaled.isRecycled) {
            upscaled.recycle()
        }
        val binarized = applyAdaptiveBwThreshold(leveled)
        if (leveled !== binarized && !leveled.isRecycled) {
            leveled.recycle()
        }
        return binarized
    }

    /**
     * Shadow Removal: multi-scale illumination surface normalization to erase hand/phone
     * shadows and soften specular glare hotspots while preserving paper brightness and ink clarity.
     */
    private fun applyShadowGlareRemoval(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val bgW = (w / 10).coerceIn(18, 112)
        val bgH = (h / 10).coerceIn(18, 112)
        val tiny = Bitmap.createScaledBitmap(source, bgW, bgH, true)
        val smooth = boxBlurBitmap(tiny, radius = 5)
        val fullBg = Bitmap.createScaledBitmap(smooth, w, h, true)
        tiny.recycle()
        smooth.recycle()

        val n = w * h
        val srcPx = IntArray(n)
        val bgPx = IntArray(n)
        source.getPixels(srcPx, 0, w, 0, 0, w, h)
        fullBg.getPixels(bgPx, 0, w, 0, 0, w, h)
        fullBg.recycle()

        for (i in 0 until n) {
            val s = srcPx[i]
            val b = bgPx[i]
            val sr = (s ushr 16) and 0xFF
            val sg = (s ushr 8) and 0xFF
            val sb = s and 0xFF

            val br = max(34, (b ushr 16) and 0xFF)
            val bg = max(34, (b ushr 8) and 0xFF)
            val bb = max(34, b and 0xFF)
            val bgLuma = max(34, (br * 77 + bg * 150 + bb * 29) shr 8)

            val lumRatio = (240f / bgLuma).coerceIn(0.86f, 2.85f)
            val r = (sr * (0.72f * lumRatio + 0.28f * (240f / br).coerceIn(0.86f, 2.85f))).roundToInt().coerceIn(0, 255)
            val g = (sg * (0.72f * lumRatio + 0.28f * (240f / bg).coerceIn(0.86f, 2.85f))).roundToInt().coerceIn(0, 255)
            val bl = (sb * (0.72f * lumRatio + 0.28f * (240f / bb).coerceIn(0.86f, 2.85f))).roundToInt().coerceIn(0, 255)
            srcPx[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
        }
        return Bitmap.createBitmap(srcPx, w, h, Bitmap.Config.ARGB_8888)
    }

    /**
     * Super Clear / Text Boost: background leveling + crisp text contrast curve +
     * high-definition multi-scale edge sharpening for razor-sharp printed documents.
     */
    private fun applySuperClearFilter(source: Bitmap, enableSuperRes: Boolean = true): Bitmap {
        val superRes = if (enableSuperRes) {
            upscaleLanczos4IfNeeded(source, minLongestEdge = 2000, maxScale = 2.0f)
        } else {
            source
        }
        val leveled = applyIlluminationDivisionNormalization(superRes, preserveSkinAndPortrait = false)
        if (superRes !== source && !superRes.isRecycled) superRes.recycle()

        val w = leveled.width
        val h = leveled.height
        val n = w * h
        val px = IntArray(n)
        leveled.getPixels(px, 0, w, 0, 0, w, h)
        if (leveled !== source && !leveled.isRecycled) leveled.recycle()

        for (i in 0 until n) {
            val c = px[i]
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val lum = (r * 77 + g * 150 + b * 29) shr 8
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            if (lum > 214 && (maxC - minC) < 36) {
                px[i] = Color.WHITE
            } else if (lum < 155) {
                val factor = 0.78f
                val nr = (r * factor).roundToInt().coerceIn(0, 255)
                val ng = (g * factor).roundToInt().coerceIn(0, 255)
                val nb = (b * factor).roundToInt().coerceIn(0, 255)
                px[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }
        val boosted = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
        val sharpened = applyMultiScaleUnsharpMask(boosted, strength = 1.25f, noiseThreshold = 4.2f)
        if (sharpened !== boosted && !boosted.isRecycled) boosted.recycle()
        return sharpened
    }

    /**
     * Deep Ink: darkens faded print, pencil marks, and light ballpoint handwriting
     * while preserving clean paper brightness and original ink hues.
     */
    private fun applyDeepInkFilter(source: Bitmap): Bitmap {
        val leveled = applyIlluminationDivisionNormalization(source, preserveSkinAndPortrait = false)
        val w = leveled.width
        val h = leveled.height
        val n = w * h
        val px = IntArray(n)
        leveled.getPixels(px, 0, w, 0, 0, w, h)
        if (leveled !== source && !leveled.isRecycled) leveled.recycle()

        // Precompute ink-deepening luminance LUT (darkens faded print/handwriting strongly, preserves clean paper)
        val inkGainLut = FloatArray(256)
        for (l in 0..255) {
            inkGainLut[l] = when {
                l == 0 -> 1f
                l >= 224 -> 1f
                else -> {
                    val norm = l / 224f
                    val target = 224f * norm.pow(2.15f)
                    (target / l.toFloat()).coerceIn(0.18f, 1f)
                }
            }
        }

        for (i in 0 until n) {
            val c = px[i]
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val lum = ((r * 77 + g * 150 + b * 29) shr 8).coerceIn(0, 255)
            val gain = inkGainLut[lum]
            if (gain < 0.999f) {
                val nr = (r * gain).roundToInt().coerceIn(0, 255)
                val ng = (g * gain).roundToInt().coerceIn(0, 255)
                val nb = (b * gain).roundToInt().coerceIn(0, 255)
                px[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }
        val darkened = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
        val crisp = applyMultiScaleUnsharpMask(darkened, strength = 0.68f, noiseThreshold = 5.0f)
        if (crisp !== darkened && !darkened.isRecycled) darkened.recycle()
        return crisp
    }

    /**
     * Clean Paper: removes yellow/aged tint and textured paper grain, whitening the page
     * surface while preserving colored stamps, signatures, and ink legibility.
     */
    private fun applyCleanPaperFilter(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val bgW = (w / 12).coerceIn(18, 96)
        val bgH = (h / 12).coerceIn(18, 96)
        val tiny = Bitmap.createScaledBitmap(source, bgW, bgH, true)
        val smooth = boxBlurBitmap(tiny, radius = 4)
        val fullBg = Bitmap.createScaledBitmap(smooth, w, h, true)
        tiny.recycle()
        smooth.recycle()

        val n = w * h
        val srcPx = IntArray(n)
        val bgPx = IntArray(n)
        source.getPixels(srcPx, 0, w, 0, 0, w, h)
        fullBg.getPixels(bgPx, 0, w, 0, 0, w, h)
        fullBg.recycle()

        for (i in 0 until n) {
            val s = srcPx[i]
            val b = bgPx[i]
            val sr = (s ushr 16) and 0xFF
            val sg = (s ushr 8) and 0xFF
            val sb = s and 0xFF

            val br = max(36, (b ushr 16) and 0xFF)
            val bg = max(36, (b ushr 8) and 0xFF)
            val bb = max(36, b and 0xFF)

            var nr = (sr * (252f / br).coerceIn(0.85f, 3.2f)).roundToInt().coerceIn(0, 255)
            var ng = (sg * (252f / bg).coerceIn(0.85f, 3.2f)).roundToInt().coerceIn(0, 255)
            var nb = (sb * (252f / bb).coerceIn(0.85f, 3.2f)).roundToInt().coerceIn(0, 255)

            val lum = (nr * 77 + ng * 150 + nb * 29) shr 8
            val chroma = max(nr, max(ng, nb)) - min(nr, min(ng, nb))
            if (lum > 192 && chroma < 42) {
                val whiten = ((lum - 192) / 38f).coerceIn(0f, 1f)
                nr = (nr + (255 - nr) * whiten).roundToInt().coerceIn(0, 255)
                ng = (ng + (255 - ng) * whiten).roundToInt().coerceIn(0, 255)
                nb = (nb + (255 - nb) * whiten).roundToInt().coerceIn(0, 255)
            }
            srcPx[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
        }
        return Bitmap.createBitmap(srcPx, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun applySharpen(source: Bitmap, amount: Float): Bitmap {
        return applyMultiScaleUnsharpMask(
            source = source,
            strength = (amount * 1.15f).coerceIn(0.15f, 1.35f),
            noiseThreshold = 5.0f
        )
    }

    private fun boxBlurBitmap(src: Bitmap, radius: Int): Bitmap {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var cnt = 0
                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, h - 1)
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, w - 1)
                        val c = px[ny * w + nx]
                        rSum += Color.red(c)
                        gSum += Color.green(c)
                        bSum += Color.blue(c)
                        cnt++
                    }
                }
                out[y * w + x] = Color.rgb(rSum / cnt, gSum / cnt, bSum / cnt)
            }
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    /**
     * Recto mode: Merges Front & Back of an ID card onto a single 300-DPI A4 page (21.0 cm x 29.7 cm -> 2480 x 3508 px)
     * with exact physical dimensions of 8.5 cm width x 5.0 cm height for both faces.
     */
    fun mergeIdCardFrontAndBack(front: Bitmap, back: Bitmap): Bitmap {
        val a4Width = 2480
        val a4Height = 3508
        val page = Bitmap.createBitmap(a4Width, a4Height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(page)
        canvas.drawColor(Color.WHITE)

        // Exact physical size on A4 (21.0 cm x 29.7 cm): 8.5 cm width x 5.0 cm height
        val cardW = a4Width * (8.5f / 21.0f)
        val cardH = a4Height * (5.0f / 29.7f)
        val left = (a4Width - cardW) / 2f
        val topY1 = a4Height * (5.5f / 29.7f)
        val topY2 = a4Height * (13.5f / 29.7f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.rgb(203, 213, 225)
        }

        val rect1 = RectF(left, topY1, left + cardW, topY1 + cardH)
        val rect2 = RectF(left, topY2, left + cardW, topY2 + cardH)
        canvas.drawBitmap(front, null, rect1, paint)
        canvas.drawRoundRect(rect1, 24f, 24f, borderPaint)

        canvas.drawBitmap(back, null, rect2, paint)
        canvas.drawRoundRect(rect2, 24f, 24f, borderPaint)
        return page
    }

    /**
     * Recto Verso mode: Places a single face (Front on page 1, Back on page 2) at exact
     * 8.5 cm x 5.0 cm centered horizontally (`left == right margin == 6.25 cm`) and at
     * identical vertical distance (`6.0 cm` from top) so duplex printing aligns Front & Back
     * millimeter-for-millimeter behind each other.
     */
    fun createIdCardSingleFaceSheet(card: Bitmap): Bitmap {
        val a4Width = 2480
        val a4Height = 3508
        val page = Bitmap.createBitmap(a4Width, a4Height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(page)
        canvas.drawColor(Color.WHITE)

        val cardW = a4Width * (8.5f / 21.0f)
        val cardH = a4Height * (5.0f / 29.7f)
        // Symmetrical horizontal centering guarantees exact alignment when flipped along the long A4 edge
        val left = (a4Width - cardW) / 2f
        val topY = a4Height * (6.0f / 29.7f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.rgb(203, 213, 225)
        }

        val rect = RectF(left, topY, left + cardW, topY + cardH)
        canvas.drawBitmap(card, null, rect, paint)
        canvas.drawRoundRect(rect, 24f, 24f, borderPaint)
        return page
    }

    /**
     * Book Mode: Splits a two-page spread cleanly down the center spine into Left & Right pages.
     */
    fun splitBookSpread(spread: Bitmap): Pair<Bitmap, Bitmap> {
        val midX = (spread.width / 2).coerceAtLeast(10)
        val leftPage = Bitmap.createBitmap(spread, 0, 0, midX, spread.height)
        val rightPage = Bitmap.createBitmap(spread, midX, 0, spread.width - midX, spread.height)
        return leftPage to rightPage
    }

    /**
     * Composites a page bitmap with its ink annotations, redactions, placed signatures, and watermark
     * for final viewing, PDF export, or image sharing.
     */
    fun compositePageWithMarkups(
        baseBitmap: Bitmap,
        annotations: PageAnnotations,
        signatures: List<Pair<SignaturePlacement, Bitmap>>,
        watermark: WatermarkConfig?
    ): Bitmap {
        val out = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val w = out.width.toFloat()
        val h = out.height.toFloat()

        // 1. Freehand strokes (Pen & Highlighter)
        annotations.strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            val path = Path()
            stroke.points.forEachIndexed { idx, pt ->
                val px = pt.x * w
                val py = pt.y * h
                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = (stroke.strokeWidthNorm * min(w, h)).coerceAtLeast(3f)
                color = stroke.colorArgb.toInt()
                if (stroke.isHighlighter) alpha = 95
            }
            canvas.drawPath(path, paint)
        }

        // 2. Redaction opaque boxes
        annotations.redactions.forEach { box ->
            val paint = Paint().apply {
                style = Paint.Style.FILL
                color = box.colorArgb.toInt()
            }
            canvas.drawRect(box.left * w, box.top * h, box.right * w, box.bottom * h, paint)
        }

        // 3. Placed E-Signatures
        signatures.forEach { (placement, sigBmp) ->
            val targetW = w * 0.28f * placement.scale
            val aspect = sigBmp.height.toFloat() / sigBmp.width.coerceAtLeast(1).toFloat()
            val targetH = targetW * aspect
            val cx = placement.centerXNorm * w
            val cy = placement.centerYNorm * h
            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(placement.rotationDeg)
            val dst = RectF(-targetW / 2f, -targetH / 2f, targetW / 2f, targetH / 2f)
            val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(sigBmp, null, dst, p)
            canvas.restore()
        }

        // 4. Custom Watermark overlay
        if (watermark != null && watermark.text.isNotBlank()) {
            val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = watermark.colorArgb.toInt()
                alpha = (watermark.opacity.coerceIn(0.05f, 0.9f) * 255).toInt()
                textSize = (min(w, h) * 0.085f).coerceAtLeast(28f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.save()
            canvas.translate(w / 2f, h / 2f)
            canvas.rotate(watermark.angleDeg)
            canvas.drawText(watermark.text, 0f, 0f, wmPaint)
            canvas.restore()
        }

        return out
    }

    fun saveBitmapAsJpeg(bitmap: Bitmap, destFile: File, quality: Int = 98): File {
        destFile.parentFile?.mkdirs()
        FileOutputStream(destFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(25, 100), fos)
        }
        return destFile
    }

    /**
     * Decodes an image from a Gallery/PhotoPicker [Uri], reads its [ExifInterface] orientation,
     * and rotates/flips the [Bitmap] upright BEFORE any edge detection runs.
     */
    fun decodeOrientedBitmapFromUri(
        contentResolver: ContentResolver,
        uri: Uri,
        maxSide: Int = 4096
    ): Bitmap? {
        val orientation = runCatching {
            contentResolver.openInputStream(uri)?.use { exifStream ->
                ExifInterface(exifStream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
        }

        var inSampleSize = 1
        if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
            while ((boundsOpts.outWidth / inSampleSize) > maxSide ||
                (boundsOpts.outHeight / inSampleSize) > maxSide
            ) {
                inSampleSize *= 2
            }
        }

        val decodeOpts = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            this.inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        }.getOrNull() ?: return null

        return applyExifOrientation(decoded, orientation)
    }

    fun decodeSampledBitmap(file: File, maxSide: Int = 4096): Bitmap? {
        if (!file.exists()) return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        var inSampleSize = 1
        while ((opts.outWidth / inSampleSize) > maxSide || (opts.outHeight / inSampleSize) > maxSide) {
            inSampleSize *= 2
        }
        val decoded = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                this.inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        ) ?: return null

        val orientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        return applyExifOrientation(decoded, orientation)
    }

    fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            else -> return bitmap
        }

        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (oriented !== bitmap) bitmap.recycle()
        return oriented
    }
}
