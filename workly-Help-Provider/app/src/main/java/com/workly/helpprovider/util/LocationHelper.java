package com.workly.helpprovider.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationHelper {
    private final FusedLocationProviderClient fusedLocationClient;
    private final LocationCallback locationCallback;
    private OnLocationUpdateListener listener;

    public interface OnLocationUpdateListener {
        void onLocationUpdate(Location location);
    }

    public LocationHelper(Context context, OnLocationUpdateListener listener) {
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.listener = listener;
        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null)
                    return;
                for (Location location : locationResult.getLocations()) {
                    if (LocationHelper.this.listener != null) {
                        LocationHelper.this.listener.onLocationUpdate(location);
                    }
                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    public void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30 * 60 * 1000)
                .setMinUpdateIntervalMillis(15 * 60 * 1000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    public void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
