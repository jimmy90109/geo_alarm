package com.github.jimmy90109.geoalarm.appactions

import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import java.util.UUID
import javax.inject.Inject

class CreateGeoAlarmUseCase @Inject constructor(
    private val repository: AlarmDataRepository,
    private val geocodingService: GeocodingService
) {

    data class Request(
        val name: String,
        val locationQuery: String,
        val radiusMeters: Double?
    )

    suspend operator fun invoke(request: Request): AppActionResult<Alarm> {
        val name = request.name.trim()
        val locationQuery = request.locationQuery.trim()

        if (name.isEmpty() || locationQuery.isEmpty()) {
            return AppActionResult.Error(
                code = "ERR_MISSING_PARAMS",
                message = "Missing name or location_query"
            )
        }

        val coordinate = geocodingService.geocode(locationQuery)
            ?: return AppActionResult.Error(
                code = "ERR_GEOCODE_FAILED",
                message = "Unable to resolve location"
            )

        val radius = request.radiusMeters
            ?.takeIf { it > 0 }
            ?: AppActionContract.DEFAULT_RADIUS_METERS

        val existing = repository.findAlarmsByName(name)
        val isDuplicate = existing.any {
            it.name == name &&
                it.latitude == coordinate.latitude &&
                it.longitude == coordinate.longitude &&
                it.radius == radius
        }
        if (isDuplicate) {
            return AppActionResult.Error(
                code = "ERR_DUPLICATE_ALARM",
                message = "Alarm already exists"
            )
        }

        val alarm = Alarm(
            id = UUID.randomUUID().toString(),
            name = name,
            latitude = coordinate.latitude,
            longitude = coordinate.longitude,
            radius = radius,
            isEnabled = false
        )
        repository.insert(alarm)

        return AppActionResult.Success(alarm)
    }
}
