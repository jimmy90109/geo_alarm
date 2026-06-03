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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentLanguage: String = "en",
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

    val analyticsEnabled: StateFlow<Boolean> = analyticsPreferencesStore.analyticsEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.AnalyticsEnabledChanged -> setAnalyticsEnabled(action.enabled)
            OnboardingAction.LanguageToggled -> toggleLanguage()
            is OnboardingAction.Completed -> completeOnboarding(action.trackAnalyticsOptIn)
        }
    }

    private fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            analyticsPreferencesStore.setAnalyticsEnabled(enabled)
        }
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
        val nextLanguageTag = if (_uiState.value.currentLanguage == "zh") "en" else "zh-TW"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLanguageTag))
        _uiState.value = _uiState.value.copy(currentLanguage = resolveCurrentLanguage())
    }

    private fun completeOnboarding(trackAnalyticsOptIn: Boolean) {
        viewModelScope.launch {
            if (trackAnalyticsOptIn) {
                telemetryTracker.trackAnalyticsOptInIfNeeded()
            }
            onboardingRepository.setSeenLocationOnboarding(true)
            _effects.emit(OnboardingEffect.Completed)
        }
    }
}
