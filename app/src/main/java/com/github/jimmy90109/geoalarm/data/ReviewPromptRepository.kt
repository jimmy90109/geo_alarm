package com.github.jimmy90109.geoalarm.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.github.jimmy90109.geoalarm.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface ReviewPromptStore {
    suspend fun recordSuccessfulArrivalAndCheckEligibility(): Boolean
    suspend fun reservePromptAttemptIfEligible(): Boolean
}

@Singleton
class ReviewPromptRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val policy: ReviewPromptPolicy,
    private val clock: Clock,
) : ReviewPromptStore {
    override suspend fun recordSuccessfulArrivalAndCheckEligibility(): Boolean = runCatching {
        val nowMillis = clock.millis()
        val firstInstallTimeMillis = getFirstInstallTimeMillis()
        var shouldRequest = false

        context.dataStore.edit { preferences ->
            val previousCount = preferences[SUCCESSFUL_ARRIVAL_TURN_OFF_COUNT_KEY] ?: 0
            val successfulCount = if (previousCount == Int.MAX_VALUE) {
                Int.MAX_VALUE
            } else {
                previousCount + 1
            }
            preferences[SUCCESSFUL_ARRIVAL_TURN_OFF_COUNT_KEY] = successfulCount

            shouldRequest = policy.shouldRequest(
                ReviewPromptEligibility(
                    successfulArrivalTurnOffCount = successfulCount,
                    firstInstallTimeMillis = firstInstallTimeMillis,
                    nowMillis = nowMillis,
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    lastAttemptAtMillis = preferences[REVIEW_LAST_ATTEMPT_AT_KEY],
                    lastAttemptVersionCode = preferences[REVIEW_LAST_ATTEMPT_VERSION_CODE_KEY],
                )
            )

        }

        shouldRequest
    }.getOrDefault(false)

    override suspend fun reservePromptAttemptIfEligible(): Boolean = runCatching {
        val nowMillis = clock.millis()
        val firstInstallTimeMillis = getFirstInstallTimeMillis()
        var reserved = false

        context.dataStore.edit { preferences ->
            reserved = policy.shouldRequest(
                ReviewPromptEligibility(
                    successfulArrivalTurnOffCount =
                        preferences[SUCCESSFUL_ARRIVAL_TURN_OFF_COUNT_KEY] ?: 0,
                    firstInstallTimeMillis = firstInstallTimeMillis,
                    nowMillis = nowMillis,
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    lastAttemptAtMillis = preferences[REVIEW_LAST_ATTEMPT_AT_KEY],
                    lastAttemptVersionCode = preferences[REVIEW_LAST_ATTEMPT_VERSION_CODE_KEY],
                )
            )
            if (reserved) {
                preferences[REVIEW_LAST_ATTEMPT_AT_KEY] = nowMillis
                preferences[REVIEW_LAST_ATTEMPT_VERSION_CODE_KEY] = BuildConfig.VERSION_CODE
            }
        }

        reserved
    }.getOrDefault(false)

    @Suppress("DEPRECATION")
    private fun getFirstInstallTimeMillis(): Long =
        context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime

    private companion object {
        val SUCCESSFUL_ARRIVAL_TURN_OFF_COUNT_KEY =
            intPreferencesKey("successful_arrival_turn_off_count")
        val REVIEW_LAST_ATTEMPT_AT_KEY = longPreferencesKey("review_last_attempt_at")
        val REVIEW_LAST_ATTEMPT_VERSION_CODE_KEY =
            intPreferencesKey("review_last_attempt_version_code")
    }
}
