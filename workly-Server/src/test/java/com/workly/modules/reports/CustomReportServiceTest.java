package com.workly.modules.reports;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomReportServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private MongoTemplate mongoTemplate;

    private CustomReportService customReportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        customReportService = new CustomReportService(jdbcTemplate, mongoTemplate);
    }

    @Test
    void executeSqlQuery_validSelect_returnsResults() {
        List<Map<String, Object>> rows = List.of(Map.of("id", 1));
        when(jdbcTemplate.queryForList("SELECT * FROM jobs")).thenReturn(rows);

        List<Map<String, Object>> result = customReportService.executeSqlQuery("SELECT * FROM jobs");

        assertEquals(1, result.size());
        verify(jdbcTemplate).queryForList("SELECT * FROM jobs");
    }

    @Test
    void executeSqlQuery_nullQuery_throws() {
        assertThrows(IllegalArgumentException.class, () -> customReportService.executeSqlQuery(null));
    }

    @Test
    void executeSqlQuery_emptyQuery_throws() {
        assertThrows(IllegalArgumentException.class, () -> customReportService.executeSqlQuery("  "));
    }

    @Test
    void executeSqlQuery_nonSelectQuery_throws() {
        assertThrows(IllegalArgumentException.class, () -> customReportService.executeSqlQuery("UPDATE jobs SET x=1"));
    }

    @Test
    void executeSqlQuery_dangerousTokens_throws() {
        assertThrows(IllegalArgumentException.class, () -> customReportService.executeSqlQuery("SELECT * FROM DROP TABLE jobs"));
        assertThrows(IllegalArgumentException.class, () -> customReportService.executeSqlQuery("SELECT DELETE FROM jobs"));
    }

    @Test
    void executeMongoQuery_valid_returnsDocuments() {
        List<Document> docs = List.of(new Document("id", "abc"));
        when(mongoTemplate.find(any(), eq(Document.class), eq("jobs"))).thenReturn(docs);

        List<Document> result = customReportService.executeMongoQuery("jobs", "{\"status\": \"COMPLETED\"}");

        assertEquals(1, result.size());
    }

    @Test
    void executeMongoQuery_invalidCollectionName_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> customReportService.executeMongoQuery("jobs; DROP", "{}"));
        assertThrows(IllegalArgumentException.class,
                () -> customReportService.executeMongoQuery(null, "{}"));
    }

    @Test
    void executeMongoQuery_emptyQuery_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> customReportService.executeMongoQuery("jobs", ""));
    }

    @Test
    void getMongoCollections_returnsList() {
        when(mongoTemplate.getCollectionNames()).thenReturn(Set.of("jobs", "users"));

        List<String> result = customReportService.getMongoCollections();

        assertEquals(2, result.size());
    }

    @Test
    void getMongoSample_returnsDocument() {
        Document doc = new Document("_id", "123");
        com.mongodb.client.MongoCollection<Document> collection = mock(com.mongodb.client.MongoCollection.class);
        com.mongodb.client.FindIterable<Document> iterable = mock(com.mongodb.client.FindIterable.class);
        when(mongoTemplate.getCollection("jobs")).thenReturn(collection);
        when(collection.find()).thenReturn(iterable);
        when(iterable.first()).thenReturn(doc);

        Document result = customReportService.getMongoSample("jobs");

        assertNotNull(result);
        assertEquals("123", result.get("_id"));
    }
}
