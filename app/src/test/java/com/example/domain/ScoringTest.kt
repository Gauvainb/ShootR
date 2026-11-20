package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {

    private val c50 = "C50 (25m/50m)"

    @Test
    fun `C50 rings score from the center outwards`() {
        assertEquals(Pair(10, true), Scoring.score(Impact(0f, 0f), c50))
        assertEquals(Pair(10, false), Scoring.score(Impact(20f, 0f), c50))
        assertEquals(Pair(9, false), Scoring.score(Impact(0f, 40f), c50))
        assertEquals(Pair(7, false), Scoring.score(Impact(60f, 80f), c50)) // exactly 100 mm
        assertEquals(Pair(0, false), Scoring.score(Impact(300f, 0f), c50))
    }

    @Test
    fun `targets without a validated table are not scored`() {
        val unscored = listOf(
            "C200 (200m)", "IPSC Classic / Métallique", "Cible Hunter / Précision",
            "Carabine 10m (ISSF)", "Pistolet 10m (ISSF)"
        )
        for (type in unscored) {
            assertFalse(type, Scoring.hasScoring(type))
            assertNull(type, Scoring.score(Impact(1f, 1f), type))
        }
        assertTrue(Scoring.hasScoring(c50))
    }
}
