package com.github.jimmy90109.geoalarm.data

import kotlinx.coroutines.flow.Flow

interface AlarmDataRepository {
    val allAlarms: Flow<List<Alarm>>
    val allSchedules: Flow<List<AlarmSchedule>>
    val allSchedulesWithAlarm: Flow<List<ScheduleWithAlarm>>

    suspend fun getAlarm(id: String): Alarm?
    suspend fun getAllAlarmsOneShot(): List<Alarm>
    suspend fun findAlarmsByName(name: String): List<Alarm>
    suspend fun insert(alarm: Alarm)
    suspend fun delete(alarm: Alarm)
    suspend fun update(alarm: Alarm)

    fun getSchedulesForAlarm(alarmId: String): Flow<List<AlarmSchedule>>
    suspend fun getSchedule(id: String): AlarmSchedule?
    suspend fun insertSchedule(schedule: AlarmSchedule)
    suspend fun deleteSchedule(schedule: AlarmSchedule)
    suspend fun updateSchedule(schedule: AlarmSchedule)
    suspend fun existsDuplicateSchedule(alarmId: String, days: Set<Int>, hour: Int, minute: Int): Boolean
    suspend fun isAlarmUsedInSchedule(alarmId: String): Boolean
}
