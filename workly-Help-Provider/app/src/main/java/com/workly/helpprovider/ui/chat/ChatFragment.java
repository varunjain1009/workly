package com.workly.helpprovider.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.workly.helpprovider.databinding.FragmentChatBinding;
import com.workly.helpprovider.data.auth.AuthManager;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatViewModel viewModel;
    private ChatAdapter adapter;
    private String otherUserId;
    private static final String TAG = "WORKLY_DEBUG";

    @Inject
    AuthManager authManager;

    @Inject
    com.workly.helpprovider.util.AppLogger appLogger;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        if (getArguments() != null) {
            otherUserId = getArguments().getString("otherUserId");
        }

        String myUserId = authManager.getMobileNumber();
        appLogger.d(TAG, "ChatFragment(Provider): Initialized - myUserId: " + myUserId + ", otherUserId: " + otherUserId);

        setupRecyclerView(myUserId);
        setupObservers();
        setupListeners();

        if (myUserId != null && otherUserId != null) {
            viewModel.init(myUserId, otherUserId);
        } else {
            appLogger.e(TAG, "ChatFragment(Provider): Cannot init chat - missing userId(s)");
        }
    }

    private void setupRecyclerView(String myUserId) {
        adapter = new ChatAdapter(myUserId);
        binding.rvChatMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvChatMessages.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null) {
                appLogger.d(TAG, "ChatFragment(Provider): " + messages.size() + " messages in conversation");
                adapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    binding.rvChatMessages.scrollToPosition(messages.size() - 1);
                }
            }
        });
    }

    private void setupListeners() {
        binding.btnSendMessage.setOnClickListener(v -> {
            String content = binding.etMessageInput.getText().toString().trim();
            if (content.isEmpty()) return;
            appLogger.d(TAG, "ChatFragment(Provider): Sending message (" + content.length() + " chars)");
            viewModel.sendMessage(content);
            binding.etMessageInput.setText("");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
