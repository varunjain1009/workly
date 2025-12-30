package com.workly.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OtpService otpService;

    @MockitoBean
    private JwtUtils jwtUtils;

    // Mock other infra dependencies that might be loaded in @SpringBootTest
    @MockitoBean
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @MockitoBean
    private com.workly.modules.profile.WorkerProfileRepository workerProfileRepository;

    @MockitoBean
    private com.workly.modules.profile.SkillSeekerProfileRepository skillSeekerProfileRepository;

    @MockitoBean
    private com.workly.modules.job.JobRepository jobRepository;

    @MockitoBean
    private com.workly.modules.review.ReviewRepository reviewRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void requestOtp_ShouldReturnSuccess() throws Exception {
        OtpRequest request = new OtpRequest();
        request.setMobileNumber("1234567890");

        mockMvc.perform(post("/api/v1/auth/otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP sent successfully"));

        verify(otpService).generateAndSendOtp("1234567890");
    }

    @Test
    void login_ShouldReturnTokenWhenValid() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setMobileNumber("1234567890");
        request.setOtp("1234");

        when(otpService.validateOtp("1234567890", "1234")).thenReturn(true);
        when(jwtUtils.generateToken("1234567890")).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"));
    }
}
