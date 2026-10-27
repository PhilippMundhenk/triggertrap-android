package com.triggertrap.core

import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * ND filter calculator: the exposure a base shutter speed becomes behind an ND filter of
 * n stops. The shutter speed table mirrors the picker in the app (index = picker position).
 */
object NdCalculator {

    /** Standard shutter speeds in milliseconds, 1/8000 s ... 30 s, in the order of the picker. */
    @JvmField
    val SHUTTER_SPEEDS_MS: DoubleArray = doubleArrayOf(
        0.12, 0.16, 0.2, 0.25, 0.31, 0.4, 0.5, 0.63, 0.8, 1.0, 1.25, 1.56, 2.0, 2.5, 3.13, 4.0, 5.0, 6.25, 8.0, 10.0,
        13.0, 17.0, 20.0, 25.0, 33.0, 40.0, 50.0, 67.0, 77.0, 100.0, 125.0, 167.0, 200.0, 250.0, 300.0, 400.0, 500.0,
        600.0, 800.0, 1000.0, 1300.0, 1600.0, 2000.0, 2500.0, 3200.0, 4000.0, 5000.0, 6000.0, 8000.0, 10000.0,
        13000.0, 15000.0, 20000.0, 25000.0, 30000.0,
    )

    /** More than 100 hours: the app shows whole hours. */
    const val HOURS_LIMIT_MS = 360_000_000L

    sealed class Result {
        /** Exposure longer than [HOURS_LIMIT_MS]: show whole hours. */
        data class Hours(val hours: Long) : Result()
        /** Exposure at least the longest table speed: show as h m s, and as a timer value. */
        data class Timed(val millis: Long, val text: String) : Result()
        /** Exposure within the table: show the nearest standard shutter speed, [index] into the table. */
        data class Nearest(val index: Int) : Result()
    }

    /** The exposure in ms of [baseIndex] behind [stops] stops of ND. */
    @JvmStatic
    fun exposureMillis(baseIndex: Int, stops: Int): Double = SHUTTER_SPEEDS_MS[baseIndex] * Math.pow(2.0, stops.toDouble())

    @JvmStatic
    fun calculate(baseIndex: Int, stops: Int): Result {
        val value = exposureMillis(baseIndex, stops)
        val result = value.toLong()
        if (result > HOURS_LIMIT_MS) return Result.Hours(result / 3_600_000)
        if (result >= SHUTTER_SPEEDS_MS[SHUTTER_SPEEDS_MS.size - 1]) return Result.Timed(result, formatHms(result))
        // the largest table entry below the value, then the nearer of it and its successor (ties go down)
        var i = baseIndex
        while (i + 1 < SHUTTER_SPEEDS_MS.size && SHUTTER_SPEEDS_MS[i + 1] < value) i++
        val lowerDiff = value - SHUTTER_SPEEDS_MS[i]
        val higherDiff = SHUTTER_SPEEDS_MS[i + 1] - value
        return Result.Nearest(if (lowerDiff <= higherDiff) i else i + 1)
    }

    @JvmStatic
    fun formatHms(millis: Long): String = String.format(
        Locale.getDefault(), "%dh %02dm %02ds",
        TimeUnit.MILLISECONDS.toHours(millis),
        TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
        TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
    )
}
