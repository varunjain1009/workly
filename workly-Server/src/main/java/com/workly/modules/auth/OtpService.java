package com.workly.modules.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {
    private final OtpProvider otpProvider;
    private final StringRedisTemplate redisTemplate;
    private static final String OTP_KEY_PREFIX = "otp:";
    private static final long OTP_EXPIRY_MINUTES = 5;

    public void generateAndSendOtp(String mobileNumber) {
        String otp = String.format("%04d", new Random().nextInt(10000));
        log.info("Generating 4-digit OTP for mobile: {}", mobileNumber);
        redisTemplate.opsForValue().set(OTP_KEY_PREFIX + mobileNumber, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
        otpProvider.sendOtp(mobileNumber, otp);
    }

    public boolean validateOtp(String mobileNumber, String otp) {
        String storedOtp = redisTemplate.opsForValue().get(OTP_KEY_PREFIX + mobileNumber);
        log.info("Validating OTP for mobile: {}. Received: {}, Stored: {}", mobileNumber, otp, storedOtp);
        if (storedOtp != null && storedOtp.equals(otp)) {
            log.info("OTP matched successfully for mobile: {}", mobileNumber);
            redisTemplate.delete(OTP_KEY_PREFIX + mobileNumber);
            return true;
        }
        log.warn("OTP mismatch or expired for mobile: {}. Received: {}, Stored: {}", mobileNumber, otp, storedOtp);
        return false;
    }
}
