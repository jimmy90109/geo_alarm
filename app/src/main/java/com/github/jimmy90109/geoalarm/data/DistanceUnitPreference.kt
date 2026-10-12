package com.github.jimmy90109.geoalarm.data

enum class DistanceUnitPreference(val id: String) {
    AUTO("auto"),
    METRIC("metric"),
    IMPERIAL("imperial");

    companion object {
        fun fromId(id: String?): DistanceUnitPreference {
            return entries.firstOrNull { it.id == id } ?: AUTO
        }
    }
}

enum class DistanceUnitSystem {
    METRIC,
    IMPERIAL,
}
