package com.workly.modules.pricing;

import lombok.Data;

@Data
public class SurgeEstimate {
    private double baseRate;
    private double surgeMultiplier;
    private long localActiveJobs;
    private double finalEstimate;
}
