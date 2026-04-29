package com.github.jimmy90109.geoalarm

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.jimmy90109.geoalarm.analytics.TelemetryTracker
import com.github.jimmy90109.geoalarm.appactions.AppActionContract
import com.github.jimmy90109.geoalarm.appactions.AppActionParsers
import com.github.jimmy90109.geoalarm.appactions.AppActionResult
import com.github.jimmy90109.geoalarm.appactions.CreateGeoAlarmUseCase
import com.github.jimmy90109.geoalarm.appactions.CreateScheduleUseCase
import com.github.jimmy90109.geoalarm.appactions.StartAlarmUseCase
import com.github.jimmy90109.geoalarm.navigation.AppRoutes
import androidx.navigation.compose.rememberNavController
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.OnboardingRepository
import com.github.jimmy90109.geoalarm.navigation.AppNavHost
import com.github.jimmy90109.geoalarm.service.GeoAlarmService
import com.github.jimmy90109.geoalarm.ui.theme.GeoAlarmTheme
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeViewModel
import com.github.jimmy90109.geoalarm.widget.GeoAlarmGlanceWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        const val ACTION_ENABLE_ALARM_FROM_WIDGET = "ENABLE_ALARM_FROM_WIDGET"
        const val EXTRA_WIDGET_ALARM_ID = "WIDGET_ALARM_ID"
    }

    @Inject
    lateinit var alarmRepository: AlarmDataRepository

    @Inject
    lateinit var onboardingRepository: OnboardingRepository

    @Inject
    lateinit var createGeoAlarmUseCase: CreateGeoAlarmUseCase

    @Inject
    lateinit var createScheduleUseCase: CreateScheduleUseCase

    @Inject
    lateinit var startAlarmUseCase: StartAlarmUseCase

    @Inject
    lateinit var telemetryTracker: TelemetryTracker

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val hasSeenOnboarding = runBlocking {
            onboardingRepository.hasSeenLocationOnboarding()
        }
        runBlocking {
            telemetryTracker.trackAppFirstOpenIfNeeded(isNewOnboardingUser = !hasSeenOnboarding)
        }
        val startDestination = resolveStartDestination(intent, hasSeenOnboarding)

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
        setIntent(intent)
        if (resolveShortcutRoute(intent) != null) {
            recreate()
            return
        }
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == GeoAlarmService.ACTION_CANCEL_ALARM) {
            val alarmId = intent.getStringExtra(GeoAlarmService.EXTRA_ALARM_ID)
            val isArrivedTurnOff = intent.getStringExtra(GeoAlarmService.EXTRA_CANCEL_SOURCE) ==
                GeoAlarmService.CANCEL_SOURCE_ARRIVAL_TURN_OFF
            if (!alarmId.isNullOrEmpty()) {
                // Find and disable the alarm
                CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val alarm = alarmRepository.getAlarm(alarmId)
                    if (alarm != null) {
                        if (isArrivedTurnOff) {
                            telemetryTracker.trackArrivedTurnOff()
                        }
                        alarmRepository.update(alarm.copy(isEnabled = false))
                        // Also stop the service explicitly just in case
                        val stopIntent = Intent(
                            this@MainActivity,
                            GeoAlarmService::class.java,
                        )
                        stopIntent.action = GeoAlarmService.ACTION_STOP
                        startService(stopIntent)
                        CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { GeoAlarmGlanceWidget().updateAll(this@MainActivity) }
                    }
                }
            }
        } else if (intent.action == AppActionContract.ACTION_CREATE_GEO_ALARM) {
            handleCreateGeoAlarmIntent(intent)
        } else if (intent.action == AppActionContract.ACTION_CREATE_SCHEDULE) {
            handleCreateScheduleIntent(intent)
        } else if (intent.action == AppActionContract.ACTION_START_ALARM) {
            handleStartAlarmIntent(intent)
        } else if (intent.action == "ENABLE_ALARM_FROM_SCHEDULE") {
            val alarmId = intent.getStringExtra("ALARM_ID")
            if (!alarmId.isNullOrEmpty()) {
                homeViewModel.handleScheduleIntent(alarmId)
            }
        } else if (intent.action == ACTION_ENABLE_ALARM_FROM_WIDGET) {
            val alarmId = intent.getStringExtra(EXTRA_WIDGET_ALARM_ID)
            if (!alarmId.isNullOrEmpty()) {
                homeViewModel.handleScheduleIntent(alarmId)
                CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { GeoAlarmGlanceWidget().updateAll(this@MainActivity) }
            }
        }
    }

    private fun handleCreateGeoAlarmIntent(intent: Intent) {
        val name = intent.getStringExtra(AppActionContract.EXTRA_NAME)?.trim().orEmpty()
        val locationQuery = intent.getStringExtra(AppActionContract.EXTRA_LOCATION_QUERY)?.trim().orEmpty()
        val radius = intent.getDoubleExtra(AppActionContract.EXTRA_RADIUS_METERS, -1.0)
            .takeIf { it > 0 }

        CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val result = createGeoAlarmUseCase(
                CreateGeoAlarmUseCase.Request(
                    name = name,
                    locationQuery = locationQuery,
                    radiusMeters = radius
                )
            )
            runOnUiThread {
                when (result) {
                    is AppActionResult.Success -> {
                        logAndNotify("APP_ACTION_CREATE_GEO_ALARM_SUCCESS", "Created alarm: ${result.value.name}")
                    }

                    is AppActionResult.Error -> {
                        logAndNotify(result.code, result.message)
                    }
                }
            }
        }
    }

    private fun handleCreateScheduleIntent(intent: Intent) {
        val alarmName = intent.getStringExtra(AppActionContract.EXTRA_ALARM_NAME)?.trim().orEmpty()
        val daysFromArray = intent.getStringArrayListExtra(AppActionContract.EXTRA_DAYS_OF_WEEK).orEmpty()
        val daysFromString = AppActionParsers.parseDays(intent.getStringExtra(AppActionContract.EXTRA_DAYS_OF_WEEK))
        val parsedDays = AppActionParsers.parseDays(daysFromArray) + daysFromString
        val parsedTime = AppActionParsers.parseTime(intent.getStringExtra(AppActionContract.EXTRA_TIME))

        if (parsedTime == null) {
            logAndNotify("ERR_INVALID_TIME", "Invalid time format")
            return
        }

        CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val result = createScheduleUseCase(
                CreateScheduleUseCase.Request(
                    alarmName = alarmName,
                    daysOfWeek = parsedDays,
                    time = parsedTime
                )
            )
            runOnUiThread {
                when (result) {
                    is AppActionResult.Success -> {
                        logAndNotify("APP_ACTION_CREATE_SCHEDULE_SUCCESS", "Created schedule for $alarmName")
                    }

                    is AppActionResult.Error -> {
                        logAndNotify(result.code, result.message)
                    }
                }
            }
        }
    }

    private fun logAndNotify(code: String, message: String) {
        Log.i(TAG, "AppActionResult[$code]: $message")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleStartAlarmIntent(intent: Intent) {
        val alarmName = intent.getStringExtra(AppActionContract.EXTRA_ALARM_NAME)?.trim().orEmpty()

        CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val result = startAlarmUseCase(StartAlarmUseCase.Request(alarmName))
            runOnUiThread {
                when (result) {
                    is AppActionResult.Success -> {
                        logAndNotify("APP_ACTION_START_ALARM_SUCCESS", "Started alarm: ${result.value.name}")
                        CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { GeoAlarmGlanceWidget().updateAll(this@MainActivity) }
                    }

                    is AppActionResult.Error -> {
                        logAndNotify(result.code, result.message)
                    }
                }
            }
        }
    }

    private fun resolveStartDestination(intent: Intent, hasSeenOnboarding: Boolean): AppRoutes {
        if (!hasSeenOnboarding) return AppRoutes.Onboarding
        return resolveShortcutRoute(intent) ?: AppRoutes.Main
    }

    private fun resolveShortcutRoute(intent: Intent): AppRoutes? {
        val data = intent.data ?: return null
        if (!isShortcutDeepLink(data)) return null

        return when (data.path) {
            "/add-alarm" -> AppRoutes.AlarmEdit()
            "/add-schedule" -> AppRoutes.ScheduleEdit()
            else -> null
        }
    }

    private fun isShortcutDeepLink(uri: Uri): Boolean {
        return uri.scheme == "geoalarm" && uri.host == "shortcut"
    }
}
