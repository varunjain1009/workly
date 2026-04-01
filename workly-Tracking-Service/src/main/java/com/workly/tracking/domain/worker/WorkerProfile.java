package com.workly.tracking.domain.worker;

import com.workly.core.MongoBaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "worker_profiles")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkerProfile extends MongoBaseEntity {
    @Id
    private String id;
    private String mobileNumber;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private double[] lastLocation; // [longitude, latitude]
}
