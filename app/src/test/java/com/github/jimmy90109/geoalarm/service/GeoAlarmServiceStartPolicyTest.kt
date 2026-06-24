package com.github.jimmy90109.geoalarm.service

import com.github.jimmy90109.geoalarm.data.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoAlarmServiceStartPolicyTest {
    @Test
    fun `destination geofence action requires immediate foreground`() {
        assertTrue(
            GeoAlarmServiceStartPolicy.requiresImmediateForeground(
                GeoAlarmService.ACTION_GEOFENCE_TRIGGERED
            )
        )
    }

    @Test
    fun `warning geofence action requires immediate foreground`() {
        assertTrue(
            GeoAlarmServiceStartPolicy.requiresImmediateForeground(
                GeoAlarmService.ACTION_WARNING_GEOFENCE_TRIGGERED
            )
        )
    }

    @Test
    fun `selectActiveAlarm returns the only enabled alarm`() {
        val enabledAlarm = alarm(id = "enabled", isEnabled = true)

        val result = GeoAlarmServiceStartPolicy.selectActiveAlarm(
            listOf(alarm(id = "disabled", isEnabled = false), enabledAlarm)
        )

        assertEquals(enabledAlarm, result)
    }

    @Test
    fun `selectActiveAlarm rejects missing enabled alarm`() {
        assertNull(
            GeoAlarmServiceStartPolicy.selectActiveAlarm(
                listOf(alarm(id = "disabled", isEnabled = false))
            )
        )
    }

    @Test
    fun `selectActiveAlarm rejects multiple enabled alarms`() {
        assertNull(
            GeoAlarmServiceStartPolicy.selectActiveAlarm(
                listOf(
                    alarm(id = "first", isEnabled = true),
                    alarm(id = "second", isEnabled = true),
                )
            )
        )
    }

    private fun alarm(id: String, isEnabled: Boolean) = Alarm(
        id = id,
        name = id,
        latitude = 1.0,
        longitude = 2.0,
        radius = 100.0,
        isEnabled = isEnabled,
    )
}
