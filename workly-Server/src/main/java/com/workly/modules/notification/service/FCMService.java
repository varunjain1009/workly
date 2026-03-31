package com.workly.modules.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class FCMService {

    /** Send a simple notification without extra data. */
    public void sendNotification(String token, String title, String body) {
        sendNotificationWithData(token, title, body, null);
    }

    /**
     * Send a notification with an optional key-value data payload.
     * The data map is added to the FCM message so the Android client can
     * read values like {@code jobId} and {@code type} in {@code onMessageReceived}.
     */
    public void sendNotificationWithData(String token, String title, String body, Map<String, String> data) {
        log.debug("FCMService: [ENTER] sendNotificationWithData - token: {}, title: {}", token, title);
        if (token == null || token.isEmpty()) {
            log.warn("FCM Token is empty, skipping notification");
            return;
        }

        try {
            Message.Builder builder = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", title)
                    .putData("click_action", "FLUTTER_NOTIFICATION_CLICK");

            if (data != null) {
                data.forEach(builder::putData);
            }

            Message message = builder.build();
            log.debug("FCMService: Message object constructed, attempting Firebase dispatch");

            try {
                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Successfully sent message: {}", response);
            } catch (IllegalStateException e) {
                log.warn("Firebase App is not initialized. Mocking notification send: {} -> {}", title, body);
            }

        } catch (Exception e) {
            log.error("Error sending FCM notification", e);
        }
        log.debug("FCMService: [EXIT] sendNotificationWithData");
    }
}

