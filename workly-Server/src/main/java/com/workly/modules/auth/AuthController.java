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
import jakarta.validation.Valid;

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
    public ApiResponse<Void> requestOtp(@Valid @RequestBody OtpRequest request) {
        log.debug("AuthController: [ENTER] requestOtp - mobileNumber param: {}", request.getMobileNumber());
        log.info("Received OTP request for mobile: {}", request.getMobileNumber());
        otpService.generateAndSendOtp(request.getMobileNumber());
        log.debug("AuthController: [EXIT] requestOtp - Internal OtpService execution finished. Returning success response.");
        return ApiResponse.success(null, "OTP sent successfully");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("AuthController: [ENTER] login - mobileNumber: {}", request.getMobileNumber());
        log.info("Received login attempt for mobile: {}", request.getMobileNumber());
        boolean isValid = otpService.validateOtp(request.getMobileNumber(), request.getOtp());
        if (!isValid) {
            log.debug("AuthController: [FAIL] login - Validation failed for {}", request.getMobileNumber());
            log.warn("Login failed for mobile: {} - Invalid or expired OTP", request.getMobileNumber());
            throw WorklyException.unauthorized("Invalid or expired OTP");
        }

        // Ensure Seeker Profile exists (Implicit Registration)
        // Ensure Seeker Profile exists transactionally
        profileService.getOrCreateSeekerProfile(request.getMobileNumber());

        log.info("Login successful for mobile: {}. Generating token.", request.getMobileNumber());
        log.debug("AuthController: Generating JWT via jwtUtils...");
        String token = jwtUtils.generateToken(request.getMobileNumber());
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .expiresIn(ttlHours * 3600L)
                .build();

        log.debug("AuthController: [EXIT] login - Succesfully returning new session token payload.");
        return ApiResponse.success(response, "Login successful");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh() {
        log.debug("AuthController: [ENTER] refresh processing invoked.");
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            log.debug("AuthController: [FAIL] refresh - Missing valid SecurityContext/Principal. Throwing Exception.");
            throw WorklyException.unauthorized("No valid session found");
        }
        String currentMobile = (String) auth.getPrincipal();
        log.info("Refreshing session for mobile: {}", currentMobile);

        log.debug("AuthController: Requesting fresh JWT replacement token...");
        String newToken = jwtUtils.generateToken(currentMobile);
        AuthResponse response = AuthResponse.builder()
                .token(newToken)
                .expiresIn(ttlHours * 3600L)
                .build();

        log.debug("AuthController: [EXIT] refresh - Returning revalidated payload containing new Token.");
        return ApiResponse.success(response, "Session revalidated");
    }
}
