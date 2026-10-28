package com.triggertrap.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Sunrise, sunset and civil twilight after the NOAA solar calculator (accurate to about a
 * minute, which is what the sunrise/sunset screen needs). Replaces the jsuntimes jar.
 */
object SunTimes {

    enum class Event(internal val zenith: Double, internal val morning: Boolean) {
        CIVIL_DAWN(96.0, true), SUNRISE(90.833, true), SUNSET(90.833, false), CIVIL_DUSK(96.0, false)
    }

    data class Day(val civilDawn: ZonedDateTime?, val sunrise: ZonedDateTime?, val sunset: ZonedDateTime?, val civilDusk: ZonedDateTime?)

    /** All four events of [date] (local calendar day in [zone]) at the given position; null where the sun does not cross that altitude. */
    @JvmStatic
    fun day(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId): Day = Day(
        eventAt(Event.CIVIL_DAWN, date, latitude, longitude, zone),
        eventAt(Event.SUNRISE, date, latitude, longitude, zone),
        eventAt(Event.SUNSET, date, latitude, longitude, zone),
        eventAt(Event.CIVIL_DUSK, date, latitude, longitude, zone),
    )

    /** The moment of [event] on [date] at the position, in [zone]; null during polar day / night. */
    @JvmStatic
    fun eventAt(event: Event, date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId): ZonedDateTime? {
        val minutesUtc = eventMinutesUtc(event, date, latitude, longitude) ?: return null
        val startOfDayUtc = date.toEpochDay() * 86_400L
        val instant = Instant.ofEpochSecond(startOfDayUtc + Math.round(minutesUtc * 60.0))
        return instant.atZone(zone)
    }

    /** Minutes after 00:00 UTC of [date] at which the event happens, or null when it does not. */
    internal fun eventMinutesUtc(event: Event, date: LocalDate, latitude: Double, longitude: Double): Double? {
        // Julian century at noon UTC of the day
        val jd = date.toEpochDay() + 2440587.5 + 0.5
        val jc = (jd - 2451545.0) / 36525.0
        val geomMeanLongSun = ((280.46646 + jc * (36000.76983 + jc * 0.0003032)) % 360.0 + 360.0) % 360.0
        val geomMeanAnomSun = 357.52911 + jc * (35999.05029 - 0.0001537 * jc)
        val eccent = 0.016708634 - jc * (0.000042037 + 0.0000001267 * jc)
        val m = Math.toRadians(geomMeanAnomSun)
        val sunEqOfCtr = sin(m) * (1.914602 - jc * (0.004817 + 0.000014 * jc)) + sin(2 * m) * (0.019993 - 0.000101 * jc) + sin(3 * m) * 0.000289
        val sunTrueLong = geomMeanLongSun + sunEqOfCtr
        val omega = Math.toRadians(125.04 - 1934.136 * jc)
        val sunAppLong = sunTrueLong - 0.00569 - 0.00478 * sin(omega)
        val meanObliq = 23.0 + (26.0 + (21.448 - jc * (46.815 + jc * (0.00059 - jc * 0.001813))) / 60.0) / 60.0
        val obliqCorr = meanObliq + 0.00256 * cos(omega)
        val declination = asin(sin(Math.toRadians(obliqCorr)) * sin(Math.toRadians(sunAppLong)))
        val varY = tan(Math.toRadians(obliqCorr / 2.0)).let { it * it }
        val l0 = Math.toRadians(geomMeanLongSun)
        val eqOfTime = 4.0 * Math.toDegrees(
            varY * sin(2 * l0) - 2 * eccent * sin(m) + 4 * eccent * varY * sin(m) * cos(2 * l0) -
                0.5 * varY * varY * sin(4 * l0) - 1.25 * eccent * eccent * sin(2 * m)
        )
        val lat = Math.toRadians(latitude)
        val cosHa = cos(Math.toRadians(event.zenith)) / (cos(lat) * cos(declination)) - tan(lat) * tan(declination)
        if (cosHa < -1.0 || cosHa > 1.0) return null
        val hourAngle = Math.toDegrees(acos(cosHa))
        val solarNoon = 720.0 - 4.0 * longitude - eqOfTime
        return if (event.morning) solarNoon - hourAngle * 4.0 else solarNoon + hourAngle * 4.0
    }
}
