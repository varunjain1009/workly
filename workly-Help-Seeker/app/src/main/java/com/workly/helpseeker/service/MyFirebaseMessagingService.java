package com.workly.helpseeker.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.workly.helpseeker.data.auth.AuthManager;
import com.workly.helpseeker.data.network.ApiService;
import com.workly.helpseeker.data.config.ConfigManager;
import com.workly.helpseeker.MainActivity;
import com.workly.helpseeker.util.AppLogger;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    AuthManager authManager;

    @Inject
    ApiService apiService;

    @Inject
    ConfigManager configManager;

    @Inject
    AppLogger appLogger;

    @Override
    public void onNewToken(@NonNull String token) {
        appLogger.d(TAG, "MyFirebaseMessagingService(Seeker): New FCM token received: " + token);
        if (authManager.isLoggedIn()) {
            java.util.Map<String, String> tokenMap = java.util.Collections.singletonMap("token", token);
            apiService.updateDeviceToken(tokenMap).enqueue(new retrofit2.Callback<com.workly.helpseeker.data.network.ApiResponse<Void>>() {
                @Override
                public void onResponse(retrofit2.Call<com.workly.helpseeker.data.network.ApiResponse<Void>> call,
                        retrofit2.Response<com.workly.helpseeker.data.network.ApiResponse<Void>> response) {
                    appLogger.d(TAG, "MyFirebaseMessagingService(Seeker): token sync HTTP " + response.code());
                }

                @Override
                public void onFailure(retrofit2.Call<com.workly.helpseeker.data.network.ApiResponse<Void>> call, Throwable t) {
                    appLogger.e(TAG, "MyFirebaseMessagingService(Seeker): token sync failed", t);
                }
            });
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        appLogger.d(TAG, "MyFirebaseMessagingService(Seeker): Message from: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            appLogger.d(TAG, "MyFirebaseMessagingService(Seeker): Data payload: " + remoteMessage.getData());

            String title = remoteMessage.getData().get("title");
            String type = remoteMessage.getData().get("type");
            if ("CONFIG_UPDATE".equals(title) || "CONFIG_UPDATE".equals(type)) {
                appLogger.d(TAG, "MyFirebaseMessagingService(Seeker): Config update received - triggering sync");
                if (configManager != null) {
                    configManager.syncConfig();
                }
            } else if ("JOB_ACCEPTED".equals(type)) {
                String jobId = remoteMessage.getData().get("jobId");
                appLogger.d(TAG, "MyFirebaseMessagingService(Seeker): Job accepted push for jobId: " + jobId);
                sendNotification(title != null ? title : "Job Accepted",
                        remoteMessage.getData().get("body") != null ? remoteMessage.getData().get("body")
                                : "Your job has been accepted by a provider",
                        jobId);
                notifyJobsUpdated(jobId);
            } else if ("JOB_COMPLETED".equals(type)) {
                String jobId = remoteMessage.getData().get("jobId");
                sendNotification(title != null ? title : "Job Completed",
                        remoteMessage.getData().get("body") != null ? remoteMessage.getData().get("body")
                                : "Your job has been completed",
                        jobId);
                notifyJobsUpdated(jobId);
            }
        }
    }

    private void notifyJobsUpdated(String jobId) {
        android.content.Intent intent = new android.content.Intent("SEEKER_JOBS_UPDATED_EVENT");
        if (jobId != null) {
            intent.putExtra("jobId", jobId);
        }
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendNotification(String title, String message, String jobId) {
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        String channelId = "workly_seeker_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Job Updates", NotificationManager.IMPORTANCE_HIGH);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (jobId != null) {
            intent.putExtra("jobId", jobId);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                jobId != null ? jobId.hashCode() : 1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify(jobId != null ? jobId.hashCode() : 1001, builder.build());
        }
    }
}
