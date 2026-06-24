package com.github.jimmy90109.geoalarm.data.location

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentLocationRepositoryTest {
    @Test
    fun `warmUp uses fresh last known location without requesting current location`() = runTest {
        val client = FakeCurrentLocationClient(
            lastLocation = DeviceLocation(25.1, 121.5, NOW_NANOS - ONE_MINUTE_NANOS)
        )
        val repository = createRepository(client = client)

        repository.warmUp()

        assertEquals(LatLng(25.1, 121.5), repository.currentLocation.value)
        assertEquals(1, client.lastKnownLocationCalls)
        assertEquals(0, client.currentLocationCalls)
    }

    @Test
    fun `warmUp requests current location when last known location is null`() = runTest {
        val client = FakeCurrentLocationClient(
            currentLocation = DeviceLocation(25.2, 121.6, NOW_NANOS)
        )
        val repository = createRepository(client = client)

        repository.warmUp()

        assertEquals(LatLng(25.2, 121.6), repository.currentLocation.value)
        assertEquals(1, client.lastKnownLocationCalls)
        assertEquals(1, client.currentLocationCalls)
    }

    @Test
    fun `warmUp does not request location without permission`() = runTest {
        val client = FakeCurrentLocationClient(
            lastLocation = DeviceLocation(25.1, 121.5, NOW_NANOS)
        )
        val repository = createRepository(
            permissionChecker = FakeLocationPermissionChecker(hasPermission = false),
            client = client
        )

        repository.warmUp()

        assertNull(repository.currentLocation.value)
        assertEquals(0, client.lastKnownLocationCalls)
        assertEquals(0, client.currentLocationCalls)
    }

    @Test
    fun `warmUp keeps null when location access fails`() = runTest {
        val client = FakeCurrentLocationClient(
            failLastLocation = true,
            failCurrentLocation = true
        )
        val repository = createRepository(client = client)

        repository.warmUp()

        assertNull(repository.currentLocation.value)
        assertEquals(1, client.lastKnownLocationCalls)
        assertEquals(1, client.currentLocationCalls)
    }

    private fun createRepository(
        permissionChecker: LocationPermissionChecker = FakeLocationPermissionChecker(),
        client: FakeCurrentLocationClient = FakeCurrentLocationClient(),
        elapsedRealtimeNanosProvider: ElapsedRealtimeNanosProvider =
            FakeElapsedRealtimeNanosProvider(NOW_NANOS)
    ): CurrentLocationRepository = DefaultCurrentLocationRepository(
        permissionChecker = permissionChecker,
        locationClient = client,
        elapsedRealtimeNanosProvider = elapsedRealtimeNanosProvider
    )

    private companion object {
        private const val NOW_NANOS = 20L * 60L * 1_000_000_000L
        private const val ONE_MINUTE_NANOS = 60L * 1_000_000_000L
    }
}

private class FakeCurrentLocationClient(
    private val lastLocation: DeviceLocation? = null,
    private val currentLocation: DeviceLocation? = null,
    private val failLastLocation: Boolean = false,
    private val failCurrentLocation: Boolean = false
) : CurrentLocationClient {
    var lastKnownLocationCalls = 0
        private set
    var currentLocationCalls = 0
        private set

    override suspend fun lastKnownLocation(): DeviceLocation? {
        lastKnownLocationCalls += 1
        if (failLastLocation) error("last location failed")
        return lastLocation
    }

    override suspend fun currentLocation(): DeviceLocation? {
        currentLocationCalls += 1
        if (failCurrentLocation) error("current location failed")
        return currentLocation
    }
}

private class FakeLocationPermissionChecker(
    private val hasPermission: Boolean = true
) : LocationPermissionChecker {
    override fun hasLocationPermission(): Boolean = hasPermission
}

private class FakeElapsedRealtimeNanosProvider(
    private val now: Long
) : ElapsedRealtimeNanosProvider {
    override fun now(): Long = now
}
