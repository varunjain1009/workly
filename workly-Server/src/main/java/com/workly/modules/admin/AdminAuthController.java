package com.workly.modules.admin;

import com.workly.core.ApiResponse;
import com.workly.core.WorklyException;
import com.workly.modules.auth.AuthResponse;
import com.workly.modules.auth.JwtUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${auth.token.ttl-hours}")
    private int ttlHours;

    @Data
    public static class AdminLoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        log.info("Admin login attempt for username: {}", request.getUsername());
        
        AdminUser adminUser = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> WorklyException.unauthorized("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), adminUser.getPasswordHash())) {
            log.warn("Login failed: Invalid password for admin {}", request.getUsername());
            throw WorklyException.unauthorized("Invalid username or password");
        }

        log.info("Admin login successful for: {}", request.getUsername());
        String token = jwtUtils.generateToken(request.getUsername());
        
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .expiresIn(ttlHours * 3600L)
                .build();

        return ApiResponse.success(response, "Admin login successful");
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;
        @NotBlank
        private String newPassword;
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Password change attempt for admin: {}", username);

        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> WorklyException.unauthorized("Admin not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), adminUser.getPasswordHash())) {
            throw WorklyException.badRequest("Current password is incorrect");
        }

        adminUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminUserRepository.save(adminUser);

        log.info("Password successfully changed for admin: {}", username);
        return ApiResponse.success(null, "Password changed successfully");
    }
}
