package com.workly.notification.service;

import com.workly.notification.domain.job.Job;
import com.workly.notification.domain.job.JobEvent;
import com.workly.notification.domain.job.JobRepository;
import com.workly.notification.domain.job.JobStatus;
import com.workly.notification.domain.worker.WorkerProfile;
import com.workly.notification.domain.worker.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobNotificationService {

    private final JobRepository jobRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final FCMService fcmService;
    private final UserTokenService userTokenService;

    @KafkaListener(topics = "job.created", groupId = "notification-group")
    public void handleJobCreated(JobEvent event) {
        log.debug("JobNotificationService: [ENTER] handleJobCreated - jobId: {}", event.getJobId());

        Job job = jobRepository.findById(event.getJobId()).orElse(null);
        if (job == null) {
            log.error("JobNotificationService: Job {} not found", event.getJobId());
            return;
        }

        if (job.getLocation() == null || job.getLocation().length < 2) {
            log.error("JobNotificationService: Job {} has no valid location", job.getId());
            return;
        }

        double maxDistanceMeters = job.getSearchRadiusKm() * 1000.0;
        Long scheduledTimeMillis = null;
        if (!job.isImmediate() && job.getScheduledTime() != null) {
            scheduledTimeMillis = job.getScheduledTime()
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        List<WorkerProfile> matchingWorkers = scheduledTimeMillis != null
                ? workerProfileRepository.findMatchingWorkersAvailableAt(
                        job.getRequiredSkills(), job.getLocation()[0], job.getLocation()[1],
                        maxDistanceMeters, scheduledTimeMillis)
                : workerProfileRepository.findMatchingWorkers(
                        job.getRequiredSkills(), job.getLocation()[0], job.getLocation()[1],
                        maxDistanceMeters);

        if (matchingWorkers.isEmpty()) {
            log.info("JobNotificationService: No matching workers for job {}", job.getId());
            return;
        }

        List<String> tokens = matchingWorkers.stream()
                .map(WorkerProfile::getDeviceToken)
                .filter(t -> t != null && !t.isEmpty())
                .toList();

        if (!tokens.isEmpty()) {
            fcmService.sendBatch(tokens, "New Job Available",
                    "A job matching your skills is available.", null);
        }
        log.debug("JobNotificationService: [EXIT] handleJobCreated - Dispatched to {} workers", tokens.size());
    }

    @KafkaListener(topics = "job.status.updated", groupId = "notification-group")
    public void handleJobStatusUpdated(JobEvent event) {
        log.debug("JobNotificationService: [ENTER] handleJobStatusUpdated - jobId: {}, status: {}",
                event.getJobId(), event.getStatus());

        Job job = jobRepository.findById(event.getJobId()).orElse(null);
        if (job == null) {
            log.error("JobNotificationService: Job {} not found for status-update", event.getJobId());
            return;
        }

        if (event.getStatus() == JobStatus.ASSIGNED) {
            String seekerToken = userTokenService.getToken(job.getSeekerMobileNumber());
            if (seekerToken != null) {
                java.util.Map<String, String> data = new java.util.HashMap<>();
                data.put("jobId", job.getId());
                data.put("type", "JOB_ACCEPTED");
                fcmService.sendNotificationWithData(seekerToken, "Job Accepted",
                        "A provider has accepted your job: " + job.getTitle(), data);
            }

            String workerToken = userTokenService.getToken(job.getWorkerMobileNumber());
            if (workerToken != null) {
                java.util.Map<String, String> workerData = new java.util.HashMap<>();
                workerData.put("jobId", job.getId());
                workerData.put("type", "JOB_ASSIGNED");
                fcmService.sendNotificationWithData(workerToken, "Job Confirmed",
                        "You have been assigned to: " + job.getTitle(), workerData);
            }

        } else if (event.getStatus() == JobStatus.COMPLETED) {
            String seekerToken = userTokenService.getToken(job.getSeekerMobileNumber());
            if (seekerToken != null) {
                java.util.Map<String, String> data = new java.util.HashMap<>();
                data.put("jobId", job.getId());
                data.put("type", "JOB_COMPLETED");
                fcmService.sendNotificationWithData(seekerToken, "Job Completed",
                        "Your job has been completed: " + job.getTitle(), data);
            }
        }

        log.debug("JobNotificationService: [EXIT] handleJobStatusUpdated");
    }
}
