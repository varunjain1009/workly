package com.workly.modules.job;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface JobRepository extends MongoRepository<Job, String> {
    List<Job> findBySeekerMobileNumber(String mobileNumber);

    List<Job> findByWorkerMobileNumber(String mobileNumber);

    List<Job> findByStatus(JobStatus status);

    List<Job> findByStatusIn(List<JobStatus> statuses);
}
