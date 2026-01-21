package com.workly.helpseeker.data.network;

public class OtpRequest {
    private String mobileNumber;

    public OtpRequest() {
    }

    public OtpRequest(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }
}
