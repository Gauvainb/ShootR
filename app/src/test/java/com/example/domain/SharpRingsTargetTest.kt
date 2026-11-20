package com.example.domain

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * The case found in QA: a clean, computer-drawn C50 with sharp printed rings, the card edge on a
 * colored background, and five holes, two of which overlap. Every ring and the card edge used to
 * come out as dozens of impacts (106 for 5 holes).
 */
class SharpRingsTargetTest {

    private val size = 1200
    private val pxPerMm = 3.2f
    private val center = size / 2f

    private val background = rgb(234, 224, 200)   // beige wall behind the card
    private val paper = rgb(230, 230, 230)
    private val ink = rgb(20, 20, 20)
    private val ringOnPaper = rgb(90, 90, 90)
    private val ringInVisual = rgb(235, 235, 235)
    private val holeColor = rgb(248, 248, 248)

    // Hole centers, in mm from the target center (x right, y down), 9 mm holes.
    private val holesMm = listOf(-12f to 6f, 0f to 0f, 6f to 4f, -3f to -10f, 14f to -8f)
    private val holeDiameterMm = 9f

    @Test
    fun `sharp rings and the card edge are not impacts, overlapping holes are split`() {
        val image = render()
        val params = ImpactDetectionParams(
            centerNormalized = Offset(0.5f, 0.5f),
            blackRadiusNormalized = 100f * pxPerMm / size,
            mmToPhoto = pxPerMm / size,
            caliberMm = null,
            cardHalfSizeNormalized = 245f * pxPerMm / size,
            digitRadiiMm = List(9) { 37.5f + 25f * it }
        )

        val found = ImpactDetector.detect(image, params).map { Offset(it.x * size, it.y * size) }

        assertEquals("detected $found", holesMm.size, found.size)
        holesMm.forEach { (xMm, yMm) ->
            val x = center + xMm * pxPerMm
            val y = center + yMm * pxPerMm
            assertTrue(
                "no impact near ($x, $y) in $found",
                found.any { hypot(it.x - x, it.y - y) <= holeDiameterMm * pxPerMm / 2f }
            )
        }
    }

    /** Color of the drawing at one point, before anti-aliasing. */
    private fun colorAt(x: Float, y: Float): Int {
        for ((xMm, yMm) in holesMm) {
            if (hypot(x - (center + xMm * pxPerMm), y - (center + yMm * pxPerMm)) <= holeDiameterMm * pxPerMm / 2f) return holeColor
        }
        val distMm = hypot(x - center, y - center) / pxPerMm
        if (distMm > 175f) return background
        // Printed rings every 25 mm, 0.6 mm wide.
        val nearestRing = Math.round(distMm / 25f) * 25f
        val onRing = nearestRing > 0f && kotlin.math.abs(distMm - nearestRing) < 0.3f
        return if (distMm <= 100f) {
            if (onRing) ringInVisual else ink
        } else {
            if (onRing) ringOnPaper else paper
        }
    }

    /** Draws the target with 2x2 supersampling, so edges are anti-aliased like on a photo. */
    private fun render(): LumaImage {
        val argb = IntArray(size * size)
        val luma = FloatArray(size * size)
        var min = 255f
        var max = 0f
        for (y in 0 until size) for (x in 0 until size) {
            var r = 0
            var g = 0
            var b = 0
            for (sy in 0..1) for (sx in 0..1) {
                val c = colorAt(x + 0.25f + 0.5f * sx, y + 0.25f + 0.5f * sy)
                r += (c shr 16) and 0xFF
                g += (c shr 8) and 0xFF
                b += c and 0xFF
            }
            r /= 4
            g /= 4
            b /= 4
            val i = y * size + x
            argb[i] = rgb(r, g, b)
            val l = 0.299f * r + 0.587f * g + 0.114f * b
            luma[i] = l
            if (l < min) min = l
            if (l > max) max = l
        }
        return LumaImage(size, size, luma, min, max, argb)
    }

    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}
