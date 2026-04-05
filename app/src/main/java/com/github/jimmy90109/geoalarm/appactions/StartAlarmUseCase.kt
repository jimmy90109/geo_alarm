package com.github.jimmy90109.geoalarm.appactions

import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import javax.inject.Inject

class StartAlarmUseCase @Inject constructor(
    private val repository: AlarmDataRepository,
    private val serviceStarter: AlarmServiceStarter
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
