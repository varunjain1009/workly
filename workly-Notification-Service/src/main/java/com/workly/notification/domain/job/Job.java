package com.workly.notification.domain.job;

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
    private List<String> requiredSkills;
    private JobStatus status;
    private LocalDateTime scheduledTime;
    private boolean immediate;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private double[] location; // [longitude, latitude]

    private int searchRadiusKm;
}
