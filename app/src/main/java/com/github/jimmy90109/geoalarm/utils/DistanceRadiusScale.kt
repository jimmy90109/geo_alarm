package com.github.jimmy90109.geoalarm.utils

import com.github.jimmy90109.geoalarm.data.DistanceUnitSystem
import kotlin.math.floor

data class DistanceRadiusScale(
    val valueRange: ClosedFloatingPointRange<Float>,
    val steps: Int,
    private val metersPerUnit: Double,
    private val stepSize: Double,
) {
    fun sliderValue(distanceMeters: Double): Float {
        return (distanceMeters / metersPerUnit).toFloat()
    }

    fun distanceMeters(sliderValue: Float): Float {
        return (sliderValue * metersPerUnit).toFloat()
    }

    fun snapDistanceMeters(distanceMeters: Double): Float {
        val value = distanceMeters / metersPerUnit
        val clamped = value.coerceIn(
            valueRange.start.toDouble(),
            valueRange.endInclusive.toDouble(),
        )
        val stepsFromStart = (clamped - valueRange.start) / stepSize
        val roundedSteps = floor(stepsFromStart + 0.5 + 1e-9)
        val snapped = valueRange.start + roundedSteps * stepSize
        return (snapped.coerceIn(
            valueRange.start.toDouble(),
            valueRange.endInclusive.toDouble(),
        ) * metersPerUnit).toFloat()
    }

    companion object {
        private const val METERS_PER_MILE = 1609.344

        fun forSystem(system: DistanceUnitSystem): DistanceRadiusScale {
            return when (system) {
                DistanceUnitSystem.METRIC -> DistanceRadiusScale(
                    valueRange = 0.5f..5.0f,
                    steps = 44,
                    metersPerUnit = 1000.0,
                    stepSize = 0.1,
                )
                DistanceUnitSystem.IMPERIAL -> DistanceRadiusScale(
                    valueRange = 0.25f..3.0f,
                    steps = 10,
                    metersPerUnit = METERS_PER_MILE,
                    stepSize = 0.25,
                )
            }
        }
    }
}
