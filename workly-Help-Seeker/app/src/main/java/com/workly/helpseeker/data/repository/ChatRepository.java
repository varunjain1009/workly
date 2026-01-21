package com.workly.helpseeker.data.repository;

import androidx.lifecycle.LiveData;
import com.workly.helpseeker.data.local.ChatMessageDao;
import com.workly.helpseeker.data.model.ChatMessage;
import com.workly.helpseeker.data.network.WebSocketManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ChatRepository {

    private final ChatMessageDao chatMessageDao;
    private final WebSocketManager webSocketManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Inject
    public ChatRepository(ChatMessageDao chatMessageDao, WebSocketManager webSocketManager) {
        this.chatMessageDao = chatMessageDao;
        this.webSocketManager = webSocketManager;
    }

    public void connect(String userId) {
        webSocketManager.connect(userId, this::saveMessage);
    }

    public void disconnect() {
        webSocketManager.disconnect();
    }

    public void sendMessage(String senderId, String receiverId, String content) {
        ChatMessage message = new ChatMessage();
        message.messageId = UUID.randomUUID().toString();
        message.senderId = senderId;
        message.receiverId = receiverId;
        message.content = content;
        message.status = "CREATED";
        message.timestamp = System.currentTimeMillis();

        saveMessage(message);
        webSocketManager.sendMessage(message);
    }

    private void saveMessage(ChatMessage message) {
        executorService.execute(() -> chatMessageDao.insert(message));
    }

    public LiveData<List<ChatMessage>> getMessages(String userId, String otherUserId) {
        return chatMessageDao.getMessagesForChat(userId, otherUserId);
    }
}
