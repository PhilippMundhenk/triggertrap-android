package com.triggertrap.fragments.preference;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.triggertrap.R;
import com.triggertrap.TTApp;

public class SettingsFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    public static final String PULSE_LENGTH_SETTING = "pref_pulse_length_setting";
    public static final String SENSOR_DELAY_SETTING = "pref_sensor_delay_setting";
    public static final String SENSOR_RESET_DELAY_SETTING = "pref_sensor_reset_delay_setting";
    public static final String DISTANCE_SPEED_SETTING = "pref_distance_speed_setting";
    public static final String DISTANCE_UNIT_SETTING = "pref_distance_unit_setting";

    public static final String SETTINGS_UPDATE_EVENT = "distance_unit_update_event";

    public interface SettingsType {
        int DISTANCE_UNIT = 0;
        int DISTANCES_SPEED_UNT = 1;
        int PULSE_LENGTH = 2;
    }

    public interface DistanceUnits {
        int METERS_KILOMETERS = 0;
        int MILES_YARDS = 1;
    }

    public interface DistanceSpeedUnit {
        int METERS_PER_SECOND = 0;
        int MILES_PER_HOUR = 1;
        int KILOMETERS_PER_HOUR = 2;
    }

    public interface SettingsEvent {
        String EVENT_TYPE = "event_type";
        String EVENT_VALUE = "event_value";
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);

        Preference versionPref = findPreference("pref_key_version");

        PackageInfo pInfo = null;
        try {
            pInfo = requireActivity().getPackageManager().getPackageInfo(requireActivity().getPackageName(), 0);
        } catch (NameNotFoundException e) {
            Log.w(TAG, "package info", e);
        }
        String version = getResources().getString(R.string.unknown);
        if (pInfo != null) {
            version = pInfo.versionName;
        }
        if (versionPref != null) {
            versionPref.setSummary(version);
        }

        //Set up the initial summaries
        for (String key : new String[]{PULSE_LENGTH_SETTING, SENSOR_DELAY_SETTING, SENSOR_RESET_DELAY_SETTING,
                DISTANCE_SPEED_SETTING, DISTANCE_UNIT_SETTING}) {
            ListPreference pref = findPreference(key);
            if (pref != null) {
                pref.setSummary(pref.getEntry());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key) {
        if (key == null) {
            return;
        }
        ListPreference pref = findPreference(key);
        if (pref == null) {
            return;
        }
        pref.setSummary(pref.getEntry());
        TTApp app = TTApp.getInstance(requireActivity());
        Intent intent = new Intent(SETTINGS_UPDATE_EVENT);
        switch (key) {
            case PULSE_LENGTH_SETTING: {
                long value = Long.parseLong(sharedPrefs.getString(key, ""));
                app.setBeepLength(value);
                intent.putExtra(SettingsEvent.EVENT_TYPE, SettingsType.PULSE_LENGTH);
                intent.putExtra(SettingsEvent.EVENT_VALUE, value);
                LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent);
                break;
            }
            case SENSOR_RESET_DELAY_SETTING:
                app.setSensorResetDelay(Integer.parseInt(sharedPrefs.getString(key, "")));
                break;
            case SENSOR_DELAY_SETTING:
                app.setSensorDelay(Integer.parseInt(sharedPrefs.getString(key, "")));
                break;
            case DISTANCE_SPEED_SETTING: {
                int value = Integer.parseInt(sharedPrefs.getString(key, ""));
                app.setDistancLapseSpeedUnit(value);
                intent.putExtra(SettingsEvent.EVENT_TYPE, SettingsType.DISTANCES_SPEED_UNT);
                intent.putExtra(SettingsEvent.EVENT_VALUE, value);
                LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent);
                break;
            }
            case DISTANCE_UNIT_SETTING: {
                int value = Integer.parseInt(sharedPrefs.getString(key, ""));
                app.setDistancLapseUnit(value);
                intent.putExtra(SettingsEvent.EVENT_TYPE, SettingsType.DISTANCE_UNIT);
                intent.putExtra(SettingsEvent.EVENT_VALUE, value);
                LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent);
                break;
            }
            default:
                break;
        }
    }
}
