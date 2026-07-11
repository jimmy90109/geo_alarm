package com.github.jimmy90109.geoalarm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.github.jimmy90109.geoalarm.analytics.TelemetryTracker
import com.github.jimmy90109.geoalarm.data.AnalyticsPreferencesStore
import com.github.jimmy90109.geoalarm.data.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentLanguage: String = "en",
    val analyticsEnabled: Boolean = false,
    val isLocaleSwitching: Boolean = false,
)

sealed interface OnboardingAction {
    data class AnalyticsEnabledChanged(val enabled: Boolean) : OnboardingAction
    data object LanguageToggled : OnboardingAction
    data class Completed(val trackAnalyticsOptIn: Boolean) : OnboardingAction
}

sealed interface OnboardingEffect {
    data object Completed : OnboardingEffect
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val analyticsPreferencesStore: AnalyticsPreferencesStore,
    private val telemetryTracker: TelemetryTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState(currentLanguage = resolveCurrentLanguage()))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<OnboardingEffect>()
    val effects: SharedFlow<OnboardingEffect> = _effects.asSharedFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.AnalyticsEnabledChanged -> setAnalyticsEnabled(action.enabled)
            OnboardingAction.LanguageToggled -> toggleLanguage()
            is OnboardingAction.Completed -> completeOnboarding(action.trackAnalyticsOptIn)
        }
    }

    private fun setAnalyticsEnabled(enabled: Boolean) {
        if (_uiState.value.isLocaleSwitching) return
        _uiState.value = _uiState.value.copy(analyticsEnabled = enabled)
    }

    private fun resolveCurrentLanguage(): String {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        return if (!currentLocales.isEmpty) {
            currentLocales.toLanguageTags().split("-")[0]
        } else {
            "en"
        }
    }

    private fun toggleLanguage() {
        val currentState = _uiState.value
        if (currentState.isLocaleSwitching) return

        val nextLanguageTag = if (currentState.currentLanguage == "zh") "en" else "zh-TW"
        _uiState.value = currentState.copy(isLocaleSwitching = true)
        viewModelScope.launch {
            delay(1000)
            _uiState.value = _uiState.value.copy(isLocaleSwitching = false)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLanguageTag))
        }
    }

    private fun completeOnboarding(trackAnalyticsOptIn: Boolean) {
        if (_uiState.value.isLocaleSwitching) return
        viewModelScope.launch {
            if (trackAnalyticsOptIn) {
                analyticsPreferencesStore.setAnalyticsEnabled(_uiState.value.analyticsEnabled)
                telemetryTracker.trackAnalyticsOptInIfNeeded()
            }
            onboardingRepository.setSeenLocationOnboarding(true)
            _effects.emit(OnboardingEffect.Completed)
        }
    }
}
