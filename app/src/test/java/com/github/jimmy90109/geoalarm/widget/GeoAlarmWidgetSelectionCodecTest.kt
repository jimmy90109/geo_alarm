package com.github.jimmy90109.geoalarm.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoAlarmWidgetSelectionCodecTest {

    @Test
    fun encode_shouldDeduplicateAndLimitToTwo() {
        val encoded = GeoAlarmWidgetSelectionCodec.encode(
            listOf("a", "", "a", "b", "c")
        )

        assertEquals("a,b", encoded)
    }

    @Test
    fun decode_shouldTrimDeduplicateAndLimitToTwo() {
        val decoded = GeoAlarmWidgetSelectionCodec.decode(" a , b, a, c ")

        assertEquals(listOf("a", "b"), decoded)
    }
}
