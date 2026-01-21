package com.workly.modules.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private com.workly.modules.auth.JwtUtils jwtUtils;

    @MockitoBean
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @MockitoBean
    private com.workly.modules.profile.WorkerProfileRepository workerProfileRepository;

    @MockitoBean
    private com.workly.modules.profile.SkillSeekerProfileRepository skillSeekerProfileRepository;

    @MockitoBean
    private JobRepository jobRepository;

    @MockitoBean
    private com.workly.modules.review.ReviewRepository reviewRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "1234567890")
    void createJob_ShouldReturnCreatedJob() throws Exception {
        Job job = new Job();
        job.setTitle("Test Job");

        when(jobService.createJob(any(Job.class))).thenReturn(job);

        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(job)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Job"));
    }

    @Test
    @WithMockUser(username = "1234567890")
    void getSeekerJobs_ShouldReturnJobs() throws Exception {
        Job job = new Job();
        job.setTitle("Seeker Job");

        when(jobService.getSeekerJobs(eq("1234567890"), any())).thenReturn(Collections.singletonList(job));

        mockMvc.perform(get("/api/v1/jobs/seeker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Seeker Job"));
    }

    @Test
    @WithMockUser(username = "1234567890")
    void getWorkerJobs_ShouldReturnJobs() throws Exception {
        Job job = new Job();
        job.setTitle("Worker Job");

        when(jobService.getWorkerJobs("1234567890")).thenReturn(Collections.singletonList(job));

        mockMvc.perform(get("/api/v1/jobs/worker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Worker Job"));
    }

    @Test
    @WithMockUser(username = "1234567890")
    void updateStatus_ShouldReturnUpdatedJob() throws Exception {
        Job job = new Job();
        job.setId("job123");
        job.setStatus(JobStatus.ASSIGNED);

        when(jobService.updateJobStatus(eq("job123"), eq(JobStatus.ASSIGNED), eq("1234567890"))).thenReturn(job);

        mockMvc.perform(patch("/api/v1/jobs/job123/status")
                .param("status", "ASSIGNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));
    }
}
