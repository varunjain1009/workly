package com.workly.search.repository.search;

import com.workly.search.model.SkillDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillSearchRepository extends ElasticsearchRepository<SkillDocument, String> {
    java.util.Optional<SkillDocument> findByCanonicalName(String canonicalName);
}
