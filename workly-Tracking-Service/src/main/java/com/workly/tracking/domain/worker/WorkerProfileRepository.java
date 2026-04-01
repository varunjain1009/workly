package com.workly.tracking.domain.worker;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WorkerProfileRepository extends MongoRepository<WorkerProfile, String> {
    List<WorkerProfile> findByMobileNumber(String mobileNumber);
    List<WorkerProfile> findByMobileNumberIn(List<String> mobileNumbers);
}
