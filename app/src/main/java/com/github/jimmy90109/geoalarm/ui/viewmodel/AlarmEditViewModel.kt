package com.github.jimmy90109.geoalarm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.DEFAULT_ALARM_ICON_KEY
import com.github.jimmy90109.geoalarm.data.location.CurrentLocationRepository
import com.github.jimmy90109.geoalarm.widget.WidgetUpdater
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    val showDeleteConfirmDialog: Boolean = false,
    val currentLocation: LatLng? = null,
    val hasUserInteractedWithMap: Boolean = false
)

sealed interface AlarmEditAction {
    data class LoadAlarm(val alarmId: String?) : AlarmEditAction
    data object MapLoaded : AlarmEditAction
    data object MapInteracted : AlarmEditAction
    data class PositionSelected(val latLng: LatLng) : AlarmEditAction
    data class SearchPositionSelected(val latLng: LatLng, val placeName: String) : AlarmEditAction
    data class RadiusChanged(val radius: Float) : AlarmEditAction
    data class NameChanged(val name: String) : AlarmEditAction
    data class IconSelected(val iconKey: String) : AlarmEditAction
    data object NextClicked : AlarmEditAction
    data object BackToMapClicked : AlarmEditAction
    data object SaveClicked : AlarmEditAction
    data object DeleteRequested : AlarmEditAction
    data object DeleteConfirmed : AlarmEditAction
    data object DeleteDialogDismissed : AlarmEditAction
    data object DeleteErrorDismissed : AlarmEditAction
}

sealed interface AlarmEditEffect {
    data class NavigateBack(val savedAlarmId: String?) : AlarmEditEffect
}

@HiltViewModel
class AlarmEditViewModel @Inject constructor(
    private val repository: AlarmDataRepository,
    private val widgetUpdater: WidgetUpdater,
    private val currentLocationRepository: CurrentLocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmEditUiState())
    val uiState: StateFlow<AlarmEditUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AlarmEditEffect>()
    val effects: SharedFlow<AlarmEditEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            currentLocationRepository.currentLocation.collect { location ->
                _uiState.value = _uiState.value.copy(currentLocation = location)
            }
        }
        viewModelScope.launch {
            currentLocationRepository.warmUp()
        }
    }

    fun onAction(action: AlarmEditAction) {
        when (action) {
            is AlarmEditAction.LoadAlarm -> loadAlarm(action.alarmId)
            AlarmEditAction.MapLoaded -> setMapLoaded()
            AlarmEditAction.MapInteracted -> markMapInteracted()
            is AlarmEditAction.PositionSelected -> updatePosition(action.latLng)
            is AlarmEditAction.SearchPositionSelected -> updatePositionFromSearch(
                action.latLng,
                action.placeName
            )
            is AlarmEditAction.RadiusChanged -> updateRadius(action.radius)
            is AlarmEditAction.NameChanged -> updateName(action.name)
            is AlarmEditAction.IconSelected -> selectIcon(action.iconKey)
            AlarmEditAction.NextClicked -> goToDetailsStep()
            AlarmEditAction.BackToMapClicked -> goToMapStep()
            AlarmEditAction.SaveClicked -> saveAlarm()
            AlarmEditAction.DeleteRequested -> requestDeleteAlarm()
            AlarmEditAction.DeleteConfirmed -> confirmDeleteAlarm()
            AlarmEditAction.DeleteDialogDismissed -> dismissDeleteConfirmDialog()
            AlarmEditAction.DeleteErrorDismissed -> dismissDeleteErrorDialog()
        }
    }

    private fun loadAlarm(alarmId: String?) {
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
                        hasUserInteractedWithMap = true,
                        isLoading = false
                    )
                    return@launch
                }
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun setMapLoaded() {
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    private fun markMapInteracted() {
        _uiState.value = _uiState.value.copy(hasUserInteractedWithMap = true)
    }

    private fun updatePosition(latLng: LatLng) {
        _uiState.value = _uiState.value.copy(
            selectedPosition = latLng,
            searchText = "",
            hasUserInteractedWithMap = true
        )
    }

    private fun updatePositionFromSearch(latLng: LatLng, placeName: String) {
        _uiState.value = _uiState.value.copy(
            selectedPosition = latLng,
            searchText = placeName,
            hasUserInteractedWithMap = true
        )
    }

    private fun updateRadius(radius: Float) {
        _uiState.value = _uiState.value.copy(radius = radius)
    }

    private fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    private fun selectIcon(iconKey: String) {
        _uiState.value = _uiState.value.copy(selectedIconKey = iconKey)
    }

    private fun goToDetailsStep() {
        if (_uiState.value.selectedPosition != null) {
            _uiState.value = _uiState.value.copy(step = AlarmEditStep.DetailsForm)
        }
    }

    private fun goToMapStep() {
        _uiState.value = _uiState.value.copy(step = AlarmEditStep.MapSelection)
    }

    private fun dismissDeleteErrorDialog() {
        _uiState.value = _uiState.value.copy(showDeleteErrorDialog = false)
    }

    private fun dismissDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
    }

    private fun saveAlarm() {
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
            _effects.emit(AlarmEditEffect.NavigateBack(alarmId))
        }
    }

    /**
     * Request to delete the alarm. Shows confirmation or error dialog.
     */
    private fun requestDeleteAlarm() {
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
    private fun confirmDeleteAlarm() {
        val existing = _uiState.value.existingAlarm ?: return
        viewModelScope.launch {
            repository.delete(existing)
            widgetUpdater.refreshAll()
            _uiState.value = _uiState.value.copy(
                isSaved = true,
                showDeleteConfirmDialog = false
            )
            _effects.emit(AlarmEditEffect.NavigateBack(null))
        }
    }
}
