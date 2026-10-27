package com.triggertrap.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

class SunTimesTest {
    private fun assertWithin(expected: LocalTime, actual: ZonedDateTime?, minutes: Long = 3) {
        assertNotNull(actual)
        val diff = abs(Duration.between(expected, actual!!.toLocalTime()).toMinutes())
        assertTrue("expected $expected got ${actual.toLocalTime()}", diff <= minutes)
    }

    @Test fun londonMidsummer() {
        val zone = ZoneId.of("Europe/London")
        val d = SunTimes.day(LocalDate.of(2024, 6, 21), 51.5074, -0.1278, zone)
        assertWithin(LocalTime.of(4, 43), d.sunrise)
        assertWithin(LocalTime.of(21, 21), d.sunset)
        assertWithin(LocalTime.of(3, 53), d.civilDawn)
        assertWithin(LocalTime.of(22, 12), d.civilDusk)
        assertEquals(LocalDate.of(2024, 6, 21), d.sunrise!!.toLocalDate())
    }

    @Test fun berlinMidwinter() {
        val zone = ZoneId.of("Europe/Berlin")
        val d = SunTimes.day(LocalDate.of(2024, 12, 21), 52.52, 13.405, zone)
        assertWithin(LocalTime.of(8, 15), d.sunrise)
        assertWithin(LocalTime.of(15, 54), d.sunset)
    }

    @Test fun sydneySouthernHemisphere() {
        val zone = ZoneId.of("Australia/Sydney")
        val d = SunTimes.day(LocalDate.of(2024, 6, 21), -33.8688, 151.2093, zone)
        assertWithin(LocalTime.of(7, 0), d.sunrise)
        assertWithin(LocalTime.of(16, 54), d.sunset)
    }

    @Test fun losAngelesWestOfGreenwich() {
        val zone = ZoneId.of("America/Los_Angeles")
        val d = SunTimes.day(LocalDate.of(2024, 6, 21), 34.0522, -118.2437, zone)
        assertWithin(LocalTime.of(5, 42), d.sunrise)
        assertWithin(LocalTime.of(20, 8), d.sunset)
    }

    @Test fun polarDayHasNoEvents() {
        val zone = ZoneId.of("Europe/Oslo")
        val d = SunTimes.day(LocalDate.of(2024, 6, 21), 69.6492, 18.9553, zone) // Tromsø
        assertNull(d.sunrise); assertNull(d.sunset); assertNull(d.civilDawn); assertNull(d.civilDusk)
    }

    @Test fun polarNightHasNoSunriseButStillTwilight() {
        val zone = ZoneId.of("Europe/Oslo")
        val d = SunTimes.day(LocalDate.of(2024, 12, 21), 69.6492, 18.9553, zone)
        assertNull(d.sunrise); assertNull(d.sunset)
        assertNotNull(d.civilDawn); assertNotNull(d.civilDusk)
    }

    @Test fun singleEventMatchesDay() {
        val zone = ZoneId.of("Europe/London")
        val date = LocalDate.of(2024, 6, 21)
        assertEquals(SunTimes.day(date, 51.5, -0.1, zone).sunset, SunTimes.eventAt(SunTimes.Event.SUNSET, date, 51.5, -0.1, zone))
    }
}
