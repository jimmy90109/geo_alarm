package com.github.jimmy90109.geoalarm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.jimmy90109.geoalarm.data.AlarmRepository
import com.github.jimmy90109.geoalarm.data.OnboardingRepository
import com.github.jimmy90109.geoalarm.data.SettingsRepository
import com.github.jimmy90109.geoalarm.utils.SharedPreferenceManager

class ViewModelFactory(
    private val application: Application,
    private val repository: AlarmRepository,
    private val settingsRepository: SettingsRepository,
    private val onboardingRepository: OnboardingRepository,
    private val sharedPreferenceManager: SharedPreferenceManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(application, repository) as T
            }

            modelClass.isAssignableFrom(AlarmEditViewModel::class.java) -> {
                AlarmEditViewModel(repository) as T
            }

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(application, settingsRepository, repository) as T
            }

            modelClass.isAssignableFrom(ScheduleEditViewModel::class.java) -> {
                ScheduleEditViewModel(application, repository, sharedPreferenceManager) as T
            }

            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> {
                OnboardingViewModel(onboardingRepository) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
