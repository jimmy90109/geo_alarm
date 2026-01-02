package com.example.geo_alarm.ui.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geo_alarm.data.AlarmRepository
import com.example.geo_alarm.data.MonitoringMethod
import com.example.geo_alarm.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val showLanguageSheet: Boolean = false,
    val showMonitoringSheet: Boolean = false,
    val anyAlarmEnabled: Boolean = false
)

class SettingsViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val alarmRepository: AlarmRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Data streams
    val monitoringMethod = settingsRepository.monitoringMethod.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonitoringMethod.GEOFENCE
        )

    init {
        // Observe alarms to update 'anyAlarmEnabled' state
        viewModelScope.launch {
            alarmRepository.allAlarms.collect { alarms ->
                _uiState.value = _uiState.value.copy(
                    anyAlarmEnabled = alarms.any { it.isEnabled })
            }
        }
    }

    // Locale Management
    val currentLanguage: String
        get() {
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            return if (!currentLocales.isEmpty) currentLocales.toLanguageTags()
                .split("-")[0] else "en"
        }

    fun setAppLocale(languageTag: String) {
        val appLocale = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(appLocale)
        dismissLanguageSheet()
    }

    // Monitoring Method Management
    fun setMonitoringMethod(method: MonitoringMethod) {
        viewModelScope.launch {
            settingsRepository.setMonitoringMethod(method)
            dismissMonitoringSheet()
        }
    }

    // UI State Controls
    fun showLanguageSheet() {
        _uiState.value = _uiState.value.copy(showLanguageSheet = true)
    }

    fun dismissLanguageSheet() {
        _uiState.value = _uiState.value.copy(showLanguageSheet = false)
    }

    fun showMonitoringSheet() {
        if (!_uiState.value.anyAlarmEnabled) {
            _uiState.value = _uiState.value.copy(showMonitoringSheet = true)
        }
    }

    fun dismissMonitoringSheet() {
        _uiState.value = _uiState.value.copy(showMonitoringSheet = false)
    }
}
