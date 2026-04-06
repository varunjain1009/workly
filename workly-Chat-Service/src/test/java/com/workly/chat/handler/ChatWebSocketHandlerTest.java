package com.workly.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workly.chat.model.Message;
import com.workly.chat.model.MessageStatus;
import com.workly.chat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatWebSocketHandlerTest {

    @Mock private ChatService chatService;
    @Mock private WebSocketSession session;

    private ObjectMapper objectMapper;
    private ChatWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        handler = new ChatWebSocketHandler(chatService, objectMapper);
    }

    private Map<String, Object> attrs(String userId) {
        Map<String, Object> m = new HashMap<>();
        if (userId != null) m.put("userId", userId);
        return m;
    }

    @Test
    void afterConnectionEstablished_validUser_storesSessionAndDeliversPending() throws Exception {
        when(session.getAttributes()).thenReturn(attrs("user1"));
        Message pending = new Message();
        pending.setId("m1");
        when(chatService.getPendingMessages("user1")).thenReturn(List.of(pending));
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        verify(chatService).getPendingMessages("user1");
        verify(chatService).markAsDelivered(pending);
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void afterConnectionEstablished_noUserId_closesSession() throws Exception {
        when(session.getAttributes()).thenReturn(attrs(null));

        handler.afterConnectionEstablished(session);

        verify(session).close(any(CloseStatus.class));
        verify(chatService, never()).getPendingMessages(any());
    }

    @Test
    void handleTextMessage_validSender_savesAndDelivers() throws Exception {
        when(session.getAttributes()).thenReturn(attrs("user1"));
        Message msg = new Message();
        msg.setSenderId("user1");
        msg.setReceiverId("user2");
        msg.setContent("Hello");

        String payload = objectMapper.writeValueAsString(msg);

        Message saved = new Message();
        saved.setId("saved1");
        saved.setSenderId("user1");
        saved.setReceiverId("user2");
        when(chatService.saveMessage(any(Message.class))).thenReturn(saved);
        when(session.isOpen()).thenReturn(true);

        handler.handleTextMessage(session, new TextMessage(payload));

        verify(chatService).saveMessage(any(Message.class));
        verify(session, atLeastOnce()).sendMessage(any(TextMessage.class)); // ack to sender
    }

    @Test
    void handleTextMessage_unauthorizedSender_skips() throws Exception {
        when(session.getAttributes()).thenReturn(attrs("user1"));
        Message msg = new Message();
        msg.setSenderId("user2"); // different from session user
        msg.setContent("Hello");

        String payload = objectMapper.writeValueAsString(msg);

        handler.handleTextMessage(session, new TextMessage(payload));

        verify(chatService, never()).saveMessage(any());
    }

    @Test
    void handleTextMessage_nullUserId_skips() throws Exception {
        when(session.getAttributes()).thenReturn(attrs(null));
        Message msg = new Message();
        msg.setSenderId("user1");
        String payload = objectMapper.writeValueAsString(msg);

        handler.handleTextMessage(session, new TextMessage(payload));

        verify(chatService, never()).saveMessage(any());
    }

    @Test
    void afterConnectionClosed_removesSession() throws Exception {
        // First connect user
        when(session.getAttributes()).thenReturn(attrs("user1"));
        when(chatService.getPendingMessages("user1")).thenReturn(List.of());
        handler.afterConnectionEstablished(session);

        // Then disconnect
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // No exception expected
    }

    @Test
    void afterConnectionClosed_noUserId_doesNothing() throws Exception {
        when(session.getAttributes()).thenReturn(attrs(null));
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        // No exception
    }

    @Test
    void handleTextMessage_receiverOnline_deliversToReceiver() throws Exception {
        // Connect receiver first
        WebSocketSession receiverSession = mock(WebSocketSession.class);
        when(receiverSession.getAttributes()).thenReturn(attrs("user2"));
        when(chatService.getPendingMessages("user2")).thenReturn(List.of());
        when(receiverSession.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(receiverSession);

        // Now sender sends message
        when(session.getAttributes()).thenReturn(attrs("user1"));
        Message msg = new Message();
        msg.setSenderId("user1");
        msg.setReceiverId("user2");
        msg.setContent("Hello");

        Message saved = new Message();
        saved.setId("s1");
        saved.setSenderId("user1");
        saved.setReceiverId("user2");
        when(chatService.saveMessage(any())).thenReturn(saved);
        when(session.isOpen()).thenReturn(true);

        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(msg)));

        verify(receiverSession).sendMessage(any(TextMessage.class));
        verify(chatService).markAsDelivered(saved);
    }
}
