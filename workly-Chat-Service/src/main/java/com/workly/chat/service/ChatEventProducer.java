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
        try {
            @SuppressWarnings({ "null", "unused" })
            var future = kafkaTemplate.send(TOPIC, message.getMessageId(), message);
            // Keying by messageId or senderId for partition ordering if needed
        } catch (Exception e) {
            log.error("Failed to publish chat event", e);
        }
    }
}
