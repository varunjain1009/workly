package com.workly.modules.config;

import com.workly.modules.notification.model.UserToken;
import com.workly.modules.notification.repository.UserTokenRepository;
import com.workly.modules.notification.service.FCMService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConfigSyncServiceTest {

    @Mock private UserTokenRepository userTokenRepository;
    @Mock private FCMService fcmService;

    private ConfigSyncService configSyncService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        configSyncService = new ConfigSyncService(userTokenRepository, fcmService);
        ReflectionTestUtils.setField(configSyncService, "notificationIntervalHours", 1);
    }

    @Test
    void notifyAppsOfConfigChange_emptyTokens_sendsNothing() {
        when(userTokenRepository.findAll()).thenReturn(List.of());

        configSyncService.notifyAppsOfConfigChange();

        verify(fcmService, never()).sendNotification(anyString(), anyString(), anyString());
    }

    @Test
    void notifyAppsOfConfigChange_tokenWithinInterval_skipped() {
        UserToken token = new UserToken();
        token.setFcmToken("fcm1");
        token.setLastConfigNotificationTime(System.currentTimeMillis()); // just now

        when(userTokenRepository.findAll()).thenReturn(List.of(token));

        configSyncService.notifyAppsOfConfigChange();

        verify(fcmService, never()).sendNotification(anyString(), anyString(), anyString());
    }

    @Test
    void notifyAppsOfConfigChange_tokenPastInterval_sends() {
        UserToken token = new UserToken();
        token.setFcmToken("fcm1");
        token.setLastConfigNotificationTime(System.currentTimeMillis() - 2 * 3600 * 1000L); // 2h ago

        when(userTokenRepository.findAll()).thenReturn(List.of(token));
        when(userTokenRepository.save(any())).thenReturn(token);

        configSyncService.notifyAppsOfConfigChange();

        verify(fcmService).sendNotification(eq("fcm1"), eq("CONFIG_UPDATE"), anyString());
        verify(userTokenRepository).save(token);
    }

    @Test
    void notifyAppsOfConfigChange_tokenNeverNotified_sends() {
        UserToken token = new UserToken();
        token.setFcmToken("fcm1");
        token.setLastConfigNotificationTime(null); // never notified

        when(userTokenRepository.findAll()).thenReturn(List.of(token));
        when(userTokenRepository.save(any())).thenReturn(token);

        configSyncService.notifyAppsOfConfigChange();

        verify(fcmService).sendNotification(eq("fcm1"), eq("CONFIG_UPDATE"), anyString());
    }

    @Test
    void forceNotifyAppsOfConfigChange_sendsToAllTokens() {
        UserToken t1 = new UserToken(); t1.setFcmToken("tok1");
        UserToken t2 = new UserToken(); t2.setFcmToken("tok2");

        when(userTokenRepository.findAll()).thenReturn(List.of(t1, t2));
        when(userTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        configSyncService.forceNotifyAppsOfConfigChange();

        verify(fcmService).sendNotification(eq("tok1"), anyString(), anyString());
        verify(fcmService).sendNotification(eq("tok2"), anyString(), anyString());
        verify(userTokenRepository, times(2)).save(any());
    }
}
