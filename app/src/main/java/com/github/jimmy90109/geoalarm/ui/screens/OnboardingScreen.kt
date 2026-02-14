package com.github.jimmy90109.geoalarm.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.jimmy90109.geoalarm.ui.components.LocationOnboardingScene
import com.github.jimmy90109.geoalarm.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
) {
    LocationOnboardingScene(
        modifier = Modifier.fillMaxSize(),
        isDarkMode = isSystemInDarkTheme(),
        onAnimationFinished = {
            viewModel.completeOnboarding(onFinished)
        },
    )
}
