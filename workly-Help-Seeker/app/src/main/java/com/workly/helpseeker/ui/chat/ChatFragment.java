package com.workly.helpseeker.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.workly.helpseeker.databinding.FragmentChatBinding;
import com.workly.helpseeker.data.local.AuthManager;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatViewModel viewModel;
    private ChatAdapter adapter;
    private String otherUserId;

    @Inject
    AuthManager authManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // Get arguments (e.g., from JobDetails)
        if (getArguments() != null) {
            otherUserId = getArguments().getString("otherUserId");
        }

        // For MVP, if not passed, fallback or error.
        // Assuming passed correctly for now.
        String myUserId = authManager.getMobileNumber();

        setupRecyclerView(myUserId);
        setupObservers();
        setupListeners();

        if (myUserId != null && otherUserId != null) {
            viewModel.init(myUserId, otherUserId);
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
                adapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    binding.rvChatMessages.scrollToPosition(messages.size() - 1);
                }
            }
        });
    }

    private void setupListeners() {
        binding.btnSendMessage.setOnClickListener(v -> {
            String content = binding.etMessageInput.getText().toString();
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
