package com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components

import kotlin.math.sin

internal fun mediaStackExitDistance(
    cardWidth: Float,
    cardHeight: Float,
    stackWidth: Float,
    stackHeight: Float,
    clearance: Float,
): Float {
    // Include the horizontal projection of a tall card at the maximum eight-degree tilt.
    val halfExtent = (cardWidth + cardHeight * sin(Math.toRadians(8.0)).toFloat()) / 2f
    val stackHalfExtent = (stackWidth + stackHeight * sin(Math.toRadians(8.0)).toFloat()) / 2f
    return (halfExtent + stackHalfExtent + clearance).coerceAtLeast(1f)
}
