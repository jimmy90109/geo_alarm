package com.github.jimmy90109.geoalarm.data.places

import android.content.Context
import android.graphics.Bitmap
import android.text.Html
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.SearchByTextRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PlaceCandidate(
    val id: String,
    val name: String,
    val address: String,
    val location: LatLng,
    val photo: Bitmap? = null,
    val photoAttribution: String = ""
)

interface PlaceSearchService {
    suspend fun search(query: String, locationBiasCenter: LatLng? = null): List<PlaceCandidate>
}

@Singleton
class AndroidPlaceSearchService @Inject constructor(
    @ApplicationContext context: Context
) : PlaceSearchService {
    private val placesClient = Places.createClient(context)

    override suspend fun search(query: String, locationBiasCenter: LatLng?): List<PlaceCandidate> {
        Log.d(TAG, "Searching shared place query=$query")
        val fields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.PHOTO_METADATAS
        )
        val request = SearchByTextRequest.builder(query, fields)
            .setMaxResultCount(MAX_RESULTS)
            .apply {
                locationBiasCenter?.let {
                    setLocationBias(CircularBounds.newInstance(it, LOCATION_BIAS_RADIUS_METERS))
                }
            }
            .build()

        return suspendCancellableCoroutine { continuation ->
            placesClient.searchByText(request)
                .addOnSuccessListener { response ->
                    if (continuation.isActive) {
                        val places = response.places.mapNotNull { place ->
                                val id = place.id ?: return@mapNotNull null
                                val location = place.location ?: return@mapNotNull null
                                place to PlaceCandidate(
                                    id = id,
                                    name = place.displayName ?: place.formattedAddress.orEmpty(),
                                    address = place.formattedAddress.orEmpty(),
                                    location = location,
                                )
                            }.take(MAX_RESULTS)
                        fetchCandidatePhotos(places) { candidates ->
                            if (continuation.isActive) {
                                Log.d(TAG, "Shared place search succeeded count=${candidates.size}")
                                continuation.resume(candidates)
                            }
                        }
                    }
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "Shared place search failed query=$query", error)
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }
    }

    private fun fetchCandidatePhotos(
        places: List<Pair<Place, PlaceCandidate>>,
        onComplete: (List<PlaceCandidate>) -> Unit
    ) {
        if (places.isEmpty()) {
            onComplete(emptyList())
            return
        }

        val candidates = places.map { it.second }.toMutableList()
        var remaining = places.size
        places.forEachIndexed { index, (place, candidate) ->
            val metadata = place.photoMetadatas?.firstOrNull()
            if (metadata == null) {
                remaining -= 1
                if (remaining == 0) onComplete(candidates)
                return@forEachIndexed
            }

            val request = FetchPhotoRequest.builder(metadata)
                .setMaxWidth(PHOTO_MAX_WIDTH)
                .setMaxHeight(PHOTO_MAX_HEIGHT)
                .build()
            placesClient.fetchPhoto(request)
                .addOnSuccessListener { response ->
                    candidates[index] = candidate.copy(
                        photo = response.bitmap,
                        photoAttribution = Html.fromHtml(
                            metadata.attributions,
                            Html.FROM_HTML_MODE_COMPACT
                        ).toString()
                    )
                }
                .addOnCompleteListener {
                    remaining -= 1
                    if (remaining == 0) onComplete(candidates)
                }
        }
    }

    private companion object {
        const val TAG = "PlaceSearchService"
        const val MAX_RESULTS = 5
        const val PHOTO_MAX_WIDTH = 640
        const val PHOTO_MAX_HEIGHT = 360
        const val LOCATION_BIAS_RADIUS_METERS = 50_000.0
    }
}
