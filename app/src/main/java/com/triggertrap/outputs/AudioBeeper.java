package com.triggertrap.outputs;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import com.triggertrap.core.PulseWaveform;

/**
 * Plays the trigger signal through the headphone jack. The signal itself and the pulse /
 * pause timing come from {@link PulseWaveform}; this class owns the {@link AudioTrack} and
 * the writer thread.
 */
public class AudioBeeper implements IBeeper, PulseWaveform.Listener {

    private static final String TAG = AudioBeeper.class.getSimpleName();
    public static final long FOREVER = PulseWaveform.FOREVER;

    private final PulseWaveform mWaveform = new PulseWaveform(this);
    private AudioTrack track;
    private volatile boolean writingSamples = true;
    private final AudioBeeperListener mListener;

    public interface AudioBeeperListener {
        void onAudioPlayStart();

        void onAudioPlayStop();

        void onAudioPlayPauseDone();
    }

    public AudioBeeper() {
        this(null, null);
    }

    public AudioBeeper(AudioBeeperListener listener, Context context) {
        mListener = listener;

        int minSize = AudioTrack.getMinBufferSize(PulseWaveform.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        try {
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(PulseWaveform.SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build())
                    .setBufferSizeInBytes(Math.max(minSize, 0))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
        } catch (Exception e) {
            // e.g. no audio output at all on the device; the app keeps running silently
            Log.e(TAG, "AudioTrack not available", e);
            track = null;
        }

        Thread writer = new Thread(() -> {
            while (writingSamples) {
                if (!mWaveform.getPaused() && track != null) {
                    mWaveform.fillStereo();
                    track.write(mWaveform.getBuffer(), 0, PulseWaveform.WRITE_SHORTS);
                } else {
                    // nothing to play: do not spin
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }, "triggertrap-audio");
        writer.setDaemon(true);
        writer.start();

        if (track != null) {
            track.setVolume(AudioTrack.getMaxVolume());
            track.play();
        }
    }

    public void play(long length) {
        mWaveform.play(length);
    }

    public void play(long length, long pauseLength) {
        mWaveform.play(length, pauseLength);
    }

    public long timeRemaining(int channel) {
        return mWaveform.timeRemaining();
    }

    public long pauseRemaining(int channel) {
        return mWaveform.pauseRemaining();
    }

    public void stop() {
        mWaveform.stop();
    }

    public void close() {
        writingSamples = false;
        if (track != null) {
            try {
                track.flush();
                track.stop();
            } catch (IllegalStateException ignored) {
                // already stopped
            }
            track.release();
            track = null;
        }
    }

    // PulseWaveform.Listener: called on the writer thread
    @Override
    public void onPulseStart() {
        if (mListener != null) {
            mListener.onAudioPlayStart();
        }
    }

    @Override
    public void onPulseStop() {
        if (mListener != null) {
            mListener.onAudioPlayStop();
        }
    }

    @Override
    public void onPauseDone() {
        if (mListener != null) {
            mListener.onAudioPlayPauseDone();
        }
    }
}
