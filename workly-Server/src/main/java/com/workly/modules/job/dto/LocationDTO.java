package com.workly.modules.job.dto;

import lombok.Data;

@Data
public class LocationDTO {
    private double latitude;
    private double longitude;
    private String address;
}
