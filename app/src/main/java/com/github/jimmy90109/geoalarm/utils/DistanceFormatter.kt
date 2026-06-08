package com.github.jimmy90109.geoalarm.utils

import java.text.NumberFormat
import java.util.Locale

object DistanceFormatter {
    fun formatMeters(distanceMeters: Int, locale: Locale = Locale.getDefault()): String {
        return NumberFormat.getIntegerInstance(locale).format(distanceMeters)
    }
}
