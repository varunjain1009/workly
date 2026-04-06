package com.workly.modules.review;

import com.workly.core.WorklyException;
import com.workly.modules.job.Job;
import com.workly.modules.job.JobStatus;
import com.workly.modules.job.outbox.OutboxEvent;
import com.workly.modules.job.outbox.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private com.workly.modules.job.JobService jobService;
    @Mock private OutboxEventRepository outboxEventRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reviewService = new ReviewService(reviewRepository, jobService, outboxEventRepository);
    }

    private Job completedJob(String seeker, String worker) {
        Job j = new Job();
        j.setStatus(JobStatus.COMPLETED);
        j.setSeekerMobileNumber(seeker);
        j.setWorkerMobileNumber(worker);
        return j;
    }

    @Test
    void submitReview_seekerRole_success() {
        Job job = completedJob("seeker1", "worker1");
        Review review = new Review();
        review.setJobId("j1");
        review.setRating(4);

        when(jobService.getJobById("j1")).thenReturn(job);
        when(reviewRepository.findByJobIdAndReviewerRole("j1", Review.ReviewerRole.SEEKER)).thenReturn(Optional.empty());
        when(reviewRepository.save(review)).thenReturn(review);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        Review result = reviewService.submitReview(review, "seeker1");

        assertEquals("seeker1", result.getSeekerMobileNumber());
        assertEquals("worker1", result.getWorkerMobileNumber());
        assertEquals(Review.ReviewerRole.SEEKER, result.getReviewerRole());
        verify(reviewRepository).save(review);
    }

    @Test
    void submitReview_workerRole_success() {
        Job job = completedJob("seeker1", "worker1");
        Review review = new Review();
        review.setJobId("j1");
        review.setRating(5);

        when(jobService.getJobById("j1")).thenReturn(job);
        when(reviewRepository.findByJobIdAndReviewerRole("j1", Review.ReviewerRole.WORKER)).thenReturn(Optional.empty());
        when(reviewRepository.save(review)).thenReturn(review);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        Review result = reviewService.submitReview(review, "worker1");
        assertEquals(Review.ReviewerRole.WORKER, result.getReviewerRole());
    }

    @Test
    void submitReview_invalidRatingLow_throws() {
        Review review = new Review();
        review.setJobId("j1");
        review.setRating(0);
        assertThrows(WorklyException.class, () -> reviewService.submitReview(review, "seeker1"));
    }

    @Test
    void submitReview_invalidRatingHigh_throws() {
        Review review = new Review();
        review.setJobId("j1");
        review.setRating(6);
        assertThrows(WorklyException.class, () -> reviewService.submitReview(review, "seeker1"));
    }

    @Test
    void submitReview_jobNotCompleted_throws() {
        Job job = new Job();
        job.setStatus(JobStatus.ASSIGNED);
        Review review = new Review();
        review.setJobId("j1");
        review.setRating(3);

        when(jobService.getJobById("j1")).thenReturn(job);
        assertThrows(WorklyException.class, () -> reviewService.submitReview(review, "seeker1"));
    }

    @Test
    void submitReview_nonParticipant_throws() {
        Job job = completedJob("seeker1", "worker1");
        Review review = new Review();
        review.setJobId("j1");
        review.setRating(3);

        when(jobService.getJobById("j1")).thenReturn(job);
        assertThrows(WorklyException.class, () -> reviewService.submitReview(review, "other"));
    }

    @Test
    void submitReview_duplicate_throws() {
        Job job = completedJob("seeker1", "worker1");
        Review review = new Review();
        review.setJobId("j1");
        review.setRating(3);

        when(jobService.getJobById("j1")).thenReturn(job);
        when(reviewRepository.findByJobIdAndReviewerRole("j1", Review.ReviewerRole.SEEKER))
                .thenReturn(Optional.of(new Review()));

        assertThrows(WorklyException.class, () -> reviewService.submitReview(review, "seeker1"));
    }

    @Test
    void disputeReview_workerDisputesSeekerReview_success() {
        Review existing = new Review();
        existing.setId("r1");
        existing.setReviewerRole(Review.ReviewerRole.SEEKER);
        existing.setWorkerMobileNumber("worker1");
        existing.setSeekerMobileNumber("seeker1");

        when(reviewRepository.findById("r1")).thenReturn(Optional.of(existing));
        when(reviewRepository.save(existing)).thenReturn(existing);

        Review result = reviewService.disputeReview("r1", "unfair", "worker1");

        assertTrue(result.isDisputed());
        assertEquals("unfair", result.getDisputeReason());
    }

    @Test
    void disputeReview_seekerDisputesWorkerReview_success() {
        Review existing = new Review();
        existing.setId("r1");
        existing.setReviewerRole(Review.ReviewerRole.WORKER);
        existing.setSeekerMobileNumber("seeker1");
        existing.setWorkerMobileNumber("worker1");

        when(reviewRepository.findById("r1")).thenReturn(Optional.of(existing));
        when(reviewRepository.save(existing)).thenReturn(existing);

        Review result = reviewService.disputeReview("r1", "unfair", "seeker1");
        assertTrue(result.isDisputed());
    }

    @Test
    void disputeReview_notFound_throws() {
        when(reviewRepository.findById("r1")).thenReturn(Optional.empty());
        assertThrows(WorklyException.class, () -> reviewService.disputeReview("r1", "reason", "user1"));
    }

    @Test
    void disputeReview_forbidden_throws() {
        Review existing = new Review();
        existing.setReviewerRole(Review.ReviewerRole.SEEKER);
        existing.setWorkerMobileNumber("worker1");
        existing.setSeekerMobileNumber("seeker1");

        when(reviewRepository.findById("r1")).thenReturn(Optional.of(existing));
        assertThrows(WorklyException.class, () -> reviewService.disputeReview("r1", "reason", "other"));
    }

    @Test
    void getWorkerReviews_returnsList() {
        when(reviewRepository.findByWorkerMobileNumberAndReviewerRole("w1", Review.ReviewerRole.SEEKER))
                .thenReturn(List.of(new Review(), new Review()));
        assertEquals(2, reviewService.getWorkerReviews("w1").size());
    }

    @Test
    void getSeekerReviews_returnsList() {
        when(reviewRepository.findBySeekerMobileNumberAndReviewerRole("s1", Review.ReviewerRole.WORKER))
                .thenReturn(List.of(new Review()));
        assertEquals(1, reviewService.getSeekerReviews("s1").size());
    }

    @Test
    void getAverageRating_withReviews_returnsAverage() {
        Review r1 = new Review(); r1.setRating(4);
        Review r2 = new Review(); r2.setRating(2);
        when(reviewRepository.findByWorkerMobileNumberAndReviewerRole("w1", Review.ReviewerRole.SEEKER))
                .thenReturn(List.of(r1, r2));
        assertEquals(3.0, reviewService.getAverageRating("w1"));
    }

    @Test
    void getAverageRating_noReviews_returnsZero() {
        when(reviewRepository.findByWorkerMobileNumberAndReviewerRole("w1", Review.ReviewerRole.SEEKER))
                .thenReturn(List.of());
        assertEquals(0.0, reviewService.getAverageRating("w1"));
    }

    @Test
    void getSeekerAverageRating_returnsAverage() {
        Review r1 = new Review(); r1.setRating(5);
        when(reviewRepository.findBySeekerMobileNumberAndReviewerRole("s1", Review.ReviewerRole.WORKER))
                .thenReturn(List.of(r1));
        assertEquals(5.0, reviewService.getSeekerAverageRating("s1"));
    }

    @Test
    void getSeekerAverageRating_noReviews_returnsZero() {
        when(reviewRepository.findBySeekerMobileNumberAndReviewerRole("s1", Review.ReviewerRole.WORKER))
                .thenReturn(List.of());
        assertEquals(0.0, reviewService.getSeekerAverageRating("s1"));
    }
}
