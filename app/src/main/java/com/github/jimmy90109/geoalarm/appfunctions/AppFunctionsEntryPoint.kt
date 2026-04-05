package com.github.jimmy90109.geoalarm.appfunctions

import com.github.jimmy90109.geoalarm.appactions.StartAlarmUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppFunctionsEntryPoint {
    fun startAlarmUseCase(): StartAlarmUseCase
}
