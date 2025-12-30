package com.workly.modules.profile;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface SkillSeekerProfileRepository extends MongoRepository<SkillSeekerProfile, String> {
    Optional<SkillSeekerProfile> findByMobileNumber(String mobileNumber);
}
