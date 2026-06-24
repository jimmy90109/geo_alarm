package com.github.jimmy90109.geoalarm.appactions

import android.content.Context
import android.content.Intent
import com.github.jimmy90109.geoalarm.analytics.TelemetryTracker
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.SettingsRepository
import com.github.jimmy90109.geoalarm.service.GeoAlarmContract
import com.github.jimmy90109.geoalarm.service.GeoAlarmService
import com.github.jimmy90109.geoalarm.utils.PaymentShortcutNotifier
import com.github.jimmy90109.geoalarm.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class AlarmTurnOffUseCase @Inject constructor(
    private val repository: AlarmDataRepository,
    private val effects: AlarmTurnOffEffects,
) {
    suspend operator fun invoke(alarmId: String, trackArrivedTurnOff: Boolean) {
        try {
            if (alarmId != GeoAlarmContract.TEST_ALARM_ID) {
                repository.getAlarm(alarmId)?.let { alarm ->
                    repository.update(alarm.copy(isEnabled = false))
                }
            }

            if (trackArrivedTurnOff) {
                effects.onArrivedTurnOff()
            }
        } finally {
            try {
                effects.stopCurrentAlarm(alarmId)
            } finally {
                effects.refreshWidgets()
            }
        }
    }
}

interface AlarmTurnOffEffects {
    suspend fun onArrivedTurnOff()
    fun stopCurrentAlarm(alarmId: String)
    suspend fun refreshWidgets()
}

class AndroidAlarmTurnOffEffects @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val telemetryTracker: TelemetryTracker,
    private val settingsRepository: SettingsRepository,
    private val alarmServiceStarter: AlarmServiceStarter,
    private val widgetUpdater: WidgetUpdater,
) : AlarmTurnOffEffects {
    override suspend fun onArrivedTurnOff() {
        telemetryTracker.trackArrivedTurnOff()
        settingsRepository.paymentShortcutFlow.first()
            ?.let { PaymentShortcutNotifier.show(context, it) }
    }

    override fun stopCurrentAlarm(alarmId: String) {
        alarmServiceStarter.stopCurrentAlarm()
        context.sendBroadcast(
            Intent(GeoAlarmContract.ACTION_ALARM_STOPPED).apply {
                setPackage(context.packageName)
                putExtra(GeoAlarmService.EXTRA_ALARM_ID, alarmId)
            }
        )
    }

    override suspend fun refreshWidgets() {
        widgetUpdater.refreshAll()
    }
}
