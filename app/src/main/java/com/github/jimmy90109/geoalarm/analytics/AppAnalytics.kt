package com.github.jimmy90109.geoalarm.analytics

interface AppAnalytics {
    fun signal(eventName: String): Boolean
}
