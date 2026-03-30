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
        log.debug("FCMService: [ENTER] sendNotification - token: {}, title: {}", token, title);
        if (token == null || token.isEmpty()) {
            log.debug("FCMService: [FAIL] Empty token, aborting notification dispatch");
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
                    .putData("type", title)           // allows onMessageReceived to identify type
                    .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
                    .build();
            log.debug("FCMService: Message object constructed, attempting Firebase dispatch");

            try {
                String response = FirebaseMessaging.getInstance().send(message);
                log.debug("FCMService: Firebase dispatch returned response: {}", response);
                log.info("Successfully sent message: {}", response);
            } catch (IllegalStateException e) {
                log.debug("FCMService: Firebase not initialized, falling back to mock mode");
                log.warn("Firebase App is not initialized. Mocking notification send: {} -> {}", title, body);
            }

        } catch (Exception e) {
            log.debug("FCMService: [ERROR] Exception during FCM send: {}", e.getMessage());
            log.error("Error sending FCM notification", e);
        }
        log.debug("FCMService: [EXIT] sendNotification");
    }
}
