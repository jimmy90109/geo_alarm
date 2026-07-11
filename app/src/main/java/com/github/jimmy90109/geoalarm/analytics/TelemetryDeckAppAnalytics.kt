package com.github.jimmy90109.geoalarm.analytics

import android.content.Context
import android.util.Log
import com.github.jimmy90109.geoalarm.BuildConfig
import com.telemetrydeck.sdk.TelemetryDeck
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class TelemetryDeckAppAnalytics @Inject constructor(
    @ApplicationContext private val appContext: Context
) : AppAnalytics {

    companion object {
        private const val TAG = "TelemetryDeckAnalytics"
    }

    private val started = AtomicBoolean(false)

    override suspend fun signal(eventName: String): Boolean = withContext(Dispatchers.Main.immediate) {
        if (BuildConfig.TELEMETRYDECK_APP_ID.isBlank()) {
            Log.w(TAG, "Skip signal $eventName because TELEMETRYDECK_APP_ID is blank")
            return@withContext false
        }

        try {
            startIfNeeded()
            TelemetryDeck.signal(eventName)
            Log.d(TAG, "Signal sent: $eventName")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to signal $eventName", t)
            false
        }
    }

    private fun startIfNeeded() {
        if (!started.get()) {
            val builder = TelemetryDeck.Builder()
                .appID(BuildConfig.TELEMETRYDECK_APP_ID)
            TelemetryDeck.start(appContext, builder)
            started.set(true)
        }
    }
}
