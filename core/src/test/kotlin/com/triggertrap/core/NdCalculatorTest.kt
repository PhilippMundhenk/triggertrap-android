package com.triggertrap.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NdCalculatorTest {
    private val table = NdCalculator.SHUTTER_SPEEDS_MS

    @Test fun tableIsAscending() {
        for (i in 1 until table.size) assertTrue(table[i] > table[i - 1])
        assertEquals(55, table.size)
    }

    @Test fun exposureDoublesPerStop() {
        assertEquals(2.0, NdCalculator.exposureMillis(9, 1), 1e-9)   // 1 ms, 1 stop
        assertEquals(8000.0, NdCalculator.exposureMillis(39, 3), 1e-9) // 1 s, 3 stops
    }

    @Test fun nearestStandardSpeedWithinTheTable() {
        // 1/125 s (8 ms) + 3 stops = 64 ms -> nearest table entry is 67 ms (index 27)
        assertEquals(NdCalculator.Result.Nearest(27), NdCalculator.calculate(18, 3))
        // 1 ms + 1 stop = 2 ms exactly -> index 12
        assertEquals(NdCalculator.Result.Nearest(12), NdCalculator.calculate(9, 1))
        // 0.12 ms + 1 stop = 0.24 ms is nearer 0.25 (index 3) than 0.2
        assertEquals(NdCalculator.Result.Nearest(3), NdCalculator.calculate(0, 1))
        // a tie goes to the lower entry: 0.5 + 0.63 midpoint is 0.565; 0.4 ms is between 0.31 and 0.5
        assertEquals(NdCalculator.Result.Nearest(5), NdCalculator.calculate(2, 1)) // 0.2 * 2 = 0.4 exactly
    }

    @Test fun longExposuresAreShownAsTimer() {
        // 30 s + 1 stop = 60 s
        val r = NdCalculator.calculate(54, 1) as NdCalculator.Result.Timed
        assertEquals(60000, r.millis)
        assertEquals("0h 01m 00s", r.text)
        // exactly the last table entry counts as timed
        assertEquals(NdCalculator.Result.Timed(30000, "0h 00m 30s"), NdCalculator.calculate(54, 0))
    }

    @Test fun veryLongExposuresAreShownInHours() {
        // 30 s + 14 stops = 491520 s = 136 h
        assertEquals(NdCalculator.Result.Hours(136), NdCalculator.calculate(54, 14))
    }

    @Test fun formatHms() {
        assertEquals("1h 01m 05s", NdCalculator.formatHms(3_665_000))
        assertEquals("0h 00m 00s", NdCalculator.formatHms(0))
    }
}
