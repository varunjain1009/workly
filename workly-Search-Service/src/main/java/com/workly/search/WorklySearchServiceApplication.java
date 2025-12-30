package com.workly.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.workly.search.repository.mongo")
@EnableElasticsearchRepositories(basePackages = "com.workly.search.repository.search")
public class WorklySearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorklySearchServiceApplication.class, args);
    }
}
