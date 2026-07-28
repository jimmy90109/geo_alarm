package com.github.jimmy90109.geoalarm.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptLaunchGateTest {
    @Test
    fun `launches only when activity is unlocked and home list is ready`() {
        assertTrue(shouldLaunchPendingReview(eligibleConditions()))
    }

    @Test
    fun `retains prompt while device is locked`() {
        assertFalse(
            shouldLaunchPendingReview(eligibleConditions().copy(isDeviceLocked = true))
        )
    }

    @Test
    fun `does not launch outside resumed home list`() {
        assertFalse(
            shouldLaunchPendingReview(eligibleConditions().copy(isActivityResumed = false))
        )
        assertFalse(
            shouldLaunchPendingReview(eligibleConditions().copy(isHomeListReady = false))
        )
    }

    @Test
    fun `does not launch without pending prompt or during another claim`() {
        assertFalse(
            shouldLaunchPendingReview(eligibleConditions().copy(hasPendingPrompt = false))
        )
        assertFalse(
            shouldLaunchPendingReview(eligibleConditions().copy(isClaimInProgress = true))
        )
    }

    private fun eligibleConditions() = ReviewPromptLaunchConditions(
        isActivityResumed = true,
        isDeviceLocked = false,
        isHomeListReady = true,
        hasPendingPrompt = true,
        isClaimInProgress = false,
    )
}
