package com.workly.config;

import com.workly.modules.config.ConfigSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.connection.Message;

import static org.mockito.Mockito.*;

class ConfigUpdateListenerTest {

    @Mock private RuntimeConfigCache configCache;
    @Mock private ConfigSyncService configSyncService;
    @Mock private Message message;

    private ConfigUpdateListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new ConfigUpdateListener(configCache, configSyncService);
    }

    @Test
    void onMessage_globalScope_updatesCacheAndNotifies() {
        when(message.getBody()).thenReturn("MAX_RADIUS|GLOBAL|50".getBytes());

        listener.onMessage(message, null);

        verify(configCache).put("MAX_RADIUS", "50");
        verify(configSyncService).forceNotifyAppsOfConfigChange();
    }

    @Test
    void onMessage_appScope_notifiesOnly() {
        when(message.getBody()).thenReturn("FEATURE_X|APP|true".getBytes());

        listener.onMessage(message, null);

        verify(configCache, never()).put(anyString(), anyString());
        verify(configSyncService).forceNotifyAppsOfConfigChange();
    }

    @Test
    void onMessage_providerScope_notifiesOnly() {
        when(message.getBody()).thenReturn("RATE|PROVIDER|100".getBytes());

        listener.onMessage(message, null);

        verify(configSyncService).forceNotifyAppsOfConfigChange();
        verify(configCache, never()).put(anyString(), anyString());
    }

    @Test
    void onMessage_invalidFormat_doesNothing() {
        when(message.getBody()).thenReturn("invalid-no-pipe".getBytes());

        listener.onMessage(message, null);

        verify(configCache, never()).put(anyString(), anyString());
        verify(configSyncService, never()).forceNotifyAppsOfConfigChange();
    }

    @Test
    void onMessage_exceptionHandled_noThrow() {
        when(message.getBody()).thenThrow(new RuntimeException("error"));

        // Should not propagate exception
        listener.onMessage(message, null);
    }
}
