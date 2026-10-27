package com.triggertrap.core

import java.util.Locale

/** Time strings shared by the notification and the screens. */
object TimeFormat {
    /** `HH:MM:SS:hh` (hours, minutes, seconds, hundredths). */
    @JvmStatic
    fun hhmmsscc(millis: Long): String {
        var seconds = (millis / 1000).toInt()
        val hundredths = ((millis - seconds * 1000L) / 10).toInt()
        var minutes = seconds / 60
        seconds -= minutes * 60
        val hours = minutes / 60
        minutes -= hours * 60
        return String.format(Locale.US, "%02d:%02d:%02d:%02d", hours, minutes, seconds, hundredths)
    }
}
