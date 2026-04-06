package com.workly.modules.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SearchServiceClientTest {

    @Mock private RestTemplate restTemplate;

    private SearchServiceClient client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new SearchServiceClient(restTemplate);
        ReflectionTestUtils.setField(client, "searchServiceUrl", "http://localhost:8083");
    }

    @Test
    void normalizeSkills_emptyList_returnsEmpty() {
        List<String> result = client.normalizeSkills(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void normalizeSkills_nullList_returnsEmpty() {
        List<String> result = client.normalizeSkills(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void normalizeSkills_success_returnsNormalized() {
        List<String> normalized = List.of("electrician", "plumber");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(normalized));

        List<String> result = client.normalizeSkills(List.of("Electrician", "plmber"));

        assertEquals(normalized, result);
    }

    @Test
    void normalizeSkills_serviceError_returnsFallback() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("service down"));

        List<String> input = List.of("plumber");
        List<String> result = client.normalizeSkills(input);

        assertEquals(input, result); // fallback to original
    }

    @Test
    void normalizeSkills_nullResponseBody_returnsFallback() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(null));

        List<String> input = List.of("plumber");
        List<String> result = client.normalizeSkills(input);

        assertEquals(input, result);
    }

    @Test
    void fallbackNormalizeSkills_returnsOriginalSkills() {
        List<String> skills = List.of("electrician");
        List<String> result = client.fallbackNormalizeSkills(skills, new RuntimeException("cb open"));
        assertEquals(skills, result);
    }
}
