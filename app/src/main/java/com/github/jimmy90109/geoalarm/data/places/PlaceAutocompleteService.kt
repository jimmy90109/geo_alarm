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
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class PlaceSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val fullText: String
)

interface PlaceAutocompleteService {
    fun startSession(): String
    fun endSession(sessionId: String)
    suspend fun suggestions(
        query: String,
        locationBiasCenter: LatLng?,
        sessionId: String
    ): List<PlaceSuggestion>

    suspend fun resolveCandidates(
        suggestions: List<PlaceSuggestion>,
        selectedPlaceId: String,
        sessionId: String
    ): List<PlaceCandidate>
}

@Singleton
class AndroidPlaceAutocompleteService @Inject constructor(
    @ApplicationContext context: Context
) : PlaceAutocompleteService {
    private val appContext = context.applicationContext
    private val placesClient = Places.createClient(context)
    private val sessions = ConcurrentHashMap<String, AutocompleteSessionToken>()

    override fun startSession(): String {
        val id = UUID.randomUUID().toString()
        sessions[id] = AutocompleteSessionToken.newInstance()
        return id
    }

    override fun endSession(sessionId: String) {
        sessions.remove(sessionId)
    }

    override suspend fun suggestions(
        query: String,
        locationBiasCenter: LatLng?,
        sessionId: String
    ): List<PlaceSuggestion> {
        val token = sessions[sessionId] ?: return emptyList()
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setSessionToken(token)
            .apply {
                locationBiasCenter?.let {
                    setLocationBias(CircularBounds.newInstance(it, LOCATION_BIAS_RADIUS_METERS))
                    setOrigin(it)
                }
            }
            .build()

        return suspendCancellableCoroutine { continuation ->
            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    if (continuation.isActive) {
                        continuation.resume(
                            response.autocompletePredictions.take(MAX_RESULTS).map { prediction ->
                                PlaceSuggestion(
                                    placeId = prediction.placeId,
                                    primaryText = prediction.getPrimaryText(null).toString(),
                                    secondaryText = prediction.getSecondaryText(null).toString(),
                                    fullText = prediction.getFullText(null).toString()
                                )
                            }
                        )
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }
    }

    override suspend fun resolveCandidates(
        suggestions: List<PlaceSuggestion>,
        selectedPlaceId: String,
        sessionId: String
    ): List<PlaceCandidate> = coroutineScope {
        val token = sessions.remove(sessionId)
        suggestions.take(MAX_RESULTS).map { suggestion ->
            async {
                runCatching {
                    fetchCandidate(
                        suggestion = suggestion,
                        sessionToken = token.takeIf { suggestion.placeId == selectedPlaceId }
                    )
                }.getOrNull()
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun fetchCandidate(
        suggestion: PlaceSuggestion,
        sessionToken: AutocompleteSessionToken?
    ): PlaceCandidate {
        val fields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.PHOTO_METADATAS
        )
        val request = FetchPlaceRequest.builder(suggestion.placeId, fields)
            .apply { sessionToken?.let(::setSessionToken) }
            .build()
        val place = suspendCancellableCoroutine { continuation ->
            placesClient.fetchPlace(request)
                .addOnSuccessListener { response ->
                    if (continuation.isActive) continuation.resume(response.place)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }
        val location = place.location ?: error("Place has no location")
        val photoMetadata = place.photoMetadatas?.firstOrNull()
        val photo = photoMetadata?.let { fetchResolvedPhotoBitmap(it) }
        return PlaceCandidate(
            id = place.id ?: suggestion.placeId,
            name = place.displayName ?: suggestion.primaryText,
            address = place.formattedAddress ?: suggestion.secondaryText,
            location = location,
            photo = photo,
            photoAttribution = photoMetadata?.attributions?.let {
                Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT).toString()
            }.orEmpty()
        )
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
        const val TAG = "PlaceAutocompleteService"
        const val MAX_RESULTS = 5
        const val PHOTO_MAX_WIDTH = 640
        const val PHOTO_MAX_HEIGHT = 360
        const val LOCATION_BIAS_RADIUS_METERS = 50_000.0
    }
}
