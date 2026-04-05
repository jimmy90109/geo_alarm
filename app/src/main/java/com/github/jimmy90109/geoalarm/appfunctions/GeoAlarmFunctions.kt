package com.github.jimmy90109.geoalarm.appfunctions

import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionNotSupportedException
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.service.AppFunction
import com.github.jimmy90109.geoalarm.appactions.AppActionResult
import com.github.jimmy90109.geoalarm.appactions.StartAlarmUseCase

class GeoAlarmFunctions(
    private val startAlarmUseCase: StartAlarmUseCase
) {

    @AppFunction
    suspend fun startAlarm(@Suppress("UNUSED_PARAMETER") appFunctionContext: AppFunctionContext, alarmName: String): String {
        val trimmedName = alarmName.trim()
        if (trimmedName.isEmpty()) {
            throw AppFunctionInvalidArgumentException("alarmName is required")
        }

        return when (val result = startAlarmUseCase(StartAlarmUseCase.Request(trimmedName))) {
            is AppActionResult.Success -> "Started alarm: ${result.value.name}"
            is AppActionResult.Error -> throw AppFunctionNotSupportedException(
                "Failed to start alarm (${result.code}): ${result.message}"
            )
        }
    }
}
