package com.github.jimmy90109.geoalarm.analytics

import javax.inject.Inject

class NoOpAppAnalytics @Inject constructor() : AppAnalytics {
    override fun signal(eventName: String): Boolean = true
}
