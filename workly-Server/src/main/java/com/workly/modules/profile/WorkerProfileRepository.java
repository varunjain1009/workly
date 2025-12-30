package com.workly.modules.profile;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface WorkerProfileRepository extends MongoRepository<WorkerProfile, String> {
    Optional<WorkerProfile> findByMobileNumber(String mobileNumber);

    @Query("{ 'available': true, 'skills': { $in: ?0 }, 'lastLocation': { $near: { $geometry: { type: 'Point', coordinates: [?1, ?2] }, $maxDistance: ?3 } } }")
    List<WorkerProfile> findMatchingWorkers(List<String> requiredSkills, double longitude, double latitude,
            double maxDistance);
}
