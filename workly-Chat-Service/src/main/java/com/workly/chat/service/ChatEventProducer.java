package com.workly.chat.service;

import com.workly.chat.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "chat-events";

    public void publishMessageCreated(Message message) {
        log.debug("ChatEventProducer: [ENTER] publishMessageCreated - messageId: {}, topic: {}", message.getMessageId(), TOPIC);
        try {
            @SuppressWarnings({ "null", "unused" })
            var future = kafkaTemplate.send(TOPIC, message.getMessageId(), message);
            // Keying by messageId or senderId for partition ordering if needed
            log.debug("ChatEventProducer: [EXIT] publishMessageCreated - messageId: {} dispatched to Kafka", message.getMessageId());
        } catch (Exception e) {
            log.error("Failed to publish chat event for messageId: {}", message.getMessageId(), e);
        }
    }
}
