package com.workly.helpprovider.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.workly.helpprovider.R;
import com.workly.helpprovider.data.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> messages = new ArrayList<>();
    private final String currentUserId;
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    public ChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.senderId != null && message.senderId.equals(currentUserId)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView content;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            content = itemView.findViewById(R.id.tv_message_content);
        }

        void bind(ChatMessage message) {
            content.setText(message.content);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView content;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            content = itemView.findViewById(R.id.tv_message_content);
        }

        void bind(ChatMessage message) {
            content.setText(message.content);
        }
    }
}
