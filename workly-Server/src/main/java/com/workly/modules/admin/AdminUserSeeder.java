package com.workly.modules.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserSeeder implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (adminUserRepository.findByUsername("admin").isEmpty()) {
            log.info("Creating default admin user...");
            AdminUser defaultAdmin = new AdminUser();
            defaultAdmin.setUsername("admin");
            defaultAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
            adminUserRepository.save(defaultAdmin);
            log.info("Default admin user created successfully.");
        }
    }
}
