package com.github.jimmy90109.geoalarm.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jimmy90109.geoalarm.ui.components.LocationOnboardingScene
import com.github.jimmy90109.geoalarm.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    showAnalyticsOptIn: Boolean,
    onFinished: () -> Unit,
) {
    val analyticsEnabled = viewModel.analyticsEnabled.collectAsStateWithLifecycle()
    val currentLanguage = viewModel.currentLanguage

    LocationOnboardingScene(
        modifier = Modifier.fillMaxSize(),
        isDarkMode = isSystemInDarkTheme(),
        currentLanguage = currentLanguage,
        onToggleLanguage = viewModel::toggleLanguage,
        showAnalyticsOptIn = showAnalyticsOptIn,
        analyticsEnabled = analyticsEnabled.value,
        onAnalyticsEnabledChange = viewModel::setAnalyticsEnabled,
        onAnimationFinished = {
            viewModel.completeOnboarding(
                trackAnalyticsOptIn = showAnalyticsOptIn,
                onCompleted = onFinished
            )
        },
    )
}
