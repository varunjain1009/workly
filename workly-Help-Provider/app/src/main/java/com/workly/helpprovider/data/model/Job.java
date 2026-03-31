package com.workly.helpprovider.data.model;

import java.io.Serializable;

public class Job implements Serializable {
    private String id;
    private String title;
    private String description;
    private String requiredSkill;
    private Location location;
    private int searchRadiusKm;
    private long preferredDateTime;
    private JobType jobType;
    private AssignmentMode assignmentMode;
    private Double budget;
    private JobStatus status;
    private String workerId;
    private Float minWorkerRating;
    private String preferredLanguage;
    private boolean toolsRequired;
    private String seekerMobileNumber;
    private String workerMobileNumber;
    private String workerName;

    public Job() {
    }

    public Job(String title, String description, String requiredSkill, Location location,
            int searchRadiusKm, long preferredDateTime, JobType jobType,
            AssignmentMode assignmentMode) {
        this.title = title;
        this.description = description;
        this.requiredSkill = requiredSkill;
        this.location = location;
        this.searchRadiusKm = searchRadiusKm;
        this.preferredDateTime = preferredDateTime;
        this.jobType = jobType;
        this.assignmentMode = assignmentMode;
        this.status = JobStatus.CREATED;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequiredSkill() {
        return requiredSkill;
    }

    public void setRequiredSkill(String requiredSkill) {
        this.requiredSkill = requiredSkill;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public int getSearchRadiusKm() {
        return searchRadiusKm;
    }

    public void setSearchRadiusKm(int searchRadiusKm) {
        this.searchRadiusKm = searchRadiusKm;
    }

    public long getPreferredDateTime() {
        return preferredDateTime;
    }

    public void setPreferredDateTime(long preferredDateTime) {
        this.preferredDateTime = preferredDateTime;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public AssignmentMode getAssignmentMode() {
        return assignmentMode;
    }

    public void setAssignmentMode(AssignmentMode assignmentMode) {
        this.assignmentMode = assignmentMode;
    }

    public Double getBudget() {
        return budget;
    }

    public void setBudget(Double budget) {
        this.budget = budget;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Float getMinWorkerRating() {
        return minWorkerRating;
    }

    public void setMinWorkerRating(Float minWorkerRating) {
        this.minWorkerRating = minWorkerRating;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public boolean isToolsRequired() {
        return toolsRequired;
    }

    public void setToolsRequired(boolean toolsRequired) {
        this.toolsRequired = toolsRequired;
    }

    public String getSeekerMobileNumber() {
        return seekerMobileNumber;
    }

    public void setSeekerMobileNumber(String seekerMobileNumber) {
        this.seekerMobileNumber = seekerMobileNumber;
    }

    public String getWorkerMobileNumber() {
        return workerMobileNumber;
    }

    public void setWorkerMobileNumber(String workerMobileNumber) {
        this.workerMobileNumber = workerMobileNumber;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }
}
