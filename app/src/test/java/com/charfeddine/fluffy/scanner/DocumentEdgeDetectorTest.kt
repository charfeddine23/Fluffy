package com.charfeddine.fluffy.scanner

import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import com.charfeddine.fluffy.scanner.data.model.DocumentQuad
import com.charfeddine.fluffy.scanner.data.model.PointNorm
import com.charfeddine.fluffy.scanner.utils.CornerStabilityTracker
import com.charfeddine.fluffy.scanner.utils.DocumentEdgeDetector
import com.charfeddine.fluffy.scanner.utils.DocumentImageProcessor
import com.charfeddine.fluffy.scanner.utils.EdgeDetectionFrameResult
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocumentEdgeDetectorTest {

    @Test
    fun `detectFromBitmap reconstructs sharp line-intersection corners for rounded ID card`() {
        val w = 500
        val h = 700
        val px = IntArray(w * h)
        // Rounded card bounds: x in 75..425 (0.15..0.85), y in 175..420 (0.25..0.60), corner radius 20px
        val left = 75f
        val top = 175f
        val right = 425f
        val bottom = 420f
        val radius = 20f

        for (y in 0 until h) {
            for (x in 0 until w) {
                val fx = x.toFloat()
                val fy = y.toFloat()
                val dx = max(max(left + radius - fx, fx - (right - radius)), 0f)
                val dy = max(max(top + radius - fy, fy - (bottom - radius)), 0f)
                val inBox = fx >= left && fx <= right && fy >= top && fy <= bottom
                val inCorner = dx == 0f || dy == 0f || hypot(dx, dy) <= radius
                val isCard = inBox && inCorner
                px[y * w + x] = if (isCard) {
                    Color.rgb(244, 246, 250)
                } else {
                    val grain = ((x * 7 + y * 13) % 10) - 5
                    Color.rgb(92 + grain, 86 + grain, 80 + grain)
                }
            }
        }

        val bmp = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
        val quad = DocumentEdgeDetector.detectFromBitmap(bmp, isIdCardHint = true)
        bmp.recycle()

        // Must be strictly ordered [TL, TR, BR, BL]
        assertTrue("Expected valid quad, got $quad", DocumentImageProcessor.isValidDocumentQuad(quad))
        assertTrue(quad.topLeft.x < quad.topRight.x)
        assertTrue(quad.bottomLeft.x < quad.bottomRight.x)
        assertTrue(quad.topLeft.y < quad.bottomLeft.y)
        assertTrue(quad.topRight.y < quad.bottomRight.y)

        assertEquals("tl.x in $quad", 0.15f, quad.topLeft.x, 0.04f)
        assertEquals("tl.y in $quad", 0.25f, quad.topLeft.y, 0.04f)
        assertEquals("br.x in $quad", 0.85f, quad.bottomRight.x, 0.04f)
        assertEquals("br.y in $quad", 0.60f, quad.bottomRight.y, 0.04f)
    }

    @Test
    fun `detectFromBitmap detects low-contrast rotated document via CLAHE and line intersection`() {
        val w = 600
        val h = 800
        val px = IntArray(w * h)
        val cx = 300.0
        val cy = 400.0
        val angle = Math.toRadians(12.0)
        val cosA = cos(angle)
        val sinA = sin(angle)
        val halfW = 180.0
        val halfH = 250.0

        for (y in 0 until h) {
            for (x in 0 until w) {
                val dx = x - cx
                val dy = y - cy
                val lx = dx * cosA + dy * sinA
                val ly = -dx * sinA + dy * cosA
                val insideDoc = kotlin.math.abs(lx) <= halfW && kotlin.math.abs(ly) <= halfH
                // Subtle low-contrast paper (lum ~215) vs light desk (lum ~178)
                px[y * w + x] = if (insideDoc) Color.rgb(218, 218, 214) else Color.rgb(176, 172, 166)
            }
        }

        val bmp = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
        val quad = DocumentEdgeDetector.detectFromBitmap(bmp, isIdCardHint = false)
        bmp.recycle()

        assertTrue("Expected valid low-contrast rotated quad, got $quad", DocumentImageProcessor.isValidDocumentQuad(quad))
        assertTrue(quad.topLeft.x < quad.topRight.x)
        assertTrue(quad.bottomLeft.x < quad.bottomRight.x)
        assertTrue(quad.topLeft.y < quad.bottomLeft.y)
        assertTrue(quad.topRight.y < quad.bottomRight.y)
        // Rotated doc topLeft sits upper-left of center; bottomRight sits lower-right of center
        assertTrue(min(quad.topLeft.x, quad.bottomLeft.x) < 0.35f)
        assertTrue(max(quad.topRight.x, quad.bottomRight.x) > 0.65f)
    }

    @Test
    fun `temporal stability tracker smooths jitter and suppresses single-frame outlier jump`() {
        val tracker = CornerStabilityTracker(windowSize = 4, steadyDurationMs = 600L, jitterThresholdNorm = 0.02f)

        val baseQuad = DocumentQuad(
            PointNorm(0.10f, 0.15f),
            PointNorm(0.90f, 0.15f),
            PointNorm(0.90f, 0.85f),
            PointNorm(0.10f, 0.85f)
        )
        val slightShift = DocumentQuad(
            PointNorm(0.11f, 0.16f),
            PointNorm(0.91f, 0.16f),
            PointNorm(0.91f, 0.86f),
            PointNorm(0.11f, 0.86f)
        )
        val outlierJump = DocumentQuad(
            PointNorm(0.50f, 0.50f),
            PointNorm(0.95f, 0.50f),
            PointNorm(0.95f, 0.95f),
            PointNorm(0.50f, 0.95f)
        )

        val s1 = tracker.update(EdgeDetectionFrameResult(baseQuad, 0.9f, isLocked = true), nowMs = 100L, autoCaptureEnabled = false)
        assertTrue(s1.isLocked)

        val s2 = tracker.update(EdgeDetectionFrameResult(slightShift, 0.9f, isLocked = true), nowMs = 130L, autoCaptureEnabled = false)
        assertTrue(s2.quad.topLeft.x in 0.101f..0.109f)

        val s3 = tracker.update(EdgeDetectionFrameResult(outlierJump, 0.9f, isLocked = true), nowMs = 160L, autoCaptureEnabled = false)
        assertFalse(s3.shouldAutoCapture)
        assertEquals(s2.quad.topLeft.x, s3.quad.topLeft.x, 0.001f)
    }

    @Test
    fun `applyExifOrientation normalizes rotated 90-degree axes upright`() {
        val raw = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        val upright = DocumentImageProcessor.applyExifOrientation(raw, ExifInterface.ORIENTATION_ROTATE_90)
        assertEquals(200, upright.width)
        assertEquals(400, upright.height)
        if (!upright.isRecycled) upright.recycle()
    }
}
