package com.triggertrap.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.triggertrap.TTApp;
import com.triggertrap.util.WarningMessageManager;

public class TriggertrapFragment extends Fragment {

    public interface BundleKey {
        String FRAGMENT_TAG = "fragment_tag";
        String IS_ACTION_ACTIVE = "is_action_active";
        String PULSE_INTERVAL = "pulse_interval";
        String MIDDLE_EXPOSURE = "middle_exposure";
        String NUMBER_EXPOSURES = "number_exposures";
        String EV_VALUE = "ev_value";
        String EASING = "easing";
        String WIFI_SLAVE_INFO = "wifi_slave_info";
    }

    protected Typeface SAN_SERIF_LIGHT = null;
    protected Typeface SAN_SERIF_THIN = null;

    public TriggertrapFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        SAN_SERIF_LIGHT = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf");
        SAN_SERIF_THIN = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Thin.ttf");
    }

    @Override
    public void onPause() {
        super.onPause();
        resetVolumeWarning();
    }

    protected int mRunningAction = TTApp.OnGoingAction.NONE;
    protected Bundle mStateBundle;

    protected int mState = State.STOPPED;

    protected interface State {
        int STARTED = 0;
        int STOPPED = 1;
    }

    public int getRunningAction() {
        return mRunningAction;
    }

    public Bundle getStateBundle() {
        mStateBundle = new Bundle();
        mStateBundle.putString(TriggertrapFragment.BundleKey.FRAGMENT_TAG, getTag());
        return mStateBundle;
    }

    /* After the volume warning has been shown we need to reset to show again */
    public void resetVolumeWarning() {
        sendWarningAction(WarningMessageManager.Action.RESET);
    }

    public void checkVolume() {
        sendWarningAction(WarningMessageManager.Action.SHOW);
    }

    private void sendWarningAction(int action) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        Intent intent = new Intent(WarningMessageManager.ACTION);
        intent.putExtra(WarningMessageManager.ACTION_TYPE, action);
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
    }

    public void setActionState(boolean actionState) {
    }

    public void dismissError() {
    }
}
