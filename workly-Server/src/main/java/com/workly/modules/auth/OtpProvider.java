package com.workly.modules.auth;

public interface OtpProvider {
    void sendOtp(String mobileNumber, String otp);
}
