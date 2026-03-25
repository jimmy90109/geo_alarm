package com.github.jimmy90109.geoalarm.appactions

object AppActionContract {
    const val ACTION_CREATE_GEO_ALARM = "com.github.jimmy90109.geoalarm.action.CREATE_GEO_ALARM"
    const val ACTION_CREATE_SCHEDULE = "com.github.jimmy90109.geoalarm.action.CREATE_SCHEDULE"

    const val EXTRA_NAME = "name"
    const val EXTRA_LOCATION_QUERY = "location_query"
    const val EXTRA_RADIUS_METERS = "radius_meters"

    const val EXTRA_ALARM_NAME = "alarm_name"
    const val EXTRA_DAYS_OF_WEEK = "days_of_week"
    const val EXTRA_TIME = "time"

    const val DEFAULT_RADIUS_METERS = 1000.0
}
