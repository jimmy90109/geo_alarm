package com.github.jimmy90109.geoalarm.appactions

import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.location.AlarmActivationPermissionChecker
import javax.inject.Inject

class StartAlarmUseCase @Inject constructor(
    private val repository: AlarmDataRepository,
    private val serviceStarter: AlarmServiceStarter,
    private val permissionChecker: AlarmActivationPermissionChecker
) {

    data class Request(val alarmName: String)

    suspend operator fun invoke(request: Request): AppActionResult<Alarm> {
        val alarmName = request.alarmName.trim()
        if (alarmName.isEmpty()) {
            return AppActionResult.Error(
                code = "ERR_MISSING_PARAMS",
                message = "Missing alarm_name"
            )
        }

        val alarms = repository.findAlarmsByName(alarmName)
        if (alarms.isEmpty()) {
            return AppActionResult.Error(
                code = "ERR_ALARM_NOT_FOUND",
                message = "Alarm not found"
            )
        }
        if (alarms.size > 1) {
            return AppActionResult.Error(
                code = "ERR_ALARM_AMBIGUOUS",
                message = "Multiple alarms found with the same name"
            )
        }

        val targetAlarm = alarms.single()

        if (!permissionChecker.hasPreciseForegroundLocation()) {
            return AppActionResult.Error(
                code = "ERR_PRECISE_LOCATION_PERMISSION_REQUIRED",
                message = "Precise location permission is required to start an alarm"
            )
        }
        if (!permissionChecker.hasBackgroundLocation()) {
            return AppActionResult.Error(
                code = "ERR_BACKGROUND_LOCATION_PERMISSION_REQUIRED",
                message = "Background location permission is required to start an alarm"
            )
        }

        val runningAlarm = repository.getAllAlarmsOneShot().find { it.isEnabled }

        if (runningAlarm?.id == targetAlarm.id) {
            return AppActionResult.Success(targetAlarm)
        }

        if (runningAlarm != null) {
            repository.update(runningAlarm.copy(isEnabled = false))
            serviceStarter.stopCurrentAlarm()
        }

        val enabledAlarm = targetAlarm.copy(isEnabled = true)
        repository.update(enabledAlarm)
        serviceStarter.startAlarm(enabledAlarm)

        return AppActionResult.Success(enabledAlarm)
    }
}
