package com.workly.modules.review.dto;

import lombok.Data;

@Data
public class ReviewDTO {
    private String id;
    private String jobId;
    private int rating;
    private String comment;
    private long createdAt;
}
