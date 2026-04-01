package com.workly.config;

import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;

/**
 * Provides a secondary-preferred {@link MongoTemplate} for read-heavy queries
 * that do not require primary consistency (job listings, profile lookups,
 * worker discovery).
 *
 * <p>Usage: inject {@code @Qualifier("secondaryMongoTemplate")} where
 * eventual-consistent reads are acceptable. All writes and job-acceptance
 * paths continue to use the default primary MongoTemplate.</p>
 *
 * <p>The MongoDB replica set is already configured with 3 nodes, so secondary
 * reads distribute read load across 2 secondaries without any infrastructure
 * change.</p>
 */
@Configuration
public class MongoReadConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean(name = "secondaryMongoTemplate")
    public MongoTemplate secondaryMongoTemplate(MongoClient mongoClient,
            MongoDatabaseFactory primaryFactory,
            MongoConverter converter) {
        // Re-use the same MongoClient (same connection pool) — just override
        // the database factory's read preference to SECONDARY_PREFERRED.
        SimpleMongoClientDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(mongoClient,
                primaryFactory.getMongoDatabase().getName());

        MongoTemplate template = new MongoTemplate(factory, converter);
        template.setReadPreference(ReadPreference.secondaryPreferred());
        return template;
    }
}
