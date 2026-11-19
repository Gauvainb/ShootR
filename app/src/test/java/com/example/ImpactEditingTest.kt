package com.example

import androidx.compose.ui.geometry.Offset
import com.example.domain.Impact
import org.junit.Assert.assertEquals
import org.junit.Test

class ImpactEditingTest {

    private fun shot(index: Int, auto: Boolean) =
        ScreenImpact(index = index, canvasOffset = Offset(index.toFloat(), 0f), realMm = Impact(index.toFloat(), 0f), isAuto = auto)

    @Test
    fun `only detections that were never touched are removed`() {
        val impacts = listOf(
            shot(1, auto = true),   // detected, untouched: removed
            shot(2, auto = false),  // placed by hand: kept
            shot(3, auto = false),  // detected then moved or edited (no longer auto): kept
            shot(4, auto = true)    // detected, untouched: removed
        )

        val kept = withoutUnconfirmedDetections(impacts)

        assertEquals(listOf(2f, 3f), kept.map { it.realMm.xMm })
        assertEquals(listOf(1, 2), kept.map { it.index })
    }

    @Test
    fun `nothing is removed when no detection is pending`() {
        val impacts = listOf(shot(1, auto = false), shot(2, auto = false))
        assertEquals(impacts, withoutUnconfirmedDetections(impacts))
    }
}
