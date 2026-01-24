package com.github.jimmy90109.geoalarm.data

/**
 * RingtoneSettings
 */
data class RingtoneSettings(
    /** If Enabled */
    val enabled: Boolean = false,
    /** Ringtone URI (null = default) */
    val ringtoneUri: String? = null,
    /** Ringtone Name */
    val ringtoneName: String? = null
)
