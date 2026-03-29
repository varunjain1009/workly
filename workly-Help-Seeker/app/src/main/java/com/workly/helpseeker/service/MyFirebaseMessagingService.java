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
            if ("CONFIG_UPDATE".equals(title) || "CONFIG_UPDATE".equals(remoteMessage.getData().get("type"))) {
                appLogger.d(TAG, "MyFirebaseMessagingService(Seeker): Config update received - triggering sync");
                if (configManager != null) {
                    configManager.syncConfig();
                }
            }
        }
    }
}
