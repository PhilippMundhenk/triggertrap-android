package com.triggertrap.inputs;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.triggertrap.TTApp;

/**
 * Sound sensor: reads the microphone, reports the level as a percentage and fires when it
 * exceeds the threshold. The recorder is created lazily on {@link #start()} because it
 * needs the RECORD_AUDIO runtime permission.
 */
public class MicVolumeMonitor {

    private static final String TAG = MicVolumeMonitor.class.getSimpleName();

    private static final int VOLUME_UPDATE_INTERVAL = 10;
    public static final int MAX_VOL_RANGE = Short.MAX_VALUE;
    public static final int MIN_VOL_RANGE = 500;
    public static final int DEFAULT_VOL_RANGE = 5000;

    public static final int SAMPLE_RATE = 16000;

    private final Context mContext;
    private AudioRecord mRecorder;
    private int mVolumeRange = DEFAULT_VOL_RANGE;
    private int mThreshold = (DEFAULT_VOL_RANGE / 2);
    private short[] mBuffer;
    private volatile boolean mIsRecording = false;
    private final VolumeListener mListener;
    private volatile boolean mIsThresholdEnabled = false;
    private long lastUpdateTime = System.currentTimeMillis();
    private long lastTriggerTime = System.currentTimeMillis();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public MicVolumeMonitor(Context context, VolumeListener listener) {
        mContext = context.getApplicationContext();
        mListener = listener;
    }

    public interface VolumeListener {
        void onVolumeUpdate(int amplitude);

        void onExceedThreshold(int amplitude);
    }

    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean initRecorder() {
        if (mRecorder != null) {
            return true;
        }
        if (!hasPermission(mContext)) {
            Log.w(TAG, "RECORD_AUDIO not granted");
            return false;
        }
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize <= 0) {
            return false;
        }
        try {
            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                recorder.release();
                return false;
            }
            mBuffer = new short[bufferSize];
            mRecorder = recorder;
            return true;
        } catch (SecurityException | IllegalArgumentException e) {
            Log.w(TAG, "AudioRecord not available", e);
            return false;
        }
    }

    private void startBufferedRead() {
        Thread reader = new Thread(() -> {
            while (mIsRecording) {
                double sum = 0;
                int readSize = mRecorder.read(mBuffer, 0, mBuffer.length);
                for (int i = 0; i < readSize; i++) {
                    sum += mBuffer[i] * mBuffer[i];
                }
                if (readSize > 0) {
                    final double amplitude = sum / readSize;
                    int rmsAmplitude = (int) Math.sqrt(amplitude);
                    if (mListener != null) {
                        rmsAmplitude = Math.min(rmsAmplitude, mVolumeRange);
                        //Get the amplitude as a percentage in the range 0-100
                        final int percentAmplitude = (int) ((float) (rmsAmplitude) / mVolumeRange * 100);
                        final long currentTime = System.currentTimeMillis();

                        if (currentTime - lastUpdateTime > VOLUME_UPDATE_INTERVAL) {
                            lastUpdateTime = currentTime;
                            mListener.onVolumeUpdate(percentAmplitude);
                        }
                        if (rmsAmplitude > mThreshold && mIsThresholdEnabled) {
                            TTApp app = TTApp.getInstance(mContext);
                            if (currentTime - lastTriggerTime > app.getSensorResetDelay()) {
                                lastTriggerTime = currentTime;
                                //Trigger the output after the sensor delay
                                handler.postDelayed(() -> mListener.onExceedThreshold(percentAmplitude), app.getSensorDelay());
                            }
                        }
                    }
                } else if (mListener != null) {
                    //Pass 0 amplitude if we have no sample.
                    mListener.onVolumeUpdate(0);
                }
            }
        }, "triggertrap-mic");
        reader.setDaemon(true);
        reader.start();
    }

    /** @return false when the microphone cannot be opened (no permission, no recorder). */
    public boolean start() {
        if (!mIsRecording) {
            if (!initRecorder()) {
                return false;
            }
            mIsRecording = true;
            mRecorder.startRecording();
            startBufferedRead();
        }
        return true;
    }

    public void stop() {
        if (mIsRecording) {
            mIsRecording = false;
            if (mRecorder != null) {
                mRecorder.stop();
            }
        }
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public void enabledThreshold() {
        mIsThresholdEnabled = true;
    }

    public void disableThreshold() {
        mIsThresholdEnabled = false;
    }

    public boolean getThresholdEnabled() {
        return mIsThresholdEnabled;
    }

    public void release() {
        stop();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }

    public void setMicSensitivity(float percentage) {
        //Calculate current threshold percentage
        float thresholdPercentage = ((float) mThreshold / mVolumeRange) * 100;

        percentage = (percentage < 0) ? 0 : percentage;
        percentage = (percentage > 100) ? 100 : percentage;
        final int volumeRange = MAX_VOL_RANGE - MIN_VOL_RANGE;
        final int percentageRange = (int) (volumeRange * percentage / 100);
        mVolumeRange = MAX_VOL_RANGE - percentageRange;
        //Reset threshold
        setThreshold(thresholdPercentage);
    }

    public void setThreshold(float percentage) {
        percentage = (percentage < 0) ? 0 : percentage;
        percentage = (percentage > 100) ? 100 : percentage;
        mThreshold = (int) (mVolumeRange * percentage / 100);
    }
}
