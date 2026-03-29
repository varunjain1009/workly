package com.workly.helpprovider.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.workly.helpprovider.data.model.ChatMessage;
import com.workly.helpprovider.data.repository.ChatRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
import java.util.List;

@HiltViewModel
public class ChatViewModel extends ViewModel {

    private final ChatRepository chatRepository;
    private final com.workly.helpprovider.util.AppLogger appLogger;
    private String currentUserId;
    private String otherUserId;
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    public ChatViewModel(ChatRepository chatRepository, com.workly.helpprovider.util.AppLogger appLogger) {
        this.chatRepository = chatRepository;
        this.appLogger = appLogger;
    }

    public void init(String userId, String otherUserId) {
        appLogger.d(TAG, "ChatViewModel(Provider): Initializing chat session - user: " + userId + " <-> " + otherUserId);
        this.currentUserId = userId;
        this.otherUserId = otherUserId;
        chatRepository.connect(userId);
    }

    public LiveData<List<ChatMessage>> getMessages() {
        appLogger.d(TAG, "ChatViewModel(Provider): Subscribing to message LiveData");
        return chatRepository.getMessages(currentUserId, otherUserId);
    }

    public void sendMessage(String content) {
        if (content != null && !content.trim().isEmpty()) {
            appLogger.d(TAG, "ChatViewModel(Provider): Queueing message for delivery");
            chatRepository.sendMessage(currentUserId, otherUserId, content);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        appLogger.d(TAG, "ChatViewModel(Provider): ViewModel cleared - disconnecting WebSocket");
        chatRepository.disconnect();
    }
}
