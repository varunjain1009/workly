package com.workly.modules.auth;

import com.workly.common.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final int ttlHours = 24;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", secret);
        ReflectionTestUtils.setField(jwtUtils, "ttlHours", ttlHours);
        jwtUtils.init();
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        String mobileNumber = "1234567890";
        String token = jwtUtils.generateToken(mobileNumber);

        assertNotNull(token);
        assertEquals(mobileNumber, jwtUtils.extractMobileNumber(token));
    }

    @Test
    void validateToken_ShouldReturnTrueForValidToken() {
        String mobileNumber = "1234567890";
        String token = jwtUtils.generateToken(mobileNumber);

        assertTrue(jwtUtils.validateToken(token, mobileNumber));
    }

    @Test
    void validateToken_ShouldReturnFalseForInvalidMobile() {
        String mobileNumber = "1234567890";
        String token = jwtUtils.generateToken(mobileNumber);

        assertFalse(jwtUtils.validateToken(token, "0987654321"));
    }
}
