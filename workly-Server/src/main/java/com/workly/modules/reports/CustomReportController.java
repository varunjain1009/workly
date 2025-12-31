package com.workly.modules.reports;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class CustomReportController {

    private final CustomReportService customReportService;

    @PostMapping("/execute")
    public ResponseEntity<?> executeQuery(@RequestBody QueryRequest request) {
        if ("SQL".equalsIgnoreCase(request.getType())) {
            return ResponseEntity.ok(customReportService.executeSqlQuery(request.getQuery()));
        } else if ("MONGO".equalsIgnoreCase(request.getType())) {
            return ResponseEntity
                    .ok(customReportService.executeMongoQuery(request.getCollection(), request.getQuery()));
        } else {
            return ResponseEntity.badRequest().body("Invalid query type. Must be SQL or MONGO");
        }
    }

    @GetMapping("/schema/sql")
    public ResponseEntity<?> getSqlSchema() {
        return ResponseEntity.ok(customReportService.getSqlSchema());
    }

    @GetMapping("/schema/mongo")
    public ResponseEntity<?> getMongoCollections() {
        return ResponseEntity.ok(customReportService.getMongoCollections());
    }

    @GetMapping("/schema/mongo/{collection}/sample")
    public ResponseEntity<?> getMongoSample(@PathVariable String collection) {
        return ResponseEntity.ok(customReportService.getMongoSample(collection));
    }
}
