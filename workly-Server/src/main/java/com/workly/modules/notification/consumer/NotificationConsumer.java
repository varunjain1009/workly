package com.workly.modules.notification.consumer;

import com.workly.modules.notification.service.FCMService;
import com.workly.modules.notification.service.UserTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final FCMService fcmService;
    private final UserTokenService userTokenService;

    @KafkaListener(topics = "chat-events", groupId = "notification-group")
    public void consumeChatEvent(Map<String, Object> message) {
        try {
            log.info("Received chat event: {}", message);

            // Assuming message payload is Map equivalent of our Message object
            String receiverId = (String) message.get("receiverId");
            String senderId = (String) message.get("senderId");
            String content = (String) message.get("content");

            String token = userTokenService.getToken(receiverId);
            if (token != null) {
                fcmService.sendNotification(token, "New Message from " + senderId, content);
            } else {
                log.info("No FCM token found for user: {}", receiverId);
            }
        } catch (Exception e) {
            log.error("Error processing chat event", e);
        }
    }
}
