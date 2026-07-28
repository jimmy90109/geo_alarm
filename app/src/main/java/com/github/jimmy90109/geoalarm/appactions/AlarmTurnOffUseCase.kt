package com.github.jimmy90109.geoalarm.appactions

import android.content.Context
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.SettingsRepository
import com.github.jimmy90109.geoalarm.data.ReviewPromptStore
import com.github.jimmy90109.geoalarm.service.GeoAlarmContract
import com.github.jimmy90109.geoalarm.utils.PaymentShortcutNotifier
import com.github.jimmy90109.geoalarm.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AlarmTurnOffResult(
    val shouldRequestInAppReview: Boolean = false,
)

@Singleton
class AlarmTurnOffUseCase @Inject constructor(
    private val repository: AlarmDataRepository,
    private val effects: AlarmTurnOffEffects,
    private val reviewPromptStore: ReviewPromptStore,
) {
    private val turnOffMutex = Mutex()

    suspend operator fun invoke(
        alarmId: String,
        trackArrivedTurnOff: Boolean,
    ): AlarmTurnOffResult = turnOffMutex.withLock {
        var wasEnabled = alarmId == GeoAlarmContract.TEST_ALARM_ID
        var shouldEvaluateReviewPrompt = false
        var cleanupConfirmed = false

        try {
            if (alarmId != GeoAlarmContract.TEST_ALARM_ID) {
                repository.getAlarm(alarmId)?.let { alarm ->
                    wasEnabled = alarm.isEnabled
                    if (alarm.isEnabled) {
                        repository.update(alarm.copy(isEnabled = false))
                    }
                }
            }

            if (trackArrivedTurnOff && wasEnabled) {
                effects.onArrivedTurnOff()
                shouldEvaluateReviewPrompt = alarmId != GeoAlarmContract.TEST_ALARM_ID
            }
        } finally {
            try {
                cleanupConfirmed = effects.stopCurrentAlarm(alarmId)
            } finally {
                effects.refreshWidgets()
            }
        }

        val shouldRequestInAppReview = shouldEvaluateReviewPrompt && cleanupConfirmed && runCatching {
            reviewPromptStore.recordSuccessfulArrivalAndCheckEligibility()
        }.getOrDefault(false)

        AlarmTurnOffResult(shouldRequestInAppReview)
    }
}

interface AlarmTurnOffEffects {
    suspend fun onArrivedTurnOff()
    suspend fun stopCurrentAlarm(alarmId: String): Boolean
    suspend fun refreshWidgets()
}

class AndroidAlarmTurnOffEffects @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val alarmServiceStarter: AlarmServiceStarter,
    private val widgetUpdater: WidgetUpdater,
) : AlarmTurnOffEffects {
    override suspend fun onArrivedTurnOff() {
        settingsRepository.paymentShortcutFlow.first()
            ?.let { PaymentShortcutNotifier.show(context, it) }
    }

    override suspend fun stopCurrentAlarm(alarmId: String): Boolean =
        alarmServiceStarter.stopCurrentAlarm(alarmId)

    override suspend fun refreshWidgets() {
        widgetUpdater.refreshAll()
    }
}
