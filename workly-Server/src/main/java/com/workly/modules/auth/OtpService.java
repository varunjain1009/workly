package com.workly.modules.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {
    private final OtpProvider otpProvider;
    private final StringRedisTemplate redisTemplate;
    private static final String OTP_KEY_PREFIX = "otp:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long OTP_EXPIRY_MINUTES = 5;

    /**
     * Generates and sends a 4-digit OTP to the specified mobile number.
     * Note: Rate limiting for this endpoint is intentionally handled upstream at
     * the API Gateway layer
     * to prevent service degradation during brute-force attacks.
     */
    public void generateAndSendOtp(String mobileNumber) {
        String otp = String.format("%04d", SECURE_RANDOM.nextInt(10000));
        log.info("Generating OTP for mobile: {}", mobileNumber);
        redisTemplate.opsForValue().set(OTP_KEY_PREFIX + mobileNumber, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
        otpProvider.sendOtp(mobileNumber, otp);
    }

    public boolean validateOtp(String mobileNumber, String otp) {
        String storedOtp = redisTemplate.opsForValue().get(OTP_KEY_PREFIX + mobileNumber);
        log.info("Validating OTP for mobile: {}", mobileNumber);
        if (storedOtp != null && storedOtp.equals(otp)) {
            log.info("OTP matched successfully for mobile: {}", mobileNumber);
            redisTemplate.delete(OTP_KEY_PREFIX + mobileNumber);
            return true;
        }
        log.warn("OTP mismatch or expired for mobile: {}", mobileNumber);
        return false;
    }
}
