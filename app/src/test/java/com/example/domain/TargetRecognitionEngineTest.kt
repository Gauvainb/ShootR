package com.example.domain

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
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

        // toImage is the inverse of toCanvas.
        for (aspect in listOf(4f / 3f, 3f / 4f, 1f)) {
            val back = ImageFit.toImage(ImageFit.toCanvas(Offset(0.3f, 0.8f), 1000f, 1000f, aspect), 1000f, 1000f, aspect)
            assertEquals(0.3f, back.x, 0.0001f)
            assertEquals(0.8f, back.y, 0.0001f)
        }
    }
}
