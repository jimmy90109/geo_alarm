package com.github.jimmy90109.geoalarm.ui.viewmodel

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteConstraintException
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmRepository
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import com.github.jimmy90109.geoalarm.service.GeoAlarmService
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val alarms: List<Alarm> = emptyList(),
    val showEditDisabledDialog: Boolean = false,
    val showSingleAlarmDialog: Boolean = false,
    val showBackgroundPermissionDialog: Boolean = false,
    val showNotificationPermissionDialog: Boolean = false,
    val showNotificationRationaleDialog: Boolean = false,
    val showAlreadyAtDestinationDialog: Boolean = false,
    val showDeleteErrorDialog: Boolean = false,
    val showScheduleConflictDialog: Boolean = false,
    val conflictingAlarmId: String? = null, // Alarm ID that schedule tried to enable
    val monitoringProgress: Int = 0,
    val monitoringDistance: Int? = null,
    val alarmToDelete: Alarm? = null, // Alarm pending deletion (shows confirmation dialog if not null)
    val highlightedAlarmId: String? = null, // Alarm ID to highlight (flash animation)
    val highlightedScheduleId: String? = null, // Schedule ID to highlight (flash animation)
)

/**
 * ViewModel for the Home Screen.
 * Manages alarm list state, dialog visibility states, and core alarm operations (enable/disable/delete).
 */
class HomeViewModel(
    application: Application,
    private val repository: AlarmRepository,
) : AndroidViewModel(application) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == GeoAlarmService.ACTION_PROGRESS_UPDATE) {
                val progress = intent.getIntExtra(GeoAlarmService.EXTRA_PROGRESS, 0)
                val distance = intent.getIntExtra(GeoAlarmService.EXTRA_REMAINING_DISTANCE, 0)
                Log.d("HomeViewModel", "Received progress: $progress, dist: $distance")
                _uiState.value = _uiState.value.copy(
                    monitoringProgress = progress, monitoringDistance = distance
                )
            }
        }
    }

    init {
        val filter = IntentFilter(GeoAlarmService.ACTION_PROGRESS_UPDATE)
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
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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

    fun toggleSchedule(schedule: AlarmSchedule, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateSchedule(schedule.copy(isEnabled = isEnabled))
        }
    }

    // Dialog controls
    fun showEditDisabledDialog() {
        _uiState.value = _uiState.value.copy(showEditDisabledDialog = true)
    }

    fun dismissEditDisabledDialog() {
        _uiState.value = _uiState.value.copy(showEditDisabledDialog = false)
    }

    fun showSingleAlarmDialog() {
        _uiState.value = _uiState.value.copy(showSingleAlarmDialog = true)
    }

    fun dismissSingleAlarmDialog() {
        _uiState.value = _uiState.value.copy(showSingleAlarmDialog = false)
    }

    fun showBackgroundPermissionDialog() {
        _uiState.value = _uiState.value.copy(showBackgroundPermissionDialog = true)
    }

    fun dismissBackgroundPermissionDialog() {
        _uiState.value = _uiState.value.copy(showBackgroundPermissionDialog = false)
    }

    fun showNotificationPermissionDialog() {
        _uiState.value = _uiState.value.copy(showNotificationPermissionDialog = true)
    }

    fun dismissNotificationPermissionDialog() {
        _uiState.value = _uiState.value.copy(showNotificationPermissionDialog = false)
    }

    fun showNotificationRationaleDialog() {
        _uiState.value = _uiState.value.copy(showNotificationRationaleDialog = true)
    }

    fun dismissNotificationRationaleDialog() {
        _uiState.value = _uiState.value.copy(showNotificationRationaleDialog = false)
    }

    fun showAlreadyAtDestinationDialog() {
        _uiState.value = _uiState.value.copy(showAlreadyAtDestinationDialog = true)
    }

    fun dismissAlreadyAtDestinationDialog() {
        _uiState.value = _uiState.value.copy(showAlreadyAtDestinationDialog = false)
    }

    fun dismissDeleteErrorDialog() {
        _uiState.value = _uiState.value.copy(showDeleteErrorDialog = false)
    }

    fun setHighlightedAlarm(alarmId: String) {
        _uiState.value = _uiState.value.copy(
            highlightedAlarmId = alarmId,
            highlightedScheduleId = null
        )
    }

    fun setHighlightedSchedule(scheduleId: String) {
        _uiState.value = _uiState.value.copy(
            highlightedScheduleId = scheduleId,
            highlightedAlarmId = null
        )
    }

    fun clearHighlight() {
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
    fun enableAlarm(alarm: Alarm, alarms: List<Alarm>, context: Context) {
        // Check if any other alarm is enabled
        val anyEnabled = alarms.any { it.isEnabled && it.id != alarm.id }
        if (anyEnabled) {
            showSingleAlarmDialog()
            return
        }

        // Check location to see if already at destination
        if (ContextCompat.checkSelfPermission(
                context, ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                context, ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            proceedEnableAlarm(alarm, context, null)
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
     * @param context Context used to stop the service.
     */
    fun disableAlarm(alarm: Alarm, context: Context) {
        viewModelScope.launch {
            repository.update(alarm.copy(isEnabled = false))
        }

        // Stop Service
        val serviceIntent = Intent(context, GeoAlarmService::class.java).apply {
            action = GeoAlarmService.ACTION_STOP
        }
        context.startService(serviceIntent)
    }

    fun handleScheduleIntent(alarmId: String) {
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

    fun dismissScheduleConflictDialog() {
        _uiState.value = _uiState.value.copy(
            showScheduleConflictDialog = false,
            conflictingAlarmId = null
        )
    }

    fun confirmScheduleConflict() {
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
    fun startTestAlarm(context: Context) {
        val serviceIntent = Intent(context, GeoAlarmService::class.java).apply {
            action = GeoAlarmService.ACTION_TEST
            putExtra(GeoAlarmService.EXTRA_ALARM_ID, "test")
            putExtra(GeoAlarmService.EXTRA_NAME, "Test Alarm")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}

