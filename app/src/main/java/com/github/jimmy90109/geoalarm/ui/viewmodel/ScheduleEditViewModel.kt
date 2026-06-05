package com.github.jimmy90109.geoalarm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import com.github.jimmy90109.geoalarm.service.ScheduleManager
import com.github.jimmy90109.geoalarm.util.ExactAlarmPermissionHelper
import com.github.jimmy90109.geoalarm.utils.SharedPreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ScheduleEditUiState(
    val selectedAlarmId: String? = null,
    val hour: Int = 8,
    val minute: Int = 0,
    val daysOfWeek: Set<Int> = emptySet(), // 1=Sun, 7=Sat
    val isSaving: Boolean = false,
    val scheduleId: String? = null, // Null if new
    val savedScheduleId: String? = null, // ID of the schedule that was just saved (for highlight animation)
    val showDeleteConfirmDialog: Boolean = false,
    val showOnboarding: Boolean = false,
    val showExactAlarmPermissionDialog: Boolean = false,
)

sealed interface ScheduleEditAction {
    data object OnboardingDismissed : ScheduleEditAction
    data class LoadSchedule(val scheduleId: String?) : ScheduleEditAction
    data class TimeChanged(val hour: Int, val minute: Int) : ScheduleEditAction
    data class DayToggled(val day: Int) : ScheduleEditAction
    data class AlarmSelected(val alarmId: String) : ScheduleEditAction
    data object SaveClicked : ScheduleEditAction
    data object DeleteRequested : ScheduleEditAction
    data object DeleteDialogDismissed : ScheduleEditAction
    data object DeleteConfirmed : ScheduleEditAction
    data object ExactAlarmPermissionDialogDismissed : ScheduleEditAction
    data object ExactAlarmPermissionSettingsRequested : ScheduleEditAction
    data object ExactAlarmSettingsReturned : ScheduleEditAction
}

sealed interface ScheduleEditEffect {
    data class NavigateBack(val savedScheduleId: String?) : ScheduleEditEffect
}

@HiltViewModel
class ScheduleEditViewModel @Inject constructor(
    application: Application,
    private val repository: AlarmDataRepository,
    private val sharedPreferenceManager: SharedPreferenceManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ScheduleEditUiState())
    val uiState: StateFlow<ScheduleEditUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ScheduleEditEffect>()
    val effects: SharedFlow<ScheduleEditEffect> = _effects.asSharedFlow()

    init {
        // Check if user has seen onboarding
        if (!sharedPreferenceManager.hasSeenScheduleOnboarding) {
            _uiState.value = _uiState.value.copy(showOnboarding = true)
        }
    }

    fun onAction(action: ScheduleEditAction) {
        when (action) {
            ScheduleEditAction.OnboardingDismissed -> dismissOnboarding()
            is ScheduleEditAction.LoadSchedule -> loadSchedule(action.scheduleId)
            is ScheduleEditAction.TimeChanged -> setTime(action.hour, action.minute)
            is ScheduleEditAction.DayToggled -> toggleDay(action.day)
            is ScheduleEditAction.AlarmSelected -> selectAlarm(action.alarmId)
            ScheduleEditAction.SaveClicked -> saveSchedule()
            ScheduleEditAction.DeleteRequested -> requestDeleteSchedule()
            ScheduleEditAction.DeleteDialogDismissed -> dismissDeleteConfirmDialog()
            ScheduleEditAction.DeleteConfirmed -> confirmDeleteSchedule()
            ScheduleEditAction.ExactAlarmPermissionDialogDismissed -> dismissExactAlarmPermissionDialog()
            ScheduleEditAction.ExactAlarmPermissionSettingsRequested -> hideExactAlarmPermissionDialog()
            ScheduleEditAction.ExactAlarmSettingsReturned -> handleExactAlarmSettingsReturned()
        }
    }

    private fun dismissOnboarding() {
        sharedPreferenceManager.hasSeenScheduleOnboarding = true
        _uiState.value = _uiState.value.copy(showOnboarding = false)
    }
    
    // ScheduleManager instance
    private val scheduleManager = ScheduleManager(application)
    private var pendingSaveAfterExactAlarmPermission = false

    val alarms: StateFlow<List<Alarm>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun loadSchedule(scheduleId: String?) {
        if (scheduleId == null) {
            // New Schedule default - preserve onboarding state
            val currentShowOnboarding = _uiState.value.showOnboarding
            _uiState.value = ScheduleEditUiState(showOnboarding = currentShowOnboarding)
            return
        }

        viewModelScope.launch {
            val schedule = repository.getSchedule(scheduleId)
            if (schedule != null) {
                _uiState.value = ScheduleEditUiState(
                    selectedAlarmId = schedule.alarmId,
                    hour = schedule.hour,
                    minute = schedule.minute,
                    daysOfWeek = schedule.daysOfWeek,
                    scheduleId = schedule.id
                )
            }
        }
    }

    private fun setTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(hour = hour, minute = minute)
    }

    private fun toggleDay(day: Int) {
        val current = _uiState.value.daysOfWeek.toMutableSet()
        if (current.contains(day)) {
            current.remove(day)
        } else {
            current.add(day)
        }
        _uiState.value = _uiState.value.copy(daysOfWeek = current)
    }

    private fun selectAlarm(alarmId: String) {
        _uiState.value = _uiState.value.copy(selectedAlarmId = alarmId)
    }

    private fun saveSchedule(checkExactAlarmPermission: Boolean = true) {
        val state = _uiState.value
        if (state.selectedAlarmId == null || state.daysOfWeek.isEmpty()) return

        if (checkExactAlarmPermission && !ExactAlarmPermissionHelper.canScheduleExactAlarms(getApplication())) {
            pendingSaveAfterExactAlarmPermission = true
            _uiState.value = _uiState.value.copy(showExactAlarmPermissionDialog = true)
            return
        }

        val scheduleId = state.scheduleId ?: UUID.randomUUID().toString()
        val schedule = AlarmSchedule(
            id = scheduleId,
            alarmId = state.selectedAlarmId,
            daysOfWeek = state.daysOfWeek,
            hour = state.hour,
            minute = state.minute,
            isEnabled = true
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            if (state.scheduleId == null) {
                repository.insertSchedule(schedule)
            } else {
                repository.updateSchedule(schedule)
            }
            // Trigger AlarmManager update
            scheduleManager.setSchedule(schedule)
            
            _uiState.value = _uiState.value.copy(savedScheduleId = scheduleId)
            _effects.emit(ScheduleEditEffect.NavigateBack(scheduleId))
        }
    }

    private fun dismissExactAlarmPermissionDialog() {
        pendingSaveAfterExactAlarmPermission = false
        _uiState.value = _uiState.value.copy(showExactAlarmPermissionDialog = false)
    }

    private fun hideExactAlarmPermissionDialog() {
        _uiState.value = _uiState.value.copy(showExactAlarmPermissionDialog = false)
    }

    private fun handleExactAlarmSettingsReturned() {
        if (!pendingSaveAfterExactAlarmPermission) return

        pendingSaveAfterExactAlarmPermission = false
        _uiState.value = _uiState.value.copy(showExactAlarmPermissionDialog = false)
        if (ExactAlarmPermissionHelper.canScheduleExactAlarms(getApplication())) {
            saveSchedule(checkExactAlarmPermission = false)
        }
    }
    
    /**
     * Request to delete the schedule. Shows confirmation dialog.
     */
    private fun requestDeleteSchedule() {
        if (_uiState.value.scheduleId == null) return
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = true)
    }

    private fun dismissDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
    }

    /**
     * Confirm and execute the deletion.
     */
    private fun confirmDeleteSchedule() {
        val state = _uiState.value
        if (state.scheduleId == null) return
        
        viewModelScope.launch {
            val schedule = repository.getSchedule(state.scheduleId)
            if (schedule != null) {
                 repository.deleteSchedule(schedule)
                 // Cancel in AlarmManager
                 scheduleManager.cancelSchedule(schedule)
                 _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
                 _effects.emit(ScheduleEditEffect.NavigateBack(null))
            }
        }
    }
}
