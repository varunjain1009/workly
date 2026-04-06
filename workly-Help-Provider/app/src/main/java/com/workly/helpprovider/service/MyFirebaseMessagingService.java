package com.workly.helpprovider.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.workly.helpprovider.data.auth.AuthManager;
import com.workly.helpprovider.data.repository.JobRepository;
import com.workly.helpprovider.data.repository.ProfileRepository;
import com.workly.helpprovider.ui.main.MainActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "workly_jobs";

    @Inject
    ProfileRepository profileRepository;

    @Inject
    JobRepository jobRepository;

    @Inject
    AuthManager authManager;

    @Inject
    com.workly.helpprovider.data.config.ConfigManager configManager;

    @Inject
    com.workly.helpprovider.util.AppLogger appLogger;

    @Override
    public void onNewToken(@NonNull String token) {
        appLogger.d(TAG, "Refreshed token: " + token);
        if (authManager.isLoggedIn() && profileRepository != null) {
            profileRepository.updateDeviceToken(token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        appLogger.d(TAG, "From: " + remoteMessage.getFrom());
        if (remoteMessage.getData().isEmpty()) {
            return;
        }

        appLogger.d(TAG, "Message data payload: " + remoteMessage.getData());

        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");
        String type = remoteMessage.getData().get("type");
        String jobId = remoteMessage.getData().get("jobId");

        if ("CONFIG_UPDATE".equals(title) || "CONFIG_UPDATE".equals(type)) {
            configManager.syncConfig();
            return;
        }

        if ("NEW_JOB_AWAITING_ACCEPTANCE".equals(type)
                || "NEW_JOB_AVAILABLE".equals(type)
                || "JOB_ASSIGNED".equals(type)) {
            jobRepository.forceRefreshAvailableJobs();
            LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
            manager.sendBroadcast(new Intent("PROVIDER_AVAILABLE_JOBS_UPDATED"));
            manager.sendBroadcast(new Intent("PROVIDER_MY_JOBS_UPDATED"));
            showNotification(
                    title != null ? title : "New Job Available",
                    body != null ? body : "New job is awaiting for acceptance",
                    jobId);
        }
    }

    private void showNotification(String title, String message, String jobId) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "Workly Jobs", NotificationManager.IMPORTANCE_HIGH));
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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        notificationManager.notify(jobId != null ? jobId.hashCode() : 1001, builder.build());
    }
}
