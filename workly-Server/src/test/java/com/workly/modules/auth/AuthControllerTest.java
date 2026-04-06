package com.workly.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workly.common.security.JwtUtils;
import com.workly.modules.profile.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OtpService otpService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

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
