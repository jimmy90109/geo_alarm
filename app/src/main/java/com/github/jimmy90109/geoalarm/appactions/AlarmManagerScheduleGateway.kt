package com.github.jimmy90109.geoalarm.appactions

import android.content.Context
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import com.github.jimmy90109.geoalarm.service.ScheduleManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AlarmManagerScheduleGateway @Inject constructor(
    @ApplicationContext context: Context
) : ScheduleGateway {
    private val scheduleManager = ScheduleManager(context)

    override suspend fun setSchedule(schedule: AlarmSchedule) {
        scheduleManager.setSchedule(schedule)
    }
}
