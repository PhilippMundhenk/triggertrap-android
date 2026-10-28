package com.triggertrap.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CubicBezierTest {
    @Test fun controlPointsAreClamped() {
        val b = CubicBezier(-1f, 2f, 0.3f, 0.7f)
        assertEquals(CubicBezier.MIN_VALUE, b.x1); assertEquals(CubicBezier.MAX_VALUE, b.y1)
        assertEquals(0.3f, b.x2); assertEquals(0.7f, b.y2)
        b.setControlPoints(0.4f, 0.1f, 5f, -5f)
        assertEquals(0.4f, b.x1); assertEquals(0.1f, b.y1)
        assertEquals(CubicBezier.MAX_VALUE, b.x2); assertEquals(CubicBezier.MIN_VALUE, b.y2)
    }

    @Test fun linearCurveIsIdentity() {
        val b = CubicBezier(0.5f, 0.5f, 0.5f, 0.5f)
        for (x in listOf(0f, 0.25f, 0.5f, 0.9f, 1f)) assertEquals(x, b.interpolate(x))
    }

    @Test fun easeInCurveStartsSlow() {
        val b = CubicBezier(0.5f, 0.0f, 0.5f, 1.0f) // the default TimeWarp curve of the app
        assertTrue(b.interpolate(0.25f) < 0.25f)
        assertTrue(b.interpolate(0.75f) > 0.75f)
        assertEquals(0.5f, b.interpolate(0.5f), 0.01f)
        assertEquals(0f, b.interpolate(0f), 0.001f)
        assertEquals(1f, b.interpolate(1f), 0.001f)
    }

    @Test fun flatCurveWhereTheSlopeIsZero() {
        // x1 == x2 at the extremes makes the slope at t=0 vanish: solver returns the guess
        val b = CubicBezier(0f, 0.5f, 1f, 0.5f)
        assertEquals(0f, b.interpolate(0f), 0.01f)
    }

    @Test fun originalPausesOnALinearCurve() {
        val b = CubicBezier(0.5f, 0.5f, 0.5f, 0.5f)
        // 5 shots over 4 s: one per second; last pause reaches the sentinel one interval later
        assertArrayEquals(longArrayOf(900, 900, 900, 900, 1000), b.originalPauses(4000, 5, 100, 1000))
    }

    @Test fun pausesWithoutOverlapAreUnchanged() {
        val b = CubicBezier(0.5f, 0.5f, 0.5f, 0.5f)
        assertArrayEquals(b.originalPauses(4000, 5, 100, 10), b.pauses(4000, 5, 100, 10))
    }

    @Test fun overlappingShotsAreThinnedOut() {
        // 100 shots in 2 s with 150 ms exposures cannot fit: the curve is linear, so every
        // pause is negative and only as many shots as fit (2000 / 160 = 12) remain.
        val b = CubicBezier(0.5f, 0.5f, 0.5f, 0.5f)
        val p = b.pauses(2000, 100, 150, 10)
        assertTrue(p.size < 100)
        assertTrue(p.all { it > 0 })
        val total = p.sum() + p.size * 150L
        assertTrue("total $total", total <= 2000 + 160)
    }

    @Test fun overlapInTheMiddleOfAnEaseInOutCurve() {
        // strong ease-in: the first shots are far apart, the last ones overlap
        val b = CubicBezier(0.98f, 0.02f, 0.98f, 0.02f)
        val original = b.originalPauses(10000, 50, 150, 10)
        assertTrue(original.any { it <= 0 })
        val p = b.pauses(10000, 50, 150, 10)
        assertTrue(p.isNotEmpty())
        assertTrue(p.all { it > 0 })
    }

    @Test fun overlapTooShortForASingleShotDropsThem() {
        val b = CubicBezier(0.5f, 0.5f, 0.5f, 0.5f)
        // 3 shots in 10 ms with 150 ms exposure: nothing fits, every pause is dropped
        val p = b.pauses(10, 3, 150, 10)
        assertEquals(0, p.size)
    }
}
