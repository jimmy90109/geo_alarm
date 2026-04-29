package com.github.jimmy90109.geoalarm.di

import android.content.Context
import com.github.jimmy90109.geoalarm.appactions.AlarmManagerScheduleGateway
import com.github.jimmy90109.geoalarm.appactions.AlarmServiceStarter
import com.github.jimmy90109.geoalarm.appactions.AndroidAlarmServiceStarter
import com.github.jimmy90109.geoalarm.appactions.AndroidGeocodingService
import com.github.jimmy90109.geoalarm.appactions.GeocodingService
import com.github.jimmy90109.geoalarm.appactions.ScheduleGateway
import com.github.jimmy90109.geoalarm.analytics.AppAnalytics
import com.github.jimmy90109.geoalarm.analytics.TelemetryDeckAppAnalytics
import com.github.jimmy90109.geoalarm.data.AlarmDao
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.AnalyticsPreferencesRepository
import com.github.jimmy90109.geoalarm.data.AnalyticsPreferencesStore
import com.github.jimmy90109.geoalarm.data.AlarmRepository
import com.github.jimmy90109.geoalarm.data.AppDatabase
import com.github.jimmy90109.geoalarm.data.ScheduleDao
import com.github.jimmy90109.geoalarm.widget.AppWidgetUpdater
import com.github.jimmy90109.geoalarm.widget.WidgetUpdater
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideAlarmDao(database: AppDatabase): AlarmDao = database.alarmDao()

    @Provides
    fun provideScheduleDao(database: AppDatabase): ScheduleDao = database.scheduleDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAlarmDataRepository(
        repository: AlarmRepository
    ): AlarmDataRepository

    @Binds
    @Singleton
    abstract fun bindGeocodingService(
        geocodingService: AndroidGeocodingService
    ): GeocodingService

    @Binds
    @Singleton
    abstract fun bindScheduleGateway(
        scheduleGateway: AlarmManagerScheduleGateway
    ): ScheduleGateway

    @Binds
    @Singleton
    abstract fun bindAlarmServiceStarter(
        alarmServiceStarter: AndroidAlarmServiceStarter
    ): AlarmServiceStarter

    @Binds
    @Singleton
    abstract fun bindWidgetUpdater(
        widgetUpdater: AppWidgetUpdater
    ): WidgetUpdater

    @Binds
    @Singleton
    abstract fun bindAppAnalytics(
        analytics: TelemetryDeckAppAnalytics
    ): AppAnalytics

    @Binds
    @Singleton
    abstract fun bindAnalyticsPreferencesStore(
        repository: AnalyticsPreferencesRepository
    ): AnalyticsPreferencesStore
}
