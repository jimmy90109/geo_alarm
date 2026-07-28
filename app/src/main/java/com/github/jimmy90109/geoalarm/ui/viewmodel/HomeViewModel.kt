package com.github.jimmy90109.geoalarm.ui.viewmodel

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.BuildConfig
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.ads.AdConsentManager
import com.github.jimmy90109.geoalarm.ads.AdsEligibility
import com.github.jimmy90109.geoalarm.ads.AdsEntitlementRepository
import com.github.jimmy90109.geoalarm.ads.HomeNativeAdManager
import com.github.jimmy90109.geoalarm.ads.HomeNativeAdState
import com.github.jimmy90109.geoalarm.appactions.AlarmTurnOffUseCase
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import com.github.jimmy90109.geoalarm.data.DistanceUnitPreference
import com.github.jimmy90109.geoalarm.data.PaymentShortcut
import com.github.jimmy90109.geoalarm.data.ScheduleWithAlarm
import com.github.jimmy90109.geoalarm.data.SettingsRepository
import com.github.jimmy90109.geoalarm.data.location.AlarmActivationPermissionChecker
import com.github.jimmy90109.geoalarm.service.GeoAlarmService
import com.github.jimmy90109.geoalarm.service.GeoAlarmContract
import com.github.jimmy90109.geoalarm.service.ScheduleManager
import com.github.jimmy90109.geoalarm.util.ExactAlarmPermissionHelper
import com.github.jimmy90109.geoalarm.widget.GeoAlarmGlanceWidget
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val alarms: List<Alarm> = emptyList(),
    val showEditDisabledDialog: Boolean = false,
    val showSingleAlarmDialog: Boolean = false,
    val showBackgroundPermissionDialog: Boolean = false,
    val showPreciseLocationPermissionDialog: Boolean = false,
    val showNotificationPermissionDialog: Boolean = false,
    val showExactAlarmPermissionDialog: Boolean = false,
    val showNotificationRationaleDialog: Boolean = false,
    val showAlreadyAtDestinationDialog: Boolean = false,
    val showDeleteErrorDialog: Boolean = false,
    val showScheduleConflictDialog: Boolean = false,
    val conflictingAlarmId: String? = null, // Alarm ID that schedule tried to enable
    val monitoringProgress: Int = 0,
    val monitoringDistance: Int? = null,
    val testActiveAlarm: Alarm? = null,
    val alarmToDelete: Alarm? = null, // Alarm pending deletion (shows confirmation dialog if not null)
    val highlightedAlarmId: String? = null, // Alarm ID to highlight (flash animation)
    val highlightedScheduleId: String? = null, // Schedule ID to highlight (flash animation)
)

data class HomeListUiState(
    val isLoading: Boolean = true,
    val alarms: List<Alarm> = emptyList(),
    val schedules: List<ScheduleWithAlarm> = emptyList(),
)

sealed interface HomeAction {
    data class ScheduleToggled(val schedule: AlarmSchedule, val isEnabled: Boolean) : HomeAction
    data object EditDisabledDialogRequested : HomeAction
    data object EditDisabledDialogDismissed : HomeAction
    data object SingleAlarmDialogRequested : HomeAction
    data object SingleAlarmDialogDismissed : HomeAction
    data object BackgroundPermissionDialogRequested : HomeAction
    data object BackgroundPermissionDialogDismissed : HomeAction
    data object BackgroundPermissionSettingsRequested : HomeAction
    data object PreciseLocationPermissionDialogDismissed : HomeAction
    data object PreciseLocationPermissionSettingsRequested : HomeAction
    data object ActivationPermissionSettingsReturned : HomeAction
    data object NotificationPermissionDialogRequested : HomeAction
    data object NotificationPermissionDialogDismissed : HomeAction
    data object ExactAlarmPermissionDialogDismissed : HomeAction
    data object ExactAlarmPermissionSettingsRequested : HomeAction
    data object ExactAlarmSettingsReturned : HomeAction
    data object NotificationRationaleDialogRequested : HomeAction
    data object NotificationRationaleDialogDismissed : HomeAction
    data object AlreadyAtDestinationDialogRequested : HomeAction
    data object AlreadyAtDestinationDialogDismissed : HomeAction
    data object DeleteErrorDialogDismissed : HomeAction
    data class AlarmHighlighted(val alarmId: String) : HomeAction
    data class ScheduleHighlighted(val scheduleId: String) : HomeAction
    data object HighlightCleared : HomeAction
    data class AlarmEnableRequested(
        val alarm: Alarm,
        val alarms: List<Alarm>,
        val context: Context
    ) : HomeAction
    data class AlarmDisableRequested(
        val alarm: Alarm,
        val trackArrivedTurnOff: Boolean = false
    ) : HomeAction
    data class PaymentShortcutSelected(val shortcut: PaymentShortcut?) : HomeAction
    data class ScheduleIntentHandled(val alarmId: String) : HomeAction
    data object ScheduleConflictDialogDismissed : HomeAction
    data object ScheduleConflictConfirmed : HomeAction
    data class TestAlarmStarted(val context: Context) : HomeAction
    data object SamsungNowBarPromptHandled : HomeAction
}

/**
 * ViewModel for the Home Screen.
 * Manages alarm list state, dialog visibility states, and core alarm operations (enable/disable/delete).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val repository: AlarmDataRepository,
    private val alarmTurnOffUseCase: AlarmTurnOffUseCase,
    private val settingsRepository: SettingsRepository,
    private val activationPermissionChecker: AlarmActivationPermissionChecker,
    private val adConsentManager: AdConsentManager,
    private val adsEntitlementRepository: AdsEntitlementRepository,
    private val homeNativeAdManager: HomeNativeAdManager,
) : AndroidViewModel(application) {
    private enum class ActivationSettingsKind {
        PRECISE,
        BACKGROUND
    }

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val scheduleManager = ScheduleManager(application)
    private var pendingScheduleToEnable: AlarmSchedule? = null
    private var pendingAlarmToEnable: Alarm? = null
    private var pendingAlarmList: List<Alarm> = emptyList()
    private var pendingActivationSettingsKind: ActivationSettingsKind? = null

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == GeoAlarmService.ACTION_PROGRESS_UPDATE) {
                val progress = intent.getIntExtra(GeoAlarmService.EXTRA_PROGRESS, 0)
                val distance = intent.getIntExtra(GeoAlarmService.EXTRA_REMAINING_DISTANCE, 0)
                Log.d("HomeViewModel", "Received progress: $progress, dist: $distance")
                _uiState.value = _uiState.value.copy(
                    monitoringProgress = progress, monitoringDistance = distance
                )
            } else if (intent?.action == GeoAlarmContract.ACTION_ALARM_STOPPED) {
                val stoppedAlarmId = intent.getStringExtra(GeoAlarmService.EXTRA_ALARM_ID)
                if (stoppedAlarmId == GeoAlarmContract.TEST_ALARM_ID) {
                    _uiState.value = _uiState.value.copy(
                        testActiveAlarm = null,
                        monitoringProgress = 0,
                        monitoringDistance = null,
                    )
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(GeoAlarmService.ACTION_PROGRESS_UPDATE)
            addAction(GeoAlarmContract.ACTION_ALARM_STOPPED)
        }
        ContextCompat.registerReceiver(
            application, progressReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(progressReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        homeNativeAdManager.clear()
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val samsungNowBarPromptHandled: StateFlow<Boolean> =
        settingsRepository.samsungNowBarPromptHandledFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    val alarms = repository.allAlarms.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val schedules = repository.allSchedulesWithAlarm.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val homeListState: StateFlow<HomeListUiState> = combine(
        repository.allAlarms,
        repository.allSchedulesWithAlarm,
    ) { alarms, schedules ->
        HomeListUiState(
            isLoading = false,
            alarms = alarms,
            schedules = schedules,
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HomeListUiState(),
        )
    val paymentShortcut = settingsRepository.paymentShortcutFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )
    val distanceUnitPreference = settingsRepository.distanceUnitPreferenceFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DistanceUnitPreference.AUTO,
    )
    val homeNativeAdState: StateFlow<HomeNativeAdState> = homeNativeAdManager.state

    init {
        viewModelScope.launch {
            combine(
                alarms,
                adConsentManager.state,
                adsEntitlementRepository.hasAdsRemoved,
            ) { alarms, consentState, hasAdsRemoved ->
                AdsEligibility.shouldShowHomeNativeAd(
                    canRequestAds = consentState.canRequestAds,
                    hasAdsRemoved = hasAdsRemoved,
                    hasHomeContent = alarms.isNotEmpty(),
                )
            }
                .distinctUntilChanged()
                .collect { eligible -> homeNativeAdManager.setEligible(eligible) }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.ScheduleToggled -> toggleSchedule(action.schedule, action.isEnabled)
            HomeAction.EditDisabledDialogRequested -> showEditDisabledDialog()
            HomeAction.EditDisabledDialogDismissed -> dismissEditDisabledDialog()
            HomeAction.SingleAlarmDialogRequested -> showSingleAlarmDialog()
            HomeAction.SingleAlarmDialogDismissed -> dismissSingleAlarmDialog()
            HomeAction.BackgroundPermissionDialogRequested -> showBackgroundPermissionDialog()
            HomeAction.BackgroundPermissionDialogDismissed -> dismissBackgroundPermissionDialog()
            HomeAction.BackgroundPermissionSettingsRequested -> hideBackgroundPermissionDialog()
            HomeAction.PreciseLocationPermissionDialogDismissed -> dismissPreciseLocationPermissionDialog()
            HomeAction.PreciseLocationPermissionSettingsRequested -> hidePreciseLocationPermissionDialog()
            HomeAction.ActivationPermissionSettingsReturned -> handleActivationPermissionSettingsReturned()
            HomeAction.NotificationPermissionDialogRequested -> showNotificationPermissionDialog()
            HomeAction.NotificationPermissionDialogDismissed -> dismissNotificationPermissionDialog()
            HomeAction.ExactAlarmPermissionDialogDismissed -> dismissExactAlarmPermissionDialog()
            HomeAction.ExactAlarmPermissionSettingsRequested -> hideExactAlarmPermissionDialog()
            HomeAction.ExactAlarmSettingsReturned -> handleExactAlarmSettingsReturned()
            HomeAction.NotificationRationaleDialogRequested -> showNotificationRationaleDialog()
            HomeAction.NotificationRationaleDialogDismissed -> dismissNotificationRationaleDialog()
            HomeAction.AlreadyAtDestinationDialogRequested -> showAlreadyAtDestinationDialog()
            HomeAction.AlreadyAtDestinationDialogDismissed -> dismissAlreadyAtDestinationDialog()
            HomeAction.DeleteErrorDialogDismissed -> dismissDeleteErrorDialog()
            is HomeAction.AlarmHighlighted -> setHighlightedAlarm(action.alarmId)
            is HomeAction.ScheduleHighlighted -> setHighlightedSchedule(action.scheduleId)
            HomeAction.HighlightCleared -> clearHighlight()
            is HomeAction.AlarmEnableRequested -> enableAlarm(action.alarm, action.alarms, action.context)
            is HomeAction.AlarmDisableRequested -> disableAlarm(
                action.alarm,
                action.trackArrivedTurnOff
            )
            is HomeAction.PaymentShortcutSelected -> setPaymentShortcut(action.shortcut)
            is HomeAction.ScheduleIntentHandled -> handleScheduleIntent(action.alarmId)
            HomeAction.ScheduleConflictDialogDismissed -> dismissScheduleConflictDialog()
            HomeAction.ScheduleConflictConfirmed -> confirmScheduleConflict()
            is HomeAction.TestAlarmStarted -> startTestAlarm(action.context)
            HomeAction.SamsungNowBarPromptHandled -> setSamsungNowBarPromptHandled()
        }
    }

    private fun toggleSchedule(schedule: AlarmSchedule, isEnabled: Boolean) {
        viewModelScope.launch {
            if (isEnabled && !ExactAlarmPermissionHelper.canScheduleExactAlarms(getApplication())) {
                pendingScheduleToEnable = schedule
                _uiState.value = _uiState.value.copy(showExactAlarmPermissionDialog = true)
                return@launch
            }

            val updatedSchedule = schedule.copy(isEnabled = isEnabled)
            repository.updateSchedule(updatedSchedule)
            if (isEnabled) {
                scheduleManager.setSchedule(updatedSchedule)
            } else {
                scheduleManager.cancelSchedule(updatedSchedule)
            }
        }
    }

    // Dialog controls
    private fun showEditDisabledDialog() {
        _uiState.value = _uiState.value.copy(showEditDisabledDialog = true)
    }

    private fun dismissEditDisabledDialog() {
        _uiState.value = _uiState.value.copy(showEditDisabledDialog = false)
    }

    private fun showSingleAlarmDialog() {
        _uiState.value = _uiState.value.copy(showSingleAlarmDialog = true)
    }

    private fun dismissSingleAlarmDialog() {
        _uiState.value = _uiState.value.copy(showSingleAlarmDialog = false)
    }

    private fun showBackgroundPermissionDialog() {
        _uiState.value = _uiState.value.copy(showBackgroundPermissionDialog = true)
    }

    private fun dismissBackgroundPermissionDialog() {
        clearPendingAlarmActivation()
        _uiState.value = _uiState.value.copy(showBackgroundPermissionDialog = false)
    }

    private fun hideBackgroundPermissionDialog() {
        pendingActivationSettingsKind = ActivationSettingsKind.BACKGROUND
        _uiState.value = _uiState.value.copy(showBackgroundPermissionDialog = false)
    }

    private fun dismissPreciseLocationPermissionDialog() {
        clearPendingAlarmActivation()
        _uiState.value = _uiState.value.copy(showPreciseLocationPermissionDialog = false)
    }

    private fun hidePreciseLocationPermissionDialog() {
        pendingActivationSettingsKind = ActivationSettingsKind.PRECISE
        _uiState.value = _uiState.value.copy(showPreciseLocationPermissionDialog = false)
    }

    private fun handleActivationPermissionSettingsReturned() {
        val alarm = pendingAlarmToEnable ?: return
        val alarms = pendingAlarmList
        val settingsKind = pendingActivationSettingsKind ?: return
        val canContinue = when (settingsKind) {
            ActivationSettingsKind.PRECISE ->
                activationPermissionChecker.hasPreciseForegroundLocation()
            ActivationSettingsKind.BACKGROUND ->
                activationPermissionChecker.hasPreciseForegroundLocation() &&
                    activationPermissionChecker.hasBackgroundLocation()
        }
        clearPendingAlarmActivation()
        _uiState.value = _uiState.value.copy(
            showPreciseLocationPermissionDialog = false,
            showBackgroundPermissionDialog = false
        )
        if (canContinue) {
            enableAlarm(alarm, alarms, getApplication())
        }
    }

    private fun savePendingAlarmActivation(alarm: Alarm, alarms: List<Alarm>) {
        pendingAlarmToEnable = alarm
        pendingAlarmList = alarms
    }

    private fun clearPendingAlarmActivation() {
        pendingAlarmToEnable = null
        pendingAlarmList = emptyList()
        pendingActivationSettingsKind = null
    }

    private fun showNotificationPermissionDialog() {
        _uiState.value = _uiState.value.copy(showNotificationPermissionDialog = true)
    }

    private fun dismissNotificationPermissionDialog() {
        _uiState.value = _uiState.value.copy(showNotificationPermissionDialog = false)
    }

    private fun dismissExactAlarmPermissionDialog() {
        pendingScheduleToEnable = null
        _uiState.value = _uiState.value.copy(showExactAlarmPermissionDialog = false)
    }

    private fun hideExactAlarmPermissionDialog() {
        _uiState.value = _uiState.value.copy(showExactAlarmPermissionDialog = false)
    }

    private fun handleExactAlarmSettingsReturned() {
        val schedule = pendingScheduleToEnable ?: return
        pendingScheduleToEnable = null
        _uiState.value = _uiState.value.copy(showExactAlarmPermissionDialog = false)

        if (ExactAlarmPermissionHelper.canScheduleExactAlarms(getApplication())) {
            toggleSchedule(schedule, isEnabled = true)
        }
    }

    private fun showNotificationRationaleDialog() {
        _uiState.value = _uiState.value.copy(showNotificationRationaleDialog = true)
    }

    private fun dismissNotificationRationaleDialog() {
        _uiState.value = _uiState.value.copy(showNotificationRationaleDialog = false)
    }

    private fun showAlreadyAtDestinationDialog() {
        _uiState.value = _uiState.value.copy(showAlreadyAtDestinationDialog = true)
    }

    private fun dismissAlreadyAtDestinationDialog() {
        _uiState.value = _uiState.value.copy(showAlreadyAtDestinationDialog = false)
    }

    private fun dismissDeleteErrorDialog() {
        _uiState.value = _uiState.value.copy(showDeleteErrorDialog = false)
    }

    private fun setHighlightedAlarm(alarmId: String) {
        _uiState.value = _uiState.value.copy(
            highlightedAlarmId = alarmId,
            highlightedScheduleId = null
        )
    }

    private fun setHighlightedSchedule(scheduleId: String) {
        _uiState.value = _uiState.value.copy(
            highlightedScheduleId = scheduleId,
            highlightedAlarmId = null
        )
    }

    private fun clearHighlight() {
        _uiState.value = _uiState.value.copy(
            highlightedAlarmId = null,
            highlightedScheduleId = null
        )
    }

    /**
     * Enables a specific alarm.
     * Checks if other alarms are enabled first (single alarm policy).
     * If enabled, it attempts to fetch the current location to verify if the user is already at the destination.
     * Finally, it starts the foreground service to monitor the alarm.
     *
     * @param alarm The alarm to enable.
     * @param alarms The list of all alarms (used for conflict checking).
     * @param context Context used to start the service.
     */
    private fun enableAlarm(alarm: Alarm, alarms: List<Alarm>, context: Context) {
        // Check if any other alarm is enabled
        val anyEnabled = alarms.any { it.isEnabled && it.id != alarm.id }
        if (anyEnabled) {
            showSingleAlarmDialog()
            return
        }

        if (!activationPermissionChecker.hasPreciseForegroundLocation()) {
            savePendingAlarmActivation(alarm, alarms)
            _uiState.value = _uiState.value.copy(showPreciseLocationPermissionDialog = true)
            return
        }

        // Check notification permission (Android 13+) for non-UI entry points (widget/schedule).
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) {
                showNotificationPermissionDialog()
                return
            }
        }

        if (!activationPermissionChecker.hasBackgroundLocation()) {
            savePendingAlarmActivation(alarm, alarms)
            showBackgroundPermissionDialog()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Check if already at destination
                val destLocation = Location("").apply {
                    latitude = alarm.latitude
                    longitude = alarm.longitude
                }
                val distance = location.distanceTo(destLocation)

                if (distance <= alarm.radius) {
                    showAlreadyAtDestinationDialog()
                } else {
                    proceedEnableAlarm(alarm, context, location)
                }
            } else {
                // Location unknown, just start
                proceedEnableAlarm(alarm, context, null)
            }
        }.addOnFailureListener {
            // Location access failed, just start
            proceedEnableAlarm(alarm, context, null)
        }
    }

    /**
     * Internal helper to commit the alarm enabled state to database and start the monitoring service.
     *
     * @param alarm The alarm to enable.
     * @param context Context used to start the service.
     * @param location The initial location if available (optional).
     */
    private fun proceedEnableAlarm(alarm: Alarm, context: Context, location: Location?) {
        viewModelScope.launch {
            repository.update(alarm.copy(isEnabled = true))
        }

        // Start Service
        val serviceIntent = Intent(context, GeoAlarmService::class.java).apply {
            action = GeoAlarmService.ACTION_START
            putExtra(GeoAlarmService.EXTRA_ALARM_ID, alarm.id)
            putExtra(GeoAlarmService.EXTRA_NAME, alarm.name)
            putExtra(GeoAlarmService.EXTRA_DEST_LAT, alarm.latitude)
            putExtra(GeoAlarmService.EXTRA_DEST_LNG, alarm.longitude)
            putExtra(GeoAlarmService.EXTRA_RADIUS, alarm.radius)
            if (location != null) {
                putExtra(GeoAlarmService.EXTRA_START_LAT, location.latitude)
                putExtra(GeoAlarmService.EXTRA_START_LNG, location.longitude)
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    /**
     * Disables the alarm and stops the monitoring service.
     *
     * @param alarm The alarm to disable.
     */
    private fun disableAlarm(alarm: Alarm, trackArrivedTurnOff: Boolean = false) {
        viewModelScope.launch {
            alarmTurnOffUseCase(alarm.id, trackArrivedTurnOff)
            if (alarm.id == GeoAlarmContract.TEST_ALARM_ID) {
                _uiState.value = _uiState.value.copy(
                    testActiveAlarm = null,
                    monitoringProgress = 0,
                    monitoringDistance = null,
                )
            }
        }
    }

    private fun setPaymentShortcut(shortcut: PaymentShortcut?) {
        viewModelScope.launch {
            settingsRepository.setPaymentShortcut(shortcut)
        }
    }

    private fun setSamsungNowBarPromptHandled() {
        viewModelScope.launch {
            settingsRepository.setSamsungNowBarPromptHandled(true)
        }
    }

    private fun handleScheduleIntent(alarmId: String) {
        viewModelScope.launch {
            val alarm = repository.getAlarm(alarmId) ?: return@launch
            
            // Check for currently enabled alarms directly from DB to avoid race condition
            // (ViewModel state might be empty if just created)
            val allAlarms = repository.getAllAlarmsOneShot()
            val runningAlarm = allAlarms.find { it.isEnabled }
            
            if (runningAlarm != null) {
                if (runningAlarm.id == alarm.id) {
                    // Same alarm already running, do nothing (or refresh UI if needed)
                    return@launch
                } else {
                    // Conflict: Another alarm is running
                    _uiState.value = _uiState.value.copy(
                        showScheduleConflictDialog = true,
                        conflictingAlarmId = alarm.id
                    )
                }
            } else {
                // No alarm running, proceed normally
                // Pass the fresh list from DB to ensure consistency
                enableAlarm(alarm, allAlarms, getApplication())
            }
        }
    }

    private fun dismissScheduleConflictDialog() {
        _uiState.value = _uiState.value.copy(
            showScheduleConflictDialog = false,
            conflictingAlarmId = null
        )
    }

    private fun confirmScheduleConflict() {
        val newAlarmId = _uiState.value.conflictingAlarmId ?: return
        viewModelScope.launch {
            // 1. Get current state from DB
            val allAlarms = repository.getAllAlarmsOneShot()
            
            // 2. Disable running alarm(s)
            val runningAlarm = allAlarms.find { it.isEnabled }
            if (runningAlarm != null) {
                // Manually disable and WAIT for completion
                repository.update(runningAlarm.copy(isEnabled = false))
                
                // Stop Service
                val serviceIntent = Intent(getApplication(), GeoAlarmService::class.java).apply {
                    action = GeoAlarmService.ACTION_STOP
                }
                getApplication<Application>().startService(serviceIntent)
            }
            
            // 3. Enable new alarm
            val newAlarm = repository.getAlarm(newAlarmId)
            if (newAlarm != null) {
                // We create a "simulated" list where the running alarm is already disabled
                // This ensures enableAlarm's internal check passes
                val updatedAlarms = allAlarms.map { 
                    if (it.id == runningAlarm?.id) it.copy(isEnabled = false) else it 
                }
                enableAlarm(newAlarm, updatedAlarms, getApplication())
            }
            
            dismissScheduleConflictDialog()
        }
    }

    /**
     * Start a test alarm that simulates arrival after 10 seconds.
     * Useful for testing without actually moving.
     */
    private fun startTestAlarm(context: Context) {
        if (!BuildConfig.DEBUG || alarms.value.any { it.isEnabled }) return

        val testAlarm = Alarm(
            id = GeoAlarmContract.TEST_ALARM_ID,
            name = context.getString(R.string.test_alarm_name),
            latitude = 0.0,
            longitude = 0.0,
            radius = 100.0,
            isEnabled = true,
        )
        _uiState.value = _uiState.value.copy(
            testActiveAlarm = testAlarm,
            monitoringProgress = 0,
            monitoringDistance = 1000,
        )

        val serviceIntent = Intent(context, GeoAlarmService::class.java).apply {
            action = GeoAlarmService.ACTION_TEST
            putExtra(GeoAlarmService.EXTRA_ALARM_ID, GeoAlarmContract.TEST_ALARM_ID)
            putExtra(GeoAlarmService.EXTRA_NAME, testAlarm.name)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

}
