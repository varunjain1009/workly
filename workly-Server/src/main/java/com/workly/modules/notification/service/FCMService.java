package com.workly.modules.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FCMService {

    public void sendNotification(String token, String title, String body) {
        if (token == null || token.isEmpty()) {
            log.warn("FCM Token is empty, skipping notification");
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("click_action", "FLUTTER_NOTIFICATION_CLICK") // Maintain backward compat if needed
                    .build();

            // Check if Firebase is initialized, otherwise mock
            try {
                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Successfully sent message: {}", response);
            } catch (IllegalStateException e) {
                log.warn("Firebase App is not initialized. Mocking notification send: {} -> {}", title, body);
            }

        } catch (Exception e) {
            log.error("Error sending FCM notification", e);
        }
    }
}
