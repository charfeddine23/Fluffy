package com.charfeddine.fluffy.scanner.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.media.ExifInterface
import androidx.camera.core.ImageProxy
import com.charfeddine.fluffy.scanner.data.model.DocumentQuad
import com.charfeddine.fluffy.scanner.data.model.PageAnnotations
import com.charfeddine.fluffy.scanner.data.model.PointNorm
import com.charfeddine.fluffy.scanner.data.model.ScannerFilterType
import com.charfeddine.fluffy.scanner.data.model.SignaturePlacement
import com.charfeddine.fluffy.scanner.data.model.WatermarkConfig
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.cos

object DocumentImageProcessor {

    /**
     * Delegates real-time 100% offline 4-corner detection to [DocumentEdgeDetector].
     */
    fun detectDocumentQuadFromImageProxy(imageProxy: ImageProxy): EdgeDetectionFrameResult =
        DocumentEdgeDetector.detectFromImageProxy(imageProxy)

    fun detectQuadFromBitmap(bitmap: Bitmap): DocumentQuad =
        DocumentEdgeDetector.detectFromBitmap(bitmap)

    /**
     * Perspective warp transformation using Matrix.setPolyToPoly (homography warp)
     * to flatten skewed pages into an upright rectangular document bitmap.
     */
    fun warpPerspective(source: Bitmap, quad: DocumentQuad, maxSidePx: Int = 2200): Bitmap {
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

        var dstW = max(topW, botW).roundToInt().coerceAtLeast(320)
        var dstH = max(leftH, rightH).roundToInt().coerceAtLeast(320)

        val largest = max(dstW, dstH)
        if (largest > maxSidePx) {
            val scale = maxSidePx.toFloat() / largest
            dstW = (dstW * scale).roundToInt().coerceAtLeast(240)
            dstH = (dstH * scale).roundToInt().coerceAtLeast(240)
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
     * Filter preset + Brightness (-100..100) + Contrast (0.5..2.0) + Sharpness (0..1) + Rotation.
     */
    fun applyEnhancements(
        source: Bitmap,
        filter: ScannerFilterType,
        brightness: Float = 0f,
        contrast: Float = 1f,
        sharpness: Float = 0f,
        rotationDeg: Int = 0
    ): Bitmap {
        val rotated = if (rotationDeg % 360 != 0) rotateBitmap(source, rotationDeg.toFloat()) else source

        val filtered = when (filter) {
            ScannerFilterType.ORIGINAL -> rotated.copy(Bitmap.Config.ARGB_8888, true)
            ScannerFilterType.GRAYSCALE -> applyGrayscaleFilter(rotated)
            ScannerFilterType.MAGIC_COLOR -> applyMagicColorFilter(rotated)
            ScannerFilterType.BW_DOCUMENT -> applyAdaptiveBwThreshold(rotated)
            ScannerFilterType.SHADOW_GLARE_REMOVAL -> applyShadowGlareRemoval(rotated)
        }

        val adjusted = if (abs(brightness) > 0.5f || abs(contrast - 1f) > 0.02f) {
            applyBrightnessContrast(filtered, brightness, contrast)
        } else {
            filtered
        }

        return if (sharpness > 0.05f) {
            applySharpen(adjusted, sharpness.coerceIn(0f, 1f))
        } else {
            adjusted
        }
    }

    private fun applyBrightnessContrast(source: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val translate = (-0.5f * contrast + 0.5f) * 255f + brightness
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
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
     * Magic Color: local illumination background division removes shadows and brightens paper
     * while boosting ink color richness and crispness.
     */
    private fun applyMagicColorFilter(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val bgScale = 10
        val bgW = (w / bgScale).coerceAtLeast(16)
        val bgH = (h / bgScale).coerceAtLeast(16)
        val tiny = Bitmap.createScaledBitmap(source, bgW, bgH, true)
        val blurredBg = boxBlurBitmap(tiny, radius = 3)
        val fullBg = Bitmap.createScaledBitmap(blurredBg, w, h, true)
        tiny.recycle()
        blurredBg.recycle()

        val srcPx = IntArray(w * h)
        val bgPx = IntArray(w * h)
        source.getPixels(srcPx, 0, w, 0, 0, w, h)
        fullBg.getPixels(bgPx, 0, w, 0, 0, w, h)
        fullBg.recycle()

        val outPx = IntArray(w * h)
        for (i in srcPx.indices) {
            val s = srcPx[i]
            val b = bgPx[i]
            val sr = Color.red(s)
            val sg = Color.green(s)
            val sb = Color.blue(s)

            val br = max(36, Color.red(b))
            val bg = max(36, Color.green(b))
            val bb = max(36, Color.blue(b))

            // Illumination division + white paper stretch
            var nr = ((sr * 246f) / br).toInt().coerceIn(0, 255)
            var ng = ((sg * 246f) / bg).toInt().coerceIn(0, 255)
            var nb = ((sb * 246f) / bb).toInt().coerceIn(0, 255)

            // Subtle S-curve ink enrichment
            val luma = (nr * 77 + ng * 150 + nb * 29) shr 8
            if (luma > 218) {
                nr = min(255, nr + ((255 - nr) * 3) / 5)
                ng = min(255, ng + ((255 - ng) * 3) / 5)
                nb = min(255, nb + ((255 - nb) * 3) / 5)
            } else if (luma < 155) {
                nr = (nr * 0.86f).toInt().coerceIn(0, 255)
                ng = (ng * 0.86f).toInt().coerceIn(0, 255)
                nb = (nb * 0.86f).toInt().coerceIn(0, 255)
            }
            outPx[i] = Color.rgb(nr, ng, nb)
        }
        return Bitmap.createBitmap(outPx, w, h, Bitmap.Config.ARGB_8888)
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

        val outPx = IntArray(w * h)
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
                outPx[idx] = if (isInk) Color.rgb(18, 22, 28) else Color.WHITE
            }
        }
        return Bitmap.createBitmap(outPx, w, h, Bitmap.Config.ARGB_8888)
    }

    /**
     * Shadow & Glare Removal: smooths uneven room shadows and softens specular glare hotspots.
     */
    private fun applyShadowGlareRemoval(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val bgW = (w / 12).coerceAtLeast(16)
        val bgH = (h / 12).coerceAtLeast(16)
        val tiny = Bitmap.createScaledBitmap(source, bgW, bgH, true)
        val smooth = boxBlurBitmap(tiny, radius = 4)
        val fullBg = Bitmap.createScaledBitmap(smooth, w, h, true)
        tiny.recycle()
        smooth.recycle()

        val srcPx = IntArray(w * h)
        val bgPx = IntArray(w * h)
        source.getPixels(srcPx, 0, w, 0, 0, w, h)
        fullBg.getPixels(bgPx, 0, w, 0, 0, w, h)
        fullBg.recycle()

        val outPx = IntArray(w * h)
        for (i in srcPx.indices) {
            val s = srcPx[i]
            val b = bgPx[i]
            val bgLuma = max(40, (Color.red(b) * 77 + Color.green(b) * 150 + Color.blue(b) * 29) shr 8)
            val ratio = (232f / bgLuma).coerceIn(0.85f, 2.4f)
            val r = (Color.red(s) * ratio).toInt().coerceIn(0, 248)
            val g = (Color.green(s) * ratio).toInt().coerceIn(0, 248)
            val bl = (Color.blue(s) * ratio).toInt().coerceIn(0, 248)
            outPx[i] = Color.rgb(r, g, bl)
        }
        return Bitmap.createBitmap(outPx, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun applySharpen(source: Bitmap, amount: Float): Bitmap {
        val w = source.width
        val h = source.height
        val px = IntArray(w * h)
        source.getPixels(px, 0, w, 0, 0, w, h)
        val out = px.copyOf()
        val k = amount * 0.85f
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                val c = px[i]
                val n = px[(y - 1) * w + x]
                val s = px[(y + 1) * w + x]
                val e = px[y * w + (x + 1)]
                val wst = px[y * w + (x - 1)]

                val avgR = (Color.red(n) + Color.red(s) + Color.red(e) + Color.red(wst)) / 4
                val avgG = (Color.green(n) + Color.green(s) + Color.green(e) + Color.green(wst)) / 4
                val avgB = (Color.blue(n) + Color.blue(s) + Color.blue(e) + Color.blue(wst)) / 4

                val nr = (Color.red(c) + ((Color.red(c) - avgR) * k)).roundToInt().coerceIn(0, 255)
                val ng = (Color.green(c) + ((Color.green(c) - avgG) * k)).roundToInt().coerceIn(0, 255)
                val nb = (Color.blue(c) + ((Color.blue(c) - avgB) * k)).roundToInt().coerceIn(0, 255)
                out[i] = Color.rgb(nr, ng, nb)
            }
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
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
     * Merges Front & Back of an ID card / Passport onto a single realistic A4 page (1240 x 1754).
     */
    fun mergeIdCardFrontAndBack(front: Bitmap, back: Bitmap): Bitmap {
        val a4Width = 1240
        val a4Height = 1754
        val page = Bitmap.createBitmap(a4Width, a4Height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(page)
        canvas.drawColor(Color.WHITE)

        val cardW = a4Width * 0.68f
        val cardH = cardW / 1.586f // Standard ISO/IEC 7810 ID-1 ratio
        val left = (a4Width - cardW) / 2f
        val topY1 = a4Height * 0.16f
        val topY2 = a4Height * 0.54f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.rgb(203, 213, 225)
        }

        val rect1 = RectF(left, topY1, left + cardW, topY1 + cardH)
        val rect2 = RectF(left, topY2, left + cardW, topY2 + cardH)
        canvas.drawBitmap(front, null, rect1, paint)
        canvas.drawRoundRect(rect1, 18f, 18f, borderPaint)

        canvas.drawBitmap(back, null, rect2, paint)
        canvas.drawRoundRect(rect2, 18f, 18f, borderPaint)
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

    fun saveBitmapAsJpeg(bitmap: Bitmap, destFile: File, quality: Int = 92): File {
        destFile.parentFile?.mkdirs()
        FileOutputStream(destFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(25, 100), fos)
        }
        return destFile
    }

    fun decodeSampledBitmap(file: File, maxSide: Int = 2048): Bitmap? {
        if (!file.exists()) return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        var inSampleSize = 1
        while ((opts.outWidth / inSampleSize) > maxSide || (opts.outHeight / inSampleSize) > maxSide) {
            inSampleSize *= 2
        }
        val decoded = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        ) ?: return null

        val orientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val rotateDeg = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotateDeg == 0f) return decoded

        val matrix = Matrix().apply { postRotate(rotateDeg) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded) decoded.recycle()
        return rotated
    }
}
