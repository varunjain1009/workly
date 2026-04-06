package com.workly.modules.monetization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workly.common.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonetizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MonetizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MonetizationService monetizationService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Test
    @WithMockUser(username = "1234567890")
    void getSubscriptionStatus_ShouldReturnStatus() throws Exception {
        when(monetizationService.isUserAuthorized("1234567890")).thenReturn(true);

        mockMvc.perform(get("/api/v1/monetization/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @WithMockUser(username = "1234567890")
    void subscribe_ShouldReturnSubscription() throws Exception {
        Subscription subscription = new Subscription();
        subscription.setPlanType("PREMIUM");

        MonetizationController.SubscriptionRequest request = new MonetizationController.SubscriptionRequest();
        request.setPlanType("PREMIUM");
        request.setDurationDays(30);

        when(monetizationService.subscribe(anyString(), anyString(), anyInt())).thenReturn(subscription);

        mockMvc.perform(post("/api/v1/monetization/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planType").value("PREMIUM"));
    }
}
