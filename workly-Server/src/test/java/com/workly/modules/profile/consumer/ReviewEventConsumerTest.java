package com.workly.modules.profile.consumer;

import com.workly.modules.profile.ProfileService;
import com.workly.modules.profile.SkillSeekerProfile;
import com.workly.modules.profile.WorkerProfile;
import com.workly.modules.review.Review;
import com.workly.modules.review.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReviewEventConsumerTest {

    @Mock private ProfileService profileService;
    @Mock private ReviewService reviewService;

    private ReviewEventConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = new ReviewEventConsumer(profileService, reviewService);
    }

    @Test
    void consumeReviewEvent_seekerRole_updatesWorkerProfile() {
        WorkerProfile wp = new WorkerProfile();
        wp.setMobileNumber("w1");

        when(reviewService.getAverageRating("w1")).thenReturn(4.9);
        Review r = new Review(); r.setRating(5);
        when(reviewService.getWorkerReviews("w1")).thenReturn(List.of(r, r, r, r, r, r, r, r, r, r,
                r, r, r, r, r, r, r, r, r, r)); // 20 reviews
        when(profileService.getWorkerProfile("w1")).thenReturn(Optional.of(wp));
        when(profileService.createOrUpdateWorkerProfile(any())).thenReturn(wp);

        consumer.consumeReviewEvent(Map.of(
                "reviewerRole", "SEEKER",
                "workerMobileNumber", "w1",
                "seekerMobileNumber", "s1"
        ));

        verify(profileService).createOrUpdateWorkerProfile(argThat(p ->
                p.getTier() == WorkerProfile.ProviderTier.SUPER_PRO));
    }

    @Test
    void consumeReviewEvent_seekerRole_premiumTier() {
        WorkerProfile wp = new WorkerProfile();
        wp.setMobileNumber("w1");

        when(reviewService.getAverageRating("w1")).thenReturn(4.6);
        Review r = new Review(); r.setRating(5);
        when(reviewService.getWorkerReviews("w1")).thenReturn(List.of(r, r, r, r, r, r, r, r, r, r)); // 10
        when(profileService.getWorkerProfile("w1")).thenReturn(Optional.of(wp));
        when(profileService.createOrUpdateWorkerProfile(any())).thenReturn(wp);

        consumer.consumeReviewEvent(Map.of(
                "reviewerRole", "SEEKER",
                "workerMobileNumber", "w1",
                "seekerMobileNumber", "s1"
        ));

        verify(profileService).createOrUpdateWorkerProfile(argThat(p ->
                p.getTier() == WorkerProfile.ProviderTier.PREMIUM));
    }

    @Test
    void consumeReviewEvent_workerRole_updatesSeekerProfile() {
        SkillSeekerProfile sp = new SkillSeekerProfile();
        sp.setMobileNumber("s1");

        when(reviewService.getSeekerAverageRating("s1")).thenReturn(4.0);
        when(reviewService.getSeekerReviews("s1")).thenReturn(List.of(new Review()));
        when(profileService.getSeekerProfile("s1")).thenReturn(Optional.of(sp));
        when(profileService.createOrUpdateSeekerProfile(any())).thenReturn(sp);

        consumer.consumeReviewEvent(Map.of(
                "reviewerRole", "WORKER",
                "workerMobileNumber", "w1",
                "seekerMobileNumber", "s1"
        ));

        verify(profileService).createOrUpdateSeekerProfile(any());
    }

    @Test
    void consumeReviewEvent_unknownRole_logsWarning() {
        // Should not throw, just log
        consumer.consumeReviewEvent(Map.of("reviewerRole", "UNKNOWN"));
        verify(profileService, never()).createOrUpdateWorkerProfile(any());
    }

    @Test
    void consumeReviewEvent_exceptionHandled_noThrow() {
        when(reviewService.getAverageRating(any())).thenThrow(new RuntimeException("DB error"));

        // Should not propagate exception
        consumer.consumeReviewEvent(Map.of(
                "reviewerRole", "SEEKER",
                "workerMobileNumber", "w1",
                "seekerMobileNumber", "s1"
        ));
    }
}
