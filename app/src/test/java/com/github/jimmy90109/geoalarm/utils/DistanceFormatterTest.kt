package com.github.jimmy90109.geoalarm.utils

import com.github.jimmy90109.geoalarm.data.DistanceUnitSystem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DistanceFormatterTest {
    @Test
    fun `metric distances switch from meters to kilometers`() {
        assertEquals(
            FormattedDistance("999", DistanceDisplayUnit.METER),
            DistanceFormatter.formatValue(999.0, DistanceUnitSystem.METRIC, Locale.US),
        )
        assertEquals(
            FormattedDistance("1", DistanceDisplayUnit.KILOMETER),
            DistanceFormatter.formatValue(1000.0, DistanceUnitSystem.METRIC, Locale.US),
        )
        assertEquals(
            FormattedDistance("3.4", DistanceDisplayUnit.KILOMETER),
            DistanceFormatter.formatValue(3365.0, DistanceUnitSystem.METRIC, Locale.US),
        )
    }

    @Test
    fun `imperial distances switch from feet to miles`() {
        assertEquals(
            FormattedDistance("328", DistanceDisplayUnit.FOOT),
            DistanceFormatter.formatValue(100.0, DistanceUnitSystem.IMPERIAL, Locale.US),
        )
        assertEquals(
            FormattedDistance("0.2", DistanceDisplayUnit.MILE),
            DistanceFormatter.formatValue(304.8, DistanceUnitSystem.IMPERIAL, Locale.US),
        )
        assertEquals(
            FormattedDistance("3.1", DistanceDisplayUnit.MILE),
            DistanceFormatter.formatValue(5000.0, DistanceUnitSystem.IMPERIAL, Locale.US),
        )
    }

    @Test
    fun `radius style preserves quarter mile precision`() {
        assertEquals(
            FormattedDistance("0.25", DistanceDisplayUnit.MILE),
            DistanceFormatter.formatValue(
                402.336,
                DistanceUnitSystem.IMPERIAL,
                Locale.US,
                DistanceFormatStyle.RADIUS,
            ),
        )
    }
}
