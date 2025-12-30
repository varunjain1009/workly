package com.workly.modules.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "fast2sms")
public class Fast2SmsOtpProvider implements OtpProvider {

    @Value("${sms.fast2sms.api.key:}")
    private String apiKey;

    @Override
    public void sendOtp(String mobileNumber, String otp) {
        log.info("Sending Fast2SMS OTP [{}] to mobile number [{}] using API Key: {}", otp, mobileNumber, apiKey);
        // Implementation for Fast2SMS API would go here
    }
}
