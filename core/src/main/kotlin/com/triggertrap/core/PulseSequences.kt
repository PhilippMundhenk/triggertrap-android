package com.triggertrap.core

/**
 * Pulse sequences for the camera trigger. A sequence is a flat array of
 * `[pulse, pause, pulse, pause, ...]` durations in milliseconds: the dongle keeps the
 * shutter pressed for `pulse` and released for `pause`.
 *
 * Pure functions; the beep (pulse) length and the HDR gap are settings passed in by
 * the caller, so every mode can be tested without Android.
 */
object PulseSequences {

    enum class Easing { EASE_IN, EASE_OUT, EASE_IN_OUT }

    /** Total duration of a sequence (sum of every pulse and pause). */
    @JvmStatic
    fun sequenceTime(sequence: LongArray): Long {
        var total = 0L
        for (i in 0 until sequence.size / 2) {
            total += sequence[i * 2]
            total += sequence[i * 2 + 1]
        }
        return total
    }

    /** One pulse per `gap`; the pulse itself counts towards the interval. */
    @JvmStatic
    fun timeLapse(gap: Long, beepLength: Long): LongArray = longArrayOf(beepLength, gap - beepLength)

    /** `count` exposures of `pulseLength`, each followed by `gap`. */
    @JvmStatic
    fun starTrail(count: Int, pulseLength: Long, gap: Long): LongArray {
        val sequence = LongArray(count * 2)
        for (i in 0 until count) {
            sequence[i * 2] = pulseLength
            sequence[i * 2 + 1] = gap
        }
        return sequence
    }

    /**
     * Long-exposure HDR bracket around [middle] ms: an odd number of exposures `count`
     * (rounded up when even), `ev` stops apart, [hdrGap] ms between them. With an
     * [interval] > 0 the last pause is stretched so one bracket starts every
     * `interval` (HDR timelapse).
     */
    @JvmStatic
    fun hdr(middle: Long, count: Int, ev: Float, interval: Long, hdrGap: Long): LongArray {
        var exposures = count
        if (exposures % 2 != 1) exposures++
        val sequence = LongArray(exposures * 2)
        val step = (exposures - 1) / 2
        var j = 0
        for (i in -step..step) {
            sequence[j] = Math.round(Math.pow(Math.pow(2.0, ev.toDouble()), i.toDouble()) * middle)
            sequence[j + 1] = hdrGap
            j += 2
        }
        if (interval > 0) {
            val total = sequenceTime(sequence)
            sequence[sequence.size - 1] = interval - total + hdrGap
        }
        return sequence
    }

    /** Exposure ramps linearly from [firstExposure] to [lastExposure] over `count` shots, one every `interval`. */
    @JvmStatic
    fun bramping(count: Int, interval: Long, firstExposure: Long, lastExposure: Long): LongArray {
        val sequence = LongArray(count * 2)
        for (i in 0 until count) {
            val fraction = i.toFloat() / (count - 1)
            val exposure = (fraction * (lastExposure - firstExposure) + firstExposure).toLong()
            sequence[i * 2] = exposure
            sequence[i * 2 + 1] = interval - exposure
        }
        return sequence
    }

    /**
     * TimeWarp: `count` shots spread over [sequenceDuration] following the [bezier] curve.
     * Pauses that would overlap are resolved by the curve (shots are dropped), a pause
     * can never be shorter than 1 ms.
     */
    @JvmStatic
    fun timeWarp(count: Int, sequenceDuration: Long, bezier: CubicBezier, beepLength: Long, minimumGap: Long): LongArray {
        val pauses = bezier.pauses(sequenceDuration, count, beepLength, minimumGap)
        val sequence = LongArray(pauses.size * 2)
        for (i in pauses.indices) {
            sequence[i * 2] = beepLength
            sequence[i * 2 + 1] = if (pauses[i] >= 0) pauses[i] else 1
        }
        return sequence
    }

    /**
     * Eased timelapse (not reachable from the UI, kept from the original code): `frames`
     * shots over `duration` with a power easing. Note the original integer division of
     * `i / frames`, which makes every gap the minimum; preserved so behaviour is unchanged.
     */
    @JvmStatic
    fun easedTimeLapse(exponent: Double, frames: Int, duration: Int, easing: Easing, beepLength: Long): LongArray {
        val buffer = 10
        val min = beepLength + buffer
        val sequence = LongArray(frames * 2)
        var previous = 0.0
        for (i in 0 until frames) {
            val ratio = (i / frames).toDouble()
            val eased = when (easing) {
                Easing.EASE_IN -> easeIn(ratio, exponent)
                Easing.EASE_OUT -> easeOut(ratio, exponent)
                Easing.EASE_IN_OUT -> easeInOut(ratio, exponent)
            }
            var gap = Math.round((eased - previous) * duration)
            if (gap < min) gap = min
            previous = eased
            sequence[i * 2] = beepLength
            sequence[i * 2 + 1] = gap - beepLength
        }
        return sequence
    }

    internal fun easeIn(frame: Double, exponent: Double) = Math.pow(frame, exponent)
    internal fun easeOut(frame: Double, exponent: Double) = 1 - Math.pow(1.0 - frame, exponent)
    internal fun easeInOut(frame: Double, exponent: Double) =
        if (frame < 0.5) easeIn(frame * 2.0, exponent) / 2.0 else 0.5 + easeOut((frame - 0.5) * 2.0, exponent) / 2.0
}
