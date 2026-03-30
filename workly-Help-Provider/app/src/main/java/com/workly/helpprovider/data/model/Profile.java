package com.workly.helpprovider.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@Entity(tableName = "profiles")
public class Profile implements Serializable {
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    private int id; // Local Room ID

    @SerializedName("mobileNumber")
    private String mobileNumber; // Also serves as server ID

    @SerializedName("name")
    private String name;

    @SerializedName("expertise")
    private String expertise; // For simplicity, storing as comma-separated or JSON string

    @SerializedName("visiting_charges")
    private double visitingCharges;
    @SerializedName("skills")
    private java.util.List<String> skills;

    @SerializedName("available")
    private boolean available;

    @SerializedName("hourlyRate")
    private double perHourCharges;


    @SerializedName("travelRadiusKm")
    private int travelRadius;

    @SerializedName("last_lat")
    private double lastLat;

    @SerializedName("last_lng")
    private double lastLng;

    // Getters and Setters
    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpertise() {
        return expertise;
    }

    public void setExpertise(String expertise) {
        this.expertise = expertise;
    }

    public double getVisitingCharges() {
        return visitingCharges;
    }

    public void setVisitingCharges(double visitingCharges) {
        this.visitingCharges = visitingCharges;
    }

    public double getPerHourCharges() {
        return perHourCharges;
    }

    public void setPerHourCharges(double perHourCharges) {
        this.perHourCharges = perHourCharges;
    }


    public int getTravelRadius() {
        return travelRadius;
    }

    public void setTravelRadius(int travelRadius) {
        this.travelRadius = travelRadius;
    }

    public double getLastLat() {
        return lastLat;
    }

    public void setLastLat(double lastLat) {
        this.lastLat = lastLat;
    }

    public double getLastLng() {
        return lastLng;
    }

    public void setLastLng(double lastLng) {
        this.lastLng = lastLng;
    }

    public java.util.List<String> getSkills() {
        return skills;
    }

    public void setSkills(java.util.List<String> skills) {
        this.skills = skills;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

}
