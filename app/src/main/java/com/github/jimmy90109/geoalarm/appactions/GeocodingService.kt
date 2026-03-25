package com.github.jimmy90109.geoalarm.appactions

interface GeocodingService {
    suspend fun geocode(locationQuery: String): GeoCoordinate?
}

data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
)
