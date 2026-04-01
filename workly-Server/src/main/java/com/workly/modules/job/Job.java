package com.workly.modules.job;

import com.workly.core.MongoBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "jobs")
@CompoundIndexes({
    // Primary query shape: jobs for a region by status and skills
    @CompoundIndex(name = "idx_region_status_skills",
            def = "{'region': 1, 'status': 1, 'requiredSkills': 1}"),
    // Worker's matching-jobs geo query filtered by region
    @CompoundIndex(name = "idx_region_status",
            def = "{'region': 1, 'status': 1}")
})
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

    /**
     * 1°×1° grid region derived from {@code location} on creation.
     * Used as the MongoDB shard key — do not update after insert.
     */
    private String region;

    private String completionOtp;
    
    // Cancellation policy fields
    private Double penaltyAmount;
    private String cancellationReason;
    
    // Growth and Retention (Promotions)
    private String appliedPromoCode;
    private double discountAmount;
}
