package com.workly.modules.notification;

import com.workly.modules.notification.consumer.NotificationConsumer;
import com.workly.modules.notification.service.FCMService;
import com.workly.modules.notification.service.UserTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificationConsumerTest {

    @Mock
    private FCMService fcmService;

    @Mock
    private UserTokenService userTokenService;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldSendNotificationWhenTokenExists() {
        // Arrange
        Map<String, Object> message = new HashMap<>();
        message.put("receiverId", "user123");
        message.put("senderId", "sender456");
        message.put("content", "Hello");

        when(userTokenService.getToken("user123")).thenReturn("fcm-token-abc");

        // Act
        notificationConsumer.consumeChatEvent(message);

        // Assert
        verify(fcmService, times(1)).sendNotification(eq("fcm-token-abc"), anyString(), eq("Hello"));
    }

    @Test
    void shouldNotSendNotificationWhenTokenMissing() {
        // Arrange
        Map<String, Object> message = new HashMap<>();
        message.put("receiverId", "userNoToken");

        when(userTokenService.getToken("userNoToken")).thenReturn(null);

        // Act
        notificationConsumer.consumeChatEvent(message);

        // Assert
        verify(fcmService, never()).sendNotification(anyString(), anyString(), anyString());
    }
}
