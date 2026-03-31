package com.workly.modules.job;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface JobRepository extends MongoRepository<Job, String> {
    List<Job> findBySeekerMobileNumber(String mobileNumber);

    List<Job> findByWorkerMobileNumber(String mobileNumber);

    /** Returns all jobs assigned to the given worker, newest first. */
    List<Job> findByWorkerMobileNumberOrderByCreatedAtDesc(String mobileNumber);

    List<Job> findByStatus(JobStatus status);

    List<Job> findByStatusIn(List<JobStatus> statuses);

    List<Job> findBySeekerMobileNumberAndStatusIn(String mobileNumber, List<JobStatus> statuses);

    // --- Paginated variants ---
    List<Job> findBySeekerMobileNumberAndStatusIn(String mobileNumber, List<JobStatus> statuses, Pageable pageable);

    List<Job> findByWorkerMobileNumberOrderByCreatedAtDesc(String mobileNumber, Pageable pageable);

    /**
     * Geo-spatial + skill filtered query for matching available jobs.
     * Uses the 2dsphere index on 'location' to return only jobs within
     * maxDistance meters of the worker's position whose requiredSkills
     * overlap with the worker's skills.
     */
    @Query("{ 'status': { $in: ?2 }, 'requiredSkills': { $in: ?3 }, 'location': { $near: { $geometry: { type: 'Point', coordinates: [?0, ?1] }, $maxDistance: ?4 } } }")
    List<Job> findMatchingJobs(double longitude, double latitude,
            List<JobStatus> statuses, List<String> workerSkills,
            double maxDistanceMeters, Pageable pageable);

    /**
     * Fallback: geo-filtered only (no skill filter), for workers who
     * haven't set skills yet.
     */
    @Query("{ 'status': { $in: ?2 }, 'location': { $near: { $geometry: { type: 'Point', coordinates: [?0, ?1] }, $maxDistance: ?3 } } }")
    List<Job> findNearbyJobs(double longitude, double latitude,
            List<JobStatus> statuses,
            double maxDistanceMeters, Pageable pageable);

    long countByStatus(JobStatus status);

    long countByStatusIn(List<JobStatus> statuses);

    @Aggregation(pipeline = {
            "{ $match: { status: ?0 } }",
            "{ $group: { _id: null, total: { $sum: '$budget' } } }"
    })
    Double sumBudgetByStatus(JobStatus status);

    long countByLocationNearAndStatusIn(org.springframework.data.geo.Point location, org.springframework.data.geo.Distance distance, List<JobStatus> statuses);
}
