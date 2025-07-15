package com.example.geo_alarm

import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "geo_alarm/notification"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "showAlarmNotification" -> {
                    val intent = Intent(this, AlarmNotificationService::class.java)
                    startService(intent)
                    result.success(null)
                }
                "stopAlarm" -> {
                    val intent = Intent(this, AlarmNotificationService::class.java)
                    intent.action = AlarmNotificationService.ACTION_STOP_ALARM
                    startService(intent)
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }
}
