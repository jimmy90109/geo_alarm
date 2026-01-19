package com.github.jimmy90109.geoalarm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AlarmEditUiState(
    val selectedPosition: LatLng? = null,
    val radius: Float = 1000f,
    val name: String = "",
    val searchText: String = "",
    val isLoading: Boolean = true,
    val showNameDialog: Boolean = false,
    val existingAlarm: Alarm? = null,
    val isSaved: Boolean = false,
    val savedAlarmId: String? = null, // ID of the alarm that was just saved (for highlight animation)
    val showDeleteErrorDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false
)

class AlarmEditViewModel(
    private val repository: AlarmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmEditUiState())
    val uiState: StateFlow<AlarmEditUiState> = _uiState.asStateFlow()

    fun loadAlarm(alarmId: String?) {
        viewModelScope.launch {
            if (alarmId != null) {
                val alarm = repository.getAlarm(alarmId)
                if (alarm != null) {
                    _uiState.value = _uiState.value.copy(
                        existingAlarm = alarm,
                        selectedPosition = LatLng(alarm.latitude, alarm.longitude),
                        radius = alarm.radius.toFloat(),
                        name = alarm.name,
                        isLoading = false
                    )
                    return@launch
                }
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun setMapLoaded() {
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    fun updatePosition(latLng: LatLng) {
        _uiState.value = _uiState.value.copy(
            selectedPosition = latLng,
            searchText = ""
        )
    }

    fun updatePositionFromSearch(latLng: LatLng, placeName: String) {
        _uiState.value = _uiState.value.copy(
            selectedPosition = latLng,
            searchText = placeName
        )
    }

    fun updateRadius(radius: Float) {
        _uiState.value = _uiState.value.copy(radius = radius)
    }

    fun showNameDialog() {
        _uiState.value = _uiState.value.copy(showNameDialog = true)
    }

    fun dismissNameDialog() {
        _uiState.value = _uiState.value.copy(showNameDialog = false)
    }

    fun dismissDeleteErrorDialog() {
        _uiState.value = _uiState.value.copy(showDeleteErrorDialog = false)
    }

    fun dismissDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
    }

    fun saveAlarm(name: String) {
        val position = _uiState.value.selectedPosition ?: return
        val existing = _uiState.value.existingAlarm

        viewModelScope.launch {
            val alarmId: String
            if (existing != null) {
                // Update existing alarm
                alarmId = existing.id
                val updatedAlarm = existing.copy(
                    name = name,
                    latitude = position.latitude,
                    longitude = position.longitude,
                    radius = _uiState.value.radius.toDouble()
                )
                repository.update(updatedAlarm)
            } else {
                // Create new alarm
                alarmId = UUID.randomUUID().toString()
                val newAlarm = Alarm(
                    id = alarmId,
                    name = name,
                    latitude = position.latitude,
                    longitude = position.longitude,
                    radius = _uiState.value.radius.toDouble(),
                    isEnabled = false
                )
                repository.insert(newAlarm)
            }
            _uiState.value = _uiState.value.copy(
                isSaved = true,
                savedAlarmId = alarmId,
                showNameDialog = false
            )
        }
    }

    /**
     * Request to delete the alarm. Shows confirmation or error dialog.
     */
    fun requestDeleteAlarm() {
        val existing = _uiState.value.existingAlarm ?: return
        viewModelScope.launch {
            // Check if alarm is used in any schedule
            val isUsedInSchedule = repository.isAlarmUsedInSchedule(existing.id)
            if (isUsedInSchedule) {
                _uiState.value = _uiState.value.copy(showDeleteErrorDialog = true)
            } else {
                _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = true)
            }
        }
    }

    /**
     * Confirm and execute the deletion.
     */
    fun confirmDeleteAlarm() {
        val existing = _uiState.value.existingAlarm ?: return
        viewModelScope.launch {
            repository.delete(existing)
            _uiState.value = _uiState.value.copy(
                isSaved = true,
                showDeleteConfirmDialog = false
            )
        }
    }
}
