package com.example.geo_alarm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.geo_alarm.ui.theme.GeoAlarmTheme
import com.example.geo_alarm.ui.screens.HomeScreen
import com.example.geo_alarm.ui.screens.AlarmEditScreen

import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.launch
import android.content.Intent


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val app = application as GeoAlarmApplication
        val repository = app.repository

        setContent {
            GeoAlarmTheme {
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            repository = repository,
                            onAddAlarm = { navController.navigate("alarm_edit") },
                            onAlarmClick = { alarm ->
                                navController.navigate("alarm_edit?alarmId=${alarm.id}")
                            }
                        )
                    }
                    
                    composable(
                        route = "alarm_edit?alarmId={alarmId}",
                        arguments = listOf(navArgument("alarmId") { type = NavType.StringType; nullable = true })
                    ) { backStackEntry ->
                        val alarmId = backStackEntry.arguments?.getString("alarmId")
                        AlarmEditScreen(
                            repository = repository,
                            alarmId = alarmId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("alarm_edit") {
                        AlarmEditScreen(
                            repository = repository,
                            alarmId = null,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
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
