package com.github.jimmy90109.geoalarm.data.location

import com.google.android.gms.maps.model.LatLng
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val elapsedRealtimeNanos: Long
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

interface CurrentLocationClient {
    suspend fun lastKnownLocation(): DeviceLocation?
    suspend fun currentLocation(): DeviceLocation?
}

interface LocationPermissionChecker {
    fun hasLocationPermission(): Boolean
}

interface ElapsedRealtimeNanosProvider {
    fun now(): Long
}

interface CurrentLocationRepository {
    val currentLocation: StateFlow<LatLng?>
    suspend fun warmUp()
}

@Singleton
class DefaultCurrentLocationRepository @Inject constructor(
    private val permissionChecker: LocationPermissionChecker,
    private val locationClient: CurrentLocationClient,
    private val elapsedRealtimeNanosProvider: ElapsedRealtimeNanosProvider
) : CurrentLocationRepository {
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    override val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val warmUpMutex = Mutex()

    override suspend fun warmUp() {
        if (!permissionChecker.hasLocationPermission()) return

        warmUpMutex.withLock {
            val lastLocation = runCatching { locationClient.lastKnownLocation() }.getOrNull()
            if (lastLocation != null && lastLocation.isFresh()) {
                _currentLocation.value = lastLocation.toLatLng()
                return
            }

            val currentLocation = runCatching { locationClient.currentLocation() }.getOrNull()
            val bestLocation = currentLocation ?: lastLocation
            if (bestLocation != null) {
                _currentLocation.value = bestLocation.toLatLng()
            }
        }
    }

    private fun DeviceLocation.isFresh(): Boolean {
        val age = elapsedRealtimeNanosProvider.now() - elapsedRealtimeNanos
        return age in 0..FRESH_LOCATION_MAX_AGE_NANOS
    }

    private companion object {
        private const val FRESH_LOCATION_MAX_AGE_NANOS = 10L * 60L * 1_000_000_000L
    }
}
