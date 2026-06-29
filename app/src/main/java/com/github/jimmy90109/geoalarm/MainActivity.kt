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
import androidx.lifecycle.lifecycleScope
import com.github.jimmy90109.geoalarm.appactions.AlarmTurnOffUseCase
import com.github.jimmy90109.geoalarm.appactions.AppActionContract
import com.github.jimmy90109.geoalarm.appactions.AppActionParsers
import com.github.jimmy90109.geoalarm.appactions.AppActionResult
import com.github.jimmy90109.geoalarm.appactions.CreateGeoAlarmUseCase
import com.github.jimmy90109.geoalarm.appactions.CreateScheduleUseCase
import com.github.jimmy90109.geoalarm.appactions.StartAlarmUseCase
import com.github.jimmy90109.geoalarm.ads.AdConsentManager
import com.github.jimmy90109.geoalarm.navigation.AppRoutes
import androidx.navigation.compose.rememberNavController
import com.github.jimmy90109.geoalarm.data.OnboardingRepository
import com.github.jimmy90109.geoalarm.data.location.CurrentLocationRepository
import com.github.jimmy90109.geoalarm.navigation.AppNavHost
import com.github.jimmy90109.geoalarm.service.GeoAlarmService
import com.github.jimmy90109.geoalarm.share.SharedPlaceParser
import com.github.jimmy90109.geoalarm.ui.theme.GeoAlarmTheme
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeAction
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
    lateinit var onboardingRepository: OnboardingRepository

    @Inject
    lateinit var alarmTurnOffUseCase: AlarmTurnOffUseCase

    @Inject
    lateinit var createGeoAlarmUseCase: CreateGeoAlarmUseCase

    @Inject
    lateinit var createScheduleUseCase: CreateScheduleUseCase

    @Inject
    lateinit var startAlarmUseCase: StartAlarmUseCase

    @Inject
    lateinit var currentLocationRepository: CurrentLocationRepository

    @Inject
    lateinit var adConsentManager: AdConsentManager

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        logSharedText(intent)

        val hasSeenOnboarding = runBlocking {
            onboardingRepository.hasSeenLocationOnboarding()
        }
        val requestedRoute = resolveRequestedRoute(intent)
        val startDestination = resolveStartDestination(hasSeenOnboarding)

        setContent {
            GeoAlarmTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    requestedDestination = requestedRoute,
                )

            }
        }

        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        lifecycleScope.launch {
            currentLocationRepository.warmUp()
        }
        adConsentManager.requestConsentUpdate(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        logSharedText(intent)
        if (resolveRequestedRoute(intent) != null) {
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
                lifecycleScope.launch {
                    alarmTurnOffUseCase(alarmId, isArrivedTurnOff)
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
                homeViewModel.onAction(HomeAction.ScheduleIntentHandled(alarmId))
            }
        } else if (intent.action == ACTION_ENABLE_ALARM_FROM_WIDGET) {
            val alarmId = intent.getStringExtra(EXTRA_WIDGET_ALARM_ID)
            if (!alarmId.isNullOrEmpty()) {
                homeViewModel.onAction(HomeAction.ScheduleIntentHandled(alarmId))
                CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { GeoAlarmGlanceWidget().updateAll(this@MainActivity) }
            }
        } else if (intent.action == Intent.ACTION_SEND &&
            intent.type?.startsWith("text/plain") == true &&
            parseSharedPlace(intent) == null
        ) {
            logAndNotify("INVALID_SHARED_PLACE", getString(R.string.invalid_shared_place))
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

    private fun resolveStartDestination(hasSeenOnboarding: Boolean): AppRoutes {
        if (!hasSeenOnboarding) return AppRoutes.Onboarding()
        return AppRoutes.Main
    }

    private fun resolveRequestedRoute(intent: Intent): AppRoutes? {
        resolveSharedPlaceRoute(intent)?.let { return it }

        val data = intent.data ?: return null
        if (!isShortcutDeepLink(data)) return null

        return when (data.path) {
            "/add-alarm" -> AppRoutes.AlarmEdit()
            "/add-schedule" -> AppRoutes.ScheduleEdit()
            else -> null
        }
    }

    private fun resolveSharedPlaceRoute(intent: Intent): AppRoutes.AlarmEdit? {
        if (intent.action != Intent.ACTION_SEND || intent.type?.startsWith("text/plain") != true) {
            return null
        }
        val sharedPlace = parseSharedPlace(intent) ?: return null
        return AppRoutes.AlarmEdit(
            sharedPlaceQuery = sharedPlace.query,
            sharedPlaceSource = sharedPlace.source
        )
    }

    private fun parseSharedPlace(intent: Intent) =
        SharedPlaceParser.parse(sharedTextCandidates(intent))

    private fun sharedTextCandidates(intent: Intent): List<String> = buildList {
        intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.let(::add)
        intent.getCharSequenceExtra(Intent.EXTRA_TITLE)?.toString()?.let(::add)
        intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()?.let(::add)
        intent.clipData?.let { clipData ->
            for (index in 0 until clipData.itemCount) {
                clipData.getItemAt(index).text?.toString()?.let(::add)
            }
        }
    }.map(String::trim).filter(String::isNotEmpty).distinct()

    private fun logSharedText(intent: Intent) {
        if (
            BuildConfig.DEBUG &&
            intent.action == Intent.ACTION_SEND &&
            intent.type?.startsWith("text/plain") == true
        ) {
            Log.d(
                TAG,
                "Received share: type=${intent.type}, " +
                    "text=${intent.getCharSequenceExtra(Intent.EXTRA_TEXT)}, " +
                    "title=${intent.getCharSequenceExtra(Intent.EXTRA_TITLE)}, " +
                    "subject=${intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)}, " +
                    "clipItems=${intent.clipData?.itemCount ?: 0}, " +
                    "allText=${sharedTextCandidates(intent)}"
            )
        }
    }

    private fun isShortcutDeepLink(uri: Uri): Boolean {
        return uri.scheme == "geoalarm" && uri.host == "shortcut"
    }
}
