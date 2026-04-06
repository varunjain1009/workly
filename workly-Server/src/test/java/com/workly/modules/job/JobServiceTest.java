package com.workly.modules.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.workly.modules.job.outbox.OutboxEvent;
import com.workly.modules.job.outbox.OutboxEventRepository;
import com.workly.modules.search.SearchServiceClient;

import com.workly.modules.profile.ProfileService;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private SearchServiceClient searchServiceClient;

    @Mock
    private JobAcceptanceService jobAcceptanceService;

    @Mock
    private ProfileService profileService;

    @Mock
    private MongoTemplate secondaryMongoTemplate;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobService(jobRepository, jobAcceptanceService, outboxEventRepository, searchServiceClient, profileService, secondaryMongoTemplate);
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
        verify(outboxEventRepository).save(any(OutboxEvent.class));
        verify(searchServiceClient).normalizeSkills(anyList());
    }

    @Test
    void updateJobStatus_ShouldUpdateAndPublishEvent() {
        String jobId = "job123";
        Job job = new Job();
        job.setId(jobId);
        job.setStatus(JobStatus.BROADCASTED);

        job.setSeekerMobileNumber("123");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        Job result = jobService.updateJobStatus(jobId, JobStatus.ASSIGNED, "123");

        assertEquals(JobStatus.ASSIGNED, result.getStatus());
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void getSeekerJobs_ShouldReturnJobs() {
        String mobile = "123";
        Job job = new Job();
        when(secondaryMongoTemplate.find(any(), eq(Job.class))).thenReturn(Collections.singletonList(job));

        List<Job> result = jobService.getSeekerJobs(mobile, null);

        assertEquals(1, result.size());
        verify(secondaryMongoTemplate).find(any(), eq(Job.class));
    }

    @Test
    void getWorkerJobs_ShouldReturnJobs() {
        String mobile = "123";
        Job job = new Job();
        when(secondaryMongoTemplate.find(any(), eq(Job.class)))
                .thenReturn(Collections.singletonList(job));

        List<Job> result = jobService.getWorkerJobs(mobile);

        assertEquals(1, result.size());
        verify(secondaryMongoTemplate).find(any(), eq(Job.class));
    }

    @Test
    void createJob_scheduledJob_setsScheduledStatus() {
        Job job = new Job();
        job.setImmediate(false);
        job.setScheduledTime(java.time.LocalDateTime.now().plusDays(1));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> {
            Job saved = i.getArgument(0); saved.setId("j1"); return saved;
        });

        Job result = jobService.createJob(job);

        assertEquals(JobStatus.SCHEDULED, result.getStatus());
    }

    @Test
    void createJob_noSkills_skipsNormalization() {
        Job job = new Job();
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> {
            Job saved = i.getArgument(0); saved.setId("j1"); return saved;
        });

        jobService.createJob(job);

        verify(searchServiceClient, never()).normalizeSkills(any());
    }

    @Test
    void updateJobStatus_notFound_throws() {
        when(jobRepository.findById("j1")).thenReturn(Optional.empty());
        assertThrows(com.workly.core.WorklyException.class,
                () -> jobService.updateJobStatus("j1", JobStatus.CANCELLED, "s1"));
    }

    @Test
    void updateJobStatus_forbidden_throws() {
        Job job = new Job(); job.setId("j1"); job.setStatus(JobStatus.BROADCASTED);
        job.setSeekerMobileNumber("s1");
        when(jobRepository.findById("j1")).thenReturn(Optional.of(job));
        assertThrows(com.workly.core.WorklyException.class,
                () -> jobService.updateJobStatus("j1", JobStatus.ASSIGNED, "other"));
    }

    @Test
    void updateJobStatus_invalidTransition_throws() {
        Job job = new Job(); job.setId("j1"); job.setStatus(JobStatus.COMPLETED);
        job.setSeekerMobileNumber("s1");
        when(jobRepository.findById("j1")).thenReturn(Optional.of(job));
        assertThrows(com.workly.core.WorklyException.class,
                () -> jobService.updateJobStatus("j1", JobStatus.BROADCASTED, "s1"));
    }

    @Test
    void completeJob_success() {
        Job job = new Job(); job.setId("j1"); job.setStatus(JobStatus.ASSIGNED);
        job.setWorkerMobileNumber("w1"); job.setCompletionOtp("1234");
        when(jobRepository.findById("j1")).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenReturn(job);

        Job result = jobService.completeJob("j1", "1234", "w1");

        assertEquals(JobStatus.COMPLETED, result.getStatus());
        verify(outboxEventRepository).save(any());
    }

    @Test
    void completeJob_alreadyCompleted_throws() {
        Job job = new Job(); job.setId("j1"); job.setStatus(JobStatus.COMPLETED);
        when(jobRepository.findById("j1")).thenReturn(Optional.of(job));
        assertThrows(com.workly.core.WorklyException.class,
                () -> jobService.completeJob("j1", "1234", "w1"));
    }

    @Test
    void completeJob_wrongStatus_throws() {
        Job job = new Job(); job.setId("j1"); job.setStatus(JobStatus.BROADCASTED);
        when(jobRepository.findById("j1")).thenReturn(Optional.of(job));
        assertThrows(com.workly.core.WorklyException.class,
                () -> jobService.completeJob("j1", "1234", "w1"));
    }

    @Test
    void completeJob_wrongWorker_throws() {
        Job job = new Job(); job.setId("j1"); job.setStatus(JobStatus.ASSIGNED);
        job.setWorkerMobileNumber("w1"); job.setCompletionOtp("1234");
        when(jobRepository.findById("j1")).thenReturn(Optional.of(job));
        assertThrows(com.workly.core.WorklyException.class,
                () -> jobService.completeJob("j1", "1234", "other"));
    }

    @Test
    void completeJob_invalidOtp_throws() {
        Job job = new Job(); job.setId("j1"); job.setStatus(JobStatus.ASSIGNED);
        job.setWorkerMobileNumber("w1"); job.setCompletionOtp("1234");
        when(jobRepository.findById("j1")).thenReturn(Optional.of(job));
        assertThrows(com.workly.core.WorklyException.class,
                () -> jobService.completeJob("j1", "9999", "w1"));
    }

    @Test
    void getJobById_found_returnsJob() {
        Job job = new Job(); job.setId("j1");
        when(jobRepository.findById("j1")).thenReturn(Optional.of(job));
        assertEquals(job, jobService.getJobById("j1"));
    }

    @Test
    void getJobById_notFound_throws() {
        when(jobRepository.findById("j1")).thenReturn(Optional.empty());
        assertThrows(com.workly.core.WorklyException.class, () -> jobService.getJobById("j1"));
    }

    @Test
    void getMatchingJobs_noWorkerProfile_returnsEmpty() {
        when(profileService.getWorkerProfile("w1")).thenReturn(Optional.empty());
        assertTrue(jobService.getMatchingJobs("w1").isEmpty());
    }

    @Test
    void getMatchingJobs_noLocation_returnsEmpty() {
        com.workly.modules.profile.WorkerProfile wp = new com.workly.modules.profile.WorkerProfile();
        wp.setLastLocation(null);
        when(profileService.getWorkerProfile("w1")).thenReturn(Optional.of(wp));
        assertTrue(jobService.getMatchingJobs("w1").isEmpty());
    }

    @Test
    void getMatchingJobs_withSkills_returnsJobs() {
        com.workly.modules.profile.WorkerProfile wp = new com.workly.modules.profile.WorkerProfile();
        wp.setLastLocation(new double[]{77.6, 12.9});
        wp.setSkills(List.of("electrician"));
        when(profileService.getWorkerProfile("w1")).thenReturn(Optional.of(wp));
        when(jobRepository.findMatchingJobs(anyDouble(), anyDouble(), anyList(), anyList(), anyDouble(), any()))
                .thenReturn(List.of(new Job()));

        List<Job> result = jobService.getMatchingJobs("w1");
        assertEquals(1, result.size());
    }

    @Test
    void getSeekerJobs_activeType_filteredByStatus() {
        when(secondaryMongoTemplate.find(any(), eq(Job.class))).thenReturn(List.of(new Job()));
        List<Job> result = jobService.getSeekerJobs("s1", "active");
        assertEquals(1, result.size());
    }

    @Test
    void getSeekerJobs_pastType_filteredByStatus() {
        when(secondaryMongoTemplate.find(any(), eq(Job.class))).thenReturn(List.of());
        List<Job> result = jobService.getSeekerJobs("s1", "past");
        assertTrue(result.isEmpty());
    }

    @Test
    void acceptJob_delegatesToAcceptanceService() {
        Job job = new Job(); job.setId("j1"); job.setStatus(JobStatus.ASSIGNED);
        when(jobAcceptanceService.acceptJob("j1", "w1")).thenReturn(job);

        jobService.acceptJob("j1", "w1");

        verify(jobAcceptanceService).acceptJob("j1", "w1");
        verify(outboxEventRepository).save(any());
    }
}
