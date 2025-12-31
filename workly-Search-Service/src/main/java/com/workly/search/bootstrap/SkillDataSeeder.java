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
    public void run(String... args) throws Exception {
        log.info("Checking for existing skills and performing initial sync...");
        skillSyncService.syncAll();
        log.info("Skill seeding and sync check completed.");
    }
}
