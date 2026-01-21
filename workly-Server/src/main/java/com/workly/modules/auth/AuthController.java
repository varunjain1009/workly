package com.workly.modules.auth;

import com.workly.core.ApiResponse;
import com.workly.core.WorklyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OtpService otpService;
    private final JwtUtils jwtUtils;
    private final com.workly.modules.profile.ProfileService profileService;

    @Value("${auth.token.ttl-hours}")
    private int ttlHours;

    @PostMapping("/otp")
    public ApiResponse<Void> requestOtp(@RequestBody OtpRequest request) {
        log.info("Received OTP request for mobile: {}", request.getMobileNumber());
        otpService.generateAndSendOtp(request.getMobileNumber());
        return ApiResponse.success(null, "OTP sent successfully");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("Received login attempt for mobile: {}", request.getMobileNumber());
        boolean isValid = otpService.validateOtp(request.getMobileNumber(), request.getOtp());
        if (!isValid) {
            log.warn("Login failed for mobile: {} - Invalid or expired OTP", request.getMobileNumber());
            throw WorklyException.unauthorized("Invalid or expired OTP");
        }

        // Ensure Seeker Profile exists (Implicit Registration)
        if (profileService.getSeekerProfile(request.getMobileNumber()).isEmpty()) {
            log.info("Creating default seeker profile for mobile: {}", request.getMobileNumber());
            com.workly.modules.profile.SkillSeekerProfile profile = new com.workly.modules.profile.SkillSeekerProfile();
            profile.setMobileNumber(request.getMobileNumber());
            profile.setCreatedAt(java.time.LocalDateTime.now());
            profileService.createOrUpdateSeekerProfile(profile);
        }

        log.info("Login successful for mobile: {}. Generating token.", request.getMobileNumber());
        String token = jwtUtils.generateToken(request.getMobileNumber());
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .expiresIn(ttlHours * 3600L)
                .build();

        return ApiResponse.success(response, "Login successful");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh() {
        String currentMobile = (String) org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        log.info("Refreshing session for mobile: {}", currentMobile);

        String newToken = jwtUtils.generateToken(currentMobile);
        AuthResponse response = AuthResponse.builder()
                .token(newToken)
                .expiresIn(ttlHours * 3600L)
                .build();

        return ApiResponse.success(response, "Session revalidated");
    }
}
