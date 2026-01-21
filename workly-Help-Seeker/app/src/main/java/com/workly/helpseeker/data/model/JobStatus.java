package com.workly.helpseeker.data.model;

import java.io.Serializable;

public enum JobStatus implements Serializable {
    CREATED, SCHEDULED, BROADCASTED, PENDING_ACCEPTANCE, ASSIGNED, COMPLETED, CANCELLED, EXPIRED
}
