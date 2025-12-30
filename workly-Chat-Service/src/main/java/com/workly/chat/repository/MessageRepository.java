package com.workly.chat.repository;

import com.workly.chat.model.Message;
import com.workly.chat.model.MessageStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByReceiverIdAndStatus(String receiverId, MessageStatus status);

    List<Message> findBySenderIdAndReceiverId(String senderId, String receiverId);
}
