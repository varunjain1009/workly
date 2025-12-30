package com.workly.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workly.chat.model.Message;
import com.workly.chat.model.MessageStatus;
import com.workly.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    // UserId -> WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            sessions.put(userId, session);
            log.info("User connected: {}", userId);

            // Deliver pending messages
            chatService.getPendingMessages(userId).forEach(msg -> {
                try {
                    sendMessage(session, msg);
                    chatService.markAsDelivered(msg);
                } catch (Exception e) {
                    log.error("Failed to deliver pending message", e);
                }
            });
        } else {
            session.close(java.util.Objects.requireNonNull(CloseStatus.BAD_DATA));
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage textMessage)
            throws Exception {
        try {
            Message message = objectMapper.readValue(textMessage.getPayload(), Message.class);
            String userId = getUserIdFromSession(session);

            if (userId == null || !userId.equals(message.getSenderId())) {
                log.warn("Unauthorized sender");
                return;
            }

            // 1. Persist
            message.setStatus(MessageStatus.SENT);
            Message savedMsg = chatService.saveMessage(message);

            // 2. Ack to Sender (Echo back with ID)
            sendMessage(session, savedMsg);

            // 3. deliver to Receiver
            WebSocketSession receiverSession = sessions.get(message.getReceiverId());
            if (receiverSession != null && receiverSession.isOpen()) {
                sendMessage(receiverSession, savedMsg);
                chatService.markAsDelivered(savedMsg);
            }

        } catch (Exception e) {
            log.error("Error handling message", e);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            sessions.remove(userId);
            log.info("User disconnected: {}", userId);
        }
    }

    private void sendMessage(WebSocketSession session, Message message) throws Exception {
        String payload = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(java.util.Objects.requireNonNull(payload)));
    }

    private String getUserIdFromSession(WebSocketSession session) {
        // Extracted from Query Param in Interceptor or attributes
        return (String) session.getAttributes().get("userId");
    }
}
