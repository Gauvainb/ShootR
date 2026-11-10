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

    private fun image(width: Int, height: Int, luma: FloatArray) = LumaImage(width, height, luma, black, white)

    @Test
    fun `locates the black visual and reports the photo geometry`() {
        val width = 400
        val height = 300
        val luma = blankImage(width, height)
        luma.disc(width, 220, 140, 54, black)

        val result = TargetRecognitionEngine.analyzeLuma(image(width, height, luma), "C50 (25m/50m)", 200f)

        assertTrue(result.visualFound)
        assertEquals(220f / width, result.detectedCenterNormalized.x, 0.005f)
        assertEquals(140f / height, result.detectedCenterNormalized.y, 0.005f)
        assertEquals(54f / width, result.detectedBlackRadiusNormalized, 0.005f)
        assertEquals(200f, result.visualDiameterMm, 0f)
        assertEquals(width.toFloat() / height, result.imageAspectRatio, 0.0001f)
    }

    @Test
    fun `holes, a sticker and an object across the visual do not move it`() {
        val width = 900
        val height = 1200
        val luma = blankImage(width, height)
        luma.disc(width, 430, 520, 210, black)
        // Bright holes inside, including on the outline, and a dark sticker sticking out.
        listOf(430 to 520, 380 to 600, 500 to 430, 630 to 560, 300 to 380).forEach { (x, y) -> luma.disc(width, x, y, 12, white) }
        luma.disc(width, 250, 640, 25, black)
        // A dark object (pistol slide) coming in from the bottom right over the visual.
        for (y in 620 until 1200) for (x in 520 until 900) if (x - 520 > (y - 620) * 0.2f && x - 520 < (y - 620) * 0.2f + 150) luma[y * width + x] = 40f

        val circle = BlackVisualFinder.find(image(width, height, luma))!!

        assertEquals(430f, circle.centerX, 3f)
        assertEquals(520f, circle.centerY, 3f)
        assertEquals(210f, circle.radius, 3f)
    }

    @Test
    fun `finds a visual that is wider than the frame`() {
        val width = 900
        val height = 1200
        val luma = blankImage(width, height)
        luma.disc(width, 450, 620, 540, black)
        // Reactive-target rings inside: complete circles, unlike the cropped visual.
        for (r in listOf(150, 250, 350)) {
            for (a in 0 until 3600) {
                val x = (450 + r * kotlin.math.cos(a * Math.PI / 1800)).toInt()
                val y = (620 + r * kotlin.math.sin(a * Math.PI / 1800)).toInt()
                for (d in -3..3) luma[(y + d) * width + x] = white
            }
        }

        val circle = BlackVisualFinder.find(image(width, height, luma))!!

        assertEquals(540f, circle.radius, 10f)
        assertEquals(450f, circle.centerX, 10f)
        assertEquals(620f, circle.centerY, 10f)
    }

    @Test
    fun `reports when there is no visual`() {
        val result = TargetRecognitionEngine.analyzeLuma(image(400, 300, blankImage(400, 300)), "C50 (25m/50m)", 200f)
        assertTrue(!result.visualFound)
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

        // toImage is the inverse of toCanvas.
        for (aspect in listOf(4f / 3f, 3f / 4f, 1f)) {
            val back = ImageFit.toImage(ImageFit.toCanvas(Offset(0.3f, 0.8f), 1000f, 1000f, aspect), 1000f, 1000f, aspect)
            assertEquals(0.3f, back.x, 0.0001f)
            assertEquals(0.8f, back.y, 0.0001f)
        }
    }
}
