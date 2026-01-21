package com.workly.helpseeker.data.model;

public class Worker {
    private String id;
    private String name;
    private double skillMatch;
    private double distanceKm;
    private String availabilityStatus;
    private float rating;
    private int reviewCount;
    private double estimatedCharges;
    private int travelRadius;
    private String profileImageUrl;

    public Worker() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSkillMatch() {
        return skillMatch;
    }

    public void setSkillMatch(double skillMatch) {
        this.skillMatch = skillMatch;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public double getEstimatedCharges() {
        return estimatedCharges;
    }

    public void setEstimatedCharges(double estimatedCharges) {
        this.estimatedCharges = estimatedCharges;
    }

    public int getTravelRadius() {
        return travelRadius;
    }

    public void setTravelRadius(int travelRadius) {
        this.travelRadius = travelRadius;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
