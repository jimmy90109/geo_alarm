package com.github.jimmy90109.geoalarm.appactions

import android.content.Context
import android.location.Geocoder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidGeocodingService @Inject constructor(
    @ApplicationContext private val context: Context
) : GeocodingService {

    @Suppress("DEPRECATION")
    override suspend fun geocode(locationQuery: String): GeoCoordinate? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context)
        val addresses = geocoder.getFromLocationName(locationQuery, 1)
        val address = addresses?.firstOrNull() ?: return@withContext null
        GeoCoordinate(address.latitude, address.longitude)
    }
}
