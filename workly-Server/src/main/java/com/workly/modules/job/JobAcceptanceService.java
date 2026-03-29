package com.workly.modules.job;

import com.workly.core.WorklyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobAcceptanceService {

    private final JobRepository jobRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String JOB_LOCK_PREFIX = "lock:job:";

    public Job acceptJob(String jobId, String workerMobileNumber) {
        log.debug("JobAcceptanceService: [ENTER] acceptJob - jobId: {}, worker: {}", jobId, workerMobileNumber);
        String lockKey = JOB_LOCK_PREFIX + jobId;

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", Duration.ofSeconds(10));
        log.debug("JobAcceptanceService: Redis distributed lock acquired: {}", acquired);

        if (Boolean.FALSE.equals(acquired)) {
            log.debug("JobAcceptanceService: [FAIL] Lock contention - another worker is processing job {}", jobId);
            throw WorklyException.badRequest("Job is currently being processed by another worker");
        }

        try {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> WorklyException.notFound("Job not found"));
            log.debug("JobAcceptanceService: Loaded job {} with current status: {}", jobId, job.getStatus());

            if (job.getStatus() != JobStatus.BROADCASTED && job.getStatus() != JobStatus.SCHEDULED) {
                log.debug("JobAcceptanceService: [FAIL] Job {} status {} is not acceptable", jobId, job.getStatus());
                throw WorklyException.badRequest("Job is no longer available for acceptance");
            }

            job.setWorkerMobileNumber(workerMobileNumber);
            job.setStatus(JobStatus.ASSIGNED);

            Job saved = jobRepository.save(job);
            log.debug("JobAcceptanceService: [EXIT] acceptJob - Job {} assigned to worker {}", jobId, workerMobileNumber);
            return saved;
        } finally {
            redisTemplate.delete(lockKey);
            log.debug("JobAcceptanceService: Redis lock released for key: {}", lockKey);
        }
    }
}
