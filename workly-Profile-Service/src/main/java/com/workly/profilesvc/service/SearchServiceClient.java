package com.workly.profilesvc.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceClient {

    private final RestTemplate restTemplate;

    @Value("${search-service.url}")
    private String searchServiceUrl;

    @CircuitBreaker(name = "searchService", fallbackMethod = "fallbackNormalizeSkills")
    public List<String> normalizeSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) return Collections.emptyList();
        try {
            String url = searchServiceUrl + "/api/v1/normalization/skills";
            HttpEntity<List<String>> request = new HttpEntity<>(skills);
            ResponseEntity<List<String>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, new ParameterizedTypeReference<List<String>>() {});
            if (response.getBody() != null) return response.getBody();
        } catch (Exception e) {
            log.error("SearchServiceClient: Failed to call search service for normalization", e);
        }
        return skills;
    }

    public List<String> fallbackNormalizeSkills(List<String> skills, Throwable t) {
        log.warn("SearchServiceClient: [FALLBACK] Circuit breaker open: {}. Using original skills.", t.getMessage());
        return skills;
    }
}
