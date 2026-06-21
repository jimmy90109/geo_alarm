package com.github.jimmy90109.geoalarm.analytics

import javax.inject.Inject

class NoOpAppAnalytics @Inject constructor() : AppAnalytics {
    override suspend fun signal(eventName: String): Boolean = true
}
