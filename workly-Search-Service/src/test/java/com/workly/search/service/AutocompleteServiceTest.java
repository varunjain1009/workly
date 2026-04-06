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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutocompleteServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private AutocompleteService autocompleteService;

    @BeforeEach
    void setUp() {
        autocompleteService = new AutocompleteService(elasticsearchOperations, redisTemplate);
        // lenient: null/blank-query tests exit before touching Redis, so this stub
        // would be flagged as unnecessary by Mockito's strict mode without it
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void autocomplete_shouldReturnEmptyList_forNullQuery() {
        List<String> result = autocompleteService.autocomplete(null);
        assertThat(result).isEmpty();
        verifyNoInteractions(elasticsearchOperations, redisTemplate);
    }

    @Test
    void autocomplete_shouldReturnEmptyList_forBlankQuery() {
        List<String> result = autocompleteService.autocomplete("   ");
        assertThat(result).isEmpty();
        verifyNoInteractions(elasticsearchOperations, redisTemplate);
    }

    @Test
    void autocomplete_shouldReturnCachedResult_onCacheHit() {
        when(valueOps.get("autocomplete:electrician")).thenReturn("Electrician,Electrical Engineer");

        List<String> result = autocompleteService.autocomplete("Electrician");

        assertThat(result).containsExactly("Electrician", "Electrical Engineer");
        verifyNoInteractions(elasticsearchOperations);
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void autocomplete_shouldQueryElasticsearch_onCacheMiss() {
        when(valueOps.get("autocomplete:plumber")).thenReturn(null);
        SearchHits<SkillDocument> hits = searchHits("Plumber");
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(SkillDocument.class))).thenReturn(hits);

        List<String> result = autocompleteService.autocomplete("Plumber");

        assertThat(result).containsExactly("Plumber");
        verify(elasticsearchOperations).search(any(CriteriaQuery.class), eq(SkillDocument.class));
    }

    @Test
    void autocomplete_shouldCacheResults_whenElasticsearchReturnsResults() {
        when(valueOps.get("autocomplete:carpenter")).thenReturn(null);
        SearchHits<SkillDocument> hits = searchHits("Carpenter");
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(SkillDocument.class))).thenReturn(hits);

        autocompleteService.autocomplete("Carpenter");

        verify(valueOps).set("autocomplete:carpenter", "Carpenter", Duration.ofMinutes(30));
    }

    @Test
    void autocomplete_shouldNotCacheResults_whenElasticsearchReturnsEmpty() {
        when(valueOps.get("autocomplete:unknown")).thenReturn(null);
        SearchHits<SkillDocument> emptyHits = emptySearchHits();
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(SkillDocument.class))).thenReturn(emptyHits);

        List<String> result = autocompleteService.autocomplete("unknown");

        assertThat(result).isEmpty();
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void autocomplete_shouldNormalizeQueryToLowercase_beforeCacheCheck() {
        when(valueOps.get("autocomplete:mechanic")).thenReturn("Mechanic");

        List<String> result = autocompleteService.autocomplete("MECHANIC");

        assertThat(result).containsExactly("Mechanic");
        verify(valueOps).get("autocomplete:mechanic");
    }

    @Test
    void autocomplete_shouldDeduplicateResults() {
        when(valueOps.get("autocomplete:elect")).thenReturn(null);
        SearchHits<SkillDocument> hits = searchHits("Electrician", "Electrician", "Electrical Engineer");
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(SkillDocument.class))).thenReturn(hits);

        List<String> result = autocompleteService.autocomplete("elect");

        assertThat(result).containsExactlyInAnyOrder("Electrician", "Electrical Engineer");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private SearchHits<SkillDocument> searchHits(String... names) {
        SearchHits<SkillDocument> hits = mock(SearchHits.class);
        List<SearchHit<SkillDocument>> hitList = Stream.of(names).map(name -> {
            SearchHit<SkillDocument> hit = mock(SearchHit.class);
            SkillDocument doc = new SkillDocument();
            doc.setCanonicalName(name);
            when(hit.getContent()).thenReturn(doc);
            return hit;
        }).toList();
        when(hits.stream()).thenReturn(hitList.stream());
        return hits;
    }

    @SuppressWarnings("unchecked")
    private SearchHits<SkillDocument> emptySearchHits() {
        SearchHits<SkillDocument> hits = mock(SearchHits.class);
        when(hits.stream()).thenReturn(Stream.of());
        return hits;
    }
}
