package com.workly.config.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Document(collection = "configs")
public class ConfigEntity {
    @Id
    private String id;

    @Indexed
    private String key; // e.g., "MAX_JOB_RADIUS"
    private String value; // e.g., "100" or "{"enabled": true}"
    private String scope; // e.g., "GLOBAL", "CITY:NYC"
    private Integer version; // e.g., 1, 2, 3

    private String createdBy; // Admin ID
    private Instant createdAt;

    // Audit info
    private boolean active; // If this is the currently active version for this scope
}
