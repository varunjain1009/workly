package com.workly.modules.notification.controller;

import com.workly.core.ApiResponse;
import com.workly.modules.notification.service.UserTokenService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/fcm-token")
@RequiredArgsConstructor
public class UserTokenController {

    private final UserTokenService userTokenService;

    @PostMapping
    public ApiResponse<Void> updateToken(@RequestBody TokenRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        userTokenService.saveToken(mobileNumber, request.getToken());
        return ApiResponse.success(null, "Token updated");
    }

    @Data
    public static class TokenRequest {
        private String token;
    }
}
