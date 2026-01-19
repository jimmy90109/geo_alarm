package com.github.jimmy90109.geoalarm.utils

import android.content.Context
import com.github.jimmy90109.geoalarm.R
import java.util.Calendar

object TimeUtils {

    fun formatScheduleTitle(context: Context, hour: Int, minute: Int, days: Set<Int>): String {
        val timeString = String.format("%02d:%02d", hour, minute)
        
        if (days.isEmpty()) {
            return timeString
        }

        // Calendar.MONDAY = 2, FRIDAY = 6
        val monToFri = setOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY
        )

        val weekends = setOf(
            Calendar.SATURDAY,
            Calendar.SUNDAY
        )

        val daysString = when {
            days.size == 7 -> context.getString(R.string.every_day)
            days.size == 5 && days.containsAll(monToFri) -> context.getString(R.string.mon_to_fri)
            days.size == 2 && days.containsAll(weekends) -> context.getString(R.string.weekends)
            else -> {
                val weekdaysDisplay = context.resources.getStringArray(R.array.weekdays_display)
                // Sort days: Sunday (1) to Saturday (7)
                val sortedDays = days.sorted()
                
                sortedDays.joinToString(" ") { day ->
                    // Calendar constant 1-based, array 0-based
                    // Sunday=1 -> index 0
                    val index = day - 1
                    if (index in weekdaysDisplay.indices) {
                        weekdaysDisplay[index]
                    } else {
                        "" // Should not happen if valid Calendar constants used
                    }
                }
            }
        }

        return "$daysString $timeString"
    }
}
