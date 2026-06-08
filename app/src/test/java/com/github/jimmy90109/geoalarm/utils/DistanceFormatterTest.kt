package com.github.jimmy90109.geoalarm.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DistanceFormatterTest {
    @Test
    fun `formatMeters adds grouping separators`() {
        assertEquals("999", DistanceFormatter.formatMeters(999, Locale.US))
        assertEquals("1,000", DistanceFormatter.formatMeters(1000, Locale.US))
        assertEquals("3,365", DistanceFormatter.formatMeters(3365, Locale.US))
        assertEquals("5,000", DistanceFormatter.formatMeters(5000, Locale.US))
    }
}
