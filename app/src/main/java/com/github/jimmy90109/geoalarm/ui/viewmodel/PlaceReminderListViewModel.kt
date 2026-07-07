package com.github.jimmy90109.geoalarm.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.data.PlaceReminderDataRepository
import com.github.jimmy90109.geoalarm.data.PlaceReminderWithItems
import com.github.jimmy90109.geoalarm.service.PlaceReminderGeofenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaceReminderPermissionState(
    val hasPreciseLocation: Boolean = false,
    val hasBackgroundLocation: Boolean = false,
    val hasNotifications: Boolean = true,
    val isLocationServiceEnabled: Boolean = true,
) {
    val canEnableReminder: Boolean
        get() = hasPreciseLocation && hasBackgroundLocation && hasNotifications && isLocationServiceEnabled
}

data class PlaceReminderListUiState(
    val isLoading: Boolean = true,
    val reminders: List<PlaceReminderWithItems> = emptyList(),
)

@HiltViewModel
class PlaceReminderListViewModel @Inject constructor(
    application: Application,
    private val repository: PlaceReminderDataRepository,
) : AndroidViewModel(application) {
    val reminders: StateFlow<List<PlaceReminderWithItems>> = repository.allReminders.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val listState: StateFlow<PlaceReminderListUiState> = repository.allReminders
        .map { reminders ->
            PlaceReminderListUiState(
                isLoading = false,
                reminders = reminders,
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            PlaceReminderListUiState(),
        )

    fun permissionState(): PlaceReminderPermissionState =
        permissionState(getApplication())

    fun setEnabled(reminderId: String, enabled: Boolean) {
        val context = getApplication<Application>()
        if (enabled && !permissionState(context).canEnableReminder) return
        viewModelScope.launch {
            repository.setEnabled(reminderId, enabled)
            PlaceReminderGeofenceManager(context, repository).syncReminder(reminderId)
        }
    }

    companion object {
        fun permissionState(context: Context): PlaceReminderPermissionState {
            val precise = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            val locationManager = context.getSystemService(LocationManager::class.java)
            val locationEnabled = locationManager?.isLocationEnabled ?: true
            return PlaceReminderPermissionState(
                hasPreciseLocation = precise,
                hasBackgroundLocation = background,
                hasNotifications = notifications,
                isLocationServiceEnabled = locationEnabled,
            )
        }
    }
}
