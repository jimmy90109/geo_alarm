package com.github.jimmy90109.geoalarm.data.places

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.text.Html
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest
import com.google.android.libraries.places.api.net.SearchByTextRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
    private val appContext = context.applicationContext
    private val placesClient = Places.createClient(context)

    override suspend fun search(query: String, locationBiasCenter: LatLng?): List<PlaceCandidate> = coroutineScope {
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

        val response = suspendCancellableCoroutine { continuation ->
            placesClient.searchByText(request)
                .addOnSuccessListener { response ->
                    if (continuation.isActive) continuation.resume(response)
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "Shared place search failed query=$query", error)
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }

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
        val candidates = fetchCandidatePhotos(places)
        Log.d(TAG, "Shared place search succeeded count=${candidates.size}")
        candidates
    }

    private suspend fun fetchCandidatePhotos(
        places: List<Pair<Place, PlaceCandidate>>
    ): List<PlaceCandidate> = coroutineScope {
        places.map { (place, candidate) ->
            async {
                val metadata = place.photoMetadatas?.firstOrNull()
                if (metadata == null) {
                    candidate
                } else {
                    candidate.copy(
                        photo = fetchResolvedPhotoBitmap(metadata),
                        photoAttribution = Html.fromHtml(
                            metadata.attributions,
                            Html.FROM_HTML_MODE_COMPACT
                        ).toString()
                    )
                }
            }
        }.awaitAll()
    }

    private suspend fun fetchResolvedPhotoBitmap(metadata: PhotoMetadata): Bitmap? {
        val request = FetchResolvedPhotoUriRequest.builder(metadata)
            .setMaxWidth(PHOTO_MAX_WIDTH)
            .setMaxHeight(PHOTO_MAX_HEIGHT)
            .build()
        val uri = suspendCancellableCoroutine { continuation ->
            placesClient.fetchResolvedPhotoUri(request)
                .addOnSuccessListener { response ->
                    if (continuation.isActive) continuation.resume(response.uri)
                }
                .addOnFailureListener { error ->
                    Log.w(TAG, "Failed to resolve place photo uri", error)
                    if (continuation.isActive) continuation.resume(null)
                }
        }
        return uri?.let { decodePhotoUri(it) }
    }

    private suspend fun decodePhotoUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            when (uri.scheme?.lowercase()) {
                "http", "https" -> URL(uri.toString()).openStream().use(BitmapFactory::decodeStream)
                else -> ImageDecoder.decodeBitmap(ImageDecoder.createSource(appContext.contentResolver, uri))
            }
        }.getOrElse { error ->
            Log.w(TAG, "Failed to decode resolved place photo uri=$uri", error)
            null
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
