package com.example.geo_alarm

import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "geo_alarm/notification"
    private lateinit var liveNotificationHelper: LiveNotificationHelper
    private lateinit var vibrationHelper: VibrationHelper

    companion object {
        private var methodChannel: MethodChannel? = null

        fun notifyFlutterAlarmClosed() {
            methodChannel?.invokeMethod("onAlarmButtonClicked", "close_alarm")
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        liveNotificationHelper = LiveNotificationHelper()
        liveNotificationHelper.initialize(this)

        vibrationHelper = VibrationHelper()

        methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        methodChannel?.setMethodCallHandler { call, result ->
            when (call.method) {
                "showLiveNotification" -> {
                    val alarmName = call.argument<String>("alarmName") ?: ""
                    val distance = call.argument<Double>("distance") ?: 0.0
                    val progress = call.argument<Int>("progress") ?: 0

                    // Start foreground service for truly non-dismissible notification
                    val serviceIntent = Intent(this, AlarmForegroundService::class.java).apply {
                        action = AlarmForegroundService.ACTION_START_FOREGROUND
                        putExtra("alarmName", alarmName)
                        putExtra("distance", distance)
                        putExtra("progress", progress)
                    }
                    startForegroundService(serviceIntent)

                    result.success(null)
                }
                "updateLiveNotification" -> {
                    val distance = call.argument<Double>("distance") ?: 0.0
                    val progress = call.argument<Int>("progress") ?: 0
                    val isArrived = call.argument<Boolean>("isArrived") ?: false

                    // Update through foreground service
                    val serviceIntent = Intent(this, AlarmForegroundService::class.java).apply {
                        action = AlarmForegroundService.ACTION_UPDATE_NOTIFICATION
                        putExtra("distance", distance)
                        putExtra("progress", progress)
                        putExtra("isArrived", isArrived)
                    }
                    startService(serviceIntent)

                    result.success(null)
                }
                "triggerAlarmVibration" -> {
                    vibrationHelper.startAlarmVibration(this)
                    result.success(null)
                }
                "hideLiveNotification" -> {
                    // Stop foreground service
                    val serviceIntent = Intent(this, AlarmForegroundService::class.java).apply {
                        action = AlarmForegroundService.ACTION_STOP_FOREGROUND
                    }
                    startService(serviceIntent)

                    vibrationHelper.stopVibration(this)
                    result.success(null)
                }
                "showAlarmNotification" -> {
                    // Legacy method - keep for compatibility
                    val intent = Intent(this, AlarmNotificationService::class.java)
                    startService(intent)
                    result.success(null)
                }
                "stopAlarm" -> {
                    // Legacy method - keep for compatibility
                    val intent = Intent(this, AlarmNotificationService::class.java)
                    intent.action = AlarmNotificationService.ACTION_STOP_ALARM
                    startService(intent)
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        methodChannel = null
    }
}
