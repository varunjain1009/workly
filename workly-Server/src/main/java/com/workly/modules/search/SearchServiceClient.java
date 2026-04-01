package com.workly.modules.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Collections;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceClient {

    private final RestTemplate restTemplate;

    @Value("${search-service.url}")
    private String searchServiceUrl;

    @CircuitBreaker(name = "searchService", fallbackMethod = "fallbackNormalizeSkills")
    public List<String> normalizeSkills(List<String> skills) {
        log.debug("SearchServiceClient: [ENTER] normalizeSkills - input skills: {}", skills);
        if (skills == null || skills.isEmpty()) {
            log.debug("SearchServiceClient: [EXIT] normalizeSkills - Empty input, returning empty list");
            return Collections.emptyList();
        }

        try {
            String url = searchServiceUrl + "/api/v1/normalization/skills";
            log.debug("SearchServiceClient: Calling external search service at: {}", url);
            HttpEntity<List<String>> request = new HttpEntity<>(skills);

            ResponseEntity<List<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<List<String>>() {
                    });

            if (response.getBody() != null) {
                log.debug("SearchServiceClient: [EXIT] normalizeSkills - Received {} normalized skills", response.getBody().size());
                return response.getBody();
            }
        } catch (Exception e) {
            log.debug("SearchServiceClient: [FAIL] normalizeSkills - External service error: {}", e.getMessage());
            log.error("Failed to call search service for normalization", e);
        }
        log.debug("SearchServiceClient: [EXIT] normalizeSkills - Falling back to original skills");
        return skills;
    }

    public List<String> fallbackNormalizeSkills(List<String> skills, Throwable t) {
        log.warn("SearchServiceClient: [FALLBACK] Circuit breaker open or error: {}. Falling back to original skills.", t.getMessage());
        return skills;
    }
}
