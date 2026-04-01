package com.workly.profilesvc.consumer;

import com.workly.profilesvc.domain.review.Review;
import com.workly.profilesvc.domain.review.ReviewRepository;
import com.workly.profilesvc.domain.worker.WorkerProfile;
import com.workly.profilesvc.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewEventConsumer {

    private final ProfileService profileService;
    private final ReviewRepository reviewRepository;

    @KafkaListener(topics = "review.submitted", groupId = "profile-group")
    public void consumeReviewEvent(Map<String, Object> message) {
        try {
            log.info("ReviewEventConsumer: Received review event: {}", message);
            String reviewerRoleStr = (String) message.get("reviewerRole");
            String seekerMobileNumber = (String) message.get("seekerMobileNumber");
            String workerMobileNumber = (String) message.get("workerMobileNumber");

            if ("SEEKER".equals(reviewerRoleStr)) {
                List<Review> reviews = reviewRepository.findByWorkerMobileNumberAndReviewerRole(
                        workerMobileNumber, Review.ReviewerRole.SEEKER);
                if (reviews.isEmpty()) return;
                double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
                int total = reviews.size();

                profileService.getWorkerProfile(workerMobileNumber).ifPresent(p -> {
                    p.setAverageRating(avg);
                    p.setTotalReviews(total);
                    if (total >= 20 && avg >= 4.8) {
                        p.setTier(WorkerProfile.ProviderTier.SUPER_PRO);
                        log.info("ReviewEventConsumer: Worker {} upgraded to SUPER_PRO", workerMobileNumber);
                    } else if (total >= 10 && avg >= 4.5) {
                        p.setTier(WorkerProfile.ProviderTier.PREMIUM);
                    } else {
                        p.setTier(WorkerProfile.ProviderTier.STANDARD);
                    }
                    profileService.createOrUpdateWorkerProfile(p);
                });

            } else if ("WORKER".equals(reviewerRoleStr)) {
                List<Review> reviews = reviewRepository.findBySeekerMobileNumberAndReviewerRole(
                        seekerMobileNumber, Review.ReviewerRole.WORKER);
                if (reviews.isEmpty()) return;
                double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
                int total = reviews.size();

                profileService.getSeekerProfile(seekerMobileNumber).ifPresent(p -> {
                    p.setAverageRating(avg);
                    p.setTotalReviews(total);
                    profileService.createOrUpdateSeekerProfile(p);
                });
            }
        } catch (Exception e) {
            log.error("ReviewEventConsumer: Error processing review.submitted event", e);
            throw new RuntimeException("Failed to process review event", e);
        }
    }
}
