package com.github.jimmy90109.geoalarm.analytics

interface AppAnalytics {
    suspend fun signal(eventName: String): Boolean
}
