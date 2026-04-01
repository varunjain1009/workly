package com.workly.notification.domain.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEvent {
    private String jobId;
    private String eventType;
    private JobStatus status;
    private String workerId;
}
