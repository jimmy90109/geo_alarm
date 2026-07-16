package com.github.jimmy90109.geoalarm.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ReliabilityBannerStateTest {
    @Test
    fun `battery warning takes priority over Samsung Now Bar prompt`() {
        assertEquals(
            ReliabilityBannerState.BatteryOptimizationWarning,
            resolveReliabilityBannerState(
                batteryOptimizationState = ReliabilityBannerState.BatteryOptimizationWarning,
                showSamsungNowBarPrompt = true,
            ),
        )
    }

    @Test
    fun `battery success takes priority over Samsung Now Bar prompt`() {
        assertEquals(
            ReliabilityBannerState.BatteryOptimizationSuccess,
            resolveReliabilityBannerState(
                batteryOptimizationState = ReliabilityBannerState.BatteryOptimizationSuccess,
                showSamsungNowBarPrompt = true,
            ),
        )
    }

    @Test
    fun `shows Samsung Now Bar prompt when no battery banner is active`() {
        assertEquals(
            ReliabilityBannerState.SamsungNowBarPrompt,
            resolveReliabilityBannerState(
                batteryOptimizationState = ReliabilityBannerState.Hidden,
                showSamsungNowBarPrompt = true,
            ),
        )
    }

    @Test
    fun `hides reliability banner after Samsung prompt is handled`() {
        assertEquals(
            ReliabilityBannerState.Hidden,
            resolveReliabilityBannerState(
                batteryOptimizationState = ReliabilityBannerState.Hidden,
                showSamsungNowBarPrompt = false,
            ),
        )
    }

    @Test
    fun `keeps Samsung prompt content while banner exits`() {
        assertEquals(
            ReliabilityBannerState.SamsungNowBarPrompt,
            reliabilityBannerStateForDisplay(
                currentState = ReliabilityBannerState.Hidden,
                lastVisibleState = ReliabilityBannerState.SamsungNowBarPrompt,
            ),
        )
    }
}
