package com.workly.modules.job;

import com.workly.core.WorklyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class JobAcceptanceService {

    private final JobRepository jobRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String JOB_LOCK_PREFIX = "lock:job:";

    public Job acceptJob(String jobId, String workerMobileNumber) {
        String lockKey = JOB_LOCK_PREFIX + jobId;

        // Try to acquire lock
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", Duration.ofSeconds(10));

        if (Boolean.FALSE.equals(acquired)) {
            throw WorklyException.badRequest("Job is currently being processed by another worker");
        }

        try {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> WorklyException.notFound("Job not found"));

            if (job.getStatus() != JobStatus.BROADCASTED && job.getStatus() != JobStatus.SCHEDULED) {
                throw WorklyException.badRequest("Job is no longer available for acceptance");
            }

            job.setWorkerMobileNumber(workerMobileNumber);
            job.setStatus(JobStatus.ASSIGNED);

            return jobRepository.save(job);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}
