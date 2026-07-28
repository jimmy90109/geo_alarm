package com.github.jimmy90109.geoalarm.appactions

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.service.GeoAlarmContract
import com.github.jimmy90109.geoalarm.service.GeoAlarmService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class AndroidAlarmServiceStarter @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AlarmServiceStarter {

    override suspend fun stopCurrentAlarm(alarmId: String): Boolean =
        withTimeoutOrNull(STOP_ACK_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                var registered = true
                lateinit var receiver: BroadcastReceiver

                fun unregisterReceiver() {
                    if (!registered) return
                    registered = false
                    runCatching { context.unregisterReceiver(receiver) }
                }

                receiver = object : BroadcastReceiver() {
                    override fun onReceive(receiverContext: Context?, intent: Intent?) {
                        val stoppedAlarmId = intent
                            ?.getStringExtra(GeoAlarmService.EXTRA_ALARM_ID)
                        if (intent?.action != GeoAlarmContract.ACTION_ALARM_STOPPED ||
                            stoppedAlarmId != alarmId
                        ) {
                            return
                        }
                        unregisterReceiver()
                        if (continuation.isActive) continuation.resume(true)
                    }
                }

                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    IntentFilter(GeoAlarmContract.ACTION_ALARM_STOPPED),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                continuation.invokeOnCancellation { unregisterReceiver() }

                runCatching {
                    context.startService(
                        Intent(context, GeoAlarmService::class.java).apply {
                            action = GeoAlarmService.ACTION_STOP
                            putExtra(GeoAlarmService.EXTRA_ALARM_ID, alarmId)
                        }
                    )
                }.onFailure {
                    unregisterReceiver()
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        } ?: false

    override fun startAlarm(alarm: Alarm) {
        val serviceIntent = Intent(context, GeoAlarmService::class.java).apply {
            action = GeoAlarmService.ACTION_START
            putExtra(GeoAlarmService.EXTRA_ALARM_ID, alarm.id)
            putExtra(GeoAlarmService.EXTRA_NAME, alarm.name)
            putExtra(GeoAlarmService.EXTRA_DEST_LAT, alarm.latitude)
            putExtra(GeoAlarmService.EXTRA_DEST_LNG, alarm.longitude)
            putExtra(GeoAlarmService.EXTRA_RADIUS, alarm.radius)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private companion object {
        const val STOP_ACK_TIMEOUT_MILLIS = 2_000L
    }
}
