package com.workly.modules.reports;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class CustomReportController {

    private final CustomReportService customReportService;

    @PostMapping("/execute")
    public ResponseEntity<?> executeQuery(@RequestBody QueryRequest request) {
        log.debug("CustomReportController: [ENTER] executeQuery - type: {}, collection: {}",
                request.getType(), request.getCollection());
        if ("SQL".equalsIgnoreCase(request.getType())) {
            log.debug("CustomReportController: Executing SQL query");
            Object result = customReportService.executeSqlQuery(request.getQuery());
            log.debug("CustomReportController: [EXIT] executeQuery - SQL done");
            return ResponseEntity.ok(result);
        } else if ("MONGO".equalsIgnoreCase(request.getType())) {
            log.debug("CustomReportController: Executing Mongo query on collection: {}", request.getCollection());
            Object result = customReportService.executeMongoQuery(request.getCollection(), request.getQuery());
            log.debug("CustomReportController: [EXIT] executeQuery - Mongo done");
            return ResponseEntity.ok(result);
        } else {
            log.debug("CustomReportController: [FAIL] executeQuery - unknown type: {}", request.getType());
            return ResponseEntity.badRequest().body("Invalid query type. Must be SQL or MONGO");
        }
    }

    @GetMapping("/schema/sql")
    public ResponseEntity<?> getSqlSchema() {
        log.debug("CustomReportController: [ENTER] getSqlSchema");
        Object schema = customReportService.getSqlSchema();
        log.debug("CustomReportController: [EXIT] getSqlSchema");
        return ResponseEntity.ok(schema);
    }

    @GetMapping("/schema/mongo")
    public ResponseEntity<?> getMongoCollections() {
        log.debug("CustomReportController: [ENTER] getMongoCollections");
        Object collections = customReportService.getMongoCollections();
        log.debug("CustomReportController: [EXIT] getMongoCollections");
        return ResponseEntity.ok(collections);
    }

    @GetMapping("/schema/mongo/{collection}/sample")
    public ResponseEntity<?> getMongoSample(@PathVariable String collection) {
        log.debug("CustomReportController: [ENTER] getMongoSample - collection: {}", collection);
        Object sample = customReportService.getMongoSample(collection);
        log.debug("CustomReportController: [EXIT] getMongoSample - collection: {}", collection);
        return ResponseEntity.ok(sample);
    }
}
