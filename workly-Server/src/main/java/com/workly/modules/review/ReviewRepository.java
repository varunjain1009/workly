package com.workly.modules.review;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByWorkerMobileNumberAndReviewerRole(String mobileNumber, Review.ReviewerRole role);

    List<Review> findBySeekerMobileNumberAndReviewerRole(String mobileNumber, Review.ReviewerRole role);

    Optional<Review> findByJobIdAndReviewerRole(String jobId, Review.ReviewerRole role);
}
