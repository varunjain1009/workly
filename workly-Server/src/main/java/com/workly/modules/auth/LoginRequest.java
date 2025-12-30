package com.workly.modules.auth;

import lombok.Data;

@Data
public class LoginRequest {
    private String mobileNumber;
    private String otp;
}
