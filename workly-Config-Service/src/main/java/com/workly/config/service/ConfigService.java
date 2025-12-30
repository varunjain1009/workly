package com.workly.config.service;

import com.workly.config.model.ConfigEntity;
import com.workly.config.repository.ConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigService {

    private final ConfigRepository configRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String CONFIG_CHANNEL = "config_updates";

    public ConfigEntity createOrUpdateConfig(String key, String value, String scope, String adminId) {
        // 1. Deactivate current active config
        configRepository.findByKeyAndScopeAndActiveTrue(key, scope)
                .ifPresent(current -> {
                    current.setActive(false);
                    configRepository.save(current);
                });

        // 2. Determine new version
        Integer maxVersion = configRepository.findByKeyAndScopeOrderByVersionDesc(key, scope)
                .stream().map(ConfigEntity::getVersion).findFirst().orElse(0);

        // 3. Create new config
        ConfigEntity newConfig = new ConfigEntity();
        newConfig.setKey(key);
        newConfig.setValue(value);
        newConfig.setScope(scope);
        newConfig.setVersion(maxVersion + 1);
        newConfig.setCreatedBy(adminId);
        newConfig.setCreatedAt(Instant.now());
        newConfig.setActive(true);

        ConfigEntity saved = configRepository.save(newConfig);

        // 4. Publish Event
        publishUpdate(key, value, scope);

        return saved;
    }

    public ConfigEntity rollback(String key, String scope, Integer targetVersion, String adminId) {
        // 1. Find target version
        ConfigEntity target = configRepository.findByKeyAndScopeAndVersion(key, scope, targetVersion)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        // 2. Treat as new update (to keep history linear)
        return createOrUpdateConfig(key, target.getValue(), scope, adminId);
    }

    public List<ConfigEntity> getAllActiveConfigs(String scope) {
        return configRepository.findAll().stream()
                .filter(c -> c.isActive() && c.getScope().equals(scope))
                .toList(); // Using Java 16+ toList() or collect(Collectors.toList())
    }

    public List<ConfigEntity> getConfigHistory(String key, String scope) {
        return configRepository.findByKeyAndScopeOrderByVersionDesc(key, scope);
    }

    public ConfigEntity getActiveConfig(String key, String scope) {
        return configRepository.findByKeyAndScopeAndActiveTrue(key, scope)
                .orElseThrow(() -> new RuntimeException("Config not found"));
    }

    private void publishUpdate(String key, String value, String scope) {
        String message = String.format("%s|%s|%s",
                java.util.Objects.requireNonNull(key),
                java.util.Objects.requireNonNull(scope),
                java.util.Objects.requireNonNull(value));
        redisTemplate.convertAndSend(CONFIG_CHANNEL, java.util.Objects.requireNonNull(message));
        log.info("Published config update: {}", java.util.Objects.requireNonNull(message));
    }
}
