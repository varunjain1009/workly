package com.workly.modules.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OtpServiceTest {

    @Mock
    private OtpProvider otpProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        otpService = new OtpService(otpProvider, redisTemplate);
    }

    @Test
    void generateAndSendOtp_ShouldStoreInRedisAndSendViaProvider() {
        String mobileNumber = "1234567890";

        otpService.generateAndSendOtp(mobileNumber);

        verify(valueOperations).set(eq("otp:" + mobileNumber), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        verify(otpProvider).sendOtp(eq(mobileNumber), anyString());
    }

    @Test
    void validateOtp_ShouldReturnTrueForValidOtp() {
        String mobileNumber = "1234567890";
        String otp = "1234";
        when(valueOperations.get("otp:" + mobileNumber)).thenReturn(otp);

        boolean result = otpService.validateOtp(mobileNumber, otp);

        assertTrue(result);
        verify(redisTemplate).delete("otp:" + mobileNumber);
    }

    @Test
    void validateOtp_ShouldReturnFalseForInvalidOtp() {
        String mobileNumber = "1234567890";
        String otp = "1234";
        when(valueOperations.get("otp:" + mobileNumber)).thenReturn("5678");

        boolean result = otpService.validateOtp(mobileNumber, otp);

        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }
}
