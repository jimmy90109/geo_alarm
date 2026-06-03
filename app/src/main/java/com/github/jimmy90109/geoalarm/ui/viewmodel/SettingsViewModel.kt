package com.github.jimmy90109.geoalarm.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.BuildConfig
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.AnalyticsPreferencesStore
import com.github.jimmy90109.geoalarm.data.RingtoneSettings
import com.github.jimmy90109.geoalarm.data.SettingsRepository
import com.github.jimmy90109.geoalarm.data.UpdateManager
import com.github.jimmy90109.geoalarm.utils.AudioUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val showLanguageSheet: Boolean = false,
    val showRingtoneSheet: Boolean = false,
    val showAnalyticsSheet: Boolean = false,
    val anyAlarmEnabled: Boolean = false,
    val isPreviewPlaying: Boolean = false,
    val previewingUri: String? = null, // null = default ringtone, or custom URI
    val isPreviewingDefault: Boolean = false, // true if previewing default ringtone
)

sealed interface SettingsAction {
    data class LocaleSelected(val languageTag: String) : SettingsAction
    data object UpdateCheckRequested : SettingsAction
    data object HomeEntryUpdateCheckRequested : SettingsAction
    data class UpdateDownloadRequested(val url: String, val sha256: String) : SettingsAction
    data class UpdateInstallRequested(val apkUri: Uri, val context: Context) : SettingsAction
    data class PendingInstallRetryRequested(val context: Context) : SettingsAction
    data object UpdateStateReset : SettingsAction
    data object LanguageSheetRequested : SettingsAction
    data object LanguageSheetDismissed : SettingsAction
    data object RingtoneSheetRequested : SettingsAction
    data object RingtoneSheetDismissed : SettingsAction
    data object AnalyticsSheetRequested : SettingsAction
    data object AnalyticsSheetDismissed : SettingsAction
    data class RingtoneEnabledChanged(val enabled: Boolean) : SettingsAction
    data class RingtoneSelected(val uri: String?, val name: String?) : SettingsAction
    data class AnalyticsEnabledChanged(val enabled: Boolean) : SettingsAction
    data class PreviewPlayRequested(
        val context: Context,
        val uriString: String? = null,
        val isDefault: Boolean = false
    ) : SettingsAction
    data class PreviewStopRequested(val context: Context? = null) : SettingsAction
}

sealed interface SettingsEffect {
    data class OpenIntent(val intent: Intent) : SettingsEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val analyticsPreferencesStore: AnalyticsPreferencesStore,
    private val alarmRepository: AlarmDataRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SettingsEffect>()
    val effects: SharedFlow<SettingsEffect> = _effects.asSharedFlow()

    private val updateManager = UpdateManager(application)
    val updateStatus = updateManager.status
    val currentVersion = BuildConfig.VERSION_NAME
    private var pendingInstallUri: Uri? = null
    private var hasCheckedUpdatesOnHomeEntry = false

    // Ringtone Settings from DataStore
    val ringtoneSettings: StateFlow<RingtoneSettings> = settingsRepository.ringtoneSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RingtoneSettings()
        )

    val analyticsEnabled: StateFlow<Boolean> = analyticsPreferencesStore.analyticsEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    // Preview player
    private var previewMediaPlayer: MediaPlayer? = null

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

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.LocaleSelected -> setAppLocale(action.languageTag)
            SettingsAction.UpdateCheckRequested -> checkForUpdates()
            SettingsAction.HomeEntryUpdateCheckRequested -> checkForUpdatesOnHomeEntry()
            is SettingsAction.UpdateDownloadRequested -> downloadUpdate(action.url, action.sha256)
            is SettingsAction.UpdateInstallRequested -> installUpdate(action.apkUri, action.context)
            is SettingsAction.PendingInstallRetryRequested -> retryPendingInstallIfPermitted(action.context)
            SettingsAction.UpdateStateReset -> resetUpdateState()
            SettingsAction.LanguageSheetRequested -> showLanguageSheet()
            SettingsAction.LanguageSheetDismissed -> dismissLanguageSheet()
            SettingsAction.RingtoneSheetRequested -> showRingtoneSheet()
            SettingsAction.RingtoneSheetDismissed -> dismissRingtoneSheet()
            SettingsAction.AnalyticsSheetRequested -> showAnalyticsSheet()
            SettingsAction.AnalyticsSheetDismissed -> dismissAnalyticsSheet()
            is SettingsAction.RingtoneEnabledChanged -> setRingtoneEnabled(action.enabled)
            is SettingsAction.RingtoneSelected -> setRingtone(action.uri, action.name)
            is SettingsAction.AnalyticsEnabledChanged -> setAnalyticsEnabled(action.enabled)
            is SettingsAction.PreviewPlayRequested -> playPreview(
                action.context,
                action.uriString,
                action.isDefault
            )
            is SettingsAction.PreviewStopRequested -> stopPreview(action.context)
        }
    }

    private fun setAppLocale(languageTag: String) {
        val appLocale = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(appLocale)
        dismissLanguageSheet()
    }

    // Update Management
    private fun checkForUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdates()
        }
    }

    private fun checkForUpdatesOnHomeEntry() {
        if (hasCheckedUpdatesOnHomeEntry) return
        hasCheckedUpdatesOnHomeEntry = true
        checkForUpdates()
    }

    private fun downloadUpdate(url: String, sha256: String) {
        viewModelScope.launch {
            updateManager.downloadUpdate(url, sha256)
        }
    }

    private fun installUpdate(apkUri: Uri, context: Context) {
        val intent = updateManager.getInstallIntent(apkUri)
        val canInstall = if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

        if (canInstall) {
            pendingInstallUri = null
            viewModelScope.launch {
                _effects.emit(SettingsEffect.OpenIntent(intent))
            }
        } else {
            pendingInstallUri = apkUri
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val permissionIntent = Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                ).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                viewModelScope.launch {
                    _effects.emit(SettingsEffect.OpenIntent(permissionIntent))
                }
            }
        }
    }

    private fun retryPendingInstallIfPermitted(context: Context) {
        val apkUri = pendingInstallUri ?: return
        val canInstall =
            context.packageManager.canRequestPackageInstalls()
        if (canInstall) {
            pendingInstallUri = null
            viewModelScope.launch {
                _effects.emit(SettingsEffect.OpenIntent(updateManager.getInstallIntent(apkUri)))
            }
        }
    }

    private fun resetUpdateState() {
        updateManager.resetState()
    }

    // UI State Controls
    private fun showLanguageSheet() {
        _uiState.value = _uiState.value.copy(showLanguageSheet = true)
    }

    private fun dismissLanguageSheet() {
        _uiState.value = _uiState.value.copy(showLanguageSheet = false)
    }

    // Ringtone Settings Controls
    private fun showRingtoneSheet() {
        _uiState.value = _uiState.value.copy(showRingtoneSheet = true)
    }

    private fun dismissRingtoneSheet() {
        stopPreview()
        _uiState.value = _uiState.value.copy(showRingtoneSheet = false)
    }

    private fun showAnalyticsSheet() {
        _uiState.value = _uiState.value.copy(showAnalyticsSheet = true)
    }

    private fun dismissAnalyticsSheet() {
        _uiState.value = _uiState.value.copy(showAnalyticsSheet = false)
    }

    private fun setRingtoneEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRingtoneEnabled(enabled)
        }
    }

    private fun setRingtone(uri: String?, name: String?) {
        viewModelScope.launch {
            settingsRepository.setRingtone(uri, name)
        }
    }

    private fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            analyticsPreferencesStore.setAnalyticsEnabled(enabled)
        }
    }

    // Preview Controls
    private fun playPreview(context: Context, uriString: String? = null, isDefault: Boolean = false) {
        stopPreview()
        _uiState.value = _uiState.value.copy(
            isPreviewPlaying = true,
            isPreviewingDefault = isDefault
        )
        previewMediaPlayer = AudioUtils.playPreview(context, uriString)
        previewMediaPlayer?.setOnCompletionListener {
            AudioUtils.abandonAudioFocus(context)
            _uiState.value = _uiState.value.copy(
                isPreviewPlaying = false,
                isPreviewingDefault = false
            )
        }
    }

    private fun stopPreview(context: Context? = null) {
        previewMediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        previewMediaPlayer = null
        context?.let { AudioUtils.abandonAudioFocus(it) }
        _uiState.value = _uiState.value.copy(
            isPreviewPlaying = false,
            isPreviewingDefault = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopPreview()
    }
}
