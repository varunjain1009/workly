package com.workly.modules.profile;

import com.workly.core.MongoBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "worker_profiles")
public class WorkerProfile extends MongoBaseEntity {
    @Id
    private String id;
    private String mobileNumber;
    private String name;
    private String bio;
    private List<String> skills;
    private double hourlyRate;
    private boolean available;
    private double travelRadiusKm;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private double[] lastLocation; // [longitude, latitude]

    private String deviceToken;

    private List<UnavailableSlot> unavailableSlots;

    @Data
    public static class UnavailableSlot {
        private long startTime;
        private long endTime;
    }
}
