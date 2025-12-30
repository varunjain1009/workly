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
        log.info("Starting config sync notification process...");
        List<UserToken> tokens = userTokenRepository.findAll();
        long now = Instant.now().toEpochMilli();
        long intervalMillis = notificationIntervalHours * 60 * 60 * 1000L;

        int sentCount = 0;
        for (UserToken token : tokens) {
            Long lastSent = token.getLastConfigNotificationTime();
            if (lastSent == null || (now - lastSent) > intervalMillis) {
                try {
                    // Send data message to trigger sync
                    fcmService.sendNotification(token.getFcmToken(), "CONFIG_UPDATE", "Configuration updated");

                    // Update last sent time
                    token.setLastConfigNotificationTime(now);
                    userTokenRepository.save(token);
                    sentCount++;
                } catch (Exception e) {
                    log.error("Failed to send config sync notification to token: {}", token.getId(), e);
                }
            }
        }
        log.info("Config sync notification process completed. Sent {} notifications.", sentCount);
    }

    /**
     * Called when app manually syncs or confirms receipt.
     */
    public void markAppAsSynced(String mobileNumber) {
        if (mobileNumber == null) return;
        
        Optional<UserToken> tokenOpt = userTokenRepository.findByMobileNumber(mobileNumber);
        if (tokenOpt.isPresent()) {
            UserToken token = tokenOpt.get();
            token.setLastSyncedTime(Instant.now().toEpochMilli());
            userTokenRepository.save(token);
            log.info("App synced for user: {}", mobileNumber);
        }
    }
}
