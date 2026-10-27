package com.triggertrap.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseWaveformTest {
    private class Events : PulseWaveform.Listener {
        var starts = 0; var stops = 0; var pauses = 0
        override fun onPulseStart() { starts++ }
        override fun onPulseStop() { stops++ }
        override fun onPauseDone() { pauses++ }
    }

    @Test fun toneIs17kHzFullScale() {
        val w = PulseWaveform()
        assertEquals(PulseWaveform.SAMPLE_COUNT, w.samples.size)
        assertEquals(0, w.samples[0].toInt())
        assertTrue(w.samples.maxOrNull()!! > 32000)
        assertTrue(w.samples.minOrNull()!! < -32000)
        // 17 kHz at 44.1 kHz: 1700 cycles = about 3400 zero crossings in 4410 samples
        var crossings = 0
        for (i in 1 until w.samples.size) if ((w.samples[i] >= 0) != (w.samples[i - 1] >= 0)) crossings++
        assertTrue("crossings $crossings", crossings in 3350..3450)
    }

    @Test fun idleUntilPlay() {
        val w = PulseWaveform()
        assertTrue(w.paused)
        assertEquals(-1, w.pauseRemaining())
        w.play(100)
        assertFalse(w.paused)
        assertEquals(100, w.timeRemaining())
        assertEquals(-1, w.pauseRemaining())
    }

    @Test fun pulseThenSilenceThenPauseDone() {
        val e = Events()
        val w = PulseWaveform(e)
        w.play(50, 50)
        assertEquals(1, e.starts)
        assertEquals(50, w.timeRemaining()); assertEquals(50, w.pauseRemaining())
        // 50 ms = 4410 counter steps = exactly one block: tone, ending with the stop sample
        w.fillStereo()
        assertEquals(0, w.limit)
        assertEquals(1, e.stops)
        assertFalse(w.paused) // a timed pause keeps the writer running
        // right channel is the inverted left channel, left starts with the NX silence
        for (i in 0 until PulseWaveform.NX_DELAY_SAMPLES) assertEquals(0, w.buffer[i * 2].toInt())
        assertEquals(w.samples[100].toInt(), w.buffer[200].toInt())
        assertEquals(-w.samples[100].toInt(), w.buffer[201].toInt())
        val last = PulseWaveform.SAMPLE_COUNT - 1
        assertEquals(w.samples[last].toInt(), w.buffer[last * 2].toInt()) // the stop sample is on both channels
        assertEquals(w.samples[last].toInt(), w.buffer[last * 2 + 1].toInt())
        // next block: silence counting the pause down to zero
        w.fillStereo()
        assertEquals(0, w.pause)
        assertEquals(0, e.pauses)
        assertEquals(0, w.buffer[2].toInt()); assertEquals(0, w.buffer[3].toInt())
        // the first sample of the following block finishes the pause
        w.fillStereo()
        assertEquals(1, e.pauses)
        assertTrue(w.paused)
        assertEquals(-1, w.pauseRemaining())
    }

    @Test fun pulseWithoutPauseGoesIdleAfterThePulse() {
        val e = Events()
        val w = PulseWaveform(e)
        w.play(10)
        assertEquals(882, w.limit)
        w.fillStereo()
        assertEquals(1, e.stops)
        assertTrue(w.paused)
        assertEquals(PulseWaveform.FOREVER, w.pause)
    }

    @Test fun zeroLengthPlaysOneStep() {
        val w = PulseWaveform()
        w.play(0, 0)
        assertEquals(1, w.limit)
        assertEquals(PulseWaveform.FOREVER, w.pause)
    }

    @Test fun foreverPulseRunsUntilStop() {
        val e = Events()
        val w = PulseWaveform(e)
        w.play(PulseWaveform.FOREVER)
        assertEquals(-1, w.timeRemaining())
        val before = w.limit
        w.fillStereo()
        assertEquals(before, w.limit) // FOREVER is not counted down
        w.stop()
        assertTrue(w.paused)
        assertEquals(1, w.limit)
        w.fillStereo()
        assertEquals(1, e.stops)
    }
}
