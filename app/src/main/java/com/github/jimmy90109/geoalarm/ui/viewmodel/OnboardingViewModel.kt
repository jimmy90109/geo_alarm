package com.github.jimmy90109.geoalarm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.data.OnboardingRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    fun completeOnboarding(onCompleted: () -> Unit) {
        viewModelScope.launch {
            onboardingRepository.setSeenLocationOnboarding(true)
            onCompleted()
        }
    }
}
