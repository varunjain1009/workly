package com.workly.helpprovider.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "chat_messages")
public class ChatMessage {
    @PrimaryKey
    @NonNull
    public String messageId;
    public String senderId;
    public String receiverId;
    public String content;
    public String status;
    public long timestamp;
}
