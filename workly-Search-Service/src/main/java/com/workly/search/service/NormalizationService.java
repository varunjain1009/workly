package com.workly.search.service;

import com.workly.search.model.SkillDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NormalizationService {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * Normalizes a list of skills.
     * 
     * @param skills List of raw skill strings (e.g., ["carpantur", "plumber"])
     * @return List of normalized skill strings (e.g., ["Carpenter", "Plumber"])
     */
    public List<String> normalizeSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }
        return skills.stream()
                .map(this::normalizeSingleSkill)
                .collect(Collectors.toList());
    }

    private String normalizeSingleSkill(String rawSkill) {
        if (rawSkill == null || rawSkill.trim().isEmpty()) {
            return rawSkill;
        }

        String query = rawSkill.trim();

        // 1. Try Exact Match (Case Insensitive)
        Criteria exactCriteria = new Criteria("canonicalName").is(query)
                .or(new Criteria("aliases").is(query));

        String bestMatch = searchOne(exactCriteria);
        if (bestMatch != null) {
            return bestMatch;
        }

        // 2. Try Fuzzy Match
        Criteria fuzzyCriteria = new Criteria("canonicalName").fuzzy(query)
                .or(new Criteria("aliases").fuzzy(query));

        bestMatch = searchOne(fuzzyCriteria);
        if (bestMatch != null) {
            log.info("Normalized '{}' to '{}'", query, bestMatch);
            return bestMatch;
        }

        // 3. Fallback: Return original
        log.warn("Could not normalize '{}', returning original", query);
        return query;
    }

    private String searchOne(Criteria criteria) {
        CriteriaQuery searchQuery = new CriteriaQuery(criteria);
        searchQuery.setPageable(org.springframework.data.domain.PageRequest.of(0, 1));

        SearchHits<SkillDocument> hits = elasticsearchOperations.search(searchQuery, SkillDocument.class);
        if (hits.hasSearchHits()) {
            return hits.getSearchHit(0).getContent().getCanonicalName();
        }
        return null;
    }
}
