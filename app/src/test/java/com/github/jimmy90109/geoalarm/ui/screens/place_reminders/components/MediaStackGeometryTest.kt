package com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStackGeometryTest {
    @Test fun compactCardsCycleLocallyInsteadOfTravellingToScreenEdge() {
        val distance = mediaStackExitDistance(120f, 112f, 120f, 112f, 8f)
        assertTrue("Compact card should circle back within roughly one card width", distance < 150f)
    }

    @Test fun mixedRotatedCardsClearTheStackBeforeChangingLayers() {
        val sizes = listOf(120f to 112f, 12f to 328f, 600f to 100f)
        for ((width, height) in sizes) {
            for ((stackWidth, stackHeight) in sizes) {
                val distance = mediaStackExitDistance(width, height, stackWidth, stackHeight, 8f)
                for (degrees in -8..8) {
                    val angle = Math.toRadians(degrees.toDouble())
                    val halfExtent = (width * abs(cos(angle)) + height * abs(sin(angle))) / 2
                    val stackHalfExtent = (stackWidth * abs(cos(angle)) + stackHeight * abs(sin(angle))) / 2
                    assertTrue("Layer swap must happen after the cards separate",
                        distance - halfExtent - stackHalfExtent >= 7.999)
                }
            }
        }
    }
}
