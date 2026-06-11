package com.github.jimmy90109.geoalarm.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidCurrentLocationClient @Inject constructor(
    @ApplicationContext context: Context
) : CurrentLocationClient {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun lastKnownLocation(): DeviceLocation? =
        fusedLocationClient.lastLocation.awaitOrNull()?.toDeviceLocation()

    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): DeviceLocation? {
        val cancellationTokenSource = CancellationTokenSource()
        return fusedLocationClient
            .getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            )
            .awaitOrNull(
                onCancel = { cancellationTokenSource.cancel() }
            )
            ?.toDeviceLocation()
    }
}

class AndroidLocationPermissionChecker @Inject constructor(
    @param:ApplicationContext private val context: Context
) : LocationPermissionChecker {
    override fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }
}

class SystemElapsedRealtimeNanosProvider @Inject constructor() : ElapsedRealtimeNanosProvider {
    override fun now(): Long = SystemClock.elapsedRealtimeNanos()
}

private fun android.location.Location.toDeviceLocation(): DeviceLocation =
    DeviceLocation(
        latitude = latitude,
        longitude = longitude,
        elapsedRealtimeNanos = elapsedRealtimeNanos
    )

private suspend fun <T> Task<T>.awaitOrNull(
    onCancel: () -> Unit = {}
): T? = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }
    addOnFailureListener {
        if (continuation.isActive) {
            continuation.resume(null)
        }
    }
    addOnCanceledListener {
        if (continuation.isActive) {
            continuation.resume(null)
        }
    }
    continuation.invokeOnCancellation { onCancel() }
}
