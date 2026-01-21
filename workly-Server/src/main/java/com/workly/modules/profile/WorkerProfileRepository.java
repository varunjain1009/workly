package com.workly.modules.profile;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface WorkerProfileRepository extends MongoRepository<WorkerProfile, String> {
    List<WorkerProfile> findByMobileNumber(String mobileNumber);

    @Query("{ 'available': true, 'skills': { $in: ?0 }, 'lastLocation': { $near: { $geometry: { type: 'Point', coordinates: [?1, ?2] }, $maxDistance: ?3 } } }")
    List<WorkerProfile> findMatchingWorkers(List<String> requiredSkills, double longitude, double latitude,
            double maxDistance);

    long countByCreatedAtAfter(java.time.LocalDateTime date);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
