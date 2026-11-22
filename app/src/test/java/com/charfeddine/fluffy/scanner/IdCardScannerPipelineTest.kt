package com.charfeddine.fluffy.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.charfeddine.fluffy.scanner.data.model.DocumentQuad
import com.charfeddine.fluffy.scanner.data.model.PointNorm
import com.charfeddine.fluffy.scanner.utils.DocumentEdgeDetector
import com.charfeddine.fluffy.scanner.utils.DocumentImageProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IdCardScannerPipelineTest {

    @Test
    fun `mergeIdCardFrontAndBack Recto places both faces at exact 8_5cm x 5_0cm on one A4 sheet`() {
        val front = Bitmap.createBitmap(850, 500, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(220, 235, 250))
        }
        val back = Bitmap.createBitmap(850, 500, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(210, 240, 225))
        }

        val sheet = DocumentImageProcessor.mergeIdCardFrontAndBack(front, back)
        assertEquals(2480, sheet.width)
        assertEquals(3508, sheet.height)

        // Expected card bounds at 300 DPI: 8.5 / 21.0 width, 5.0 / 29.7 height
        val expectedW = (2480f * (8.5f / 21.0f)).toInt()
        val expectedH = (3508f * (5.0f / 29.7f)).toInt()
        val leftX = ((2480f - expectedW) / 2f).toInt()
        val midX = 2480 / 2
        val top1CenterY = (3508f * (5.5f / 29.7f) + expectedH * 0.5f).toInt()
        val top2CenterY = (3508f * (13.5f / 29.7f) + expectedH * 0.5f).toInt()

        // Verify outside paper background is pure white
        assertEquals(Color.WHITE, sheet.getPixel(40, 40))
        // Verify front face center & left/right margin symmetry
        val frontPixel = sheet.getPixel(midX, top1CenterY)
        assertTrue(Color.blue(frontPixel) > Color.red(frontPixel))
        val backPixel = sheet.getPixel(midX, top2CenterY)
        assertTrue(Color.green(backPixel) > Color.red(backPixel))

        // Verify left & right edges match exact 8.5 cm span
        assertEquals(Color.WHITE, sheet.getPixel((leftX - 18).coerceAtLeast(0), top1CenterY))
        assertEquals(Color.WHITE, sheet.getPixel((leftX + expectedW + 18).coerceAtMost(2479), top1CenterY))

        front.recycle()
        back.recycle()
        sheet.recycle()
    }

    @Test
    fun `createIdCardSingleFaceSheet Recto Verso aligns front and back symmetrically for duplex print`() {
        val front = Bitmap.createBitmap(850, 500, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(40, 90, 200))
        }
        val back = Bitmap.createBitmap(850, 500, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(30, 150, 90))
        }

        val frontPage = DocumentImageProcessor.createIdCardSingleFaceSheet(front)
        val backPage = DocumentImageProcessor.createIdCardSingleFaceSheet(back)

        assertEquals(2480, frontPage.width)
        assertEquals(3508, frontPage.height)
        assertEquals(2480, backPage.width)
        assertEquals(3508, backPage.height)

        val boundsFront = findColoredBoundingBox(frontPage)
        val boundsBack = findColoredBoundingBox(backPage)

        // Both sheets must have identical pixel bounding boxes and equal left/right margins (6.25 cm each)
        assertTrue(abs(boundsFront.left - boundsBack.left) <= 2)
        assertTrue(abs(boundsFront.right - boundsBack.right) <= 2)
        assertTrue(abs(boundsFront.top - boundsBack.top) <= 2)
        assertTrue(abs(boundsFront.bottom - boundsBack.bottom) <= 2)

        val actualW = (boundsFront.right - boundsFront.left).toFloat()
        val actualH = (boundsFront.bottom - boundsFront.top).toFloat()
        val expectedW = 2480f * (8.5f / 21.0f)
        val expectedH = 3508f * (5.0f / 29.7f)
        assertTrue("Width $actualW vs expected $expectedW", abs(actualW - expectedW) <= 8f)
        assertTrue("Height $actualH vs expected $expectedH", abs(actualH - expectedH) <= 8f)

        val leftMargin = boundsFront.left
        val rightMargin = frontPage.width - 1 - boundsFront.right
        assertTrue("Symmetrical margin mismatch: L=$leftMargin R=$rightMargin", abs(leftMargin - rightMargin) <= 4)

        front.recycle()
        back.recycle()
        frontPage.recycle()
        backPage.recycle()
    }

    @Test
    fun `detectIdCardWithGuidance locates tilted textured ID card on busy tabletop`() {
        val sceneW = 720
        val sceneH = 960
        val bmp = Bitmap.createBitmap(sceneW, sceneH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(92, 74, 58)) // Wooden table background

        // Add subtle tabletop grain stripes
        val grainPaint = Paint().apply {
            color = Color.rgb(104, 83, 66)
            strokeWidth = 3f
        }
        for (y in 0 until sceneH step 18) {
            canvas.drawLine(0f, y.toFloat(), sceneW.toFloat(), (y + 6).toFloat(), grainPaint)
        }

        // Draw tilted bright ID card (approx 46% x 22% area, ~6% - 10% total frame)
        canvas.save()
        canvas.translate(sceneW * 0.5f, sceneH * 0.48f)
        canvas.rotate(-4.5f)
        val cardW = sceneW * 0.62f
        val cardH = cardW / 1.586f
        val cardRect = RectF(-cardW / 2f, -cardH / 2f, cardW / 2f, cardH / 2f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(242, 246, 250)
        }
        canvas.drawRoundRect(cardRect, 14f, 14f, cardPaint)
        // Simulated portrait & printed lines inside the ID card
        val headerPaint = Paint().apply { color = Color.rgb(25, 85, 165) }
        canvas.drawRect(-cardW / 2f, -cardH / 2f, cardW / 2f, -cardH / 2f + 26f, headerPaint)
        val textPaint = Paint().apply { color = Color.rgb(30, 41, 59); strokeWidth = 5f }
        canvas.drawLine(-cardW * 0.1f, -cardH * 0.1f, cardW * 0.35f, -cardH * 0.1f, textPaint)
        canvas.drawLine(-cardW * 0.1f, cardH * 0.1f, cardW * 0.30f, cardH * 0.1f, textPaint)
        canvas.restore()

        val guide = DocumentQuad(
            PointNorm(0.07f, 0.31f),
            PointNorm(0.93f, 0.31f),
            PointNorm(0.93f, 0.65f),
            PointNorm(0.07f, 0.65f)
        )
        val detected = DocumentEdgeDetector.detectIdCardWithGuidance(bmp, guide)
        assertTrue(
            "Expected valid ID quad, got $detected",
            DocumentImageProcessor.isValidDocumentQuad(detected, isIdCardHint = true)
        )
        // Center of detected quad should be close to center of scene (0.50, 0.48)
        val cx = (detected.topLeft.x + detected.topRight.x + detected.bottomRight.x + detected.bottomLeft.x) * 0.25f
        val cy = (detected.topLeft.y + detected.topRight.y + detected.bottomRight.y + detected.bottomLeft.y) * 0.25f
        assertTrue("Center X off: $cx", abs(cx - 0.50f) < 0.08f)
        assertTrue("Center Y off: $cy", abs(cy - 0.48f) < 0.08f)
        bmp.recycle()
    }

    private fun findColoredBoundingBox(sheet: Bitmap): android.graphics.Rect {
        var minX = sheet.width
        var minY = sheet.height
        var maxX = 0
        var maxY = 0
        for (y in 0 until sheet.height step 2) {
            for (x in 0 until sheet.width step 2) {
                val c = sheet.getPixel(x, y)
                val lum = (Color.red(c) + Color.green(c) + Color.blue(c)) / 3
                if (lum < 245) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        return android.graphics.Rect(minX, minY, maxX, maxY)
    }
}
