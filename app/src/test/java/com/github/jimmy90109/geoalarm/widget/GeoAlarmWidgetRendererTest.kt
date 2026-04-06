package com.github.jimmy90109.geoalarm.widget

import com.github.jimmy90109.geoalarm.data.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoAlarmWidgetRendererTest {

    @Test
    fun resolveSelectedAlarms_shouldKeepOrderAndLimitToTwo() {
        val alarmA = Alarm(id = "a", name = "A", latitude = 0.0, longitude = 0.0, radius = 100.0, isEnabled = false)
        val alarmB = Alarm(id = "b", name = "B", latitude = 0.0, longitude = 0.0, radius = 100.0, isEnabled = false)
        val alarmC = Alarm(id = "c", name = "C", latitude = 0.0, longitude = 0.0, radius = 100.0, isEnabled = false)

        val result = GeoAlarmWidgetRenderer.resolveSelectedAlarms(
            listOf(alarmA, alarmB, alarmC),
            listOf("c", "missing", "a", "b")
        )

        assertEquals(listOf("c", "a"), result.map { it.id })
    }
}
