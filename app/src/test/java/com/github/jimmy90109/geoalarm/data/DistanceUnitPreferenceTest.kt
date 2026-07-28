package com.github.jimmy90109.geoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceUnitPreferenceTest {
    @Test
    fun `stored ids decode and invalid values fall back to automatic`() {
        assertEquals(DistanceUnitPreference.METRIC, DistanceUnitPreference.fromId("metric"))
        assertEquals(DistanceUnitPreference.IMPERIAL, DistanceUnitPreference.fromId("imperial"))
        assertEquals(DistanceUnitPreference.AUTO, DistanceUnitPreference.fromId(null))
        assertEquals(DistanceUnitPreference.AUTO, DistanceUnitPreference.fromId("unknown"))
    }
}
