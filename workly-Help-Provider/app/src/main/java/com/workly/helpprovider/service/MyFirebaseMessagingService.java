package com.workly.helpprovider.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.workly.helpprovider.data.auth.AuthManager;
import com.workly.helpprovider.data.repository.ProfileRepository;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Inject
    ProfileRepository profileRepository;

    @Inject
    AuthManager authManager;

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        if (authManager.isLoggedIn()) {
            // We can update the token if the user is logged in
            // Ideally calling repository
            // Since services are started by system, Hilt injection should work if
            // @AndroidEntryPoint is present
            if (profileRepository != null) {
                profileRepository.updateDeviceToken(token);
            }
        } else {
            // Store locally and send on login?
            // authManager.saveTempToken(token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            if (/* Check if data needs to be processed by long running job */ false) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                scheduleJob();
            } else {
                // Handle message within 10 seconds
                handleNow();
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            // Show notification if app is in foreground, otherwise system handles it.
        }

        // Also if you intend on generating your own notifications as a result of a
        // received FCM
        // message, here is where that should be initiated. See sendNotification method
        // below.
    }

    private void scheduleJob() {
        // WorkManager
    }

    private void handleNow() {
        // Process
    }
}
