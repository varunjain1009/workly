package com.workly.modules.job;

import com.workly.core.WorklyException;
import com.workly.modules.job.outbox.OutboxEvent;
import com.workly.modules.job.outbox.OutboxEventRepository;
import com.workly.modules.search.SearchServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final JobAcceptanceService jobAcceptanceService;
    private final OutboxEventRepository outboxEventRepository;
    private final SearchServiceClient searchServiceClient;

    private static final String JOB_TOPIC = "job.created";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Map<JobStatus, Set<JobStatus>> VALID_TRANSITIONS = Map.of(
            JobStatus.CREATED, Set.of(JobStatus.BROADCASTED, JobStatus.SCHEDULED, JobStatus.CANCELLED),
            JobStatus.SCHEDULED, Set.of(JobStatus.BROADCASTED, JobStatus.CANCELLED),
            JobStatus.BROADCASTED,
            Set.of(JobStatus.ASSIGNED, JobStatus.PENDING_ACCEPTANCE, JobStatus.CANCELLED, JobStatus.EXPIRED),
            JobStatus.PENDING_ACCEPTANCE, Set.of(JobStatus.ASSIGNED, JobStatus.CANCELLED),
            JobStatus.ASSIGNED, Set.of(JobStatus.COMPLETED, JobStatus.CANCELLED));

    @Transactional
    public Job createJob(Job job) {
        if (job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty()) {
            job.setRequiredSkills(searchServiceClient.normalizeSkills(job.getRequiredSkills()));
        }

        job.setStatus(JobStatus.CREATED);
        if (job.isImmediate()) {
            job.setStatus(JobStatus.BROADCASTED);
        } else if (job.getScheduledTime() != null) {
            job.setStatus(JobStatus.SCHEDULED);
        }

        // Generate completion OTP
        job.setCompletionOtp(String.format("%04d", SECURE_RANDOM.nextInt(10000)));

        Job savedJob = jobRepository.save(job);

        // Publish event to Kafka
        JobEvent event = JobEvent.builder()
                .jobId(savedJob.getId())
                .eventType("JOB_CREATED")
                .status(savedJob.getStatus())
                .build();
        saveOutboxEvent(JOB_TOPIC, event);

        return savedJob;
    }

    @Transactional
    public Job updateJobStatus(String jobId, JobStatus status, String requestingUserMobile) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> WorklyException.notFound("Job not found"));

        if (!job.getSeekerMobileNumber().equals(requestingUserMobile)) {
            throw WorklyException.forbidden("You are not authorized to update this job");
        }

        Set<JobStatus> allowedStatuses = VALID_TRANSITIONS.get(job.getStatus());
        if (allowedStatuses == null || !allowedStatuses.contains(status)) {
            throw WorklyException.badRequest(
                    String.format("Cannot transition from %s to %s", job.getStatus(), status));
        }

        job.setStatus(status);
        Job savedJob = jobRepository.save(job);

        JobEvent event = JobEvent.builder()
                .jobId(savedJob.getId())
                .eventType("JOB_STATUS_UPDATED")
                .status(savedJob.getStatus())
                .workerId(savedJob.getWorkerMobileNumber())
                .build();
        saveOutboxEvent("job.status.updated", event);

        return savedJob;
    }

    @Transactional
    public Job updateJob(String jobId, Job jobDetails, String requestingUserMobile) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> WorklyException.notFound("Job not found"));

        if (!job.getSeekerMobileNumber().equals(requestingUserMobile)) {
            throw WorklyException.forbidden("You are not authorized to update this job");
        }

        if (jobDetails.getScheduledTime() != null) {
            job.setScheduledTime(jobDetails.getScheduledTime());
            job.setStatus(JobStatus.SCHEDULED); // Ensure status is SCHEDULED if time is updated
        }

        Job savedJob = jobRepository.save(job);

        JobEvent event = JobEvent.builder()
                .jobId(savedJob.getId())
                .eventType("JOB_UPDATED")
                .status(savedJob.getStatus())
                .build();

        saveOutboxEvent(JOB_TOPIC, event);

        return savedJob;
    }

    private void saveOutboxEvent(String topic, Object payload) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setTopic(topic);
        outboxEvent.setPayload(payload);
        outboxEventRepository.save(outboxEvent);
    }

    public List<Job> getSeekerJobs(String mobileNumber, String type) {
        log.debug("Fetching seeker jobs from database for mobile: {}, type: {}", mobileNumber, type);
        List<Job> jobs;

        if ("active".equalsIgnoreCase(type)) {
            List<JobStatus> activeStatuses = List.of(
                    JobStatus.CREATED,
                    JobStatus.BROADCASTED,
                    JobStatus.SCHEDULED,
                    JobStatus.PENDING_ACCEPTANCE,
                    JobStatus.ASSIGNED);
            jobs = jobRepository.findBySeekerMobileNumberAndStatusIn(mobileNumber, activeStatuses);
        } else if ("past".equalsIgnoreCase(type)) {
            List<JobStatus> pastStatuses = List.of(
                    JobStatus.COMPLETED,
                    JobStatus.CANCELLED,
                    JobStatus.EXPIRED);
            jobs = jobRepository.findBySeekerMobileNumberAndStatusIn(mobileNumber, pastStatuses);
        } else {
            jobs = jobRepository.findBySeekerMobileNumber(mobileNumber);
        }

        if (jobs.isEmpty()) {
            log.info("No {} jobs found in database for seeker mobile: {}", type != null ? type : "all", mobileNumber);
        } else {
            log.info("Found {} {} jobs in database for seeker mobile: {}", jobs.size(), type != null ? type : "all",
                    mobileNumber);
        }
        return jobs;
    }

    public List<Job> getWorkerJobs(String mobileNumber) {
        log.debug("Fetching worker jobs from database for mobile: {}", mobileNumber);
        List<Job> jobs = jobRepository.findByWorkerMobileNumber(mobileNumber);
        if (jobs.isEmpty()) {
            log.info("No jobs found in database for worker mobile: {}", mobileNumber);
        } else {
            log.info("Found {} jobs in database for worker mobile: {}", jobs.size(), mobileNumber);
        }
        return jobs;
    }

    public List<Job> getMatchingJobs(String workerMobile) {
        // For now, return all BROADCASTED and SCHEDULED jobs.
        // Todo: Add skill and location filtering.
        return jobRepository.findByStatusIn(List.of(JobStatus.BROADCASTED, JobStatus.SCHEDULED));
    }

    @Transactional
    public void acceptJob(String jobId, String workerMobile) {
        Job job = jobAcceptanceService.acceptJob(jobId, workerMobile);

        JobEvent event = JobEvent.builder()
                .jobId(job.getId())
                .eventType("JOB_ACCEPTED")
                .status(job.getStatus())
                .workerId(workerMobile)
                .build();
        saveOutboxEvent(JOB_TOPIC, event);
    }

    @Transactional
    public Job completeJob(String jobId, String otp, String requestingUserMobile) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> WorklyException.notFound("Job not found"));

        if (job.getStatus() == JobStatus.COMPLETED) {
            throw WorklyException.badRequest("Job is already completed");
        }
        if (job.getStatus() != JobStatus.ASSIGNED) {
            throw WorklyException.badRequest("Job must be in ASSIGNED status to complete");
        }

        // Verify Worker
        if (job.getWorkerMobileNumber() == null || !job.getWorkerMobileNumber().equals(requestingUserMobile)) {
            throw WorklyException.forbidden("You are not the assigned worker for this job");
        }

        if (job.getCompletionOtp() == null || !job.getCompletionOtp().equals(otp)) {
            throw WorklyException.badRequest("Invalid OTP");
        }

        job.setStatus(JobStatus.COMPLETED);
        jobRepository.save(job);

        JobEvent event = JobEvent.builder()
                .jobId(job.getId())
                .eventType("JOB_COMPLETED")
                .status(job.getStatus())
                .workerId(requestingUserMobile)
                .build();
        saveOutboxEvent(JOB_TOPIC, event);
        return job; // Added return statement
    }

    public Job getJobById(String jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> WorklyException.notFound("Job not found"));
    }
}
