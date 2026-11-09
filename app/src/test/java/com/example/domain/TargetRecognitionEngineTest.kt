package com.example.domain

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class TargetRecognitionEngineTest {

    private val white = 255f
    private val black = 0f

    private fun blankImage(width: Int, height: Int) = FloatArray(width * height) { white }

    private fun FloatArray.disc(width: Int, cx: Int, cy: Int, radius: Int, value: Float) {
        val height = size / width
        for (y in (cy - radius).coerceAtLeast(0)..(cy + radius).coerceAtMost(height - 1)) {
            for (x in (cx - radius).coerceAtLeast(0)..(cx + radius).coerceAtMost(width - 1)) {
                if (hypot((x - cx).toFloat(), (y - cy).toFloat()) <= radius) this[y * width + x] = value
            }
        }
    }

    private fun FloatArray.rect(width: Int, left: Int, top: Int, right: Int, bottom: Int, value: Float) {
        for (y in top until bottom) for (x in left until right) this[y * width + x] = value
    }

    private fun image(width: Int, height: Int, luma: FloatArray) = LumaImage(width, height, luma, black, white)

    private fun assertContainsPoint(points: List<Offset>, width: Int, height: Int, x: Int, y: Int) {
        val found = points.any { hypot(it.x * width - x, it.y * height - y) <= 1.5f }
        assertTrue("no impact detected near ($x, $y) in $points", found)
    }

    @Test
    fun `finds dark holes on paper and bright holes inside the black visual`() {
        val size = 800
        val luma = blankImage(size, size)
        luma.disc(size, 400, 400, 160, black)
        val onPaper = listOf(100 to 120, 650 to 200, 300 to 700)
        val inVisual = listOf(400 to 400, 350 to 460, 470 to 330)
        onPaper.forEach { (x, y) -> luma.disc(size, x, y, 8, black) }
        inVisual.forEach { (x, y) -> luma.disc(size, x, y, 8, white) }

        val impacts = TargetRecognitionEngine.findImpactBlobs(
            image = image(size, size, luma),
            centerNormalized = Offset(0.5f, 0.5f),
            blackRadiusNormalized = 160f / size
        )

        assertEquals(onPaper.size + inVisual.size, impacts.size)
        (onPaper + inVisual).forEach { (x, y) -> assertContainsPoint(impacts, size, size, x, y) }
    }

    @Test
    fun `large dark areas are never reported as impacts`() {
        val size = 800
        val luma = blankImage(size, size)
        luma.disc(size, 400, 400, 160, black)
        // A wedge-shaped shadow far larger than any bullet hole, plus one real hole. Abandoning the
        // flood fill midway through such a shape used to leave hole-sized leftovers behind.
        for (y in 560 until 790) {
            luma.rect(size, left = 20, top = y, right = (20 + (y - 560) * 3).coerceAtMost(795), bottom = y + 1, value = black)
        }
        luma.disc(size, 650, 200, 8, black)

        val impacts = TargetRecognitionEngine.findImpactBlobs(
            image = image(size, size, luma),
            centerNormalized = Offset(0.5f, 0.5f),
            blackRadiusNormalized = 160f / size
        )

        assertEquals(1, impacts.size)
        assertContainsPoint(impacts, size, size, 650, 200)
    }

    @Test
    fun `hole coordinates are normalized per axis on non square photos`() {
        val width = 800
        val height = 600
        val luma = blankImage(width, height)
        luma.disc(width, 400, 300, 120, black)
        luma.disc(width, 700, 90, 8, black)

        val impacts = TargetRecognitionEngine.findImpactBlobs(
            image = image(width, height, luma),
            centerNormalized = Offset(0.5f, 0.5f),
            blackRadiusNormalized = 120f / width
        )

        assertEquals(1, impacts.size)
        assertEquals(700f / width, impacts[0].x, 0.002f)
        assertEquals(90f / height, impacts[0].y, 0.002f)
    }

    @Test
    fun `recognizes a C50 target and reports the photo geometry`() {
        val width = 400
        val height = 300
        val luma = blankImage(width, height)
        // min(width, height) * 0.45 = 135 px card radius; 54 px visual radius => ratio 0.40 (C50)
        luma.disc(width, 220, 140, 54, black)

        val result = TargetRecognitionEngine.analyzeLuma(image(width, height, luma))

        assertEquals("C50 (25m/50m)", result.targetType)
        assertEquals(220f / width, result.detectedCenterNormalized.x, 0.01f)
        assertEquals(140f / height, result.detectedCenterNormalized.y, 0.01f)
        assertEquals(54f / width, result.detectedBlackRadiusNormalized, 0.01f)
        assertEquals(width.toFloat() / height, result.imageAspectRatio, 0.0001f)
    }

    @Test
    fun `fit mapping accounts for letterboxing`() {
        // Landscape 4:3 photo in a square canvas: full width, bars above and below.
        assertEquals(1000f, ImageFit.displayedWidth(1000f, 1000f, 4f / 3f), 0.01f)
        assertEquals(Offset(0f, 125f), ImageFit.toCanvas(Offset(0f, 0f), 1000f, 1000f, 4f / 3f))
        assertEquals(Offset(500f, 500f), ImageFit.toCanvas(Offset(0.5f, 0.5f), 1000f, 1000f, 4f / 3f))
        assertEquals(Offset(1000f, 875f), ImageFit.toCanvas(Offset(1f, 1f), 1000f, 1000f, 4f / 3f))

        // Portrait 3:4 photo: full height, bars left and right.
        assertEquals(750f, ImageFit.displayedWidth(1000f, 1000f, 3f / 4f), 0.01f)
        assertEquals(Offset(125f, 0f), ImageFit.toCanvas(Offset(0f, 0f), 1000f, 1000f, 3f / 4f))

        // Square photo: identity scaling, as before.
        assertEquals(Offset(250f, 750f), ImageFit.toCanvas(Offset(0.25f, 0.75f), 1000f, 1000f, 1f))
    }
}
