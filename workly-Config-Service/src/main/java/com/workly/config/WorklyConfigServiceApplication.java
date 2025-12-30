package com.workly.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.workly.config.repository")
public class WorklyConfigServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorklyConfigServiceApplication.class, args);
    }
}
