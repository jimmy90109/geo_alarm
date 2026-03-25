package com.github.jimmy90109.geoalarm.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AlarmRepository @Inject constructor(
    private val alarmDao: AlarmDao,
    private val scheduleDao: ScheduleDao
) : AlarmDataRepository {
    override val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()
    override val allSchedules: Flow<List<AlarmSchedule>> = scheduleDao.getAllSchedules()
    override val allSchedulesWithAlarm: Flow<List<ScheduleWithAlarm>> = scheduleDao.getAllSchedulesWithAlarm()

    // Alarm Operations
    override suspend fun getAlarm(id: String): Alarm? {
        return alarmDao.getAlarmById(id)
    }

    override suspend fun getAllAlarmsOneShot(): List<Alarm> {
        return alarmDao.getAllAlarmsOneShot()
    }

    override suspend fun findAlarmsByName(name: String): List<Alarm> {
        return alarmDao.findAlarmsByName(name)
    }

    override suspend fun insert(alarm: Alarm) {
        alarmDao.insertAlarm(alarm)
    }

    override suspend fun delete(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm)
    }

    override suspend fun update(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
    }

    // Schedule Operations
    override fun getSchedulesForAlarm(alarmId: String): Flow<List<AlarmSchedule>> {
        return scheduleDao.getSchedulesForAlarm(alarmId)
    }

    override suspend fun getSchedule(id: String): AlarmSchedule? {
        return scheduleDao.getScheduleById(id)
    }

    override suspend fun insertSchedule(schedule: AlarmSchedule) {
        scheduleDao.insertSchedule(schedule)
    }

    override suspend fun deleteSchedule(schedule: AlarmSchedule) {
        scheduleDao.deleteSchedule(schedule)
    }

    override suspend fun updateSchedule(schedule: AlarmSchedule) {
        scheduleDao.updateSchedule(schedule)
    }

    override suspend fun existsDuplicateSchedule(
        alarmId: String,
        days: Set<Int>,
        hour: Int,
        minute: Int
    ): Boolean {
        return scheduleDao.getSchedulesForAlarmOneShot(alarmId).any { schedule ->
            schedule.daysOfWeek == days && schedule.hour == hour && schedule.minute == minute
        }
    }

    override suspend fun isAlarmUsedInSchedule(alarmId: String): Boolean {
        return scheduleDao.isAlarmUsedInSchedule(alarmId)
    }
}
