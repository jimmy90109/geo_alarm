package com.github.jimmy90109.geoalarm.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jimmy90109.geoalarm.ui.components.LanguageSwitchingOverlay
import com.github.jimmy90109.geoalarm.ui.components.LocationOnboardingScene
import com.github.jimmy90109.geoalarm.ui.viewmodel.OnboardingAction
import com.github.jimmy90109.geoalarm.ui.viewmodel.OnboardingEffect
import com.github.jimmy90109.geoalarm.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    showAnalyticsOptIn: Boolean,
    onFinished: () -> Unit,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.Completed -> onFinished()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LocationOnboardingScene(
            modifier = Modifier.fillMaxSize(),
            isDarkMode = isSystemInDarkTheme(),
            currentLanguage = uiState.value.currentLanguage,
            onToggleLanguage = { viewModel.onAction(OnboardingAction.LanguageToggled) },
            showAnalyticsOptIn = showAnalyticsOptIn,
            analyticsEnabled = uiState.value.analyticsEnabled,
            onAnalyticsEnabledChange = {
                viewModel.onAction(OnboardingAction.AnalyticsEnabledChanged(it))
            },
            onAnimationFinished = {
                viewModel.onAction(OnboardingAction.Completed(trackAnalyticsOptIn = showAnalyticsOptIn))
            },
        )
        LanguageSwitchingOverlay(
            visible = uiState.value.isLocaleSwitching,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
