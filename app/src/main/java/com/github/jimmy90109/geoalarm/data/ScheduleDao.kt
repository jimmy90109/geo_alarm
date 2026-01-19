package com.github.jimmy90109.geoalarm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM alarm_schedules")
    fun getAllSchedules(): Flow<List<AlarmSchedule>>

    @Transaction
    @Query("SELECT * FROM alarm_schedules")
    fun getAllSchedulesWithAlarm(): Flow<List<ScheduleWithAlarm>>

    @Query("SELECT * FROM alarm_schedules WHERE alarmId = :alarmId")
    fun getSchedulesForAlarm(alarmId: String): Flow<List<AlarmSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: AlarmSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: AlarmSchedule)

    @Update
    suspend fun updateSchedule(schedule: AlarmSchedule)
    
    @Query("SELECT * FROM alarm_schedules WHERE id = :id")
    suspend fun getScheduleById(id: String): AlarmSchedule?

    @Query("SELECT EXISTS(SELECT 1 FROM alarm_schedules WHERE alarmId = :alarmId LIMIT 1)")
    suspend fun isAlarmUsedInSchedule(alarmId: String): Boolean
}
