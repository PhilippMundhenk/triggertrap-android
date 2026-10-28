package com.triggertrap.outputs;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.triggertrap.core.PulseWaveform;

/**
 * Plays the trigger signal through the audio output. The signal itself and the pulse /
 * pause timing come from {@link PulseWaveform}; this class owns the {@link AudioTrack} and
 * the writer thread.
 *
 * Between pulses the writer normally sleeps and the track starves. With the
 * {@link #KEEP_STREAM_OPEN_SETTING} setting on it streams silence instead, so a Bluetooth
 * receiver never suspends the link and does not clip the start of the next pulse.
 */
public class AudioBeeper implements IBeeper, PulseWaveform.Listener {

    private static final String TAG = AudioBeeper.class.getSimpleName();
    public static final long FOREVER = PulseWaveform.FOREVER;
    /** Settings key (default shared preferences): stream silence between pulses. */
    public static final String KEEP_STREAM_OPEN_SETTING = "pref_keep_audio_stream_open";

    private static final short[] SILENCE = new short[PulseWaveform.WRITE_SHORTS];
    private static final long IDLE_SLEEP_MS = 10;

    private final PulseWaveform mWaveform = new PulseWaveform(this);
    private AudioTrack track;
    private volatile boolean writingSamples = true;
    private volatile boolean keepStreamOpen = false;
    private volatile long silenceBlocksWritten = 0;
    private final AudioBeeperListener mListener;
    private final SharedPreferences mPrefs;
    // SharedPreferences keeps its listeners weakly: this field keeps the listener alive
    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener = (prefs, key) -> {
        if (KEEP_STREAM_OPEN_SETTING.equals(key)) {
            setKeepStreamOpen(prefs.getBoolean(KEEP_STREAM_OPEN_SETTING, false));
        }
    };

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

        if (context != null) {
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            keepStreamOpen = mPrefs.getBoolean(KEEP_STREAM_OPEN_SETTING, false);
            mPrefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
        } else {
            mPrefs = null;
        }

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
                } else if (keepStreamOpen) {
                    // idle, but the output must not sleep: a block of silence paces the loop
                    if (track != null) {
                        track.write(SILENCE, 0, PulseWaveform.WRITE_SHORTS);
                    } else if (!sleep(PulseWaveform.BLOCK_MS)) {
                        return;
                    }
                    silenceBlocksWritten++;
                } else {
                    // nothing to play: do not spin
                    if (!sleep(IDLE_SLEEP_MS)) {
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

    private static boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            return false;
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

    /** Stream silence between pulses (Bluetooth receivers) instead of letting the track starve. */
    public void setKeepStreamOpen(boolean keepOpen) {
        keepStreamOpen = keepOpen;
    }

    public boolean isKeepingStreamOpen() {
        return keepStreamOpen;
    }

    /** Blocks of silence written so far while idle; grows only while the stream is kept open. */
    public long getSilenceBlocksWritten() {
        return silenceBlocksWritten;
    }

    public void close() {
        writingSamples = false;
        if (mPrefs != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
        }
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
