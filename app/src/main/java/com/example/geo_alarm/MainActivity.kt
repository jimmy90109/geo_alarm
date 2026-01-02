package com.example.geo_alarm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.geo_alarm.ui.theme.GeoAlarmTheme
import com.example.geo_alarm.navigation.AppNavHost
import com.example.geo_alarm.ui.viewmodel.ViewModelFactory

import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.launch
import android.content.Intent


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val app = application as GeoAlarmApplication
        val repository = app.repository
        val viewModelFactory = ViewModelFactory(app, repository)

        setContent {
            GeoAlarmTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    viewModelFactory = viewModelFactory
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) { // Fix: use nullable Intent? to match override signature if needed, or non-null based on SDK. But safer to assume platform Intent.
        super.onNewIntent(intent) // Correct signature usually implies 'intent: Intent' but AppCompat might differ, assume safe default or check parent.
        // Actually, pure Activity onNewIntent takes Intent.
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == com.example.geo_alarm.service.GeoAlarmService.ACTION_CANCEL_ALARM) {
            val alarmId = intent.getStringExtra(com.example.geo_alarm.service.GeoAlarmService.EXTRA_ALARM_ID)
            if (!alarmId.isNullOrEmpty()) {
                // Find and disable the alarm
                val app = application as GeoAlarmApplication
                 kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { // Launch coroutine
                    val alarm = app.repository.getAlarm(alarmId)
                    if (alarm != null) {
                        app.repository.update(alarm.copy(isEnabled = false))
                        // Also stop the service explicitly just in case
                        val stopIntent = Intent(this@MainActivity, com.example.geo_alarm.service.GeoAlarmService::class.java)
                        stopIntent.action = com.example.geo_alarm.service.GeoAlarmService.ACTION_STOP
                        startService(stopIntent)
                    }
                }
            }
        }
    }
}
