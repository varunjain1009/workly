package com.workly.modules.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import com.workly.modules.search.SearchServiceClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SearchServiceClient searchServiceClient;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jobService = new JobService(jobRepository, kafkaTemplate, searchServiceClient);
    }

    @Test
    void createJob_ShouldSetStatusAndOtpAndPublishEvent() {
        Job job = new Job();
        job.setImmediate(true);
        job.setRequiredSkills(List.of("skill1"));
        when(searchServiceClient.normalizeSkills(anyList())).thenReturn(List.of("NormalizedSkill"));

        when(jobRepository.save(any(Job.class))).thenAnswer(i -> {
            Job saved = i.getArgument(0);
            saved.setId("job123");
            return saved;
        });

        Job result = jobService.createJob(job);

        assertNotNull(result.getCompletionOtp());
        assertEquals(JobStatus.BROADCASTED, result.getStatus());
        verify(jobRepository).save(any(Job.class));
        verify(kafkaTemplate).send(eq("job.created"), any(JobEvent.class));
        verify(searchServiceClient).normalizeSkills(anyList());
    }

    @Test
    void updateJobStatus_ShouldUpdateAndPublishEvent() {
        String jobId = "job123";
        Job job = new Job();
        job.setId(jobId);
        job.setStatus(JobStatus.CREATED);

        job.setSeekerMobileNumber("123");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        Job result = jobService.updateJobStatus(jobId, JobStatus.ASSIGNED, "123");

        assertEquals(JobStatus.ASSIGNED, result.getStatus());
        verify(kafkaTemplate).send(eq("job.status.updated"), any(JobEvent.class));
    }

    @Test
    void getSeekerJobs_ShouldReturnJobs() {
        String mobile = "123";
        Job job = new Job();
        when(jobRepository.findBySeekerMobileNumber(mobile)).thenReturn(Collections.singletonList(job));

        List<Job> result = jobService.getSeekerJobs(mobile, null);

        assertEquals(1, result.size());
        verify(jobRepository).findBySeekerMobileNumber(mobile);
    }

    @Test
    void getWorkerJobs_ShouldReturnJobs() {
        String mobile = "123";
        Job job = new Job();
        when(jobRepository.findByWorkerMobileNumber(mobile)).thenReturn(Collections.singletonList(job));

        List<Job> result = jobService.getWorkerJobs(mobile);

        assertEquals(1, result.size());
        verify(jobRepository).findByWorkerMobileNumber(mobile);
    }
}
