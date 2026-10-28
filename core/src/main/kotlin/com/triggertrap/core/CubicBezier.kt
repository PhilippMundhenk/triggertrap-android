package com.triggertrap.core

/**
 * Cubic bezier easing curve (CSS `cubic-bezier` semantics) used by TimeWarp. Control
 * points are clamped to 0.02..0.98 because the Newton-Raphson solver loses accuracy
 * beyond that.
 */
class CubicBezier(x1: Float, y1: Float, x2: Float, y2: Float) {

    var x1 = 0f; private set
    var y1 = 0f; private set
    var x2 = 0f; private set
    var y2 = 0f; private set

    init { setControlPoints(x1, y1, x2, y2) }

    fun setControlPoints(x1: Float, y1: Float, x2: Float, y2: Float) {
        this.x1 = clamp(x1); this.y1 = clamp(y1); this.x2 = clamp(x2); this.y2 = clamp(y2)
    }

    private fun clamp(v: Float) = when {
        v > MAX_VALUE -> MAX_VALUE
        v < MIN_VALUE -> MIN_VALUE
        else -> v
    }

    private fun a(aA1: Float, aA2: Float) = 1.0f - 3.0f * aA2 + 3.0f * aA1
    private fun b(aA1: Float, aA2: Float) = 3.0f * aA2 - 6.0f * aA1
    private fun c(aA1: Float) = 3.0f * aA1
    private fun calcBezier(t: Float, aA1: Float, aA2: Float) = ((a(aA1, aA2) * t + b(aA1, aA2)) * t + c(aA1)) * t
    private fun slope(t: Float, aA1: Float, aA2: Float) = 3.0f * a(aA1, aA2) * t * t + 2.0f * b(aA1, aA2) * t + c(aA1)

    private fun tForX(x: Float): Float {
        var guess = x
        for (i in 0 until 4) {
            val currentSlope = slope(guess, x1, x2)
            if (currentSlope == 0.0f) return guess
            val currentX = calcBezier(guess, x1, x2) - x
            guess -= currentX / currentSlope
        }
        return guess
    }

    /** y for a given x in 0..1; a curve with x1 == y1 and x2 == y2 is linear. */
    fun interpolate(input: Float): Float {
        if (x1 == y1 && x2 == y2) return input
        return calcBezier(tForX(input), y1, y2)
    }

    /** The shot times over [sequenceDuration]; the last entry is a sentinel one interval after the end. */
    internal fun timeIntervals(sequenceDuration: Long, count: Int, exposure: Long, gap: Long): LongArray {
        val minInterval = exposure + gap
        val intervals = LongArray(count + 1)
        intervals[count] = sequenceDuration + minInterval
        for (i in 0 until count) {
            val fraction = i.toFloat() / (count - 1)
            intervals[i] = (interpolate(fraction) * sequenceDuration).toLong()
        }
        return intervals
    }

    /** Pauses between consecutive shots without any overlap correction (may be negative). */
    fun originalPauses(sequenceDuration: Long, count: Int, exposure: Long, gap: Long): LongArray {
        val intervals = timeIntervals(sequenceDuration, count, exposure, gap)
        return LongArray(count) { i -> intervals[i + 1] - intervals[i] - exposure }
    }

    /**
     * Pauses between shots with overlaps resolved: where the curve asks for shots faster
     * than `exposure + gap` allows, as many evenly spaced shots as fit are kept and the
     * rest are dropped. The result can therefore be shorter than [count].
     */
    fun pauses(sequenceDuration: Long, count: Int, exposure: Long, gap: Long): LongArray {
        val overlapIndices = ArrayList<Int>()
        val adjusted = ArrayList<Long>()
        val intervals = timeIntervals(sequenceDuration, count, exposure, gap)
        val pauses = LongArray(count)
        var inNegativeSequence = false
        for (i in 0 until count) {
            pauses[i] = intervals[i + 1] - intervals[i] - exposure
            if (pauses[i] <= 0 && !inNegativeSequence) {
                overlapIndices.add(i); inNegativeSequence = true
            } else if (pauses[i] > 0 && inNegativeSequence) {
                overlapIndices.add(i); inNegativeSequence = false
            }
        }
        var overlapCount = 0
        var i = 0
        while (i < count) {
            if (pauses[i] > 0) {
                adjusted.add(pauses[i])
            } else if (overlapIndices.size > 0) {
                val startIndex = overlapIndices[overlapCount]
                val endIndex = overlapIndices[overlapCount + 1]
                val overlapPeriod = intervals[endIndex] - intervals[startIndex]
                val shots = (overlapPeriod / (exposure + gap)).toInt()
                if (shots != 0) {
                    val newGap = (overlapPeriod - exposure * shots) / shots
                    repeat(shots) { adjusted.add(newGap) }
                }
                i = endIndex
                overlapCount += 2
            }
            i++
        }
        return adjusted.toLongArray()
    }

    companion object {
        const val MAX_VALUE = 0.98f
        const val MIN_VALUE = 0.02f
    }
}
