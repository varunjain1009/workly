package com.workly.search.service;

import com.workly.search.model.SkillDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NormalizationServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private NormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        normalizationService = new NormalizationService(elasticsearchOperations);
    }

    @Test
    void normalizeSkills_shouldReturnCorrectNormalization() {
        // Arrange
        String rawSkill = "carpantur";
        String normalizedSkill = "Carpenter";

        SkillDocument doc = new SkillDocument();
        doc.setCanonicalName(normalizedSkill);

        @SuppressWarnings("unchecked")
        SearchHit<SkillDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);

        @SuppressWarnings("unchecked")
        SearchHits<SkillDocument> hits = mock(SearchHits.class);
        when(hits.hasSearchHits()).thenReturn(true);
        when(hits.getSearchHit(0)).thenReturn(hit);

        // Mocking search for both exact check (which fails/succeeds) and fuzzy check
        // For simplicity in unit test, we return the same hit for any query
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(SkillDocument.class)))
                .thenReturn(hits);

        // Act
        List<String> result = normalizationService.normalizeSkills(List.of(rawSkill));

        // Assert
        assertEquals(1, result.size());
        assertEquals(normalizedSkill, result.get(0));
    }

    @Test
    void normalizeSkills_shouldReturnOriginalIfNoMatch() {
        // Arrange
        String rawSkill = "unknown_skill";

        @SuppressWarnings("unchecked")
        SearchHits<SkillDocument> emptyHits = mock(SearchHits.class);
        when(emptyHits.hasSearchHits()).thenReturn(false);

        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(SkillDocument.class)))
                .thenReturn(emptyHits);

        // Act
        List<String> result = normalizationService.normalizeSkills(List.of(rawSkill));

        // Assert
        assertEquals(1, result.size());
        assertEquals(rawSkill, result.get(0));
    }
}
