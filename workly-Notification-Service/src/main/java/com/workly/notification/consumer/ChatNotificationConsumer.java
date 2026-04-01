package com.workly.notification.consumer;

import com.workly.notification.service.FCMService;
import com.workly.notification.service.UserTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatNotificationConsumer {

    private final FCMService fcmService;
    private final UserTokenService userTokenService;

    @KafkaListener(topics = "chat-events", groupId = "notification-group")
    public void consumeChatEvent(Map<String, Object> message) {
        try {
            log.info("ChatNotificationConsumer: Received chat event: {}", message);
            String receiverId = (String) message.get("receiverId");
            String senderId = (String) message.get("senderId");
            String content = (String) message.get("content");

            String token = userTokenService.getToken(receiverId);
            if (token != null) {
                fcmService.sendNotification(token, "New Message from " + senderId, content);
            } else {
                log.info("ChatNotificationConsumer: No FCM token for user: {}", receiverId);
            }
        } catch (Exception e) {
            log.error("ChatNotificationConsumer: Error processing chat event", e);
            throw new RuntimeException("Failed to process chat event", e);
        }
    }
}
