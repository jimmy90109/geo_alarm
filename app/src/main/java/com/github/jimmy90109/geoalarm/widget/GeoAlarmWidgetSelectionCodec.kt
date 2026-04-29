package com.github.jimmy90109.geoalarm.widget

object GeoAlarmWidgetSelectionCodec {
    private const val DELIMITER = ","

    fun encode(ids: List<String>): String {
        return ids.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(2)
            .joinToString(DELIMITER)
    }

    fun decode(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(DELIMITER)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(2)
    }
}
