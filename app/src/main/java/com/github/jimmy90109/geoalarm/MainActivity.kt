package com.github.jimmy90109.geoalarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.jimmy90109.geoalarm.navigation.AppRoutes
import androidx.navigation.compose.rememberNavController
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.OnboardingRepository
import com.github.jimmy90109.geoalarm.navigation.AppNavHost
import com.github.jimmy90109.geoalarm.service.GeoAlarmService
import com.github.jimmy90109.geoalarm.ui.theme.GeoAlarmTheme
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var alarmRepository: AlarmDataRepository

    @Inject
    lateinit var onboardingRepository: OnboardingRepository

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val hasSeenOnboarding = runBlocking {
            onboardingRepository.hasSeenLocationOnboarding()
        }
        val startDestination = if (hasSeenOnboarding) {
            AppRoutes.Main
        } else {
            AppRoutes.Onboarding
        }

        setContent {
            GeoAlarmTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination,
                )

            }
        }

        if (savedInstanceState == null) {
            handleIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == GeoAlarmService.ACTION_CANCEL_ALARM) {
            val alarmId = intent.getStringExtra(GeoAlarmService.EXTRA_ALARM_ID)
            if (!alarmId.isNullOrEmpty()) {
                // Find and disable the alarm
                CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val alarm = alarmRepository.getAlarm(alarmId)
                    if (alarm != null) {
                        alarmRepository.update(alarm.copy(isEnabled = false))
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
        } else if (intent.action == "ENABLE_ALARM_FROM_SCHEDULE") {
            val alarmId = intent.getStringExtra("ALARM_ID")
            if (!alarmId.isNullOrEmpty()) {
                homeViewModel.handleScheduleIntent(alarmId)
            }
        }
    }
}
