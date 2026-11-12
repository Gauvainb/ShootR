package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

class RingModelTest {

    private val c50 = TargetScoring.tableFor("C50 (25m/50m)")
    private val rings = TargetScoring.printedRingRadiiMm("C50 (25m/50m)")!!

    /**
     * A C50 target photographed at an angle: 2 px/mm across, squeezed vertically by [squeeze],
     * with black ink, white rings in the visual and grey rings on the paper.
     */
    private fun tiltedTarget(squeeze: Float, width: Int = 1000, height: Int = 1000): LumaImage {
        val cx = width / 2f
        val cy = height / 2f
        val pxPerMm = 2f
        val random = Random(7)
        val luma = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Distance in mm on the target, undoing the squeeze.
                val mm = hypot((x - cx) / pxPerMm, (y - cy) / (pxPerMm * squeeze))
                val onRing = rings.any { abs(mm - it) < 0.6f }
                val v = when {
                    mm <= 100f -> if (onRing && mm < 99f) 220f else 25f
                    onRing -> 120f
                    else -> 230f
                }
                luma[y * width + x] = (v + random.nextInt(-5, 6)).coerceIn(0f, 255f)
            }
        }
        return LumaImage(width, height, luma, 0f, 255f)
    }

    @Test
    fun `scores follow the rings on a tilted photo`() {
        val squeeze = 0.8f
        val image = tiltedTarget(squeeze)
        // The visual as the circle finder reports it: its mean radius (180 px, between 160 and 200).
        val model = RingModel.build(image, 500f, 500f, visualRadiusPx = 180f, visualRadiusMm = 100f, printedRingsMm = rings)
        assertNotNull(model)

        // Hits in the middle of every zone, straight up (where the squeeze is strongest).
        for (zone in 1..9) {
            val distMm = 237.5f - 25f * (zone - 1)
            val y = 500f - distMm * 2f * squeeze
            assertEquals("zone $zone straight up", zone, c50.score(model!!.distanceMm(500f, y)).first)
            // A single circle scaled on the visual's width gets it wrong.
            val circleMm = (500f - y) / 2f
            if (zone in 1..6) assertNotEquals("circle model, zone $zone", zone, c50.score(circleMm).first)
            assertEquals(distMm, model.distanceMm(500f, y), 4f)
        }
        // Sideways, where nothing is squeezed, both agree.
        for (zone in 1..9) {
            val distMm = 237.5f - 25f * (zone - 1)
            assertEquals("zone $zone sideways", zone, c50.score(model!!.distanceMm(500f + distMm * 2f, 500f)).first)
        }
    }

    @Test
    fun `scoring tables`() {
        assertEquals(Pair(10, true), c50.score(10f))
        assertEquals(Pair(10, false), c50.score(20f))
        assertEquals(Pair(7, false), c50.score(100f))
        assertEquals(Pair(1, false), c50.score(249f))
        assertEquals(Pair(0, false), c50.score(251f))
        assertEquals(Pair(10, false), TargetScoring.tableFor("Pistolet 10m (ISSF)").score(5f))
        assertEquals(c50, TargetScoring.tableFor("Cible Hunter / Précision"))
    }
}
