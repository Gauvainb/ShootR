package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BallisticsCalculatorTest {

    private val tolerance = 0.01f

    @Test
    fun `an empty group has no size`() {
        val stats = BallisticsCalculator.calculateStats(emptyList(), distanceMeters = 50f)
        assertEquals(0f, stats.esMm, 0f)
        assertEquals(0f, stats.moa, 0f)
    }

    @Test
    fun `one impact has no size but a mean point offset from the center`() {
        val stats = BallisticsCalculator.calculateStats(
            impacts = listOf(Impact(3f, -2f)),
            targetCenter = Impact(1f, 1f),
            distanceMeters = 50f
        )
        assertEquals(0f, stats.esMm, 0f)
        assertEquals(0f, stats.moa, 0f)
        assertEquals(2f, stats.mpiDeltaXMm, tolerance)
        assertEquals(-3f, stats.mpiDeltaYMm, tolerance)
    }

    @Test
    fun `width, height and extreme spread of a three shot group`() {
        // Points: (0,0), (10,5), (4,-3). Width 10, height 8, longest side (0,0)-(10,5).
        val stats = BallisticsCalculator.calculateStats(
            impacts = listOf(Impact(0f, 0f), Impact(10f, 5f), Impact(4f, -3f)),
            distanceMeters = 50f
        )
        assertEquals(10f, stats.lMm, tolerance)
        assertEquals(8f, stats.hMm, tolerance)
        assertEquals(18f, stats.hPlusL, tolerance)
        assertEquals(Math.sqrt(125.0).toFloat(), stats.esMm, tolerance)
    }

    @Test
    fun `mean point of impact is measured from the target center`() {
        // Mean of (0,0), (10,5), (4,-3) is (4.67, 0.67); center is (1, 1).
        val stats = BallisticsCalculator.calculateStats(
            impacts = listOf(Impact(0f, 0f), Impact(10f, 5f), Impact(4f, -3f)),
            targetCenter = Impact(1f, 1f),
            distanceMeters = 50f
        )
        assertEquals(3.67f, stats.mpiDeltaXMm, tolerance)
        assertEquals(-0.33f, stats.mpiDeltaYMm, tolerance)
    }

    @Test
    fun `one MOA at 100 m is about 29 mm`() {
        val stats = BallisticsCalculator.calculateStats(
            impacts = listOf(Impact(0f, 0f), Impact(29.09f, 0f)),
            distanceMeters = 100f
        )
        assertEquals(1f, stats.moa, tolerance)
    }

    @Test
    fun `moa scales with distance`() {
        val impacts = listOf(Impact(0f, 0f), Impact(10f, 0f))
        val at50 = BallisticsCalculator.calculateStats(impacts, distanceMeters = 50f).moa
        val at100 = BallisticsCalculator.calculateStats(impacts, distanceMeters = 100f).moa
        assertEquals(at50 / 2f, at100, tolerance)
    }

    @Test
    fun `click correction moves the point of impact back to the center`() {
        // A 1 MOA offset at 100 m with 1/4 MOA clicks: 4 clicks, in the opposite direction.
        assertEquals(-4, BallisticsCalculator.calculateClicksCorrection(29.09f, 100f, 0.25f))
        assertEquals(4, BallisticsCalculator.calculateClicksCorrection(-29.09f, 100f, 0.25f))
        assertEquals(0, BallisticsCalculator.calculateClicksCorrection(0f, 100f, 0.25f))
    }

    @Test
    fun `click correction is zero when the distance or the click value is unknown`() {
        assertEquals(0, BallisticsCalculator.calculateClicksCorrection(10f, 0f, 0.25f))
        assertEquals(0, BallisticsCalculator.calculateClicksCorrection(10f, 100f, 0f))
    }
}
