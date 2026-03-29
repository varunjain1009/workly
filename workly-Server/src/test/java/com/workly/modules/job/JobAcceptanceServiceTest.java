package com.workly.modules.job;

import com.workly.core.WorklyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JobAcceptanceServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private com.workly.modules.profile.ProfileService profileService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private JobAcceptanceService jobAcceptanceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        jobAcceptanceService = new JobAcceptanceService(jobRepository, redisTemplate, profileService);
    }

    @Test
    void acceptJob_ShouldSucceedWhenLockAcquiredAndJobAvailable() {
        String jobId = "job123";
        String workerMobile = "456";
        Job job = new Job();
        job.setStatus(JobStatus.BROADCASTED);

        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        Job result = jobAcceptanceService.acceptJob(jobId, workerMobile);

        assertEquals(JobStatus.ASSIGNED, result.getStatus());
        assertEquals(workerMobile, result.getWorkerMobileNumber());
        verify(redisTemplate).delete("lock:job:" + jobId);
    }

    @Test
    void acceptJob_ShouldThrowExceptionWhenLockNotAcquired() {
        String jobId = "job123";
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThrows(WorklyException.class, () -> jobAcceptanceService.acceptJob(jobId, "456"));
    }
}
