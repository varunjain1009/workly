package com.workly.modules.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FCMServiceTest {

    private FCMService fcmService;

    @BeforeEach
    void setUp() {
        fcmService = new FCMService();
    }

    @Test
    void sendNotification_emptyToken_doesNotThrow() {
        // Empty token - should log warning and return
        assertDoesNotThrow(() -> fcmService.sendNotification("", "title", "body"));
    }

    @Test
    void sendNotification_nullToken_doesNotThrow() {
        assertDoesNotThrow(() -> fcmService.sendNotificationWithData(null, "title", "body", null));
    }

    @Test
    void sendNotificationWithData_validToken_firebaseNotInitialized_handlesGracefully() {
        // Firebase is not initialized in tests, so it will throw IllegalStateException
        // which is caught internally
        assertDoesNotThrow(() -> fcmService.sendNotificationWithData("some-token", "title", "body", Map.of("key", "val")));
    }

    @Test
    void fallbackSendNotificationWithData_doesNotThrow() {
        assertDoesNotThrow(() ->
                fcmService.fallbackSendNotificationWithData("token", "title", "body", null, new RuntimeException("cb open")));
    }

    @Test
    void sendBatch_emptyList_returnsZero() {
        assertEquals(0, fcmService.sendBatch(List.of(), "title", "body", null));
    }

    @Test
    void sendBatch_nullList_returnsZero() {
        assertEquals(0, fcmService.sendBatch(null, "title", "body", null));
    }

    @Test
    void sendBatch_listWithNullTokens_filtersAndHandles() {
        // Tokens with nulls/empties should be filtered
        assertDoesNotThrow(() -> fcmService.sendBatch(List.of("", "valid-token"), "title", "body", null));
    }
}
