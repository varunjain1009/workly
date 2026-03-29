package com.workly.helpseeker.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.workly.helpseeker.data.model.ChatMessage;
import com.workly.helpseeker.data.repository.ChatRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
import java.util.List;

@HiltViewModel
public class ChatViewModel extends ViewModel {

    private final ChatRepository chatRepository;
    private final com.workly.helpseeker.util.AppLogger appLogger;
    private String currentUserId;
    private String otherUserId;
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    public ChatViewModel(ChatRepository chatRepository, com.workly.helpseeker.util.AppLogger appLogger) {
        this.chatRepository = chatRepository;
        this.appLogger = appLogger;
    }

    public void init(String userId, String otherUserId) {
        appLogger.d(TAG, "ChatViewModel(Seeker): Initializing chat session - user: " + userId + " <-> " + otherUserId);
        this.currentUserId = userId;
        this.otherUserId = otherUserId;
        chatRepository.connect(userId);
    }

    public LiveData<List<ChatMessage>> getMessages() {
        appLogger.d(TAG, "ChatViewModel(Seeker): Subscribing to message LiveData");
        return chatRepository.getMessages(currentUserId, otherUserId);
    }

    public void sendMessage(String content) {
        if (content != null && !content.trim().isEmpty()) {
            appLogger.d(TAG, "ChatViewModel(Seeker): Queueing message for delivery");
            chatRepository.sendMessage(currentUserId, otherUserId, content);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        appLogger.d(TAG, "ChatViewModel(Seeker): ViewModel cleared - disconnecting WebSocket");
        chatRepository.disconnect();
    }
}
