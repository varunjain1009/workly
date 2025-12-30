package com.workly.modules.review;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByWorkerMobileNumber(String mobileNumber);

    Optional<Review> findByJobId(String jobId);
}
