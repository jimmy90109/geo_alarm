package com.example.geo_alarm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.geo_alarm.data.AlarmRepository

class ViewModelFactory(
    private val application: Application,
    private val repository: AlarmRepository
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
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
