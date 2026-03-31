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
        log.debug("NotificationService: [ENTER] handleJobCreated - Kafka event consumed for jobId: {}", event.getJobId());
        log.info("Received job.created event for jobId: {}", event.getJobId());

        com.workly.modules.job.Job job = jobRepository.findById(event.getJobId()).orElse(null);
        if (job == null) {
            log.debug("NotificationService: [FAIL] Job entity {} not found in MongoDB", event.getJobId());
            log.error("Job not found for notification: {}", event.getJobId());
            return;
        }
        log.debug("NotificationService: Loaded job {} - type: immediate={}, location: {}", job.getId(), job.isImmediate(), job.getLocation());

        // Find matching workers
        // Job location is double[] {lon, lat}
        if (job.getLocation() == null || job.getLocation().length < 2) {
            log.error("Job {} has no valid location", job.getId());
            return;
        }

        Long scheduledTimeMillis = null;
        if (!job.isImmediate() && job.getScheduledTime() != null) {
            scheduledTimeMillis = job.getScheduledTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        java.util.List<com.workly.modules.profile.WorkerProfile> matchingWorkers = matchingService.findMatches(
                job.getRequiredSkills(), // List<String>
                job.getLocation()[0], // Longitude
                job.getLocation()[1], // Latitude
                job.getSearchRadiusKm(), // int
                scheduledTimeMillis);

        // Logic Refinement based on Job Type
        if (job.isImmediate()) {
            if (matchingWorkers.isEmpty()) {
                log.info("No matching workers found for IMMEDIATE job {}. No notifications sent.", job.getId());
                return;
            }
        } else if (job.getScheduledTime() != null) {
            // Workers are already filtered by the DB query now via findMatchingWorkersAvailableAt
            if (matchingWorkers.isEmpty()) {
                log.info("No available workers found for SCHEDULED job {} at {}. No notifications sent.", job.getId(),
                        job.getScheduledTime());
                return;
            }
        }

        // In a real scenario, this would call FCM Admin SDK for the filtered list
        log.debug("NotificationService: Dispatching FCM push to {} matched workers", matchingWorkers.size());
        for (com.workly.modules.profile.WorkerProfile worker : matchingWorkers) {
            sendPushNotification(worker.getDeviceToken(), "New Job Available",
                    "A job matching your skills is available.");
        }
        log.debug("NotificationService: [EXIT] handleJobCreated - All push notifications dispatched");
    }

    // Removed isWorkerAvailable as logic is now pushed to MongoDB aggregation

    /**
     * Called when a worker flips their availability to true.
     * Scans all BROADCASTED jobs and notifies the worker about any that match
     * their skills and fall within the job's search radius.
     */
    public void notifyNewlyAvailableWorker(com.workly.modules.profile.WorkerProfile worker) {
        if (worker.getLastLocation() == null || worker.getLastLocation().length < 2) {
            log.warn("NotificationService: Worker {} has no location — skipping re-notification", worker.getMobileNumber());
            return;
        }
        if (worker.getSkills() == null || worker.getSkills().isEmpty()) {
            log.debug("NotificationService: Worker {} has no skills — skipping re-notification", worker.getMobileNumber());
            return;
        }

        double workerLon = worker.getLastLocation()[0];
        double workerLat = worker.getLastLocation()[1];

        java.util.List<com.workly.modules.job.Job> openJobs =
                jobRepository.findByStatus(com.workly.modules.job.JobStatus.BROADCASTED);

        log.debug("NotificationService: re-notification scan — {} BROADCASTED jobs, worker skills: {}",
                openJobs.size(), worker.getSkills());

        int notified = 0;
        for (com.workly.modules.job.Job job : openJobs) {
            if (job.getLocation() == null || job.getLocation().length < 2) continue;
            if (job.getRequiredSkills() == null || job.getRequiredSkills().isEmpty()) continue;

            // Check skill overlap
            boolean skillMatch = job.getRequiredSkills().stream()
                    .anyMatch(s -> worker.getSkills().contains(s));
            if (!skillMatch) continue;

            // Check distance
            double distanceKm = haversineKm(workerLat, workerLon,
                    job.getLocation()[1], job.getLocation()[0]);
            if (distanceKm <= job.getSearchRadiusKm()) {
                log.debug("NotificationService: Job {} matches worker {} (distance={}km, radius={}km)",
                        job.getId(), worker.getMobileNumber(), String.format("%.1f", distanceKm), job.getSearchRadiusKm());
                sendPushNotification(worker.getDeviceToken(), "New Job Available",
                        "A job matching your skills is available: " + job.getTitle());
                notified++;
            }
        }
        log.info("NotificationService: Notified worker {} about {} matching open jobs", worker.getMobileNumber(), notified);
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @KafkaListener(topics = "job.status.updated", groupId = "notification-group")
    public void handleJobStatusUpdated(JobEvent event) {
        log.debug("NotificationService: [ENTER] handleJobStatusUpdated - jobId: {}, newStatus: {}", event.getJobId(), event.getStatus());
        log.info("Received job.status.updated event for jobId: {} with status: {}", event.getJobId(),
                event.getStatus());
        log.debug("NotificationService: [EXIT] handleJobStatusUpdated - Status change notification pending implementation");
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
