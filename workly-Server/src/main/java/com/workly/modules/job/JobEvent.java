package com.workly.modules.job;

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
    private String eventType; // JOB_CREATED, JOB_UPDATED, JOB_COMPLETED
    private JobStatus status;
    private String workerId;
}
