package com.example

/**
 * Impacts left after "remove the detected ones": only the impacts that the automatic detection
 * placed and that the user has not moved, edited or confirmed are removed. Hand-placed impacts and
 * detected ones the user touched stay. Numbers are reassigned in order.
 */
fun withoutUnconfirmedDetections(impacts: List<ScreenImpact>): List<ScreenImpact> =
    impacts.filter { !it.isAuto }.mapIndexed { i, imp -> imp.copy(index = i + 1) }
