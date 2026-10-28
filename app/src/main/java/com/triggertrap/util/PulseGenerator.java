package com.triggertrap.util;

import android.content.Context;

import com.triggertrap.TTApp;
import com.triggertrap.core.PulseSequences;
import com.triggertrap.fragments.TimeWarpFragment;

/**
 * Pulse sequences for the fragments. The arithmetic lives in {@link PulseSequences} (core
 * module, unit tested); this class only supplies the settings (beep length, HDR gap).
 */
public class PulseGenerator {

    private final Context mAppContext;

    //Easing options.
    public interface Easing {
        int EASE_IN = 0;
        int EASE_OUT = 1;
        int EASE_IN_OUT = 2;
    }

    public PulseGenerator(Context appContext) {
        //Make sure we have an application context.
        mAppContext = appContext.getApplicationContext();
    }

    public static long getSequenceTime(long[] sequence) {
        return PulseSequences.sequenceTime(sequence);
    }

    private long beepLength() {
        return TTApp.getInstance(mAppContext).getBeepLength();
    }

    public long[] getTimeLapseSequence(long gap) {
        return PulseSequences.timeLapse(gap, beepLength());
    }

    /**
     * Generate a pulse sequence for Star trail mode
     *
     * @param count       Number of pulses
     * @param pulseLength Length of pulse exposure
     * @param gap         Length of pulse gap
     */
    public long[] getStarTrailSequence(int count, long pulseLength, long gap) {
        return PulseSequences.starTrail(count, pulseLength, gap);
    }

    /**
     * Generate a HDR or HDR timelapse sequence
     *
     * @param middle   The middle shutter time in milliseconds e.g 500
     * @param count    The Number of exposures
     * @param ev       The exposure value 0.33 , 0.5 ,1 or 2
     * @param interval The interval between HDR sequences, 0 if we do not want timelapse
     * @return The HDR pulse sequence
     */
    public long[] getHdrSequence(long middle, int count, float ev, long interval) {
        return PulseSequences.hdr(middle, count, ev, interval, TTApp.getInstance(mAppContext).getHDRGapLength());
    }

    public long[] getBrampingSequence(int count, long interval, long firstExposure, long lastExposure) {
        return PulseSequences.bramping(count, interval, firstExposure, lastExposure);
    }

    public long[] getTimeWarpSequence(int count, long sequenceDuration, CubicBezierInterpolator interpolator) {
        return PulseSequences.timeWarp(count, sequenceDuration, interpolator.getCurve(), beepLength(),
                TimeWarpFragment.MINIMUM_TIMEWARP_GAP);
    }

    public long[] getEasedTimeLapSequence(double exponent, int frames, int duration, int easingType) {
        PulseSequences.Easing easing;
        switch (easingType) {
            case Easing.EASE_IN:
                easing = PulseSequences.Easing.EASE_IN;
                break;
            case Easing.EASE_OUT:
                easing = PulseSequences.Easing.EASE_OUT;
                break;
            default:
                easing = PulseSequences.Easing.EASE_IN_OUT;
        }
        return PulseSequences.easedTimeLapse(exponent, frames, duration, easing, beepLength());
    }
}
