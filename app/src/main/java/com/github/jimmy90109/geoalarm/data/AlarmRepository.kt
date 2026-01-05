package com.github.jimmy90109.geoalarm.data

import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val alarmDao: AlarmDao) {
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()

    suspend fun getAlarm(id: String): Alarm? {
        return alarmDao.getAlarmById(id)
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
}
