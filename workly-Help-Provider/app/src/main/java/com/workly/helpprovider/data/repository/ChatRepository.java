package com.workly.helpprovider.data.repository;

import androidx.lifecycle.LiveData;
import com.workly.helpprovider.data.local.ChatMessageDao;
import com.workly.helpprovider.data.model.ChatMessage;
import com.workly.helpprovider.data.network.WebSocketManager;
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
    private final com.workly.helpprovider.util.AppLogger appLogger;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    public ChatRepository(ChatMessageDao chatMessageDao, WebSocketManager webSocketManager, com.workly.helpprovider.util.AppLogger appLogger) {
        this.chatMessageDao = chatMessageDao;
        this.webSocketManager = webSocketManager;
        this.appLogger = appLogger;
    }

    public void connect(String userId) {
        appLogger.d(TAG, "ChatRepository: Connecting WebSocket for User: " + userId);
        webSocketManager.connect(userId, this::saveMessage);
    }

    public void disconnect() {
        appLogger.d(TAG, "ChatRepository: Disconnecting WebSocket session instance.");
        webSocketManager.disconnect();
    }

    public void sendMessage(String senderId, String receiverId, String content) {
        appLogger.d(TAG, "ChatRepository: Assembling new message for persistence and transmission.");
        ChatMessage message = new ChatMessage();
        message.messageId = UUID.randomUUID().toString();
        message.senderId = senderId;
        message.receiverId = receiverId;
        message.content = content;
        message.status = "CREATED";
        message.timestamp = System.currentTimeMillis();

        appLogger.d(TAG, "ChatRepository: Executing save function -> web socket push.");
        saveMessage(message);
        webSocketManager.sendMessage(message);
    }

    private void saveMessage(ChatMessage message) {
        appLogger.d(TAG, "ChatRepository: Scheduling async Room DAO insertion for chat payload.");
        executorService.execute(() -> chatMessageDao.insert(message));
    }

    public LiveData<List<ChatMessage>> getMessages(String userId, String otherUserId) {
        appLogger.d(TAG, "ChatRepository: Querying LiveData snapshot for " + userId + " <-> " + otherUserId);
        return chatMessageDao.getMessagesForChat(userId, otherUserId);
    }
}
