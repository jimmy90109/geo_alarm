package com.github.jimmy90109.geoalarm.ui.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.BuildConfig
import com.github.jimmy90109.geoalarm.data.AlarmRepository
import com.github.jimmy90109.geoalarm.data.SettingsRepository
import com.github.jimmy90109.geoalarm.data.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val showLanguageSheet: Boolean = false,
    val anyAlarmEnabled: Boolean = false,
)

class SettingsViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val alarmRepository: AlarmRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val updateManager = UpdateManager(application)
    val updateStatus = updateManager.status
    val currentVersion = BuildConfig.VERSION_NAME

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

    // Update Management
    fun checkForUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdates()
        }
    }

    fun downloadUpdate(url: String) {
        viewModelScope.launch {
            updateManager.downloadUpdate(url)
        }
    }

    fun installUpdate(file: java.io.File, context: android.content.Context) {
        val intent = updateManager.getInstallIntent(file)
        val canInstall = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

        if (canInstall) {
            context.startActivity(intent)
        } else {
            // Request permission to install unknown apps
             if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                 val permissionIntent = android.content.Intent(
                     android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                 ).apply {
                     data = android.net.Uri.parse("package:${context.packageName}")
                 }
                 context.startActivity(permissionIntent)
             }
        }
    }

    fun getInstallIntent(file: java.io.File): android.content.Intent =
        updateManager.getInstallIntent(file)

    fun resetUpdateState() {
        updateManager.resetState()
    }

    // UI State Controls
    fun showLanguageSheet() {
        _uiState.value = _uiState.value.copy(showLanguageSheet = true)
    }

    fun dismissLanguageSheet() {
        _uiState.value = _uiState.value.copy(showLanguageSheet = false)
    }
}
