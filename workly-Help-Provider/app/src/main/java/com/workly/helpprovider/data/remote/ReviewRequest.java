package com.workly.helpprovider.data.remote;

public class ReviewRequest {
    private String jobId;
    private int rating;
    private String comment;

    public ReviewRequest(String jobId, int rating, String comment) {
        this.jobId = jobId;
        this.rating = rating;
        this.comment = comment;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
