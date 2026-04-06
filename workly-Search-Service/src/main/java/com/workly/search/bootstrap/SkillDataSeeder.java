package com.workly.search.bootstrap;

import com.workly.search.service.SkillSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SkillDataSeeder implements CommandLineRunner {

    private final SkillSyncService skillSyncService;

    @Override
    public void run(String... args) {
        log.info("Checking for existing skills and performing initial sync...");
        try {
            skillSyncService.syncAll();
            log.info("Skill seeding and sync check completed.");
        } catch (Exception e) {
            log.warn("Skill sync skipped — Elasticsearch unavailable at startup ({}). Skill normalisation will be degraded until ES comes up.", e.getMessage());
        }
    }
}
