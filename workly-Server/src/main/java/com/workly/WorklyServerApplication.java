package com.workly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableMongoAuditing
@EnableJpaAuditing
@EnableScheduling
@PropertySource("classpath:config.properties")
public class WorklyServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorklyServerApplication.class, args);
    }
}
