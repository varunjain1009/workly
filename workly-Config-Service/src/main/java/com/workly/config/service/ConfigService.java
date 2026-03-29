package com.workly.config.service;

import com.workly.config.model.ConfigEntity;
import com.workly.config.repository.ConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigService {

    private final ConfigRepository configRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String CONFIG_CHANNEL = "config_updates";

    @Transactional
    public ConfigEntity createOrUpdateConfig(String key, String value, String scope, String adminId) {
        log.debug("ConfigService: [ENTER] createOrUpdateConfig - key: {}, scope: {}, adminId: {}", key, scope, adminId);
        // 1. Deactivate current active config
        configRepository.findByKeyAndScopeAndActiveTrue(key, scope)
                .ifPresent(current -> {
                    log.debug("ConfigService: createOrUpdateConfig - deactivating existing version: {}", current.getVersion());
                    current.setActive(false);
                    configRepository.save(current);
                });

        // 2. Determine new version
        Integer maxVersion = configRepository.findByKeyAndScopeOrderByVersionDesc(key, scope)
                .stream().map(ConfigEntity::getVersion).findFirst().orElse(0);
        log.debug("ConfigService: createOrUpdateConfig - maxVersion: {}, new version will be: {}", maxVersion, maxVersion + 1);

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
        log.debug("ConfigService: createOrUpdateConfig - saved config id: {}, version: {}", saved.getId(), saved.getVersion());

        // 4. Publish Event
        publishUpdate(key, value, scope);

        log.debug("ConfigService: [EXIT] createOrUpdateConfig - key: {}, version: {}", key, saved.getVersion());
        return saved;
    }

    public ConfigEntity rollback(String key, String scope, Integer targetVersion, String adminId) {
        log.debug("ConfigService: [ENTER] rollback - key: {}, scope: {}, targetVersion: {}", key, scope, targetVersion);
        // 1. Find target version
        ConfigEntity target = configRepository.findByKeyAndScopeAndVersion(key, scope, targetVersion)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        log.debug("ConfigService: rollback - found target config id: {}, will create new version from it", target.getId());
        // 2. Treat as new update (to keep history linear)
        ConfigEntity result = createOrUpdateConfig(key, target.getValue(), scope, adminId);
        log.debug("ConfigService: [EXIT] rollback - key: {}, rolled back to value from version {}", key, targetVersion);
        return result;
    }

    public List<ConfigEntity> getAllActiveConfigs(String scope) {
        log.debug("ConfigService: [ENTER] getAllActiveConfigs - scope: {}", scope);
        List<ConfigEntity> configs = configRepository.findByScopeAndActiveTrue(scope);
        log.debug("ConfigService: [EXIT] getAllActiveConfigs - scope: {}, count: {}", scope, configs.size());
        return configs;
    }

    public List<ConfigEntity> getConfigHistory(String key, String scope) {
        log.debug("ConfigService: [ENTER] getConfigHistory - key: {}, scope: {}", key, scope);
        List<ConfigEntity> history = configRepository.findByKeyAndScopeOrderByVersionDesc(key, scope);
        log.debug("ConfigService: [EXIT] getConfigHistory - key: {}, {} versions found", key, history.size());
        return history;
    }

    public ConfigEntity getActiveConfig(String key, String scope) {
        log.debug("ConfigService: [ENTER] getActiveConfig - key: {}, scope: {}", key, scope);
        ConfigEntity config = configRepository.findByKeyAndScopeAndActiveTrue(key, scope)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        log.debug("ConfigService: [EXIT] getActiveConfig - key: {}, version: {}", key, config.getVersion());
        return config;
    }

    private void publishUpdate(String key, String value, String scope) {
        log.debug("ConfigService: [ENTER] publishUpdate - key: {}, scope: {}", key, scope);
        String message = String.format("%s|%s|%s",
                java.util.Objects.requireNonNull(key),
                java.util.Objects.requireNonNull(scope),
                java.util.Objects.requireNonNull(value));
        redisTemplate.convertAndSend(CONFIG_CHANNEL, java.util.Objects.requireNonNull(message));
        log.info("Published config update: {}", java.util.Objects.requireNonNull(message));
        log.debug("ConfigService: [EXIT] publishUpdate - message sent on channel: {}", CONFIG_CHANNEL);
    }
}
