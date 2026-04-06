package com.workly.config.controller;

import com.workly.config.model.ConfigEntity;
import com.workly.config.service.ConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfigService configService;

    @MockitoBean
    private com.workly.config.repository.ConfigRepository configRepository;

    @MockitoBean
    private com.workly.common.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private com.workly.common.security.RateLimitFilter rateLimitFilter;

    @MockitoBean
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Test
    void createConfig_shouldReturn200WithCreatedEntity() throws Exception {
        ConfigEntity entity = configEntity("MAX_RADIUS", "GLOBAL", "50", 1, true);
        when(configService.createOrUpdateConfig("MAX_RADIUS", "50", "GLOBAL", "admin1")).thenReturn(entity);

        mockMvc.perform(post("/api/v1/configs")
                        .param("key", "MAX_RADIUS")
                        .param("value", "50")
                        .param("scope", "GLOBAL")
                        .param("adminId", "admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("MAX_RADIUS"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getAllConfigs_shouldReturnListForScope() throws Exception {
        List<ConfigEntity> configs = List.of(
                configEntity("K1", "GLOBAL", "v1", 1, true),
                configEntity("K2", "GLOBAL", "v2", 1, true));
        when(configService.getAllActiveConfigs("GLOBAL")).thenReturn(configs);

        mockMvc.perform(get("/api/v1/configs").param("scope", "GLOBAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllConfigs_shouldDefaultScopeToGlobal() throws Exception {
        when(configService.getAllActiveConfigs("GLOBAL")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/configs"))
                .andExpect(status().isOk());

        verify(configService).getAllActiveConfigs("GLOBAL");
    }

    @Test
    void getHistory_shouldReturnAllVersionsForKey() throws Exception {
        List<ConfigEntity> history = List.of(
                configEntity("KEY", "GLOBAL", "v2", 2, true),
                configEntity("KEY", "GLOBAL", "v1", 1, false));
        when(configService.getConfigHistory("KEY", "GLOBAL")).thenReturn(history);

        mockMvc.perform(get("/api/v1/configs/KEY/history").param("scope", "GLOBAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getConfig_shouldReturnActiveConfig() throws Exception {
        ConfigEntity active = configEntity("KEY", "GLOBAL", "100", 3, true);
        when(configService.getActiveConfig("KEY", "GLOBAL")).thenReturn(active);

        mockMvc.perform(get("/api/v1/configs/KEY").param("scope", "GLOBAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("100"))
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    void rollback_shouldReturnRolledBackConfig() throws Exception {
        ConfigEntity rolled = configEntity("KEY", "GLOBAL", "50", 3, true);
        when(configService.rollback("KEY", "GLOBAL", 1, "admin")).thenReturn(rolled);

        mockMvc.perform(post("/api/v1/configs/KEY/rollback")
                        .param("scope", "GLOBAL")
                        .param("version", "1")
                        .param("adminId", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("50"))
                .andExpect(jsonPath("$.version").value(3));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ConfigEntity configEntity(String key, String scope, String value, int version, boolean active) {
        ConfigEntity e = new ConfigEntity();
        e.setKey(key);
        e.setScope(scope);
        e.setValue(value);
        e.setVersion(version);
        e.setActive(active);
        return e;
    }
}
