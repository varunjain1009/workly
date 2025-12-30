package com.workly.search.service;

import com.workly.search.model.SkillDocument;
import com.workly.search.repository.search.SkillSearchRepository;
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

    private final SkillSearchRepository skillSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "autocomplete:";

    public List<String> autocomplete(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String normalizedQuery = query.toLowerCase().trim();
        String cacheKey = CACHE_PREFIX + normalizedQuery;

        // 1. Check Cache
        String cachedResult = redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            return List.of(cachedResult.split(","));
        }

        // 2. Query Elasticsearch
        List<String> results = searchInElasticsearch(normalizedQuery);

        // 3. Cache Result (if not empty)
        if (!results.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, String.join(",", results), Duration.ofMinutes(30));
        }

        return results;
    }

    private List<String> searchInElasticsearch(String query) {
        // Construct Criteria Query
        // Match prefix OR fuzzy OR alias match
        Criteria criteria = new Criteria("canonicalName").contains(query)
                .or(new Criteria("canonicalName").fuzzy(query))
                .or(new Criteria("aliases").is(query)); // Analyzer on index handles phonetics

        CriteriaQuery searchQuery = new CriteriaQuery(criteria)
                .setPageable(PageRequest.of(0, 5));

        SearchHits<SkillDocument> hits = elasticsearchOperations.search(searchQuery, SkillDocument.class);
        return hits.stream()
                .map(hit -> hit.getContent().getCanonicalName())
                .distinct()
                .collect(Collectors.toList());
    }
}
