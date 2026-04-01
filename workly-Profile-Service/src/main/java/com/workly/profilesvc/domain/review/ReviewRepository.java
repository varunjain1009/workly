package com.workly.profilesvc.domain.review;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByWorkerMobileNumberAndReviewerRole(String mobileNumber, Review.ReviewerRole role);
    List<Review> findBySeekerMobileNumberAndReviewerRole(String mobileNumber, Review.ReviewerRole role);
}
