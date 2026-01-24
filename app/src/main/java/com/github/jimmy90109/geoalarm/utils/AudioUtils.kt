package com.github.jimmy90109.geoalarm.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log

/**
 * Audio utility class
 */
object AudioUtils {
    private const val TAG = "AudioUtils"

    // Audio focus request for pausing other apps
    private var audioFocusRequest: AudioFocusRequest? = null

    // Headphone device types
    private val HEADPHONE_TYPES = setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_USB_HEADSET,
    ).let { types ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            types + AudioDeviceInfo.TYPE_BLE_HEADSET + AudioDeviceInfo.TYPE_BLE_SPEAKER
        } else {
            types
        }
    }

    /**
     * Check if headphones are connected (Bluetooth or wired)
     */
    fun isHeadphoneConnected(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        val connected = devices.any { device ->
            device.type in HEADPHONE_TYPES
        }

        Log.d(TAG, "Headphone connected: $connected, devices: ${devices.map { it.type }}")
        return connected
    }

    /**
     * Get the ringtone display name
     */
    fun getRingtoneName(context: Context, uriString: String?): String? {
        if (uriString == null) return null
        return try {
            val uri = Uri.parse(uriString)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.getTitle(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ringtone name", e)
            null
        }
    }

    /**
     * Request audio focus (pauses other music)
     */
    private fun requestAudioFocus(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .build()
        
        audioFocusRequest = focusRequest
        val result = audioManager.requestAudioFocus(focusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    /**
     * Abandon audio focus (resumes other music)
     */
    fun abandonAudioFocus(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
            audioFocusRequest = null
        }
    }

    /**
     * Play ringtone via media channel
     * @param uriString Ringtone URI, null means use default ringtone
     * @return MediaPlayer instance, caller is responsible for stop() and release()
     */
    fun playRingtoneViaMedia(context: Context, uriString: String? = null): MediaPlayer? {
        return try {
            val ringtoneUri = if (uriString != null) {
                Uri.parse(uriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: return null
            }

            // Request audio focus to pause other music
            requestAudioFocus(context)

            MediaPlayer().apply {
                setDataSource(context, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play ringtone via media", e)
            null
        }
    }

    /**
     * Play preview (non-looping)
     */
    fun playPreview(context: Context, uriString: String? = null): MediaPlayer? {
        return try {
            val ringtoneUri = if (uriString != null) {
                Uri.parse(uriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: return null
            }

            // Request audio focus to pause other music
            requestAudioFocus(context)

            MediaPlayer().apply {
                setDataSource(context, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = false
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play preview", e)
            null
        }
    }
}
