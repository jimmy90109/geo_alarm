package com.github.jimmy90109.geoalarm.utils

import android.content.Context
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.DistanceUnitSystem
import java.text.NumberFormat
import java.util.Locale

enum class DistanceFormatStyle {
    STANDARD,
    RADIUS,
}

enum class DistanceDisplayUnit {
    METER,
    KILOMETER,
    FOOT,
    MILE,
}

data class FormattedDistance(
    val value: String,
    val unit: DistanceDisplayUnit,
)

object DistanceFormatter {
    private const val METERS_PER_KILOMETER = 1000.0
    private const val FEET_PER_MILE = 5280.0
    private const val METERS_PER_MILE = 1609.344

    fun format(
        context: Context,
        distanceMeters: Double,
        system: DistanceUnitSystem,
        locale: Locale = context.resources.configuration.locales[0],
        style: DistanceFormatStyle = DistanceFormatStyle.STANDARD,
    ): String {
        val formatted = formatValue(distanceMeters, system, locale, style)
        val resource = when (formatted.unit) {
            DistanceDisplayUnit.METER -> R.string.distance_value_meters
            DistanceDisplayUnit.KILOMETER -> R.string.distance_value_kilometers
            DistanceDisplayUnit.FOOT -> R.string.distance_value_feet
            DistanceDisplayUnit.MILE -> R.string.distance_value_miles
        }
        return context.getString(resource, formatted.value)
    }

    fun formatValue(
        distanceMeters: Double,
        system: DistanceUnitSystem,
        locale: Locale = Locale.getDefault(),
        style: DistanceFormatStyle = DistanceFormatStyle.STANDARD,
    ): FormattedDistance {
        val nonNegativeMeters = distanceMeters.coerceAtLeast(0.0)
        return when (system) {
            DistanceUnitSystem.METRIC -> formatMetric(nonNegativeMeters, locale)
            DistanceUnitSystem.IMPERIAL -> formatImperial(nonNegativeMeters, locale, style)
        }
    }

    private fun formatMetric(
        distanceMeters: Double,
        locale: Locale,
    ): FormattedDistance {
        return if (distanceMeters < METERS_PER_KILOMETER) {
            FormattedDistance(formatNumber(distanceMeters, locale, 0), DistanceDisplayUnit.METER)
        } else {
            FormattedDistance(
                formatNumber(distanceMeters / METERS_PER_KILOMETER, locale, 1),
                DistanceDisplayUnit.KILOMETER,
            )
        }
    }

    private fun formatImperial(
        distanceMeters: Double,
        locale: Locale,
        style: DistanceFormatStyle,
    ): FormattedDistance {
        val feet = distanceMeters * FEET_PER_MILE / METERS_PER_MILE
        return if (feet < 1000.0) {
            FormattedDistance(formatNumber(feet, locale, 0), DistanceDisplayUnit.FOOT)
        } else {
            FormattedDistance(
                formatNumber(
                    feet / FEET_PER_MILE,
                    locale,
                    if (style == DistanceFormatStyle.RADIUS) 2 else 1,
                ),
                DistanceDisplayUnit.MILE,
            )
        }
    }

    private fun formatNumber(
        value: Double,
        locale: Locale,
        maximumFractionDigits: Int,
    ): String {
        return NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            this.maximumFractionDigits = maximumFractionDigits
        }.format(value)
    }
}
