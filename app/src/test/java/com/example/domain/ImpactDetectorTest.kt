package com.example.domain

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

class ImpactDetectorTest {

    /**
     * Synthetic photo of a C50-like target: 800 px wide, 1.4 px/mm, black visual of 200 mm with
     * printed rings and digits, uneven lighting, blur and sensor noise.
     */
    private class Scene(val width: Int = 800, val height: Int = 800) {
        val pxPerMm = 1.4f
        val centerX = width / 2
        val centerY = height / 2
        val blackRadius = 100f * pxPerMm
        val luma = FloatArray(width * height) { PAPER }

        init {
            disc(centerX.toFloat(), centerY.toFloat(), blackRadius, INK)
            // Scoring rings every 25 mm, 2 px wide: dark on the paper, white in the visual.
            for (ring in 1..10) {
                val r = ring * 25f * pxPerMm
                circle(centerX.toFloat(), centerY.toFloat(), r, if (r < blackRadius) PAPER else INK)
            }
            // Digits, drawn as "8" outlines, on both sides of the visual's edge.
            for (ring in listOf(1.5f, 2.5f, 3.5f, 4.5f, 5.5f)) {
                val r = ring * 25f * pxPerMm
                val value = if (r < blackRadius) PAPER else INK
                for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    val x = centerX + dx * r
                    val y = centerY + dy * r
                    circle(x, y - 4f, 4f, value)
                    circle(x, y + 4f, 4f, value)
                }
            }
        }

        fun set(x: Int, y: Int, value: Float) {
            if (x in 0 until width && y in 0 until height) luma[y * width + x] = value
        }

        fun disc(cx: Float, cy: Float, radius: Float, value: Float) {
            for (y in (cy - radius - 1).toInt()..(cy + radius + 1).toInt()) {
                for (x in (cx - radius - 1).toInt()..(cx + radius + 1).toInt()) {
                    if (hypot(x - cx, y - cy) <= radius) set(x, y, value)
                }
            }
        }

        /** 2 px wide circle outline. */
        fun circle(cx: Float, cy: Float, radius: Float, value: Float) {
            for (y in (cy - radius - 2).toInt()..(cy + radius + 2).toInt()) {
                for (x in (cx - radius - 2).toInt()..(cx + radius + 2).toInt()) {
                    if (abs(hypot(x - cx, y - cy) - radius) <= 1f) set(x, y, value)
                }
            }
        }

        /** A bullet hole: dark backing seen through the paper, lighter than ink inside the visual. */
        fun hole(x: Float, y: Float, diameterMm: Float) {
            val inVisual = hypot(x - centerX, y - centerY) < blackRadius
            disc(x, y, diameterMm * pxPerMm / 2f, if (inVisual) HOLE_IN_VISUAL else HOLE_ON_PAPER)
        }

        fun render(): LumaImage {
            val blurred = FloatArray(luma.size)
            for (y in 0 until height) for (x in 0 until width) {
                var sum = 0f
                var n = 0
                for (dy in -1..1) for (dx in -1..1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until width && ny in 0 until height) {
                        sum += luma[ny * width + nx]
                        n++
                    }
                }
                blurred[y * width + x] = sum / n
            }
            val random = Random(42)
            var min = 255f
            var max = 0f
            for (y in 0 until height) for (x in 0 until width) {
                val lighting = 0.88f + 0.12f * x / width
                val v = (blurred[y * width + x] * lighting + random.nextInt(-6, 7)).coerceIn(0f, 255f)
                blurred[y * width + x] = v
                if (v < min) min = v
                if (v > max) max = v
            }
            return LumaImage(width, height, blurred, min, max)
        }

        fun params(caliberMm: Float?, searchRadiusMm: Float? = 375f) = ImpactDetectionParams(
            centerNormalized = Offset(centerX.toFloat() / width, centerY.toFloat() / height),
            blackRadiusNormalized = blackRadius / width,
            holeDiameterNormalized = caliberMm?.let { it * pxPerMm / width },
            searchRadiusNormalized = searchRadiusMm?.let { it * pxPerMm / width }
        )
    }

    private fun assertImpacts(scene: Scene, expected: List<Pair<Float, Float>>, found: List<Offset>, tolerancePx: Float = 2f) {
        val foundPx = found.map { Offset(it.x * scene.width, it.y * scene.height) }
        assertEquals("detected $foundPx", expected.size, found.size)
        expected.forEach { (x, y) ->
            assertTrue("no impact near ($x, $y) in $foundPx", foundPx.any { hypot(it.x - x, it.y - y) <= tolerancePx })
        }
    }

    @Test
    fun `finds holes on the paper and in the black visual, ignoring rings and digits`() {
        val scene = Scene()
        val holes = listOf(
            150f to 180f, 640f to 210f, 230f to 610f, 600f to 590f, 190f to 395f, // on the paper
            400f to 400f, 372f to 446f, 455f to 352f, 330f to 340f                 // in the visual
        )
        holes.forEach { (x, y) -> scene.hole(x, y, diameterMm = 9f) }

        val found = ImpactDetector.detect(scene.render(), scene.params(caliberMm = 9f))

        assertImpacts(scene, holes, found)
    }

    @Test
    fun `a blank target yields no impact`() {
        val scene = Scene()
        assertTrue(ImpactDetector.detect(scene.render(), scene.params(caliberMm = 9f)).isEmpty())
        assertTrue(ImpactDetector.detect(scene.render(), scene.params(caliberMm = 5.6f)).isEmpty())
    }

    @Test
    fun `small caliber holes are found`() {
        val scene = Scene()
        val holes = listOf(160f to 200f, 620f to 240f, 410f to 388f, 380f to 430f)
        holes.forEach { (x, y) -> scene.hole(x, y, diameterMm = 5.6f) }

        val found = ImpactDetector.detect(scene.render(), scene.params(caliberMm = 5.6f))

        assertImpacts(scene, holes, found)
    }

    @Test
    fun `a hole crossing a ring line is still found`() {
        val scene = Scene()
        // Ring 5 lies at 125 mm = 175 px from the center, on the paper.
        val holes = listOf(400f + 175f to 400f, 400f to 400f - 172f)
        holes.forEach { (x, y) -> scene.hole(x, y, diameterMm = 9f) }

        val found = ImpactDetector.detect(scene.render(), scene.params(caliberMm = 9f))

        assertImpacts(scene, holes, found, tolerancePx = 3f)
    }

    @Test
    fun `overlapping holes are split`() {
        val scene = Scene()
        val pair = listOf(200f to 200f, 209f to 204f)
        val triple = listOf(600f to 600f, 611f to 601f, 605f to 611f)
        val single = listOf(420f to 380f)
        (pair + triple + single).forEach { (x, y) -> scene.hole(x, y, diameterMm = 9f) }

        val found = ImpactDetector.detect(scene.render(), scene.params(caliberMm = 9f))

        assertImpacts(scene, pair + triple + single, found, tolerancePx = 3.5f)
    }

    @Test
    fun `shadows, stickers and objects off the card are ignored`() {
        val scene = Scene()
        // Wedge-shaped shadow across the paper.
        for (y in 620 until 700) for (x in 150 until 150 + (y - 620) * 3) scene.set(x, y, INK)
        // Round sticker three times wider than a bullet hole.
        scene.disc(620f, 180f, 9f * scene.pxPerMm * 1.5f, INK)
        // Hole-sized dark object beyond the edge of the card (searched radius is 375 mm = 525 px).
        scene.disc(20f, 20f, 6f, INK)
        scene.hole(250f, 250f, diameterMm = 9f)

        val found = ImpactDetector.detect(scene.render(), scene.params(caliberMm = 9f))

        assertImpacts(scene, listOf(250f to 250f), found)
    }

    @Test
    fun `holes are found without knowing the scale`() {
        val scene = Scene()
        val holes = listOf(150f to 180f, 640f to 210f, 400f to 400f, 372f to 446f)
        holes.forEach { (x, y) -> scene.hole(x, y, diameterMm = 9f) }

        val found = ImpactDetector.detect(scene.render(), scene.params(caliberMm = null, searchRadiusMm = null))

        assertImpacts(scene, holes, found)
    }

    @Test
    fun `coordinates are normalized per axis on non square photos`() {
        val scene = Scene(width = 800, height = 600)
        scene.hole(700f, 90f, diameterMm = 9f)

        val found = ImpactDetector.detect(scene.render(), scene.params(caliberMm = 9f))

        assertEquals(1, found.size)
        assertEquals(700f / 800f, found[0].x, 0.003f)
        assertEquals(90f / 600f, found[0].y, 0.003f)
    }

    @Test
    fun `a photo without contrast yields no impact`() {
        val flat = LumaImage(400, 400, FloatArray(400 * 400) { 120f + (it % 7) }, 120f, 126f)
        val params = ImpactDetectionParams(Offset(0.5f, 0.5f), blackRadiusNormalized = 0.2f)
        assertTrue(ImpactDetector.detect(flat, params).isEmpty())
    }

    @Test
    fun `already placed impacts are not detected twice`() {
        val known = listOf(Offset(100f, 100f), Offset(300f, 300f))
        val detected = listOf(Offset(103f, 98f), Offset(200f, 200f), Offset(300f, 311f))

        assertEquals(
            listOf(Offset(200f, 200f), Offset(300f, 311f)),
            ImpactDetector.excludeKnown(known, detected, minDistance = 10f)
        )
    }

    @Test
    fun `caliber labels are converted to millimeters`() {
        assertEquals(5.59f, Caliber.diameterMm(".22 LR")!!, 0.01f)
        assertEquals(9f, Caliber.diameterMm("9x19 mm")!!, 0.01f)
        assertEquals(5.56f, Caliber.diameterMm("5.56x45 mm / .223 Rem")!!, 0.01f)
        assertEquals(7.82f, Caliber.diameterMm(".308 Win")!!, 0.01f)
        assertEquals(4.5f, Caliber.diameterMm("4.5 mm (.177)")!!, 0.01f)
        assertEquals(7.62f, Caliber.diameterMm("7.62x39 mm")!!, 0.01f)
        assertEquals(9.07f, Caliber.diameterMm(".357 Magnum")!!, 0.01f)
        assertEquals(9.65f, Caliber.diameterMm("38 Special")!!, 0.01f)
        assertEquals(5.59f, Caliber.diameterMm("22 LR")!!, 0.01f)
        assertNull(Caliber.diameterMm("N/A"))
        assertNull(Caliber.diameterMm(""))
    }

    @Test
    fun `target dimensions are known for the scored target types`() {
        assertEquals(TargetSpec(200f, 500f), TargetSpecs.forTargetType("C50 (25m/50m)"))
        assertEquals(TargetSpec(30.5f, 80f), TargetSpecs.forTargetType("Carabine 10m (ISSF)"))
        assertEquals(TargetSpec(59.5f, 170f), TargetSpecs.forTargetType("Pistolet 10m (ISSF)"))
        assertEquals(TargetSpec(400f, 800f), TargetSpecs.forTargetType("C200 (200m)"))
        assertEquals(0f, TargetSpecs.forTargetType("IPSC Classic / Métallique")!!.blackVisualDiameterMm)
        assertNull(TargetSpecs.forTargetType("Cible Hunter / Précision"))
    }

    private companion object {
        const val PAPER = 235f
        const val INK = 30f
        const val HOLE_ON_PAPER = 45f
        const val HOLE_IN_VISUAL = 185f
    }
}
