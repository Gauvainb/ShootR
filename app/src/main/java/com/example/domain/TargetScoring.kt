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

/**
 * Scoring tables. Only the C50 has one: the other targets (C200, IPSC, Hunter, 10 m rifle and
 * pistol) have no table checked against a rulebook, so they get no points.
 */
object TargetScoring {
    private val c50 = ScoringTable(
        innerTenRadiusMm = 12.5f,
        zones = listOf(
            10 to 25f, 9 to 50f, 8 to 75f, 7 to 100f, 6 to 125f, 5 to 150f, 4 to 175f, 3 to 200f, 2 to 225f, 1 to 250f
        ).map { ScoringZone(it.first, it.second) }
    )

    /** Scoring table of a target type, or null when the target is not scored. */
    fun tableFor(targetType: String): ScoringTable? = if (targetType.contains("C50")) c50 else null

    /**
     * Radii of the rings printed on the target, when they have been checked against real photos:
     * the scoring then follows the rings seen on the photo. Null when only the black visual is used.
     */
    fun printedRingRadiiMm(targetType: String): List<Float>? =
        if (targetType.contains("C50")) listOf(12.5f, 25f, 50f, 75f, 100f, 125f, 150f, 175f, 200f, 225f, 250f) else null
}
