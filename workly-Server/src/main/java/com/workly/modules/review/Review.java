package com.workly.modules.review;

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
    private int rating; // 1-5
    private String comment;
    private ReviewerRole reviewerRole;
    
    // Trust and Safety
    private boolean disputed;
    private String disputeReason;

    public enum ReviewerRole {
        SEEKER, // Seeker left a review for the Worker
        WORKER  // Worker left a review for the Seeker
    }
}
