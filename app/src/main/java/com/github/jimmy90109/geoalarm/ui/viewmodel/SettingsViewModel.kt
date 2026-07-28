package com.github.jimmy90109.geoalarm.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.BuildConfig
import com.github.jimmy90109.geoalarm.ads.AdConsentManager
import com.github.jimmy90109.geoalarm.ads.AdConsentState
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.DistanceUnitPreference
import com.github.jimmy90109.geoalarm.data.PaymentShortcut
import com.github.jimmy90109.geoalarm.data.RingtoneSettings
import com.github.jimmy90109.geoalarm.data.SettingsRepository
import com.github.jimmy90109.geoalarm.utils.AudioUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val showLanguageSheet: Boolean = false,
    val showDistanceUnitSheet: Boolean = false,
    val showRingtoneSheet: Boolean = false,
    val showPaymentShortcutSheet: Boolean = false,
    val showFullScreenIntentSheet: Boolean = false,
    val anyAlarmEnabled: Boolean = false,
    val isPreviewPlaying: Boolean = false,
    val previewingUri: String? = null, // null = default ringtone, or custom URI
    val isPreviewingDefault: Boolean = false, // true if previewing default ringtone
    val isLocaleSwitching: Boolean = false,
)

sealed interface SettingsAction {
    data class LocaleSelected(val languageTag: String) : SettingsAction
    data object LanguageSheetRequested : SettingsAction
    data object LanguageSheetDismissed : SettingsAction
    data object DistanceUnitSheetRequested : SettingsAction
    data object DistanceUnitSheetDismissed : SettingsAction
    data class DistanceUnitSelected(val preference: DistanceUnitPreference) : SettingsAction
    data object RingtoneSheetRequested : SettingsAction
    data object RingtoneSheetDismissed : SettingsAction
    data object PaymentShortcutSheetRequested : SettingsAction
    data object PaymentShortcutSheetDismissed : SettingsAction
    data object FullScreenIntentSheetRequested : SettingsAction
    data object FullScreenIntentSheetDismissed : SettingsAction
    data class RingtoneEnabledChanged(val enabled: Boolean) : SettingsAction
    data class RingtoneSelected(val uri: String?, val name: String?) : SettingsAction
    data class PaymentShortcutSelected(val shortcut: PaymentShortcut?) : SettingsAction
    data class AdPrivacyOptionsRequested(val activity: Activity) : SettingsAction
    data class PreviewPlayRequested(
        val context: Context,
        val uriString: String? = null,
        val isDefault: Boolean = false
    ) : SettingsAction
    data class PreviewStopRequested(val context: Context? = null) : SettingsAction
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val alarmRepository: AlarmDataRepository,
    private val adConsentManager: AdConsentManager,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val currentVersion = BuildConfig.VERSION_NAME

    // Ringtone Settings from DataStore
    val ringtoneSettings: StateFlow<RingtoneSettings> = settingsRepository.ringtoneSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RingtoneSettings()
        )

    val paymentShortcut: StateFlow<PaymentShortcut?> = settingsRepository.paymentShortcutFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val distanceUnitPreference: StateFlow<DistanceUnitPreference> =
        settingsRepository.distanceUnitPreferenceFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DistanceUnitPreference.AUTO,
        )

    val adConsentState: StateFlow<AdConsentState> = adConsentManager.state

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
            SettingsAction.LanguageSheetRequested -> showLanguageSheet()
            SettingsAction.LanguageSheetDismissed -> dismissLanguageSheet()
            SettingsAction.DistanceUnitSheetRequested -> showDistanceUnitSheet()
            SettingsAction.DistanceUnitSheetDismissed -> dismissDistanceUnitSheet()
            is SettingsAction.DistanceUnitSelected -> setDistanceUnitPreference(action.preference)
            SettingsAction.RingtoneSheetRequested -> showRingtoneSheet()
            SettingsAction.RingtoneSheetDismissed -> dismissRingtoneSheet()
            SettingsAction.PaymentShortcutSheetRequested -> showPaymentShortcutSheet()
            SettingsAction.PaymentShortcutSheetDismissed -> dismissPaymentShortcutSheet()
            SettingsAction.FullScreenIntentSheetRequested -> showFullScreenIntentSheet()
            SettingsAction.FullScreenIntentSheetDismissed -> dismissFullScreenIntentSheet()
            is SettingsAction.RingtoneEnabledChanged -> setRingtoneEnabled(action.enabled)
            is SettingsAction.RingtoneSelected -> setRingtone(action.uri, action.name)
            is SettingsAction.PaymentShortcutSelected -> setPaymentShortcut(action.shortcut)
            is SettingsAction.AdPrivacyOptionsRequested ->
                adConsentManager.showPrivacyOptionsForm(action.activity)
            is SettingsAction.PreviewPlayRequested -> playPreview(
                action.context,
                action.uriString,
                action.isDefault
            )
            is SettingsAction.PreviewStopRequested -> stopPreview(action.context)
        }
    }

    private fun setAppLocale(languageTag: String) {
        val currentState = _uiState.value
        if (currentState.isLocaleSwitching) return
        if (languageTag.substringBefore("-") == currentLanguage) {
            dismissLanguageSheet()
            return
        }

        _uiState.value = currentState.copy(
            showLanguageSheet = false,
            isLocaleSwitching = true,
        )
        viewModelScope.launch {
            delay(1000)
            _uiState.value = _uiState.value.copy(isLocaleSwitching = false)
            val appLocale = LocaleListCompat.forLanguageTags(languageTag)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    // UI State Controls
    private fun showLanguageSheet() {
        _uiState.value = _uiState.value.copy(showLanguageSheet = true)
    }

    private fun dismissLanguageSheet() {
        _uiState.value = _uiState.value.copy(showLanguageSheet = false)
    }

    private fun showDistanceUnitSheet() {
        _uiState.value = _uiState.value.copy(showDistanceUnitSheet = true)
    }

    private fun dismissDistanceUnitSheet() {
        _uiState.value = _uiState.value.copy(showDistanceUnitSheet = false)
    }

    // Ringtone Settings Controls
    private fun showRingtoneSheet() {
        _uiState.value = _uiState.value.copy(showRingtoneSheet = true)
    }

    private fun dismissRingtoneSheet() {
        stopPreview()
        _uiState.value = _uiState.value.copy(showRingtoneSheet = false)
    }

    private fun showPaymentShortcutSheet() {
        _uiState.value = _uiState.value.copy(showPaymentShortcutSheet = true)
    }

    private fun dismissPaymentShortcutSheet() {
        _uiState.value = _uiState.value.copy(showPaymentShortcutSheet = false)
    }

    private fun showFullScreenIntentSheet() {
        _uiState.value = _uiState.value.copy(showFullScreenIntentSheet = true)
    }

    private fun dismissFullScreenIntentSheet() {
        _uiState.value = _uiState.value.copy(showFullScreenIntentSheet = false)
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

    private fun setPaymentShortcut(shortcut: PaymentShortcut?) {
        viewModelScope.launch {
            settingsRepository.setPaymentShortcut(shortcut)
        }
    }

    private fun setDistanceUnitPreference(preference: DistanceUnitPreference) {
        dismissDistanceUnitSheet()
        viewModelScope.launch {
            settingsRepository.setDistanceUnitPreference(preference)
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
