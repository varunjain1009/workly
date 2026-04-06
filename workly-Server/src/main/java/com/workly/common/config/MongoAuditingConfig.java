package com.workly.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@ConditionalOnBean(name = "mongoMappingContext")
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.workly")
public class MongoAuditingConfig {
}
