package com.triggertrap.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseSequencesTest {
    @Test fun sequenceTimeSumsPulsesAndPauses() {
        assertEquals(0, PulseSequences.sequenceTime(longArrayOf()))
        assertEquals(1500, PulseSequences.sequenceTime(longArrayOf(500, 1000)))
        assertEquals(60, PulseSequences.sequenceTime(longArrayOf(10, 20, 10, 20)))
    }

    @Test fun timeLapseIntervalIncludesThePulse() {
        assertArrayEquals(longArrayOf(150, 850), PulseSequences.timeLapse(1000, 150))
        assertEquals(1000, PulseSequences.sequenceTime(PulseSequences.timeLapse(1000, 150)))
    }

    @Test fun starTrailRepeatsExposureAndGap() {
        assertArrayEquals(longArrayOf(90000, 5000, 90000, 5000, 90000, 5000), PulseSequences.starTrail(3, 90000, 5000))
        assertArrayEquals(longArrayOf(), PulseSequences.starTrail(0, 1, 1))
    }

    @Test fun hdrBracketsAroundTheMiddleExposure() {
        // 3 exposures, 1 EV apart around 2 s: 1 s, 2 s, 4 s with the gap between them
        assertArrayEquals(longArrayOf(1000, 150, 2000, 150, 4000, 150), PulseSequences.hdr(2000, 3, 1f, 0, 150))
        // half-stop steps
        val half = PulseSequences.hdr(1000, 3, 0.5f, 0, 100)
        assertEquals(707, half[0]); assertEquals(1000, half[2]); assertEquals(1414, half[4])
    }

    @Test fun hdrEvenCountIsRoundedUpToOdd() {
        assertEquals(5 * 2, PulseSequences.hdr(1000, 4, 1f, 0, 100).size)
        assertEquals(1 * 2, PulseSequences.hdr(1000, 1, 1f, 0, 100).size)
    }

    @Test fun hdrTimelapseStretchesTheLastPauseToTheInterval() {
        val s = PulseSequences.hdr(2000, 3, 1f, 20000, 150)
        assertEquals(20000, PulseSequences.sequenceTime(s))
        assertEquals(20000 - (1000 + 150 + 2000 + 150 + 4000), s[5])
    }

    @Test fun brampingRampsLinearly() {
        val s = PulseSequences.bramping(3, 10000, 2000, 8000)
        assertArrayEquals(longArrayOf(2000, 8000, 5000, 5000, 8000, 2000), s)
        assertEquals(30000, PulseSequences.sequenceTime(s))
    }

    @Test fun timeWarpUsesTheCurveAndNeverPausesBelowOneMs() {
        val linear = CubicBezier(0.5f, 0.5f, 0.5f, 0.5f)
        val s = PulseSequences.timeWarp(5, 4000, linear, 150, 10)
        assertEquals(10, s.size)
        for (i in 0 until 5) assertEquals(150, s[i * 2])
        assertTrue(s.filterIndexed { i, _ -> i % 2 == 1 }.all { it >= 1 })
        // every pulse starts 1000 ms after the previous one on a linear curve
        assertEquals(850, s[1]); assertEquals(850, s[3])
    }

    @Test fun easedTimeLapseKeepsEveryGapAtLeastTheMinimum() {
        for (easing in PulseSequences.Easing.values()) {
            val s = PulseSequences.easedTimeLapse(2.0, 4, 10000, easing, 150)
            assertEquals(8, s.size)
            for (i in 0 until 4) {
                assertEquals(150, s[i * 2])
                assertEquals(10, s[i * 2 + 1]) // minimum gap (beep + 10 ms buffer) minus the beep
            }
        }
    }

    @Test fun easingFunctions() {
        assertEquals(0.25, PulseSequences.easeIn(0.5, 2.0), 1e-9)
        assertEquals(0.75, PulseSequences.easeOut(0.5, 2.0), 1e-9)
        assertEquals(0.125, PulseSequences.easeInOut(0.25, 2.0), 1e-9)
        assertEquals(0.875, PulseSequences.easeInOut(0.75, 2.0), 1e-9)
    }
}
