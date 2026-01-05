package com.github.jimmy90109.geoalarm.utils

import android.content.Context
import android.os.PowerManager
import android.util.Log

object WakeLocker {
    private const val TAG = "WakeLocker"
    private var wakeLock: PowerManager.WakeLock? = null

    /**
     * Acquires a WakeLock to keep the CPU running and ensuring the screen can be turned on.
     *
     * @param context The application context.
     */
    fun acquire(context: Context) {
        if (wakeLock != null) wakeLock?.release()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP, // This flag ensures the screen turns on when acquired
            "$TAG::Lock"
        )
        // Acquire the lock for a maximum of 10 minutes to prevent battery drain
        wakeLock?.acquire(10 * 60 * 1000L)
        Log.d(TAG, "WakeLock acquired")
    }

    /**
     * Releases the potentially held WakeLock.
     */
    fun release() {
        if (wakeLock != null && wakeLock?.isHeld == true) {
            wakeLock?.release()
            Log.d(TAG, "WakeLock released")
        }
        wakeLock = null
    }
}
