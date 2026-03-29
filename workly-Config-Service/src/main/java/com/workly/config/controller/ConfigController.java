package com.workly.config.controller;

import com.workly.config.model.ConfigEntity;
import com.workly.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/configs")
@RequiredArgsConstructor
@Slf4j
public class ConfigController {

    private final ConfigService configService;

    @PostMapping
    public ConfigEntity createConfig(@RequestParam String key,
            @RequestParam String value,
            @RequestParam(defaultValue = "GLOBAL") String scope,
            @RequestParam String adminId) {
        log.debug("ConfigController: [ENTER] createConfig - key: {}, scope: {}, adminId: {}", key, scope, adminId);
        ConfigEntity result = configService.createOrUpdateConfig(key, value, scope, adminId);
        log.debug("ConfigController: [EXIT] createConfig - key: {}, version: {}", key, result.getVersion());
        return result;
    }

    @GetMapping
    public java.util.List<ConfigEntity> getAllConfigs(@RequestParam(defaultValue = "GLOBAL") String scope) {
        log.debug("ConfigController: [ENTER] getAllConfigs - scope: {}", scope);
        java.util.List<ConfigEntity> configs = configService.getAllActiveConfigs(scope);
        log.debug("ConfigController: [EXIT] getAllConfigs - scope: {}, count: {}", scope, configs.size());
        return configs;
    }

    @GetMapping("/{key}/history")
    public java.util.List<ConfigEntity> getHistory(@PathVariable String key,
            @RequestParam(defaultValue = "GLOBAL") String scope) {
        log.debug("ConfigController: [ENTER] getHistory - key: {}, scope: {}", key, scope);
        java.util.List<ConfigEntity> history = configService.getConfigHistory(key, scope);
        log.debug("ConfigController: [EXIT] getHistory - key: {}, {} versions", key, history.size());
        return history;
    }

    @GetMapping("/{key}")
    public ConfigEntity getConfig(@PathVariable String key,
            @RequestParam(defaultValue = "GLOBAL") String scope) {
        log.debug("ConfigController: [ENTER] getConfig - key: {}, scope: {}", key, scope);
        ConfigEntity config = configService.getActiveConfig(key, scope);
        log.debug("ConfigController: [EXIT] getConfig - key: {}, version: {}", key, config.getVersion());
        return config;
    }

    @PostMapping("/{key}/rollback")
    public ConfigEntity rollback(@PathVariable String key,
            @RequestParam(defaultValue = "GLOBAL") String scope,
            @RequestParam Integer version,
            @RequestParam String adminId) {
        log.debug("ConfigController: [ENTER] rollback - key: {}, scope: {}, targetVersion: {}, adminId: {}",
                key, scope, version, adminId);
        ConfigEntity result = configService.rollback(key, scope, version, adminId);
        log.debug("ConfigController: [EXIT] rollback - key: {}, new version: {}", key, result.getVersion());
        return result;
    }
}
