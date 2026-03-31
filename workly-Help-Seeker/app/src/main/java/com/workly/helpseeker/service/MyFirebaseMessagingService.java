package com.workly.helpseeker.service;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.workly.helpseeker.data.auth.AuthManager;
import com.workly.helpseeker.data.config.ConfigManager;
import com.workly.helpseeker.util.AppLogger;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    AuthManager authManager;

    @Inject
    ConfigManager configManager;

    @Inject
    AppLogger appLogger;

    @Override
    public void onNewToken(@NonNull String token) {
        appLogger.d(TAG, "MyFirebaseMessagingService(Seeker): New FCM token received: " + token);
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
                
                // Show OS notification
                sendNotification(title != null ? title : "Job Accepted", "Your job has been accepted by a provider", jobId);
                
                // Fire local broadcast
                if (jobId != null) {
                    android.content.Intent intent = new android.content.Intent("JOB_ACCEPTED_EVENT");
                    intent.putExtra("jobId", jobId);
                    androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                }
            }
        }
    }

    private void sendNotification(String title, String message, String jobId) {
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        String channelId = "workly_seeker_channel";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, "Job Updates", android.app.NotificationManager.IMPORTANCE_HIGH);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH);

        if (notificationManager != null) {
            notificationManager.notify(jobId != null ? jobId.hashCode() : 1001, builder.build());
        }
    }
}
