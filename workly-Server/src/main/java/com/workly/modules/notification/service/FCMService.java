package com.workly.modules.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.BatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
@Slf4j
public class FCMService {

    private static final int MAX_BATCH_SIZE = 500; // Firebase limit

    /** Send a simple notification without extra data. */
    public void sendNotification(String token, String title, String body) {
        sendNotificationWithData(token, title, body, null);
    }

    /**
     * Send a notification with an optional key-value data payload.
     */
    @CircuitBreaker(name = "fcm", fallbackMethod = "fallbackSendNotificationWithData")
    public void sendNotificationWithData(String token, String title, String body, Map<String, String> data) {
        if (token == null || token.isEmpty()) {
            log.warn("FCM Token is empty, skipping notification");
            return;
        }

        try {
            Message message = buildMessage(token, title, body, data);
            try {
                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Successfully sent message: {}", response);
            } catch (IllegalStateException e) {
                log.warn("Firebase App is not initialized. Mocking notification send: {} -> {}", title, body);
            }
        } catch (Exception e) {
            log.error("Error sending FCM notification", e);
            throw new RuntimeException("FCM send failed", e);
        }
    }

    public void fallbackSendNotificationWithData(String token, String title, String body, Map<String, String> data, Throwable t) {
        log.warn("FCMService: [FALLBACK] Circuit breaker active or call failed. Skipping FCM message: {}. Error: {}", title, t.getMessage());
    }

    /**
     * Batch-send notifications to multiple tokens. Firebase allows up to 500
     * messages per sendEach() call. This method automatically chunks larger lists.
     *
     * @return number of successfully sent messages
     */
    @CircuitBreaker(name = "fcmBatch", fallbackMethod = "fallbackSendBatch")
    public int sendBatch(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) return 0;

        // Filter out nulls/empties
        List<String> validTokens = tokens.stream()
                .filter(t -> t != null && !t.isEmpty())
                .toList();

        if (validTokens.isEmpty()) return 0;

        log.debug("FCMService: Sending batch notification to {} recipients", validTokens.size());
        int totalSuccess = 0;

        // Process in chunks of MAX_BATCH_SIZE
        for (int i = 0; i < validTokens.size(); i += MAX_BATCH_SIZE) {
            List<String> chunk = validTokens.subList(i, Math.min(i + MAX_BATCH_SIZE, validTokens.size()));

            List<Message> messages = new ArrayList<>(chunk.size());
            for (String token : chunk) {
                messages.add(buildMessage(token, title, body, data));
            }

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);
                totalSuccess += response.getSuccessCount();
                if (response.getFailureCount() > 0) {
                    log.warn("FCMService: Batch chunk had {} failures out of {}",
                            response.getFailureCount(), chunk.size());
                }
            } catch (IllegalStateException e) {
                log.warn("Firebase App is not initialized. Mocking batch send of {} messages: {}", chunk.size(), title);
                totalSuccess += chunk.size(); // count as success in mock mode
            } catch (Exception e) {
                log.error("FCMService: Error sending batch chunk of {} messages", chunk.size(), e);
            }
        }

        log.info("FCMService: Batch complete — {}/{} sent successfully", totalSuccess, validTokens.size());
        if (totalSuccess == 0 && !validTokens.isEmpty()) {
            throw new RuntimeException("All FCM batch iterations failed");
        }
        return totalSuccess;
    }

    public int fallbackSendBatch(List<String> tokens, String title, String body, Map<String, String> data, Throwable t) {
        log.warn("FCMService: [FALLBACK] Circuit breaker active or completely failed. Skipping FCM batch. Error: {}", t.getMessage());
        return 0;
    }

    private Message buildMessage(String token, String title, String body, Map<String, String> data) {
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

        return builder.build();
    }
}

