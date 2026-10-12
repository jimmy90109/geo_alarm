package com.github.jimmy90109.geoalarm.utils

import com.github.jimmy90109.geoalarm.data.DistanceUnitSystem
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceRadiusScaleTest {
    @Test
    fun `metric scale snaps to nearest hundred meters with ties upward`() {
        val scale = DistanceRadiusScale.forSystem(DistanceUnitSystem.METRIC)

        assertEquals(44, scale.steps)
        assertEquals(1400f, scale.snapDistanceMeters(1350.0), 0.01f)
        assertEquals(500f, scale.snapDistanceMeters(100.0), 0.01f)
        assertEquals(5000f, scale.snapDistanceMeters(6000.0), 0.01f)
    }

    @Test
    fun `imperial scale uses quarter mile steps and clamps to three miles`() {
        val scale = DistanceRadiusScale.forSystem(DistanceUnitSystem.IMPERIAL)

        assertEquals(10, scale.steps)
        assertEquals(804.672f, scale.snapDistanceMeters(1000.0), 0.01f)
        assertEquals(4828.032f, scale.snapDistanceMeters(5000.0), 0.01f)
        assertEquals(0.25f, scale.sliderValue(402.336), 0.0001f)
    }
}
