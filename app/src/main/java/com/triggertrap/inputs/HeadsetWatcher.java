package com.triggertrap.inputs;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class HeadsetWatcher {

    public static final int HEADSET_PLUGGED = 1;
    public static final int HEADSET_UNPLUGGED = 0;

    private static final String TAG = HeadsetWatcher.class.getSimpleName();

    private final HeadsetBroadcastReceiver headsetReceiver;

    //Support two listeners Primary is always set secondary is transient
    private final ArrayList<HeadsetListener> mListeners;

    public interface HeadsetListener {
        void onHeadsetChanged(int state);
    }

    public HeadsetWatcher(Context ctx, HeadsetListener listener) {
        headsetReceiver = new HeadsetBroadcastReceiver(this);
        // ACTION_HEADSET_PLUG is a protected system broadcast, so the receiver does not have to be exported
        ContextCompat.registerReceiver(ctx, headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        mListeners = new ArrayList<>();
        mListeners.add(listener);
    }

    public void changed(int state) {
        Log.d(TAG, "State changed " + state);
        for (HeadsetListener listener : mListeners) {
            if (listener != null) {
                listener.onHeadsetChanged(state);
            }
        }
    }

    public void addSecondryListener(HeadsetListener listener) {
        mListeners.add(1, listener);
    }

    public void unregister(Context ctx) {
        ctx.unregisterReceiver(headsetReceiver);
    }

    public static class HeadsetBroadcastReceiver extends BroadcastReceiver {
        protected final HeadsetWatcher watcher;

        public HeadsetBroadcastReceiver(HeadsetWatcher watcher) {
            super();
            this.watcher = watcher;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                int headsetState = intent.getIntExtra("state", 0);
                watcher.changed(headsetState);
            }
        }
    }
}
