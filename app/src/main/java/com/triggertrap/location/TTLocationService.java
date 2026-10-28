package com.triggertrap.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * Distance updates for Distance Lapse from the platform {@link LocationManager} (GPS, with
 * the network provider as a fallback). Replaces the Google Play Services location client.
 */
public class TTLocationService implements android.location.LocationListener {

    private static final String TAG = TTLocationService.class.getSimpleName();
    private static final long UPDATE_INTERVAL_MS = 1000;
    private static final float UPDATE_DISTANCE_M = 0;

    private final Context mContext;
    private final LocationManager mLocationManager;
    private double mStartLatitude = 0;
    private double mStartLongtitude = 0;
    private LocationListener mListener;
    private boolean mRunning = false;

    public interface LocationListener {
        void onDistanceChanged(float distance, float speed);
    }

    public TTLocationService(Activity parentActivity) {
        mContext = parentActivity.getApplicationContext();
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
    }

    /** True when the fine location permission has been granted. */
    public boolean servicesConnected() {
        return ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isRunning() {
        return mRunning;
    }

    public void startLocationService() {
        mStartLatitude = 0;
        mStartLongtitude = 0;
        if (!servicesConnected() || mLocationManager == null) {
            Log.w(TAG, "No location permission, distance lapse cannot start");
            return;
        }
        try {
            if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL_MS, UPDATE_DISTANCE_M, this);
            }
            if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL_MS, UPDATE_DISTANCE_M, this);
            }
            mRunning = true;
        } catch (SecurityException e) {
            Log.w(TAG, "Location permission revoked", e);
        }
    }

    public void stopLocationService() {
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }
        mRunning = false;
    }

    public void setListener(LocationListener listener) {
        mListener = listener;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        //Zeroth position gives distance in meters.
        float[] results = new float[3];

        if (mStartLatitude != 0) {
            Location.distanceBetween(mStartLatitude, mStartLongtitude, location.getLatitude(), location.getLongitude(), results);
        } else {
            results[0] = 0;
        }

        mStartLatitude = location.getLatitude();
        mStartLongtitude = location.getLongitude();

        if (mListener != null) {
            mListener.onDistanceChanged(results[0], location.getSpeed());
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }
}
