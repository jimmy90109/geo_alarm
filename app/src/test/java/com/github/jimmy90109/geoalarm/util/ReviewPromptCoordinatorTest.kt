package com.github.jimmy90109.geoalarm.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptCoordinatorTest {
    @Test
    fun `multiple marks produce one consumable pending prompt`() {
        val coordinator = ReviewPromptCoordinator()

        coordinator.markPending()
        coordinator.markPending()

        assertTrue(coordinator.hasPending())
        assertTrue(coordinator.consumePending())
        assertFalse(coordinator.consumePending())
        assertFalse(coordinator.hasPending())
    }

    @Test
    fun `new coordinator does not retain pending prompt`() {
        ReviewPromptCoordinator().markPending()

        assertFalse(ReviewPromptCoordinator().hasPending())
    }
}
