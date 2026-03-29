package com.workly.modules.review;

import com.workly.core.WorklyException;
import com.workly.modules.job.Job;
import com.workly.modules.job.JobStatus;
import com.workly.modules.job.outbox.OutboxEvent;
import com.workly.modules.job.outbox.OutboxEventRepository;
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
    private final OutboxEventRepository outboxEventRepository;

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

        Review.ReviewerRole role;
        if (job.getSeekerMobileNumber().equals(reviewerMobile)) {
            role = Review.ReviewerRole.SEEKER;
        } else if (job.getWorkerMobileNumber().equals(reviewerMobile)) {
            role = Review.ReviewerRole.WORKER;
        } else {
            throw WorklyException.forbidden("Only participants of this job can submit a review");
        }

        if (reviewRepository.findByJobIdAndReviewerRole(review.getJobId(), role).isPresent()) {
            log.debug("ReviewService: [FAIL] Duplicate review attempt for job {} by role {}", review.getJobId(), role);
            throw WorklyException.badRequest("A review already exists from your side for this job");
        }

        review.setSeekerMobileNumber(job.getSeekerMobileNumber());
        review.setWorkerMobileNumber(job.getWorkerMobileNumber());
        review.setReviewerRole(role);

        Review saved = reviewRepository.save(review);
        log.debug("ReviewService: [EXIT] submitReview - Review persisted with rating {}", saved.getRating());

        // Save to Outbox for asynchronous profile update via Kafka
        OutboxEvent event = new OutboxEvent();
        event.setTopic("review.submitted");
        event.setPayload(saved);
        outboxEventRepository.save(event);

        return saved;
    }

    public Review disputeReview(String reviewId, String reason, String mobileNumber) {
        log.debug("ReviewService: [ENTER] disputeReview - reviewId: {}, mobile: {}", reviewId, mobileNumber);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> WorklyException.notFound("Review not found"));

        if (review.getReviewerRole() == Review.ReviewerRole.SEEKER && !review.getWorkerMobileNumber().equals(mobileNumber)) {
            throw WorklyException.forbidden("You can only dispute reviews left for you");
        }
        if (review.getReviewerRole() == Review.ReviewerRole.WORKER && !review.getSeekerMobileNumber().equals(mobileNumber)) {
            throw WorklyException.forbidden("You can only dispute reviews left for you");
        }

        review.setDisputed(true);
        review.setDisputeReason(reason);
        Review saved = reviewRepository.save(review);
        log.info("ReviewService: Review {} disputed by {}", reviewId, mobileNumber);
        return saved;
    }

    public List<Review> getWorkerReviews(String mobileNumber) {
        log.debug("ReviewService: [ENTER] getWorkerReviews - mobile: {}", mobileNumber);
        // Reviews about worker are written by seeker
        List<Review> reviews = reviewRepository.findByWorkerMobileNumberAndReviewerRole(mobileNumber, Review.ReviewerRole.SEEKER);
        log.debug("ReviewService: [EXIT] getWorkerReviews - Found {} reviews", reviews.size());
        return reviews;
    }

    public List<Review> getSeekerReviews(String mobileNumber) {
        log.debug("ReviewService: [ENTER] getSeekerReviews - mobile: {}", mobileNumber);
        // Reviews about seeker are written by worker
        List<Review> reviews = reviewRepository.findBySeekerMobileNumberAndReviewerRole(mobileNumber, Review.ReviewerRole.WORKER);
        log.debug("ReviewService: [EXIT] getSeekerReviews - Found {} reviews", reviews.size());
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

    public double getSeekerAverageRating(String mobileNumber) {
        log.debug("ReviewService: [ENTER] getSeekerAverageRating - mobile: {}", mobileNumber);
        List<Review> reviews = getSeekerReviews(mobileNumber);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
    }
}
