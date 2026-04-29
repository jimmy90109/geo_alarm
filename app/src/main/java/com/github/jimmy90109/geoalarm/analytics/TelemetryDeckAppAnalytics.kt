package com.github.jimmy90109.geoalarm.analytics

import android.content.Context
import android.util.Log
import com.github.jimmy90109.geoalarm.BuildConfig
import com.telemetrydeck.sdk.TelemetryDeck
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryDeckAppAnalytics @Inject constructor(
    @ApplicationContext private val appContext: Context
) : AppAnalytics {

    companion object {
        private const val TAG = "TelemetryDeckAnalytics"
    }

    private val started = AtomicBoolean(false)

    override fun signal(eventName: String): Boolean {
        if (BuildConfig.TELEMETRYDECK_APP_ID.isBlank()) {
            Log.w(TAG, "Skip signal $eventName because TELEMETRYDECK_APP_ID is blank")
            return false
        }

        startIfNeeded()
        try {
            TelemetryDeck.signal(eventName)
            Log.d(TAG, "Signal sent: $eventName")
            return true
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to signal $eventName", t)
            return false
        }
    }

    private fun startIfNeeded() {
        if (started.compareAndSet(false, true)) {
            val builder = TelemetryDeck.Builder()
                .appID(BuildConfig.TELEMETRYDECK_APP_ID)
            TelemetryDeck.start(appContext, builder)
        }
    }
}
