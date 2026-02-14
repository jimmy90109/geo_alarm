package com.github.jimmy90109.geoalarm.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the app.
 * Using sealed interface with @Serializable for compile-time safety.
 */
@Serializable
sealed interface AppRoutes {
    @Serializable
    data object Main : AppRoutes

    @Serializable
    data object Onboarding : AppRoutes

    @Serializable
    data class AlarmEdit(val alarmId: String? = null) : AppRoutes

    @Serializable
    data class ScheduleEdit(val scheduleId: String? = null) : AppRoutes

    @Serializable
    data object BatteryOptimization : AppRoutes
}
