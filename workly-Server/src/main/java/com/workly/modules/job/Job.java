package com.workly.modules.job;

import com.workly.core.MongoBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "jobs")
public class Job extends MongoBaseEntity {
    @Id
    private String id;
    private String seekerMobileNumber;
    private String workerMobileNumber;
    private String title;
    private String description;
    private List<String> requiredSkills;
    private double budget;
    private JobStatus status;
    private LocalDateTime scheduledTime;
    private boolean immediate;
    private JobType jobType;
    private AssignmentMode assignmentMode;

    private String address;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private double[] location; // [longitude, latitude]

    private int searchRadiusKm;

    private String completionOtp;
    
    // Cancellation policy fields
    private Double penaltyAmount;
    private String cancellationReason;
}
