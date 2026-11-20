package com.example.domain

/**
 * Points attributed to an impact from its distance to the center of the target.
 *
 * Only the C50 target has a scoring table in this app. Other targets (C200, IPSC, Hunter, 10 m
 * rifle and pistol) have no validated table yet, so the app does not award points for them: their
 * impacts still count for the group size, the MOA and the mean point of impact.
 */
object Scoring {

    /** True when points are computed for [targetType]. */
    fun hasScoring(targetType: String): Boolean = targetType.contains("C50")

    /**
     * Score and inner-ten flag of an impact at [realMm] from the center, or null when the target has
     * no scoring table.
     */
    fun score(realMm: Impact, targetType: String): Pair<Int, Boolean>? {
        if (!hasScoring(targetType)) return null
        // C50: rings every 25 mm, the 10 ring is 50 mm across, the inner ten (mouche) is 25 mm across.
        val distMm = kotlin.math.hypot(realMm.xMm, realMm.yMm)
        return when {
            distMm <= 12.5f -> Pair(10, true)
            distMm <= 25.0f -> Pair(10, false)
            distMm <= 50.0f -> Pair(9, false)
            distMm <= 75.0f -> Pair(8, false)
            distMm <= 100.0f -> Pair(7, false)
            distMm <= 125.0f -> Pair(6, false)
            distMm <= 150.0f -> Pair(5, false)
            distMm <= 175.0f -> Pair(4, false)
            distMm <= 200.0f -> Pair(3, false)
            distMm <= 225.0f -> Pair(2, false)
            distMm <= 250.0f -> Pair(1, false)
            else -> Pair(0, false)
        }
    }
}
