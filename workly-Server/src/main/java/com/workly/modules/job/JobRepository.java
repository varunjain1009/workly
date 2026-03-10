package com.workly.modules.job;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface JobRepository extends MongoRepository<Job, String> {
    List<Job> findBySeekerMobileNumber(String mobileNumber);

    List<Job> findByWorkerMobileNumber(String mobileNumber);

    List<Job> findByStatus(JobStatus status);

    List<Job> findByStatusIn(List<JobStatus> statuses);

    List<Job> findBySeekerMobileNumberAndStatusIn(String mobileNumber, List<JobStatus> statuses);

    long countByStatus(JobStatus status);

    long countByStatusIn(List<JobStatus> statuses);

    @Aggregation(pipeline = {
            "{ $match: { status: ?0 } }",
            "{ $group: { _id: null, total: { $sum: '$budget' } } }"
    })
    Double sumBudgetByStatus(JobStatus status);
}
