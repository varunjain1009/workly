package com.workly.modules.profile;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface SkillSeekerProfileRepository extends MongoRepository<SkillSeekerProfile, String> {
    Optional<SkillSeekerProfile> findByMobileNumber(String mobileNumber);

    long countByCreatedAtAfter(java.time.LocalDateTime date);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
