package com.triggertrap.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test fun formatsHoursMinutesSecondsHundredths() {
        assertEquals("00:00:00:00", TimeFormat.hhmmsscc(0))
        assertEquals("00:00:01:50", TimeFormat.hhmmsscc(1500))
        assertEquals("01:02:03:04", TimeFormat.hhmmsscc(3_723_040))
        assertEquals("00:59:59:99", TimeFormat.hhmmsscc(3_599_999))
    }
}
