package com.workly.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@ConditionalOnBean(name = "entityManagerFactory")
@EnableJpaAuditing
public class JpaAuditingConfig {
}
