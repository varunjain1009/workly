package com.workly.search.repository.mongo;

import com.workly.search.model.Skill;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillRepository extends MongoRepository<Skill, String> {
    java.util.Optional<Skill> findByCanonicalName(String canonicalName);
}
