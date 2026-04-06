package com.workly.modules.notification;

import com.workly.modules.notification.model.UserToken;
import com.workly.modules.notification.repository.UserTokenRepository;
import com.workly.modules.notification.service.UserTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserTokenServiceTest {

    @Mock private UserTokenRepository userTokenRepository;

    private UserTokenService userTokenService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userTokenService = new UserTokenService(userTokenRepository);
    }

    @Test
    void saveToken_newUser_createsToken() {
        when(userTokenRepository.findByMobileNumber("m1")).thenReturn(Optional.empty());
        when(userTokenRepository.save(any(UserToken.class))).thenAnswer(inv -> inv.getArgument(0));

        userTokenService.saveToken("m1", "fcm-token-123");

        verify(userTokenRepository).save(argThat(t ->
                "m1".equals(t.getMobileNumber()) && "fcm-token-123".equals(t.getFcmToken())));
    }

    @Test
    void saveToken_existingUser_updatesToken() {
        UserToken existing = new UserToken();
        existing.setMobileNumber("m1");
        existing.setFcmToken("old-token");
        when(userTokenRepository.findByMobileNumber("m1")).thenReturn(Optional.of(existing));
        when(userTokenRepository.save(any())).thenReturn(existing);

        userTokenService.saveToken("m1", "new-token");

        assertEquals("new-token", existing.getFcmToken());
        verify(userTokenRepository).save(existing);
    }

    @Test
    void getToken_found_returnsToken() {
        UserToken token = new UserToken();
        token.setFcmToken("fcm-abc");
        when(userTokenRepository.findByMobileNumber("m1")).thenReturn(Optional.of(token));

        assertEquals("fcm-abc", userTokenService.getToken("m1"));
    }

    @Test
    void getToken_notFound_returnsNull() {
        when(userTokenRepository.findByMobileNumber("m1")).thenReturn(Optional.empty());
        assertNull(userTokenService.getToken("m1"));
    }
}
