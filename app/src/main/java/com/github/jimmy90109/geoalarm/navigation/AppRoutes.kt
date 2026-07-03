package com.github.jimmy90109.geoalarm.navigation

import com.github.jimmy90109.geoalarm.share.SharedPlaceSource
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
    data class Onboarding(val showAnalyticsOptIn: Boolean = true) : AppRoutes

    @Serializable
    data class AlarmEdit(
        val alarmId: String? = null,
        val sharedPlaceQuery: String? = null,
        val sharedPlaceSource: SharedPlaceSource? = null
    ) : AppRoutes

    @Serializable
    data class ScheduleEdit(val scheduleId: String? = null) : AppRoutes

    @Serializable
    data class PlaceReminderEdit(
        val reminderId: String? = null,
        val initialLatitude: Double? = null,
        val initialLongitude: Double? = null,
        val initialPlaceName: String? = null,
        val initialAddress: String? = null,
        val initialIconKey: String? = null,
        val initialRadiusMeters: Int? = null,
    ) : AppRoutes

    @Serializable
    data class PlaceReminderPlacePicker(
        val reminderId: String? = null,
        val createEditOnComplete: Boolean = false,
    ) : AppRoutes

    @Serializable
    data class PlaceReminderDetail(val reminderId: String) : AppRoutes

}
