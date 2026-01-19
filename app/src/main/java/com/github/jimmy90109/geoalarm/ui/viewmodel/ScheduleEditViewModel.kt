package com.github.jimmy90109.geoalarm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmRepository
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import com.github.jimmy90109.geoalarm.service.ScheduleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ScheduleEditUiState(
    val selectedAlarmId: String? = null,
    val hour: Int = 8,
    val minute: Int = 0,
    val daysOfWeek: Set<Int> = emptySet(), // 1=Sun, 7=Sat
    val isSaving: Boolean = false,
    val scheduleId: String? = null, // Null if new
    val savedScheduleId: String? = null, // ID of the schedule that was just saved (for highlight animation)
    val showDeleteConfirmDialog: Boolean = false
)

class ScheduleEditViewModel(
    application: Application,
    private val repository: AlarmRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ScheduleEditUiState())
    val uiState: StateFlow<ScheduleEditUiState> = _uiState.asStateFlow()
    
    // ScheduleManager instance
    private val scheduleManager = ScheduleManager(application)

    val alarms: StateFlow<List<Alarm>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadSchedule(scheduleId: String?) {
        if (scheduleId == null) {
            // New Schedule default
            _uiState.value = ScheduleEditUiState()
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

    fun setTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(hour = hour, minute = minute)
    }

    fun toggleDay(day: Int) {
        val current = _uiState.value.daysOfWeek.toMutableSet()
        if (current.contains(day)) {
            current.remove(day)
        } else {
            current.add(day)
        }
        _uiState.value = _uiState.value.copy(daysOfWeek = current)
    }

    fun selectAlarm(alarmId: String) {
        _uiState.value = _uiState.value.copy(selectedAlarmId = alarmId)
    }

    fun saveSchedule(onSuccess: (savedScheduleId: String) -> Unit) {
        val state = _uiState.value
        if (state.selectedAlarmId == null || state.daysOfWeek.isEmpty()) return

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
            onSuccess(scheduleId)
        }
    }
    
    /**
     * Request to delete the schedule. Shows confirmation dialog.
     */
    fun requestDeleteSchedule() {
        if (_uiState.value.scheduleId == null) return
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = true)
    }

    fun dismissDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
    }

    /**
     * Confirm and execute the deletion.
     */
    fun confirmDeleteSchedule(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.scheduleId == null) return
        
        viewModelScope.launch {
            val schedule = repository.getSchedule(state.scheduleId)
            if (schedule != null) {
                 repository.deleteSchedule(schedule)
                 // Cancel in AlarmManager
                 scheduleManager.cancelSchedule(schedule)
                 _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
                 onSuccess()
            }
        }
    }
}
