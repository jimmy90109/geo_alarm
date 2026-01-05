package com.github.jimmy90109.geoalarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.rememberNavController
import com.github.jimmy90109.geoalarm.navigation.AppNavHost
import com.github.jimmy90109.geoalarm.service.GeoAlarmService
import com.github.jimmy90109.geoalarm.ui.theme.GeoAlarmTheme
import com.github.jimmy90109.geoalarm.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GeoAlarmApplication
        val repository = app.repository
        val settingsRepository = app.settingsRepository
        val viewModelFactory = ViewModelFactory(app, repository, settingsRepository)

        setContent {
            GeoAlarmTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    viewModelFactory = viewModelFactory,
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
        if (intent.action == GeoAlarmService.ACTION_CANCEL_ALARM) {
            val alarmId = intent.getStringExtra(GeoAlarmService.EXTRA_ALARM_ID)
            if (!alarmId.isNullOrEmpty()) {
                // Find and disable the alarm
                val app = application as GeoAlarmApplication
                CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val alarm = app.repository.getAlarm(alarmId)
                    if (alarm != null) {
                        app.repository.update(alarm.copy(isEnabled = false))
                        // Also stop the service explicitly just in case
                        val stopIntent = Intent(
                            this@MainActivity,
                            GeoAlarmService::class.java,
                        )
                        stopIntent.action = GeoAlarmService.ACTION_STOP
                        startService(stopIntent)
                    }
                }
            }
        }
    }
}
