package com.workly.search.service;

import com.workly.search.model.SkillDocument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutocompleteService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "autocomplete:";

    public List<String> autocomplete(String query) {
        log.debug("AutocompleteService: [ENTER] autocomplete - query: '{}'", query);
        if (query == null || query.trim().isEmpty()) {
            log.debug("AutocompleteService: autocomplete - empty query, returning empty list");
            return List.of();
        }

        String normalizedQuery = query.toLowerCase().trim();
        String cacheKey = CACHE_PREFIX + normalizedQuery;

        // 1. Check Cache
        String cachedResult = redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            List<String> cached = List.of(cachedResult.split(","));
            log.debug("AutocompleteService: [EXIT] autocomplete - cache HIT for '{}', {} results", normalizedQuery, cached.size());
            return cached;
        }
        log.debug("AutocompleteService: autocomplete - cache MISS for '{}', querying Elasticsearch", normalizedQuery);

        // 2. Query Elasticsearch
        List<String> results = searchInElasticsearch(normalizedQuery);

        // 3. Cache Result (if not empty)
        if (!results.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, String.join(",", results), Duration.ofMinutes(30));
            log.debug("AutocompleteService: autocomplete - cached {} results for '{}'", results.size(), normalizedQuery);
        }

        log.debug("AutocompleteService: [EXIT] autocomplete - query: '{}', {} results", normalizedQuery, results.size());
        return results;
    }

    private List<String> searchInElasticsearch(String query) {
        log.debug("AutocompleteService: [ENTER] searchInElasticsearch - query: '{}'", query);
        try {
            // Construct Criteria Query
            // Match prefix OR fuzzy OR alias match
            Criteria criteria = new Criteria("canonicalName").contains(query)
                    .or(new Criteria("canonicalName").fuzzy(query))
                    .or(new Criteria("aliases").is(query)); // Analyzer on index handles phonetics

            CriteriaQuery searchQuery = new CriteriaQuery(criteria)
                    .setPageable(PageRequest.of(0, 5));

            SearchHits<SkillDocument> hits = elasticsearchOperations.search(searchQuery, SkillDocument.class);
            List<String> results = hits.stream()
                    .map(hit -> hit.getContent().getCanonicalName())
                    .distinct()
                    .collect(Collectors.toList());
            log.debug("AutocompleteService: [EXIT] searchInElasticsearch - query: '{}', totalHits: {}, distinct results: {}",
                    query, hits.getTotalHits(), results.size());
            return results;
        } catch (Exception e) {
            log.warn("AutocompleteService: Elasticsearch unavailable for query '{}' — returning empty list ({})", query, e.getMessage());
            return List.of();
        }
    }
}
