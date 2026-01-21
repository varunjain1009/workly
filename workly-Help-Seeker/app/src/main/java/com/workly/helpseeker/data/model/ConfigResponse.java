package com.workly.helpseeker.data.model;

import com.google.gson.annotations.SerializedName;

public class ConfigResponse {
    @SerializedName("locationUpdateIntervalMinutes")
    private int locationUpdateIntervalMinutes;

    @SerializedName("otpResendDelaySeconds")
    private int otpResendDelaySeconds;

    @SerializedName("debugEnabled")
    private boolean debugEnabled;

    @SerializedName("jobMaxRadiusKm")
    private int jobMaxRadiusKm;

    @SerializedName("jobMinAdvanceHours")
    private int jobMinAdvanceHours;

    @SerializedName("assignmentMode")
    private String assignmentMode;

    @SerializedName("chatUrl")
    private String chatUrl;

    @SerializedName("monetisation")
    private MonetisationConfig monetisation;

    public int getLocationUpdateIntervalMinutes() {
        return locationUpdateIntervalMinutes;
    }

    public int getOtpResendDelaySeconds() {
        return otpResendDelaySeconds;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public int getJobMinAdvanceHours() { return jobMinAdvanceHours; }

    public int getJobMaxRadiusKm() {
        return jobMaxRadiusKm;
    }

    public String getAssignmentMode() {
        return assignmentMode;
    }
    
    public String getChatUrl() {
        return chatUrl;
    }

    public MonetisationConfig getMonetisation() {
        return monetisation;
    }

    public static class MonetisationConfig {
        @SerializedName("enabled")
        private boolean enabled;
        @SerializedName("model")
        private String model;
        @SerializedName("allowBrowseWithoutPayment")
        private boolean allowBrowseWithoutPayment;
        
        public boolean isEnabled() { return enabled; }
        public String getModel() { return model; }
        public boolean isAllowBrowseWithoutPayment() { return allowBrowseWithoutPayment; }
    }
}
