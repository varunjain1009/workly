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
        log.debug("ChatService: [ENTER] saveMessage - from: {}, to: {}", message.getSenderId(), message.getReceiverId());
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(Instant.now());
        }
        if (message.getStatus() == null) {
            message.setStatus(MessageStatus.CREATED);
            log.debug("ChatService: saveMessage - status defaulted to CREATED");
        }
        Message saved = messageRepository.save(message);
        log.debug("ChatService: saveMessage - persisted messageId: {}", saved.getMessageId());
        chatEventProducer.publishMessageCreated(saved);
        log.debug("ChatService: [EXIT] saveMessage - messageId: {}", saved.getMessageId());
        return saved;
    }

    public void markAsDelivered(Message message) {
        log.debug("ChatService: [ENTER] markAsDelivered - messageId: {}", message.getMessageId());
        message.setStatus(MessageStatus.DELIVERED);
        message.setDeliveredAt(Instant.now());
        messageRepository.save(message);
        log.debug("ChatService: [EXIT] markAsDelivered - messageId: {} marked DELIVERED", message.getMessageId());
    }

    public List<Message> getPendingMessages(String userId) {
        log.debug("ChatService: [ENTER] getPendingMessages - userId: {}", userId);
        List<Message> pending = messageRepository.findByReceiverIdAndStatus(userId, MessageStatus.SENT); // SENT means stored but not yet
                                                                                        // delivered to receiver
        log.debug("ChatService: [EXIT] getPendingMessages - userId: {}, pending count: {}", userId, pending.size());
        return pending;
    }
}
