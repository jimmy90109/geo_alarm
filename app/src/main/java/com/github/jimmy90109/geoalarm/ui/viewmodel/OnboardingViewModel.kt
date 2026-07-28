package com.github.jimmy90109.geoalarm.ui.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.data.OnboardingRepository
import com.github.jimmy90109.geoalarm.utils.AppLanguageResolver
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
    val isLocaleSwitching: Boolean = false,
)

sealed interface OnboardingAction {
    data object LanguageToggled : OnboardingAction
    data object Completed : OnboardingAction
}

sealed interface OnboardingEffect {
    data object Completed : OnboardingEffect
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    application: Application,
    private val onboardingRepository: OnboardingRepository,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OnboardingUiState(currentLanguage = resolveCurrentLanguage()))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<OnboardingEffect>()
    val effects: SharedFlow<OnboardingEffect> = _effects.asSharedFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.LanguageToggled -> toggleLanguage()
            OnboardingAction.Completed -> completeOnboarding()
        }
    }

    private fun resolveCurrentLanguage(): String {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val applicationLanguageTags = currentLocales
            .takeUnless { it.isEmpty }
            ?.toLanguageTags()
        val effectiveLanguage = getApplication<Application>()
            .resources
            .configuration
            .locales[0]
            .language

        return AppLanguageResolver.resolve(
            applicationLanguageTags = applicationLanguageTags,
            effectiveLanguage = effectiveLanguage,
        )
    }

    private fun toggleLanguage() {
        val currentState = _uiState.value
        if (currentState.isLocaleSwitching) return

        val currentLanguage = resolveCurrentLanguage()
        val nextLanguageTag = if (currentLanguage == "zh") "en" else "zh-TW"
        _uiState.value = currentState.copy(isLocaleSwitching = true)
        viewModelScope.launch {
            delay(1000)
            _uiState.value = _uiState.value.copy(
                currentLanguage = nextLanguageTag.substringBefore("-"),
                isLocaleSwitching = false,
            )
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLanguageTag))
        }
    }

    private fun completeOnboarding() {
        if (_uiState.value.isLocaleSwitching) return
        viewModelScope.launch {
            onboardingRepository.setSeenLocationOnboarding(true)
            _effects.emit(OnboardingEffect.Completed)
        }
    }
}
