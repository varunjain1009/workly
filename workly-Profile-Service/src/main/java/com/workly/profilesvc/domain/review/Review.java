package com.workly.profilesvc.domain.review;

import com.workly.core.MongoBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "reviews")
public class Review extends MongoBaseEntity {
    @Id
    private String id;
    private String jobId;
    private String seekerMobileNumber;
    private String workerMobileNumber;
    private int rating;
    private String comment;
    private ReviewerRole reviewerRole;

    public enum ReviewerRole {
        SEEKER, WORKER
    }
}
