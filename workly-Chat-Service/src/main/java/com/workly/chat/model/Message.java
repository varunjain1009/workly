package com.workly.chat.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Document(collection = "messages")
public class Message {
    @Id
    private String id;
    private String messageId; // Client-side or UUID
    private String senderId;
    private String receiverId;
    private String content;
    private MessageStatus status;
    private Instant createdAt;
    private Instant deliveredAt;
}
