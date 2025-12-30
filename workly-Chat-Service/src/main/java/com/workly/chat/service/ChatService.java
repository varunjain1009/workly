package com.workly.chat.service;

import com.workly.chat.model.Message;
import com.workly.chat.model.MessageStatus;
import com.workly.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final MessageRepository messageRepository;
    private final ChatEventProducer chatEventProducer;

    public Message saveMessage(Message message) {
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(Instant.now());
        }
        if (message.getStatus() == null) {
            message.setStatus(MessageStatus.CREATED);
        }
        Message saved = messageRepository.save(message);
        chatEventProducer.publishMessageCreated(saved);
        return saved;
    }

    public void markAsDelivered(Message message) {
        message.setStatus(MessageStatus.DELIVERED);
        message.setDeliveredAt(Instant.now());
        messageRepository.save(message);
    }

    public List<Message> getPendingMessages(String userId) {
        return messageRepository.findByReceiverIdAndStatus(userId, MessageStatus.SENT); // SENT means stored but not yet
                                                                                        // delivered to receiver
    }
}
