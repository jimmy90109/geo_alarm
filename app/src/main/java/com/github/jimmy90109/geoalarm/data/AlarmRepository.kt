package com.github.jimmy90109.geoalarm.data

import kotlinx.coroutines.flow.Flow

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val scheduleDao: ScheduleDao
) {
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()
    val allSchedules: Flow<List<AlarmSchedule>> = scheduleDao.getAllSchedules()
    val allSchedulesWithAlarm: Flow<List<ScheduleWithAlarm>> = scheduleDao.getAllSchedulesWithAlarm()

    // Alarm Operations
    suspend fun getAlarm(id: String): Alarm? {
        return alarmDao.getAlarmById(id)
    }

    suspend fun getAllAlarmsOneShot(): List<Alarm> {
        return alarmDao.getAllAlarmsOneShot()
    }

    suspend fun insert(alarm: Alarm) {
        alarmDao.insertAlarm(alarm)
    }

    suspend fun delete(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm)
    }

    suspend fun update(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
    }

    // Schedule Operations
    fun getSchedulesForAlarm(alarmId: String): Flow<List<AlarmSchedule>> {
        return scheduleDao.getSchedulesForAlarm(alarmId)
    }

    suspend fun getSchedule(id: String): AlarmSchedule? {
        return scheduleDao.getScheduleById(id)
    }

    suspend fun insertSchedule(schedule: AlarmSchedule) {
        scheduleDao.insertSchedule(schedule)
    }

    suspend fun deleteSchedule(schedule: AlarmSchedule) {
        scheduleDao.deleteSchedule(schedule)
    }

    suspend fun updateSchedule(schedule: AlarmSchedule) {
        scheduleDao.updateSchedule(schedule)
    }

    suspend fun isAlarmUsedInSchedule(alarmId: String): Boolean {
        return scheduleDao.isAlarmUsedInSchedule(alarmId)
    }
}
