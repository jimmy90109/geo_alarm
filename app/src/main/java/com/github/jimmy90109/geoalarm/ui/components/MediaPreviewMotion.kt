package com.github.jimmy90109.geoalarm.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

internal object MediaPreviewMotion {
    fun <T> spatial(visibilityThreshold: T? = null) = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 600f,
        visibilityThreshold = visibilityThreshold,
    )

    fun <T> effects() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 1000f,
    )

    fun <T> rebound(visibilityThreshold: T? = null) = spring(
        dampingRatio = 0.85f,
        stiffness = 600f,
        visibilityThreshold = visibilityThreshold,
    )
}
