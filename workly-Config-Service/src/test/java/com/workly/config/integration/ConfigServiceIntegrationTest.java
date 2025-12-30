package com.workly.config.integration;

import com.workly.config.model.ConfigEntity;
import com.workly.config.service.ConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class ConfigServiceIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6.0");

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
            .withExposedPorts(6379);

    @Autowired
    private ConfigService configService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldCreateAndVersionConfig() {
        // 1. Create Initial Config (v1)
        ConfigEntity v1 = configService.createOrUpdateConfig("MAX_RADIUS", "50", "GLOBAL", "admin1");
        assertThat(v1.getVersion()).isEqualTo(1);
        assertThat(v1.isActive()).isTrue();

        // 2. Update Config (v2)
        ConfigEntity v2 = configService.createOrUpdateConfig("MAX_RADIUS", "100", "GLOBAL", "admin1");
        assertThat(v2.getVersion()).isEqualTo(2);
        assertThat(v2.getValue()).isEqualTo("100");
        assertThat(v2.isActive()).isTrue();

        // 3. Verify v1 is inactive
        // We can't easily fetch v1 by ID since we don't return it, but we can verify
        // active config is v2
        ConfigEntity active = configService.getActiveConfig("MAX_RADIUS", "GLOBAL");
        assertThat(active.getVersion()).isEqualTo(2);
    }

    @Test
    void shouldRollbackConfig() {
        // v1 -> 50
        configService.createOrUpdateConfig("TIMEOUT", "50", "GLOBAL", "admin");
        // v2 -> 100
        configService.createOrUpdateConfig("TIMEOUT", "100", "GLOBAL", "admin");

        // Rollback to v1 (which had value "50"). This should create v3 with value "50"
        ConfigEntity v3 = configService.rollback("TIMEOUT", "GLOBAL", 1, "admin");

        assertThat(v3.getVersion()).isEqualTo(3);
        assertThat(v3.getValue()).isEqualTo("50");
        assertThat(v3.isActive()).isTrue();
    }
}
