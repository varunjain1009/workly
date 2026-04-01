package com.workly.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Lightweight HTTP client that calls the main workly-Server
 * to ensure a seeker profile exists on login.
 * In monolith mode, the server handles this directly.
 */
@Slf4j
@Service
public class ProfileClient {

    private final RestTemplate restTemplate;

    @Value("${profile-service.url:http://localhost:8080}")
    private String profileServiceUrl;

    public ProfileClient() {
        this.restTemplate = new RestTemplate();
    }

    public void ensureSeekerProfileExists(String mobileNumber) {
        try {
            String url = profileServiceUrl + "/api/v1/profile/seeker/ensure?mobile=" + mobileNumber;
            restTemplate.postForEntity(url, null, Void.class);
            log.debug("ProfileClient: Ensured seeker profile for {}", mobileNumber);
        } catch (Exception e) {
            log.warn("ProfileClient: Could not ensure seeker profile for {} - {}", mobileNumber, e.getMessage());
            // Non-fatal: profile will be created on first access
        }
    }
}
