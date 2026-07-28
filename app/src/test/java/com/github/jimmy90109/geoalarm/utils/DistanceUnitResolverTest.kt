package com.github.jimmy90109.geoalarm.utils

import com.github.jimmy90109.geoalarm.data.DistanceUnitPreference
import com.github.jimmy90109.geoalarm.data.DistanceUnitSystem
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceUnitResolverTest {
    @Test
    fun `manual preference ignores locale`() {
        assertEquals(
            DistanceUnitSystem.METRIC,
            DistanceUnitResolver.resolve(DistanceUnitPreference.METRIC, Locale.US),
        )
        assertEquals(
            DistanceUnitSystem.IMPERIAL,
            DistanceUnitResolver.resolve(DistanceUnitPreference.IMPERIAL, Locale.TAIWAN),
        )
    }

    @Test
    fun `automatic preference honors measurement system locale extension`() {
        val metricUsLocale = Locale.forLanguageTag("en-US-u-ms-metric")
        val imperialTaiwanLocale = Locale.forLanguageTag("zh-TW-u-ms-ussystem")

        assertEquals(
            DistanceUnitSystem.METRIC,
            DistanceUnitResolver.resolveAutomatic(metricUsLocale) {
                DistanceUnitSystem.IMPERIAL
            },
        )
        assertEquals(
            DistanceUnitSystem.IMPERIAL,
            DistanceUnitResolver.resolveAutomatic(imperialTaiwanLocale) {
                DistanceUnitSystem.METRIC
            },
        )
    }

    @Test
    fun `automatic preference uses regional fallback without override`() {
        assertEquals(
            DistanceUnitSystem.IMPERIAL,
            DistanceUnitResolver.resolveAutomatic(Locale.UK) {
                DistanceUnitSystem.IMPERIAL
            },
        )
        assertEquals(
            DistanceUnitSystem.METRIC,
            DistanceUnitResolver.resolveAutomatic(Locale.TAIWAN) {
                DistanceUnitSystem.METRIC
            },
        )
    }
}
