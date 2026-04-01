package com.workly.profilesvc.domain.worker;

import com.workly.core.MongoBaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private int totalReviews;
    private double averageRating;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private double[] lastLocation; // [longitude, latitude]

    private String deviceToken;
    private List<UnavailableSlot> unavailableSlots;
    private boolean kycVerified;
    private String idDocumentUrl;
    private ProviderTier tier = ProviderTier.STANDARD;

    public enum ProviderTier {
        STANDARD, PREMIUM, SUPER_PRO
    }

    @Data
    public static class UnavailableSlot {
        private long startTime;
        private long endTime;
    }
}
