package com.workly.modules.location;

import lombok.Data;

@Data
public class LocationUpdateRequest {
    private double longitude;
    private double latitude;
}
