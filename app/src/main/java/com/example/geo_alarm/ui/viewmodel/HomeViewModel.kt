package com.example.geo_alarm.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geo_alarm.data.Alarm
import com.example.geo_alarm.data.AlarmRepository
import com.example.geo_alarm.service.GeoAlarmService
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val alarms: List<Alarm> = emptyList(),
    val showEditDisabledDialog: Boolean = false,
    val showSingleAlarmDialog: Boolean = false,
    val showBackgroundPermissionDialog: Boolean = false,
    val showNotificationPermissionDialog: Boolean = false,
    val showAlreadyAtDestinationDialog: Boolean = false,
    val showLanguageSheet: Boolean = false
)

class HomeViewModel(
    application: Application,
    private val repository: AlarmRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val alarms = repository.allAlarms

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

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

    fun showAlreadyAtDestinationDialog() {
        _uiState.value = _uiState.value.copy(showAlreadyAtDestinationDialog = true)
    }

    fun dismissAlreadyAtDestinationDialog() {
        _uiState.value = _uiState.value.copy(showAlreadyAtDestinationDialog = false)
    }

    fun showLanguageSheet() {
        _uiState.value = _uiState.value.copy(showLanguageSheet = true)
    }

    fun dismissLanguageSheet() {
        _uiState.value = _uiState.value.copy(showLanguageSheet = false)
    }

    // Alarm operations
    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.delete(alarm)
        }
    }

    fun restoreAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.insert(alarm)
        }
    }

    fun enableAlarm(alarm: Alarm, alarms: List<Alarm>, context: Context) {
        // Check if any other alarm is enabled
        val anyEnabled = alarms.any { it.isEnabled && it.id != alarm.id }
        if (anyEnabled) {
            showSingleAlarmDialog()
            return
        }

        try {
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
                            putExtra(GeoAlarmService.EXTRA_START_LAT, location.latitude)
                            putExtra(GeoAlarmService.EXTRA_START_LNG, location.longitude)
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // Handle permission exception
        }
    }

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
}
