package com.workly.modules.review;

import com.workly.core.WorklyException;
import com.workly.modules.job.Job;
import com.workly.modules.job.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final com.workly.modules.job.JobService jobService;

    public Review submitReview(Review review, String reviewerMobile) {
        log.debug("ReviewService: [ENTER] submitReview - jobId: {}, reviewer: {}, rating: {}", review.getJobId(), reviewerMobile, review.getRating());
        if (review.getRating() < 1 || review.getRating() > 5) {
            log.debug("ReviewService: [FAIL] Invalid rating value: {}", review.getRating());
            throw WorklyException.badRequest("Rating must be between 1 and 5");
        }

        Job job = jobService.getJobById(review.getJobId());
        log.debug("ReviewService: Loaded job {} with status: {}", review.getJobId(), job.getStatus());

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw WorklyException.badRequest("Reviews can only be submitted for completed jobs");
        }

        if (!job.getSeekerMobileNumber().equals(reviewerMobile)) {
            throw WorklyException.forbidden("Only the job seeker can submit a review");
        }

        if (reviewRepository.findByJobId(review.getJobId()).isPresent()) {
            log.debug("ReviewService: [FAIL] Duplicate review attempt for job {}", review.getJobId());
            throw WorklyException.badRequest("A review already exists for this job");
        }

        review.setSeekerMobileNumber(job.getSeekerMobileNumber());
        review.setWorkerMobileNumber(job.getWorkerMobileNumber());

        Review saved = reviewRepository.save(review);
        log.debug("ReviewService: [EXIT] submitReview - Review persisted with rating {}", saved.getRating());
        return saved;
    }

    public List<Review> getWorkerReviews(String mobileNumber) {
        log.debug("ReviewService: [ENTER] getWorkerReviews - mobile: {}", mobileNumber);
        List<Review> reviews = reviewRepository.findByWorkerMobileNumber(mobileNumber);
        log.debug("ReviewService: [EXIT] getWorkerReviews - Found {} reviews", reviews.size());
        return reviews;
    }

    public double getAverageRating(String mobileNumber) {
        log.debug("ReviewService: [ENTER] getAverageRating - mobile: {}", mobileNumber);
        List<Review> reviews = getWorkerReviews(mobileNumber);
        if (reviews.isEmpty()) {
            log.debug("ReviewService: [EXIT] getAverageRating - No reviews found, returning 0.0");
            return 0.0;
        }
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        log.debug("ReviewService: [EXIT] getAverageRating - Average: {} from {} reviews", avg, reviews.size());
        return avg;
    }
}
