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

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceClient {

    private final RestTemplate restTemplate;

    @Value("${search-service.url}")
    private String searchServiceUrl;

    public List<String> normalizeSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String url = searchServiceUrl + "/api/v1/normalization/skills";
            HttpEntity<List<String>> request = new HttpEntity<>(skills);

            ResponseEntity<List<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<List<String>>() {
                    });

            if (response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Failed to call search service for normalization", e);
            // Fallback to original skills
        }
        return skills;
    }
}
