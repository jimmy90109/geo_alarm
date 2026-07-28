package com.github.jimmy90109.geoalarm.data

import javax.inject.Inject

data class ReviewPromptEligibility(
    val successfulArrivalTurnOffCount: Int,
    val firstInstallTimeMillis: Long,
    val nowMillis: Long,
    val currentVersionCode: Int,
    val lastAttemptAtMillis: Long?,
    val lastAttemptVersionCode: Int?,
)

class ReviewPromptPolicy @Inject constructor() {
    fun shouldRequest(input: ReviewPromptEligibility): Boolean {
        val installAge = input.nowMillis - input.firstInstallTimeMillis
        val cooldownElapsed = input.lastAttemptAtMillis?.let { lastAttempt ->
            input.nowMillis - lastAttempt >= MIN_ATTEMPT_INTERVAL_MILLIS
        } ?: true

        return input.successfulArrivalTurnOffCount >= MIN_SUCCESSFUL_ARRIVALS &&
            installAge >= MIN_INSTALL_AGE_MILLIS &&
            input.lastAttemptVersionCode != input.currentVersionCode &&
            cooldownElapsed
    }

    companion object {
        const val MIN_SUCCESSFUL_ARRIVALS = 3
        const val MIN_INSTALL_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000
        const val MIN_ATTEMPT_INTERVAL_MILLIS = 90L * 24 * 60 * 60 * 1000
    }
}
