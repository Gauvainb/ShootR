package com.example.domain

/** A scoring zone: hits up to [outerRadiusMm] from the center score [score]. */
data class ScoringZone(val score: Int, val outerRadiusMm: Float)

/**
 * How a target is scored, from the distance of a hit to its center.
 *
 * @property innerTenRadiusMm hits within this radius are inner tens (mouches)
 * @property zones sorted from the center outwards
 */
data class ScoringTable(val innerTenRadiusMm: Float, val zones: List<ScoringZone>) {
    /** Score of a hit at [distMm] from the center, and whether it is an inner ten. */
    fun score(distMm: Float): Pair<Int, Boolean> {
        if (distMm <= innerTenRadiusMm) return Pair(zones.first().score, true)
        val zone = zones.firstOrNull { distMm <= it.outerRadiusMm } ?: return Pair(0, false)
        return Pair(zone.score, false)
    }
}

object TargetScoring {
    private fun table(innerTen: Float, vararg zones: Pair<Int, Float>) =
        ScoringTable(innerTen, zones.map { ScoringZone(it.first, it.second) })

    private val c50 = table(
        12.5f,
        10 to 25f, 9 to 50f, 8 to 75f, 7 to 100f, 6 to 125f, 5 to 150f, 4 to 175f, 3 to 200f, 2 to 225f, 1 to 250f
    )

    /** Scoring of a target type. Unknown types are scored as a C50. */
    fun tableFor(targetType: String): ScoringTable = when {
        targetType.contains("Carabine 10m") -> table(
            1f, 10 to 2.5f, 9 to 5f, 8 to 7.5f, 7 to 10f, 6 to 13f, 5 to 16f, 4 to 19f, 3 to 22f, 2 to 25f, 1 to 30f
        )
        targetType.contains("Pistolet 10m") -> table(
            2.5f, 10 to 5.75f, 9 to 13.5f, 8 to 21.5f, 7 to 29.75f, 6 to 38f, 5 to 46f, 4 to 54f, 3 to 62f, 2 to 70f, 1 to 85f
        )
        targetType.contains("C200") -> table(
            25f, 10 to 50f, 9 to 100f, 8 to 150f, 7 to 200f, 6 to 250f, 5 to 300f, 4 to 350f, 1 to 400f
        )
        else -> c50
    }

    /**
     * Radii of the rings printed on the target, when they have been checked against real photos:
     * the scoring then follows the rings seen on the photo. Null when only the black visual is used.
     */
    fun printedRingRadiiMm(targetType: String): List<Float>? = when {
        targetType.contains("C50") -> listOf(12.5f, 25f, 50f, 75f, 100f, 125f, 150f, 175f, 200f, 225f, 250f)
        else -> null
    }
}
