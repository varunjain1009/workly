package com.workly.modules.job.outbox;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "outbox_events")
public class OutboxEvent {
    @Id
    private String id;
    private String topic;
    private Object payload; // Typically serialized JSON, but can map to JobEvent directly
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean processed = false;
    private int retryCount = 0;
    private boolean failed = false;
}
