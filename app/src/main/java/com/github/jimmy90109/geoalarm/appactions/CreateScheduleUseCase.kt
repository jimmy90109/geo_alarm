package com.github.jimmy90109.geoalarm.appactions

import android.content.Context
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import com.github.jimmy90109.geoalarm.util.ExactAlarmPermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

class CreateScheduleUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: AlarmDataRepository,
    private val scheduleGateway: ScheduleGateway
) {

    data class Request(
        val alarmName: String,
        val daysOfWeek: Set<Int>,
        val time: LocalTime
    )

    suspend operator fun invoke(request: Request): AppActionResult<AlarmSchedule> {
        val alarmName = request.alarmName.trim()
        if (alarmName.isEmpty() || request.daysOfWeek.isEmpty()) {
            return AppActionResult.Error(
                code = "ERR_MISSING_PARAMS",
                message = "Missing alarm_name or days_of_week"
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

        val alarm = alarms.single()
        val hour = request.time.hour
        val minute = request.time.minute

        val isDuplicate = repository.existsDuplicateSchedule(
            alarmId = alarm.id,
            days = request.daysOfWeek,
            hour = hour,
            minute = minute
        )
        if (isDuplicate) {
            return AppActionResult.Error(
                code = "ERR_DUPLICATE_SCHEDULE",
                message = "Schedule already exists"
            )
        }

        if (!ExactAlarmPermissionHelper.canScheduleExactAlarms(context)) {
            return AppActionResult.Error(
                code = "ERR_EXACT_ALARM_PERMISSION_REQUIRED",
                message = "Exact alarm permission is required to create schedules"
            )
        }

        val schedule = AlarmSchedule(
            id = UUID.randomUUID().toString(),
            alarmId = alarm.id,
            daysOfWeek = request.daysOfWeek,
            hour = hour,
            minute = minute,
            isEnabled = true
        )

        repository.insertSchedule(schedule)
        scheduleGateway.setSchedule(schedule)

        return AppActionResult.Success(schedule)
    }
}
