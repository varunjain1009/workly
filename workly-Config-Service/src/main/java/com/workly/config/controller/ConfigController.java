package com.workly.config.controller;

import com.workly.config.model.ConfigEntity;
import com.workly.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @PostMapping
    public ConfigEntity createConfig(@RequestParam String key,
            @RequestParam String value,
            @RequestParam(defaultValue = "GLOBAL") String scope,
            @RequestParam String adminId) {
        return configService.createOrUpdateConfig(key, value, scope, adminId);
    }

    @GetMapping
    public java.util.List<ConfigEntity> getAllConfigs(@RequestParam(defaultValue = "GLOBAL") String scope) {
        return configService.getAllActiveConfigs(scope);
    }

    @GetMapping("/{key}/history")
    public java.util.List<ConfigEntity> getHistory(@PathVariable String key,
            @RequestParam(defaultValue = "GLOBAL") String scope) {
        return configService.getConfigHistory(key, scope);
    }

    @GetMapping("/{key}")
    public ConfigEntity getConfig(@PathVariable String key,
            @RequestParam(defaultValue = "GLOBAL") String scope) {
        return configService.getActiveConfig(key, scope);
    }

    @PostMapping("/{key}/rollback")
    public ConfigEntity rollback(@PathVariable String key,
            @RequestParam(defaultValue = "GLOBAL") String scope,
            @RequestParam Integer version,
            @RequestParam String adminId) {
        return configService.rollback(key, scope, version, adminId);
    }
}
