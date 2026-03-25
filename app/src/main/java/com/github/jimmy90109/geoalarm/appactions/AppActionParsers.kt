package com.github.jimmy90109.geoalarm.appactions

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object AppActionParsers {
    private val dayMapping = mapOf(
        "sun" to 1,
        "sunday" to 1,
        "mon" to 2,
        "monday" to 2,
        "tue" to 3,
        "tues" to 3,
        "tuesday" to 3,
        "wed" to 4,
        "wednesday" to 4,
        "thu" to 5,
        "thur" to 5,
        "thurs" to 5,
        "thursday" to 5,
        "fri" to 6,
        "friday" to 6,
        "sat" to 7,
        "saturday" to 7
    )

    fun parseDays(raw: String?): Set<Int> {
        if (raw.isNullOrBlank()) return emptySet()

        return raw
            .split(',', ';', '|')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { token ->
                token.toIntOrNull()?.takeIf { it in 1..7 }
                    ?: dayMapping[token.lowercase(Locale.US)]
            }
            .toSet()
    }

    fun parseDays(rawValues: List<String>): Set<Int> {
        if (rawValues.isEmpty()) return emptySet()
        return rawValues.flatMap { parseDays(it) }.toSet()
    }

    fun parseTime(raw: String?): LocalTime? {
        if (raw.isNullOrBlank()) return null

        val text = raw.trim()
        return parseWithPattern(text, "H:mm")
            ?: parseWithPattern(text, "HH:mm")
            ?: parseWithPattern(text, "H:mm:ss")
            ?: parseWithPattern(text, "HH:mm:ss")
    }

    private fun parseWithPattern(raw: String, pattern: String): LocalTime? {
        return try {
            LocalTime.parse(raw, DateTimeFormatter.ofPattern(pattern))
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
