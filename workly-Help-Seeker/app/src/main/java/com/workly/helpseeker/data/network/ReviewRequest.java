package com.workly.helpseeker.data.network;

public class ReviewRequest {
    private String jobId;
    private int rating;
    private String comment;

    public ReviewRequest(String jobId, int rating, String comment) {
        this.jobId = jobId;
        this.rating = rating;
        this.comment = comment;
    }
}
