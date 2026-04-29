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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val analyticsPreferencesStore: AnalyticsPreferencesStore,
    private val telemetryTracker: TelemetryTracker
) : ViewModel() {

    val analyticsEnabled: StateFlow<Boolean> = analyticsPreferencesStore.analyticsEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            analyticsPreferencesStore.setAnalyticsEnabled(enabled)
        }
    }

    val currentLanguage: String
        get() {
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            return if (!currentLocales.isEmpty) {
                currentLocales.toLanguageTags().split("-")[0]
            } else {
                "en"
            }
        }

    fun toggleLanguage() {
        val nextLanguageTag = if (currentLanguage == "zh") "en" else "zh-TW"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLanguageTag))
    }

    fun completeOnboarding(onCompleted: () -> Unit) {
        viewModelScope.launch {
            telemetryTracker.trackAnalyticsOptInIfNeeded()
            onboardingRepository.setSeenLocationOnboarding(true)
            onCompleted()
        }
    }
}
