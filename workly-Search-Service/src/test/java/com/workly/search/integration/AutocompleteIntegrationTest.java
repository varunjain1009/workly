package com.workly.search.integration;

import com.workly.search.service.AutocompleteService;
import com.workly.search.service.SkillSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import org.testcontainers.images.builder.ImageFromDockerfile;

@SpringBootTest
@Testcontainers
class AutocompleteIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6.0");

    static String imageId;

    static {
        try {
            imageId = new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> builder
                            .from("docker.elastic.co/elasticsearch/elasticsearch:7.17.10")
                            .run("bin/elasticsearch-plugin install analysis-phonetic")
                            .build())
                    .get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Container
    @ServiceConnection
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse(imageId).asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch"))
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withStartupTimeout(Duration.ofMinutes(2));

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
            .withExposedPorts(6379);

    @Autowired
    private SkillSyncService skillSyncService;

    @Autowired
    private AutocompleteService autocompleteService;

    @Test
    void shouldFindElectricianWithFuzzySearch() throws InterruptedException {
        // 1. Seed Data (Mongo -> ES)
        skillSyncService.syncAll();

        // Allow ES to index (simple sleep for test simplicity, ideally use Awaitility)
        Thread.sleep(2000);

        // 2. Exact Match
        List<String> exact = autocompleteService.autocomplete("Electrician");
        assertThat(exact).contains("Electrician");

        // 3. Fuzzy Match (Typo: "electrisian")
        List<String> fuzzy = autocompleteService.autocomplete("electrisian");
        assertThat(fuzzy).contains("Electrician");

        // 4. Phonetic/Alias Match (Typo: "electrishan")
        List<String> phonetic = autocompleteService.autocomplete("electrishan");
        assertThat(phonetic).contains("Electrician");
    }
}
