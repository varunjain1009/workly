package com.workly.chat.service;

import com.workly.chat.model.Message;
import com.workly.chat.model.MessageStatus;
import com.workly.chat.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatEventProducer chatEventProducer;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(messageRepository, chatEventProducer);
    }

    @Test
    void saveMessage_shouldPersistAndPublishEvent() {
        Message input = message("u1", "u2", "Hello");
        Message saved = message("u1", "u2", "Hello");
        saved.setId("mongo-id-1");
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        Message result = chatService.saveMessage(input);

        verify(messageRepository).save(input);
        verify(chatEventProducer).publishMessageCreated(saved);
        assertThat(result.getId()).isEqualTo("mongo-id-1");
    }

    @Test
    void saveMessage_shouldDefaultStatusToCreated_whenStatusIsNull() {
        Message input = message("u1", "u2", "Hi");
        input.setStatus(null);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        chatService.saveMessage(input);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MessageStatus.CREATED);
    }

    @Test
    void saveMessage_shouldNotOverrideStatus_whenAlreadySet() {
        Message input = message("u1", "u2", "Hi");
        input.setStatus(MessageStatus.SENT);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        chatService.saveMessage(input);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MessageStatus.SENT);
    }

    @Test
    void saveMessage_shouldDefaultCreatedAt_whenNull() {
        Message input = message("u1", "u2", "Hello");
        input.setCreatedAt(null);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        chatService.saveMessage(input);
        Instant after = Instant.now();

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Instant ts = captor.getValue().getCreatedAt();
        assertThat(ts).isNotNull();
        assertThat(ts).isBetween(before, after);
    }

    @Test
    void saveMessage_shouldNotOverrideCreatedAt_whenAlreadySet() {
        Message input = message("u1", "u2", "Hello");
        Instant fixed = Instant.parse("2026-01-01T00:00:00Z");
        input.setCreatedAt(fixed);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        chatService.saveMessage(input);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(fixed);
    }

    @Test
    void markAsDelivered_shouldSetStatusAndDeliveredAt() {
        Message msg = message("u1", "u2", "Hello");
        msg.setStatus(MessageStatus.SENT);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        chatService.markAsDelivered(msg);
        Instant after = Instant.now();

        assertThat(msg.getStatus()).isEqualTo(MessageStatus.DELIVERED);
        assertThat(msg.getDeliveredAt()).isBetween(before, after);
        verify(messageRepository).save(msg);
    }

    @Test
    void getPendingMessages_shouldReturnMessagesWithSentStatus() {
        List<Message> pending = List.of(message("u3", "u1", "Hey"), message("u4", "u1", "Hi"));
        when(messageRepository.findByReceiverIdAndStatus("u1", MessageStatus.SENT)).thenReturn(pending);

        List<Message> result = chatService.getPendingMessages("u1");

        assertThat(result).hasSize(2);
        verify(messageRepository).findByReceiverIdAndStatus("u1", MessageStatus.SENT);
    }

    @Test
    void getPendingMessages_shouldReturnEmptyList_whenNoPendingMessages() {
        when(messageRepository.findByReceiverIdAndStatus("u1", MessageStatus.SENT)).thenReturn(List.of());

        List<Message> result = chatService.getPendingMessages("u1");

        assertThat(result).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Message message(String sender, String receiver, String content) {
        Message m = new Message();
        m.setSenderId(sender);
        m.setReceiverId(receiver);
        m.setContent(content);
        return m;
    }
}
