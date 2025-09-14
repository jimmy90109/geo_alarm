package com.example.geo_alarm

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class VibrationHelper {
    private var vibrator: Vibrator? = null

    fun startAlarmVibration(context: Context) {
        vibrator = getVibrator(context)

        if (vibrator?.hasVibrator() == true) {
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 200, 1000) // Alarm pattern

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(pattern, 0) // Repeat indefinitely
                vibrator?.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    fun stopVibration(context: Context) {
        if (vibrator == null) {
            vibrator = getVibrator(context)
        }
        vibrator?.cancel()
        vibrator = null
    }

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}