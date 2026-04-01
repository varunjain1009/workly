package com.workly.notification.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FcmTokenRepository {
    private final StringRedisTemplate redisTemplate;
    private static final String FCM_TOKEN_PREFIX = "fcm:";

    public void saveToken(String mobileNumber, String token) {
        redisTemplate.opsForValue().set(FCM_TOKEN_PREFIX + mobileNumber, token);
    }

    public String getToken(String mobileNumber) {
        return redisTemplate.opsForValue().get(FCM_TOKEN_PREFIX + mobileNumber);
    }
}
