package com.github.jimmy90109.geoalarm.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface MainRoutes {
    @Serializable
    data object Home : MainRoutes
    
    @Serializable
    data object Settings : MainRoutes
}
