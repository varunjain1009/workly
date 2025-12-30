package com.workly.helpseeker.service;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.workly.helpseeker.data.auth.AuthManager;
import com.workly.helpseeker.data.config.ConfigManager;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Inject
    AuthManager authManager;

    @Inject
    ConfigManager configManager;

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // TODO: Send to server if needed for user targeting
        // authManager.updatePushToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            String title = remoteMessage.getData().get("title");
            if ("CONFIG_UPDATE".equals(title) || "CONFIG_UPDATE".equals(remoteMessage.getData().get("type"))) {
                Log.d(TAG, "Received Config Update Notification");
                if (configManager != null) {
                    configManager.syncConfig();
                }
            }
        }
    }
}
