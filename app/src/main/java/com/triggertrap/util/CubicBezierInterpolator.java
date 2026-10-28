package com.triggertrap.util;

import android.view.animation.Interpolator;

import com.triggertrap.core.CubicBezier;

/** Android {@link Interpolator} view of the core {@link CubicBezier} curve (TimeWarp). */
public class CubicBezierInterpolator implements Interpolator {

    private final CubicBezier mCurve;

    public CubicBezierInterpolator(float x1, float y1, float x2, float y2) {
        mCurve = new CubicBezier(x1, y1, x2, y2);
    }

    public CubicBezier getCurve() {
        return mCurve;
    }

    public void setControlPoints(float x1, float y1, float x2, float y2) {
        mCurve.setControlPoints(x1, y1, x2, y2);
    }

    @Override
    public float getInterpolation(float input) {
        return mCurve.interpolate(input);
    }

    public long[] getOriginalPauses(long sequenceDuration, int count, long exposure, long gap) {
        return mCurve.originalPauses(sequenceDuration, count, exposure, gap);
    }

    public long[] getPauses(long sequenceDuration, int count, long exposure, long gap) {
        return mCurve.pauses(sequenceDuration, count, exposure, gap);
    }
}
