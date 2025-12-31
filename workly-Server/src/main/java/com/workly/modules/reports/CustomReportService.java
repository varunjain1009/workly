package com.workly.modules.reports;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomReportService {

    private final JdbcTemplate jdbcTemplate;
    private final MongoTemplate mongoTemplate;

    private static final Pattern SQL_BLOCKLIST = Pattern
            .compile("(?i).*\\b(DROP|DELETE|TRUNCATE|UPDATE|INSERT|ALTER|GRANT|REVOKE)\\b.*");

    public List<Map<String, Object>> executeSqlQuery(String query) {
        log.info("Executing SQL Query: {}", query);
        if (SQL_BLOCKLIST.matcher(query).matches()) {
            throw new IllegalArgumentException("Destructive queries (DROP, DELETE, UPDATE, etc.) are not allowed.");
        }
        return jdbcTemplate.queryForList(query);
    }

    public List<Document> executeMongoQuery(String collectionName, String jsonQuery) {
        log.info("Executing Mongo Query on collection: {}", collectionName);
        BasicQuery query = new BasicQuery(jsonQuery);
        return mongoTemplate.find(query, Document.class, collectionName);
    }

    public Map<String, List<String>> getSqlSchema() {
        Map<String, List<String>> schema = new HashMap<>();
        String sql = "SELECT table_name, column_name, data_type FROM information_schema.columns WHERE table_schema = 'public' ORDER BY table_name, ordinal_position";

        try {
            jdbcTemplate.query(sql, rs -> {
                String tableName = rs.getString("table_name");
                String columnName = rs.getString("column_name");
                String dataType = rs.getString("data_type");

                schema.computeIfAbsent(tableName, k -> new ArrayList<>())
                        .add(columnName + " (" + dataType + ")");
            });
        } catch (Exception e) {
            log.error("Error fetching SQL schema", e);
            throw new RuntimeException("Failed to fetch SQL schema", e);
        }
        return schema;
    }

    public List<String> getMongoCollections() {
        return new ArrayList<>(mongoTemplate.getCollectionNames());
    }

    public Document getMongoSample(String collectionName) {
        return mongoTemplate.getCollection(collectionName).find().first();
    }
}
