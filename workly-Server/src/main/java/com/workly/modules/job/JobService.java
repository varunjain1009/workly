package com.workly.modules.job;

import com.workly.core.WorklyException;
import com.workly.modules.search.SearchServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SearchServiceClient searchServiceClient;

    private static final String JOB_TOPIC = "job.created";

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
        job.setCompletionOtp(String.format("%04d", new Random().nextInt(10000)));

        Job savedJob = jobRepository.save(job);

        // Publish event to Kafka
        JobEvent event = JobEvent.builder()
                .jobId(savedJob.getId())
                .eventType("JOB_CREATED")
                .status(savedJob.getStatus())
                .build();
        kafkaTemplate.send(JOB_TOPIC, event);

        return savedJob;
    }

    public Job updateJobStatus(String jobId, JobStatus status, String requestingUserMobile) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> WorklyException.notFound("Job not found"));

        if (!job.getSeekerMobileNumber().equals(requestingUserMobile)) {
            throw WorklyException.forbidden("You are not authorized to update this job");
        }

        job.setStatus(status);
        Job savedJob = jobRepository.save(job);

        JobEvent event = JobEvent.builder()
                .jobId(savedJob.getId())
                .eventType("JOB_STATUS_UPDATED")
                .status(savedJob.getStatus())
                .build();
        kafkaTemplate.send("job.status.updated", event);

        return savedJob;
    }

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
        kafkaTemplate.send(JOB_TOPIC, event);

        return savedJob;
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

    public void acceptJob(String jobId, String workerMobile) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> WorklyException.notFound("Job not found"));

        if (job.getStatus() != JobStatus.BROADCASTED && job.getStatus() != JobStatus.SCHEDULED) {
            throw WorklyException.badRequest("Job is not available for acceptance");
        }

        // Check if manual selection
        if (job.getAssignmentMode() == com.workly.modules.job.AssignmentMode.MANUAL_SELECT) {
            // Apply for job (PENDING_ACCEPTANCE) - Logic to be added for "Applications"
            // For MVP simplicty, let's treat everything as FIRST_ACCEPT or just ASSIGNED
            // But if MANUAL, we should probably add to a list of applicants?
            // For now, let's handle FIRST_ACCEPT behavior (ASSIGNED)
            // Or if MANUAL, set to PENDING_ACCEPTANCE and set workerId?
            // Use ASSIGNED for now for simplicity of flow.
            job.setStatus(JobStatus.PENDING_ACCEPTANCE);
        } else {
            job.setStatus(JobStatus.ASSIGNED);
        }

        job.setWorkerMobileNumber(workerMobile);

        jobRepository.save(job);

        JobEvent event = JobEvent.builder()
                .jobId(job.getId())
                .eventType("JOB_ACCEPTED")
                .status(job.getStatus())
                .workerId(workerMobile)
                .build();
        kafkaTemplate.send(JOB_TOPIC, event);
    }

    public void completeJob(String jobId, String otp, String workerMobile) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> WorklyException.notFound("Job not found"));

        if (!job.getStatus().equals(JobStatus.ASSIGNED)) {
            if (job.getStatus().equals(JobStatus.COMPLETED)) {
                throw WorklyException.badRequest("Job is already completed");
            }
        }

        // Verify Worker
        if (job.getWorkerMobileNumber() == null || !job.getWorkerMobileNumber().equals(workerMobile)) {
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
                .workerId(workerMobile)
                .build();
        kafkaTemplate.send(JOB_TOPIC, event);
    }

    public Job getJobById(String jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> WorklyException.notFound("Job not found"));
    }
}
