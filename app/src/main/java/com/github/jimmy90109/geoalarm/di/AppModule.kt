package com.github.jimmy90109.geoalarm.di

import android.content.Context
import com.github.jimmy90109.geoalarm.appactions.AlarmManagerScheduleGateway
import com.github.jimmy90109.geoalarm.appactions.AlarmServiceStarter
import com.github.jimmy90109.geoalarm.appactions.AlarmTurnOffEffects
import com.github.jimmy90109.geoalarm.appactions.AndroidAlarmTurnOffEffects
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
import com.github.jimmy90109.geoalarm.data.location.AndroidCurrentLocationClient
import com.github.jimmy90109.geoalarm.data.location.AndroidAlarmActivationPermissionChecker
import com.github.jimmy90109.geoalarm.data.location.AndroidLocationPermissionChecker
import com.github.jimmy90109.geoalarm.data.location.AlarmActivationPermissionChecker
import com.github.jimmy90109.geoalarm.data.location.CurrentLocationClient
import com.github.jimmy90109.geoalarm.data.location.CurrentLocationRepository
import com.github.jimmy90109.geoalarm.data.location.DefaultCurrentLocationRepository
import com.github.jimmy90109.geoalarm.data.location.ElapsedRealtimeNanosProvider
import com.github.jimmy90109.geoalarm.data.location.LocationPermissionChecker
import com.github.jimmy90109.geoalarm.data.location.SystemElapsedRealtimeNanosProvider
import com.github.jimmy90109.geoalarm.data.places.AndroidPlaceSearchService
import com.github.jimmy90109.geoalarm.data.places.AndroidPlaceAutocompleteService
import com.github.jimmy90109.geoalarm.data.places.PlaceAutocompleteService
import com.github.jimmy90109.geoalarm.data.places.PlaceSearchService
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
    abstract fun bindAlarmTurnOffEffects(
        effects: AndroidAlarmTurnOffEffects
    ): AlarmTurnOffEffects

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

    @Binds
    @Singleton
    abstract fun bindCurrentLocationRepository(
        repository: DefaultCurrentLocationRepository
    ): CurrentLocationRepository

    @Binds
    @Singleton
    abstract fun bindCurrentLocationClient(
        client: AndroidCurrentLocationClient
    ): CurrentLocationClient

    @Binds
    @Singleton
    abstract fun bindLocationPermissionChecker(
        checker: AndroidLocationPermissionChecker
    ): LocationPermissionChecker

    @Binds
    @Singleton
    abstract fun bindAlarmActivationPermissionChecker(
        checker: AndroidAlarmActivationPermissionChecker
    ): AlarmActivationPermissionChecker

    @Binds
    @Singleton
    abstract fun bindElapsedRealtimeNanosProvider(
        provider: SystemElapsedRealtimeNanosProvider
    ): ElapsedRealtimeNanosProvider

    @Binds
    @Singleton
    abstract fun bindPlaceSearchService(
        service: AndroidPlaceSearchService
    ): PlaceSearchService

    @Binds
    @Singleton
    abstract fun bindPlaceAutocompleteService(
        service: AndroidPlaceAutocompleteService
    ): PlaceAutocompleteService
}
