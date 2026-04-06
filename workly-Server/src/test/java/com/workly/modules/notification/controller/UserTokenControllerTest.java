package com.workly.modules.notification.controller;

import com.workly.modules.notification.service.UserTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserTokenControllerTest {

    @Mock private UserTokenService userTokenService;

    private UserTokenController controller;

    @BeforeEach
    void setUp() {
        controller = new UserTokenController(userTokenService);
    }

    @Test
    void updateToken_savesToken() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("9876543210", null, List.of()));

        UserTokenController.TokenRequest req = new UserTokenController.TokenRequest();
        req.setToken("fcm-token-abc");

        var result = controller.updateToken(req);

        assertTrue(result.isSuccess());
        verify(userTokenService).saveToken("9876543210", "fcm-token-abc");
        SecurityContextHolder.clearContext();
    }
}
