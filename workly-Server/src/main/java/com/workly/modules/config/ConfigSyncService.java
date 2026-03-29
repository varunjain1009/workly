package com.workly.modules.config;

import com.workly.modules.notification.model.UserToken;
import com.workly.modules.notification.repository.UserTokenRepository;
import com.workly.modules.notification.service.FCMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConfigSyncService {

    private final UserTokenRepository userTokenRepository;
    private final FCMService fcmService;

    @Value("${custom.config.sync.notification-interval-hours:1}")
    private int notificationIntervalHours;

    /**
     * Triggered when configuration changes.
     * Iterates through user tokens and sends FCM if stored notification interval
     * has passed.
     */
    public void notifyAppsOfConfigChange() {
        log.debug("ConfigSyncService: [ENTER] notifyAppsOfConfigChange - interval: {}h", notificationIntervalHours);
        log.info("Starting config sync notification process...");
        List<UserToken> tokens = userTokenRepository.findAll();
        log.debug("ConfigSyncService: Found {} registered user tokens", tokens.size());
        long now = Instant.now().toEpochMilli();
        long intervalMillis = (long) notificationIntervalHours * 60 * 60 * 1000;

        int sentCount = 0;
        for (UserToken token : tokens) {
            Long lastSent = token.getLastConfigNotificationTime();
            if (lastSent == null || (now - lastSent) > intervalMillis) {
                try {
                    log.debug("ConfigSyncService: Sending config update to token: {}", token.getId());
                    fcmService.sendNotification(token.getFcmToken(), "CONFIG_UPDATE", "Configuration updated");

                    token.setLastConfigNotificationTime(now);
                    userTokenRepository.save(token);
                    sentCount++;
                } catch (Exception e) {
                    log.debug("ConfigSyncService: [FAIL] Config notification to token {} failed: {}", token.getId(), e.getMessage());
                    log.error("Failed to send config sync notification to token: {}", token.getId(), e);
                }
            } else {
                log.debug("ConfigSyncService: Skipping token {} - last notified {}ms ago (threshold: {}ms)", token.getId(), now - lastSent, intervalMillis);
            }
        }
        log.debug("ConfigSyncService: [EXIT] notifyAppsOfConfigChange - dispatched {} of {} tokens", sentCount, tokens.size());
        log.info("Config sync notification process completed. Sent {} notifications.", sentCount);
    }

    /**
     * Called when app manually syncs or confirms receipt.
     */
    public void markAppAsSynced(String mobileNumber) {
        log.debug("ConfigSyncService: [ENTER] markAppAsSynced - mobile: {}", mobileNumber);
        if (mobileNumber == null) return;
        
        Optional<UserToken> tokenOpt = userTokenRepository.findByMobileNumber(mobileNumber);
        if (tokenOpt.isPresent()) {
            UserToken token = tokenOpt.get();
            token.setLastSyncedTime(Instant.now().toEpochMilli());
            userTokenRepository.save(token);
            log.debug("ConfigSyncService: [EXIT] markAppAsSynced - Sync timestamp recorded");
            log.info("App synced for user: {}", mobileNumber);
        } else {
            log.debug("ConfigSyncService: [EXIT] markAppAsSynced - No token found for {}", mobileNumber);
        }
    }
}
