package com.workly.helpprovider.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.workly.helpprovider.data.remote.ApiService;
import com.workly.helpprovider.ui.main.MainActivity;
import com.workly.helpprovider.util.LocationHelper;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class AvailabilityService extends Service implements LocationHelper.OnLocationUpdateListener {
    private static final String CHANNEL_ID = "AvailabilityChannel";
    private LocationHelper locationHelper;

    @Inject
    ApiService apiService;

    @Override
    public void onCreate() {
        super.onCreate();
        locationHelper = new LocationHelper(this, this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Availability Active")
                .setContentText("You are currently online and visible to customers.")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
        apiService.getPublicConfig().enqueue(
                new Callback<com.workly.helpprovider.data.remote.ApiResponse<com.workly.helpprovider.data.model.ConfigResponse>>() {
                    @Override
                    public void onResponse(
                            Call<com.workly.helpprovider.data.remote.ApiResponse<com.workly.helpprovider.data.model.ConfigResponse>> call,
                            Response<com.workly.helpprovider.data.remote.ApiResponse<com.workly.helpprovider.data.model.ConfigResponse>> response) {
                        long interval = 30 * 60 * 1000; // Default 30 mins
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            interval = response.body().getData().getLocationUpdateIntervalMinutes() * 60 * 1000L;
                        }
                        locationHelper.startLocationUpdates(interval);
                    }

                    @Override
                    public void onFailure(
                            Call<com.workly.helpprovider.data.remote.ApiResponse<com.workly.helpprovider.data.model.ConfigResponse>> call,
                            Throwable t) {
                        locationHelper.startLocationUpdates(30 * 60 * 1000); // Default fallback
                    }
                });

        return START_STICKY;
    }

    @Override
    public void onLocationUpdate(Location location) {
        Map<String, Double> locData = new HashMap<>();
        locData.put("latitude", location.getLatitude());
        locData.put("longitude", location.getLongitude());

        apiService.updateLocation(locData).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Availability Service Channel",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationHelper != null) {
            locationHelper.stopLocationUpdates();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
