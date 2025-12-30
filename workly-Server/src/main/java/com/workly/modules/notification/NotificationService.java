package com.workly.modules.notification;

import com.workly.modules.job.JobEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    // private final FcmTokenRepository fcmTokenRepository;
    private final com.workly.modules.job.JobRepository jobRepository;
    private final com.workly.modules.matching.MatchingService matchingService;

    @KafkaListener(topics = "job.created", groupId = "notification-group")
    public void handleJobCreated(JobEvent event) {
        log.info("Received job.created event for jobId: {}", event.getJobId());

        com.workly.modules.job.Job job = jobRepository.findById(event.getJobId()).orElse(null);
        if (job == null) {
            log.error("Job not found for notification: {}", event.getJobId());
            return;
        }

        // Find matching workers
        // Job location is double[] {lon, lat}
        if (job.getLocation() == null || job.getLocation().length < 2) {
            log.error("Job {} has no valid location", job.getId());
            return;
        }

        java.util.List<com.workly.modules.profile.WorkerProfile> matchingWorkers = matchingService.findMatches(
                job.getRequiredSkills(), // List<String>
                job.getLocation()[0], // Longitude
                job.getLocation()[1], // Latitude
                job.getSearchRadiusKm()); // int

        // Logic Refinement based on Job Type
        if (job.isImmediate()) {
            if (matchingWorkers.isEmpty()) {
                log.info("No matching workers found for IMMEDIATE job {}. No notifications sent.", job.getId());
                return;
            }
        } else if (job.getScheduledTime() != null) {
            // Filter out workers who are unavailable at the scheduled time
            long jobTime = job.getScheduledTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

            matchingWorkers = matchingWorkers.stream()
                    .filter(worker -> isWorkerAvailable(worker, jobTime))
                    .collect(java.util.stream.Collectors.toList());

            if (matchingWorkers.isEmpty()) {
                log.info("No available workers found for SCHEDULED job {} at {}. No notifications sent.", job.getId(),
                        job.getScheduledTime());
                return;
            }
        }

        // In a real scenario, this would call FCM Admin SDK for the filtered list
        for (com.workly.modules.profile.WorkerProfile worker : matchingWorkers) {
            sendPushNotification(worker.getDeviceToken(), "New Job Available",
                    "A job matching your skills is available.");
        }
    }

    private boolean isWorkerAvailable(com.workly.modules.profile.WorkerProfile worker, long jobTime) {
        if (worker.getUnavailableSlots() == null || worker.getUnavailableSlots().isEmpty()) {
            return true;
        }
        for (com.workly.modules.profile.WorkerProfile.UnavailableSlot slot : worker.getUnavailableSlots()) {
            if (jobTime >= slot.getStartTime() && jobTime <= slot.getEndTime()) {
                return false;
            }
        }
        return true;
    }

    @KafkaListener(topics = "job.status.updated", groupId = "notification-group")
    public void handleJobStatusUpdated(JobEvent event) {
        log.info("Received job.status.updated event for jobId: {} with status: {}", event.getJobId(),
                event.getStatus());
        // Notify seeker or worker based on status change
    }

    private void sendPushNotification(String topicOrToken, String title, String body) {
        if (topicOrToken == null || topicOrToken.isEmpty()) {
            log.warn("Cannot send notification: Token is null or empty");
            return;
        }
        log.info("Sending FCM Push Notification to {}: {} - {}", topicOrToken, title, body);

        // Use FirebaseMessaging.getInstance() here.
        // For MVP, since we don't have the google-services.json and private key setup
        // on server,
        // we will just LOG it.
        // In a real implementation:
        /*
         * Message message = Message.builder()
         * .putData("title", title)
         * .putData("body", body)
         * .setToken(topicOrToken)
         * .build();
         * try {
         * String response = FirebaseMessaging.getInstance().send(message);
         * log.info("Successfully sent message: " + response);
         * } catch (FirebaseMessagingException e) {
         * log.error("Error sending message", e);
         * }
         */

        // Simulating 3rd party call
        log.info("[MOCK FCM] Sent to token: [{}], Title: [{}], Body: [{}]", topicOrToken, title, body);
    }
}
