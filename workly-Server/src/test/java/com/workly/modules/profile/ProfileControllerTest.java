package com.workly.modules.profile;

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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private com.workly.modules.auth.JwtUtils jwtUtils;

    @MockitoBean
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @MockitoBean
    private WorkerProfileRepository workerProfileRepository;

    @MockitoBean
    private SkillSeekerProfileRepository skillSeekerProfileRepository;

    @MockitoBean
    private com.workly.modules.job.JobRepository jobRepository;

    @MockitoBean
    private com.workly.modules.review.ReviewRepository reviewRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "1234567890")
    void getWorkerProfile_ShouldReturnProfile() throws Exception {
        WorkerProfile profile = new WorkerProfile();
        profile.setMobileNumber("1234567890");

        when(profileService.getWorkerProfile("1234567890")).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/v1/profiles/worker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mobileNumber").value("1234567890"));
    }

    @Test
    @WithMockUser(username = "1234567890")
    void createOrUpdateWorkerProfile_ShouldReturnUpdatedProfile() throws Exception {
        WorkerProfile profile = new WorkerProfile();
        profile.setName("John");

        when(profileService.createOrUpdateWorkerProfile(any())).thenReturn(profile);

        mockMvc.perform(post("/api/v1/profiles/worker")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("John"));
    }

    @Test
    @WithMockUser(username = "1234567890")
    void getSeekerProfile_ShouldReturnProfile() throws Exception {
        SkillSeekerProfile profile = new SkillSeekerProfile();
        profile.setMobileNumber("1234567890");

        when(profileService.getSeekerProfile("1234567890")).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/v1/profiles/seeker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mobileNumber").value("1234567890"));
    }

    @Test
    @WithMockUser(username = "1234567890")
    void createOrUpdateSeekerProfile_ShouldReturnUpdatedProfile() throws Exception {
        SkillSeekerProfile profile = new SkillSeekerProfile();
        profile.setName("John Doe");

        when(profileService.createOrUpdateSeekerProfile(any())).thenReturn(profile);

        mockMvc.perform(post("/api/v1/profiles/seeker")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("John Doe"));
    }

    @Test
    @WithMockUser(username = "1234567890")
    void updateAvailability_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(patch("/api/v1/profiles/worker/availability")
                .param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Availability updated"));
    }
}
