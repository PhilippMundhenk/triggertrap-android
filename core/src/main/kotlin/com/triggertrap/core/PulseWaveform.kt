package com.triggertrap.core

/**
 * The signal the dongle listens to: a 17 kHz sine on both channels, the right channel
 * inverted, with a short silence at the start of every block on the left (shutter) channel
 * for Samsung NX cameras. Produces stereo PCM blocks and counts the pulse / pause down in
 * samples; the audio output only has to pump [fillStereo] into the track.
 *
 * Timing is kept exactly as in the original app: a pulse of `length` ms is `length * 441 / 5`
 * counter steps, one step per generated mono sample, while the output writes only the first
 * half of every generated block (see [WRITE_SHORTS]); both together give real time.
 */
class PulseWaveform(private val listener: Listener? = null) {

    interface Listener {
        fun onPulseStart()
        fun onPulseStop()
        fun onPauseDone()
    }

    /** Mono tone samples of one block. */
    val samples: ShortArray = ShortArray(SAMPLE_COUNT).also { s ->
        val increment = 2 * Math.PI * WAVE_FREQUENCY / SAMPLE_RATE
        var angle = 0.0
        for (i in s.indices) {
            s[i] = (Math.sin(angle) * Short.MAX_VALUE).toInt().toShort()
            angle += increment
            if (angle > 2 * Math.PI) angle -= 2 * Math.PI
        }
    }

    /** Interleaved stereo block filled by [fillStereo]. */
    val buffer: ShortArray = ShortArray(SAMPLE_COUNT * 2)

    @Volatile var limit: Long = 0; private set
    @Volatile var pause: Long = FOREVER; private set
    /** True while nothing has to be written (idle); the audio thread sleeps instead of filling. */
    @Volatile var paused: Boolean = true; private set

    /**
     * Press the shutter for [lengthMs] (FOREVER = until [stop]), then release for [pauseMs]
     * (0 or FOREVER = no timed pause, the output goes idle after the pulse).
     *
     * The original multiplied FOREVER by 441 and overflowed, which left the writer streaming
     * silence after every single pulse; FOREVER is passed through unchanged instead.
     */
    fun play(lengthMs: Long, pauseMs: Long) {
        var cycles = if (lengthMs == FOREVER) FOREVER else lengthMs * 441 / 5
        var pauseCycles = if (pauseMs == FOREVER) FOREVER else pauseMs * 441 / 5
        if (pauseCycles == 0L) pauseCycles = FOREVER
        if (cycles == 0L) cycles = 1
        limit = cycles
        pause = pauseCycles
        paused = false
        listener?.onPulseStart()
    }

    /** A single pulse of [lengthMs] without a timed pause. */
    fun play(lengthMs: Long) = play(lengthMs, 0)

    /** Release the shutter now and forget the pause. */
    fun stop() {
        paused = true
        limit = 1
        pause = FOREVER
    }

    /** Remaining pulse time in ms, -1 when the pulse runs until [stop]. */
    fun timeRemaining(): Long = if (limit == FOREVER) -1 else limit / 441 * 5

    /** Remaining pause time in ms, -1 when there is no timed pause. */
    fun pauseRemaining(): Long = if (pause == FOREVER) -1 else pause / 441 * 5

    /**
     * Fill [buffer] with the next stereo block and advance the counters. Fires
     * [Listener.onPulseStop] when the pulse ends and [Listener.onPauseDone] when the pause ends.
     */
    fun fillStereo() {
        for (i in samples.indices) {
            if (limit > 1) {
                // silence the first 44 samples of the shutter channel for Samsung NX cameras
                buffer[i * 2] = if (i < NX_DELAY_SAMPLES) 0 else samples[i]
                buffer[i * 2 + 1] = (-samples[i]).toShort()
                if (limit != FOREVER) limit--
            } else if (limit == 1L) {
                buffer[i * 2] = samples[i]
                buffer[i * 2 + 1] = samples[i]
                limit--
                if (pause == FOREVER) paused = true
                listener?.onPulseStop()
            } else {
                buffer[i * 2] = 0
                buffer[i * 2 + 1] = 0
                if (pause == 0L) {
                    pause = FOREVER
                    paused = true
                    listener?.onPauseDone()
                } else if (pause != FOREVER) {
                    pause--
                }
            }
        }
    }

    companion object {
        const val FOREVER = Long.MAX_VALUE
        const val SAMPLE_RATE = 44100
        const val WAVE_FREQUENCY = 17000.0
        const val SAMPLE_COUNT = 4410
        const val NX_DELAY_SAMPLES = 44
        /** How many shorts of [buffer] the output writes per block (the first half: 2205 frames = 50 ms). */
        const val WRITE_SHORTS = SAMPLE_COUNT
    }
}
