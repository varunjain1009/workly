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
    private String currentUserId;
    private String otherUserId;

    @Inject
    public ChatViewModel(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    public void init(String userId, String otherUserId) {
        this.currentUserId = userId;
        this.otherUserId = otherUserId;
        chatRepository.connect(userId);
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return chatRepository.getMessages(currentUserId, otherUserId);
    }

    public void sendMessage(String content) {
        if (content != null && !content.trim().isEmpty()) {
            chatRepository.sendMessage(currentUserId, otherUserId, content);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        chatRepository.disconnect();
    }
}
