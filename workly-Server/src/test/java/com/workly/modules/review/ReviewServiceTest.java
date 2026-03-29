package com.workly.modules.review;

import com.workly.core.WorklyException;
import com.workly.modules.job.Job;
import com.workly.modules.job.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.workly.modules.job.outbox.OutboxEvent;
import com.workly.modules.job.outbox.OutboxEventRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private com.workly.modules.job.JobService jobService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reviewService = new ReviewService(reviewRepository, jobService, outboxEventRepository);
    }

    @Test
    void submitReview_ShouldSucceedForCompletedJob() {
        String jobId = "job123";
        Job job = new Job();
        job.setStatus(JobStatus.COMPLETED);
        job.setSeekerMobileNumber("123");
        job.setWorkerMobileNumber("456");

        Review review = new Review();
        review.setJobId(jobId);
        review.setRating(5);

        when(jobService.getJobById(jobId)).thenReturn(job);
        when(reviewRepository.findByJobIdAndReviewerRole(jobId, Review.ReviewerRole.SEEKER)).thenReturn(Optional.empty());
        when(reviewRepository.save(review)).thenReturn(review);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        Review result = reviewService.submitReview(review, "123");

        assertNotNull(result);
        assertEquals("123", result.getSeekerMobileNumber());
        assertEquals("456", result.getWorkerMobileNumber());
        verify(reviewRepository).save(review);
    }

    @Test
    void submitReview_ShouldThrowExceptionIfJobNotCompleted() {
        String jobId = "job123";
        Job job = new Job();
        job.setStatus(JobStatus.ASSIGNED);

        Review review = new Review();
        review.setJobId(jobId);

        when(jobService.getJobById(jobId)).thenReturn(job);

        assertThrows(WorklyException.class, () -> reviewService.submitReview(review, "123"));
    }
}
