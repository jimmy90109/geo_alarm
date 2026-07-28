package com.github.jimmy90109.geoalarm.util

data class ReviewPromptLaunchConditions(
    val isActivityResumed: Boolean,
    val isDeviceLocked: Boolean,
    val isHomeListReady: Boolean,
    val hasPendingPrompt: Boolean,
    val isClaimInProgress: Boolean,
)

fun shouldLaunchPendingReview(conditions: ReviewPromptLaunchConditions): Boolean =
    conditions.isActivityResumed &&
        !conditions.isDeviceLocked &&
        conditions.isHomeListReady &&
        conditions.hasPendingPrompt &&
        !conditions.isClaimInProgress
