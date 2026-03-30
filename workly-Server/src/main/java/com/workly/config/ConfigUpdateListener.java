package com.workly.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigUpdateListener implements MessageListener {

    private final RuntimeConfigCache configCache;
    private final com.workly.modules.config.ConfigSyncService configSyncService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String msg = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("Received config update: {}", msg);

            // Format: key|scope|value
            String[] parts = msg.split("\\|", 3);
            if (parts.length == 3) {
                String key = parts[0];
                String scope = parts[1];
                String value = parts[2];

                // For MVP, we ignore scope or assume GLOBAL
                if ("GLOBAL".equals(scope)) {
                    configCache.put(key, value);
                    log.info("Updated runtime config: {} = {}", key, value);
                    // Push update immediately to all registered app devices
                    configSyncService.forceNotifyAppsOfConfigChange();
                } else if ("APP".equals(scope) || "PROVIDER".equals(scope) || "SEEKER".equals(scope)) {
                    // App-targeted config: push FCM so apps re-fetch; not applied in backend cache
                    log.info("App-scoped config change for scope={}, key={} — triggering FCM push", scope, key);
                    configSyncService.forceNotifyAppsOfConfigChange();
                }
            }
        } catch (Exception e) {
            log.error("Failed to process config update", e);
        }
    }
}
