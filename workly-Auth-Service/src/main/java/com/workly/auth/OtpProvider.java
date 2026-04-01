package com.workly.auth;

public interface OtpProvider {
    void sendOtp(String mobileNumber, String otp);
}
