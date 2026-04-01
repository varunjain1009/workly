package com.workly.notification.domain.job;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface JobRepository extends MongoRepository<Job, String> {
    List<Job> findByStatus(JobStatus status);
}
