package com.github.jimmy90109.geoalarm.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptPolicyTest {
    private val policy = ReviewPromptPolicy()
    private val now = 200L * DAY_MILLIS

    @Test
    fun `requires seven full days since install`() {
        assertFalse(policy.shouldRequest(eligibleInput(firstInstallTimeMillis = now - 7 * DAY_MILLIS + 1)))
        assertTrue(policy.shouldRequest(eligibleInput(firstInstallTimeMillis = now - 7 * DAY_MILLIS)))
    }

    @Test
    fun `requires three successful arrival turn offs`() {
        assertFalse(policy.shouldRequest(eligibleInput(successfulArrivalTurnOffCount = 2)))
        assertTrue(policy.shouldRequest(eligibleInput(successfulArrivalTurnOffCount = 3)))
    }

    @Test
    fun `allows only one attempt per version`() {
        assertFalse(policy.shouldRequest(eligibleInput(lastAttemptVersionCode = CURRENT_VERSION)))
        assertTrue(policy.shouldRequest(eligibleInput(lastAttemptVersionCode = CURRENT_VERSION - 1)))
    }

    @Test
    fun `new version still observes ninety day cooldown`() {
        assertFalse(
            policy.shouldRequest(
                eligibleInput(
                    lastAttemptAtMillis = now - 90 * DAY_MILLIS + 1,
                    lastAttemptVersionCode = CURRENT_VERSION - 1,
                )
            )
        )
        assertTrue(
            policy.shouldRequest(
                eligibleInput(
                    lastAttemptAtMillis = now - 90 * DAY_MILLIS,
                    lastAttemptVersionCode = CURRENT_VERSION - 1,
                )
            )
        )
    }

    private fun eligibleInput(
        successfulArrivalTurnOffCount: Int = 3,
        firstInstallTimeMillis: Long = now - 30 * DAY_MILLIS,
        lastAttemptAtMillis: Long? = null,
        lastAttemptVersionCode: Int? = null,
    ) = ReviewPromptEligibility(
        successfulArrivalTurnOffCount = successfulArrivalTurnOffCount,
        firstInstallTimeMillis = firstInstallTimeMillis,
        nowMillis = now,
        currentVersionCode = CURRENT_VERSION,
        lastAttemptAtMillis = lastAttemptAtMillis,
        lastAttemptVersionCode = lastAttemptVersionCode,
    )

    private companion object {
        const val CURRENT_VERSION = 12
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
