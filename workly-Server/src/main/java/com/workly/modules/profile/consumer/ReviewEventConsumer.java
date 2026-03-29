package com.workly.modules.profile.consumer;

import com.workly.modules.profile.ProfileService;
import com.workly.modules.review.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewEventConsumer {

    private final ProfileService profileService;
    private final ReviewService reviewService;

    @KafkaListener(topics = "review.submitted", groupId = "profile-group")
    public void consumeReviewEvent(Map<String, Object> message) {
        try {
            log.info("Profile module received review event: {}", message);

            String reviewerRoleStr = (String) message.get("reviewerRole");
            String seekerMobileNumber = (String) message.get("seekerMobileNumber");
            String workerMobileNumber = (String) message.get("workerMobileNumber");

            if ("SEEKER".equals(reviewerRoleStr)) {
                // Seeker submitted a review. We aggregate the target worker's profile.
                double avg = reviewService.getAverageRating(workerMobileNumber);
                int total = reviewService.getWorkerReviews(workerMobileNumber).size();
                
                profileService.getWorkerProfile(workerMobileNumber).ifPresent(p -> {
                    p.setAverageRating(avg);
                    p.setTotalReviews(total);
                    
                    if (total >= 20 && avg >= 4.8) {
                        p.setTier(com.workly.modules.profile.WorkerProfile.ProviderTier.SUPER_PRO);
                        log.info("Worker {} upgraded to SUPER_PRO tier!", workerMobileNumber);
                    } else if (total >= 10 && avg >= 4.5) {
                        p.setTier(com.workly.modules.profile.WorkerProfile.ProviderTier.PREMIUM);
                    } else {
                        p.setTier(com.workly.modules.profile.WorkerProfile.ProviderTier.STANDARD);
                    }
                    
                    profileService.createOrUpdateWorkerProfile(p);
                    log.info("Updated worker {} with new average: {}", workerMobileNumber, avg);
                });

            } else if ("WORKER".equals(reviewerRoleStr)) {
                // Worker submitted a review. We aggregate the target seeker's profile.
                double avg = reviewService.getSeekerAverageRating(seekerMobileNumber);
                int total = reviewService.getSeekerReviews(seekerMobileNumber).size();
                
                profileService.getSeekerProfile(seekerMobileNumber).ifPresent(p -> {
                    p.setAverageRating(avg);
                    p.setTotalReviews(total);
                    profileService.createOrUpdateSeekerProfile(p);
                    log.info("Updated seeker {} with new average: {}", seekerMobileNumber, avg);
                });
            } else {
                log.warn("Unknown reviewerRole received in event: {}", reviewerRoleStr);
            }

        } catch (Exception e) {
            log.error("Error processing review.submitted event in profile module", e);
        }
    }
}
