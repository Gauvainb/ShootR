package com.example.domain

import kotlin.math.hypot

/**
 * Points attributed to an impact from its distance to the center of the target.
 *
 * Only the C50 target has a scoring table in this app (see [TargetScoring]). Other targets (C200,
 * IPSC, Hunter, 10 m rifle and pistol) have no validated table yet, so the app does not award
 * points for them: their impacts still count for the group size, the MOA and the mean point of
 * impact.
 */
object Scoring {

    /** True when points are computed for [targetType]. */
    fun hasScoring(targetType: String): Boolean = TargetScoring.tableFor(targetType) != null

    /**
     * Score and inner-ten flag of an impact at [realMm] from the center, or null when the target has
     * no scoring table.
     */
    fun score(realMm: Impact, targetType: String): Pair<Int, Boolean>? =
        TargetScoring.tableFor(targetType)?.score(hypot(realMm.xMm, realMm.yMm))
}
