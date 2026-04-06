package com.workly.config.service;

import com.workly.config.model.ConfigEntity;
import com.workly.config.repository.ConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private ConfigRepository configRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    private ConfigService configService;

    @BeforeEach
    void setUp() {
        configService = new ConfigService(configRepository, redisTemplate);
        // convertAndSend returns Long (subscriber count), not void
        lenient().when(redisTemplate.convertAndSend(anyString(), anyString())).thenReturn(1L);
    }

    // ── createOrUpdateConfig ─────────────────────────────────────────────────

    @Test
    void createOrUpdateConfig_shouldCreateFirstVersion_whenNoExistingConfig() {
        when(configRepository.findByKeyAndScopeAndActiveTrue("KEY", "GLOBAL")).thenReturn(Optional.empty());
        when(configRepository.findByKeyAndScopeOrderByVersionDesc("KEY", "GLOBAL")).thenReturn(List.of());
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfigEntity result = configService.createOrUpdateConfig("KEY", "value1", "GLOBAL", "admin");

        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getValue()).isEqualTo("value1");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getKey()).isEqualTo("KEY");
        assertThat(result.getScope()).isEqualTo("GLOBAL");
        assertThat(result.getCreatedBy()).isEqualTo("admin");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void createOrUpdateConfig_shouldDeactivatePreviousActiveVersion() {
        ConfigEntity existing = configEntity("KEY", "GLOBAL", "old-value", 1, true);
        when(configRepository.findByKeyAndScopeAndActiveTrue("KEY", "GLOBAL")).thenReturn(Optional.of(existing));
        when(configRepository.findByKeyAndScopeOrderByVersionDesc("KEY", "GLOBAL"))
                .thenReturn(List.of(existing));
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        configService.createOrUpdateConfig("KEY", "new-value", "GLOBAL", "admin");

        assertThat(existing.isActive()).isFalse();
        // save called twice: once to deactivate old, once to persist new
        verify(configRepository, times(2)).save(any());
    }

    @Test
    void createOrUpdateConfig_shouldIncrementVersion_whenPreviousVersionsExist() {
        ConfigEntity v1 = configEntity("KEY", "GLOBAL", "val", 1, false);
        ConfigEntity v2 = configEntity("KEY", "GLOBAL", "val", 2, true);
        when(configRepository.findByKeyAndScopeAndActiveTrue("KEY", "GLOBAL")).thenReturn(Optional.of(v2));
        when(configRepository.findByKeyAndScopeOrderByVersionDesc("KEY", "GLOBAL"))
                .thenReturn(List.of(v2, v1));
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfigEntity result = configService.createOrUpdateConfig("KEY", "val3", "GLOBAL", "admin");

        assertThat(result.getVersion()).isEqualTo(3);
    }

    @Test
    void createOrUpdateConfig_shouldPublishToRedis() {
        when(configRepository.findByKeyAndScopeAndActiveTrue("K", "S")).thenReturn(Optional.empty());
        when(configRepository.findByKeyAndScopeOrderByVersionDesc("K", "S")).thenReturn(List.of());
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        configService.createOrUpdateConfig("K", "V", "S", "admin");

        verify(redisTemplate).convertAndSend("config_updates", "K|S|V");
    }

    // ── rollback ─────────────────────────────────────────────────────────────

    @Test
    void rollback_shouldCreateNewVersionWithTargetValue() {
        ConfigEntity v1 = configEntity("TIMEOUT", "GLOBAL", "50", 1, false);
        when(configRepository.findByKeyAndScopeAndVersion("TIMEOUT", "GLOBAL", 1)).thenReturn(Optional.of(v1));
        when(configRepository.findByKeyAndScopeAndActiveTrue("TIMEOUT", "GLOBAL")).thenReturn(Optional.empty());
        when(configRepository.findByKeyAndScopeOrderByVersionDesc("TIMEOUT", "GLOBAL"))
                .thenReturn(List.of(v1));
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfigEntity result = configService.rollback("TIMEOUT", "GLOBAL", 1, "admin");

        assertThat(result.getValue()).isEqualTo("50");
        assertThat(result.getVersion()).isEqualTo(2);
    }

    @Test
    void rollback_shouldThrowRuntimeException_whenVersionNotFound() {
        when(configRepository.findByKeyAndScopeAndVersion("KEY", "GLOBAL", 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.rollback("KEY", "GLOBAL", 99, "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Version not found");
    }

    // ── getActiveConfig ───────────────────────────────────────────────────────

    @Test
    void getActiveConfig_shouldReturnActiveConfig() {
        ConfigEntity active = configEntity("KEY", "GLOBAL", "val", 2, true);
        when(configRepository.findByKeyAndScopeAndActiveTrue("KEY", "GLOBAL")).thenReturn(Optional.of(active));

        ConfigEntity result = configService.getActiveConfig("KEY", "GLOBAL");

        assertThat(result).isSameAs(active);
    }

    @Test
    void getActiveConfig_shouldThrowRuntimeException_whenNotFound() {
        when(configRepository.findByKeyAndScopeAndActiveTrue("KEY", "GLOBAL")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.getActiveConfig("KEY", "GLOBAL"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Config not found");
    }

    // ── getAllActiveConfigs ────────────────────────────────────────────────────

    @Test
    void getAllActiveConfigs_shouldReturnAllForScope() {
        List<ConfigEntity> list = List.of(
                configEntity("K1", "GLOBAL", "v1", 1, true),
                configEntity("K2", "GLOBAL", "v2", 1, true));
        when(configRepository.findByScopeAndActiveTrue("GLOBAL")).thenReturn(list);

        List<ConfigEntity> result = configService.getAllActiveConfigs("GLOBAL");

        assertThat(result).hasSize(2);
    }

    // ── getConfigHistory ──────────────────────────────────────────────────────

    @Test
    void getConfigHistory_shouldReturnAllVersionsOrderedDesc() {
        List<ConfigEntity> history = List.of(
                configEntity("K", "GLOBAL", "v3", 3, true),
                configEntity("K", "GLOBAL", "v2", 2, false),
                configEntity("K", "GLOBAL", "v1", 1, false));
        when(configRepository.findByKeyAndScopeOrderByVersionDesc("K", "GLOBAL")).thenReturn(history);

        List<ConfigEntity> result = configService.getConfigHistory("K", "GLOBAL");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getVersion()).isEqualTo(3);
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
