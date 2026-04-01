package com.workly.matching.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MatchingRequest {
    @NotEmpty
    private List<String> requiredSkills;

    @Min(-180) @Max(180)
    private double longitude;

    @Min(-90) @Max(90)
    private double latitude;

    @Min(1) @Max(200)
    private int radiusKm;

    private Long scheduledTimeMillis; // null for immediate jobs
}
