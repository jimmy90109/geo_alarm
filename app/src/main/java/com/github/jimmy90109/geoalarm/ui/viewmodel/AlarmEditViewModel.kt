package com.github.jimmy90109.geoalarm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.DEFAULT_ALARM_ICON_KEY
import com.github.jimmy90109.geoalarm.widget.WidgetUpdater
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class AlarmEditStep {
    MapSelection,
    DetailsForm
}

data class AlarmEditUiState(
    val selectedPosition: LatLng? = null,
    val radius: Float = 1000f,
    val name: String = "",
    val searchText: String = "",
    val selectedIconKey: String = DEFAULT_ALARM_ICON_KEY,
    val step: AlarmEditStep = AlarmEditStep.MapSelection,
    val isLoading: Boolean = true,
    val existingAlarm: Alarm? = null,
    val isSaved: Boolean = false,
    val savedAlarmId: String? = null, // ID of the alarm that was just saved (for highlight animation)
    val showDeleteErrorDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false
)

@HiltViewModel
class AlarmEditViewModel @Inject constructor(
    private val repository: AlarmDataRepository,
    private val widgetUpdater: WidgetUpdater
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
                        selectedIconKey = alarm.iconKey,
                        step = AlarmEditStep.MapSelection,
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

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun selectIcon(iconKey: String) {
        _uiState.value = _uiState.value.copy(selectedIconKey = iconKey)
    }

    fun goToDetailsStep() {
        if (_uiState.value.selectedPosition != null) {
            _uiState.value = _uiState.value.copy(step = AlarmEditStep.DetailsForm)
        }
    }

    fun goToMapStep() {
        _uiState.value = _uiState.value.copy(step = AlarmEditStep.MapSelection)
    }

    fun dismissDeleteErrorDialog() {
        _uiState.value = _uiState.value.copy(showDeleteErrorDialog = false)
    }

    fun dismissDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
    }

    fun saveAlarm() {
        val state = _uiState.value
        val position = state.selectedPosition ?: return
        val name = state.name.trim()
        if (name.isBlank()) return
        val existing = state.existingAlarm

        viewModelScope.launch {
            val alarmId: String
            if (existing != null) {
                // Update existing alarm
                alarmId = existing.id
                val updatedAlarm = existing.copy(
                    name = name,
                    latitude = position.latitude,
                    longitude = position.longitude,
                    radius = state.radius.toDouble(),
                    iconKey = state.selectedIconKey
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
                    radius = state.radius.toDouble(),
                    isEnabled = false,
                    iconKey = state.selectedIconKey
                )
                repository.insert(newAlarm)
            }
            widgetUpdater.refreshAll()
            _uiState.value = _uiState.value.copy(
                isSaved = true,
                savedAlarmId = alarmId,
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
            widgetUpdater.refreshAll()
            _uiState.value = _uiState.value.copy(
                isSaved = true,
                showDeleteConfirmDialog = false
            )
        }
    }
}
