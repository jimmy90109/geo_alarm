package com.example.geo_alarm.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface MainRoutes {
    @Serializable
    data object Home : MainRoutes
    
    @Serializable
    data object Settings : MainRoutes
}
