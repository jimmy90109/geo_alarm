package com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun placeReminderLandscapeControlWidth(windowWidth: Dp): Dp {
    val cutout = WindowInsets.displayCutout.asPaddingValues()
    val direction = LocalLayoutDirection.current
    val availableWidth = (windowWidth - cutout.calculateStartPadding(direction) -
        cutout.calculateEndPadding(direction)).coerceAtLeast(0.dp)
    return minOf(360.dp, availableWidth * 0.4f)
}
