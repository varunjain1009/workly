package com.workly.config.bootstrap;

import com.workly.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConfigDataSeeder implements CommandLineRunner {

    private final ConfigService configService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking for existing configurations...");

        // We can check simply by listing global configs
        if (!configService.getAllActiveConfigs("GLOBAL").isEmpty()) {
            log.info("Configurations already exist. Skipping seeding.");
            return;
        }

        log.info("No configurations found. Seeding default values.");

        seed("custom.location.update-interval-minutes", "60");
        seed("custom.auth.otp.resend-delay-seconds", "300");
        seed("custom.app.debug-enabled", "true");
        seed("custom.job.max-radius-km", "50");
        seed("custom.job.min-advance-hours", "2");
        seed("custom.assignment.mode", "FIRST_ACCEPT");
        seed("custom.monetisation.enabled", "false");
        seed("custom.monetisation.model", "PER_JOB");
        seed("custom.monetisation.allow-browse-without-payment", "true");
        seed("custom.chat.url", "ws://192.168.31.112:8082/ws/chat");

        log.info("Configuration seeding completed.");
    }

    private void seed(String key, String value) {
        configService.createOrUpdateConfig(key, value, "GLOBAL", "system-seeder");
    }
}
