package com.workly.modules.pricing;

import com.workly.modules.job.JobRepository;
import com.workly.modules.job.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService {

    private final JobRepository jobRepository;

    public SurgeEstimate estimatePrice(double lat, double lon, double baseRate) {
        log.debug("PricingService: [ENTER] estimatePrice - lat: {}, lon: {}, baseRate: {}", lat, lon, baseRate);
        
        // Define active/pending states indicative of market demand
        List<JobStatus> activeStatuses = Arrays.asList(
                JobStatus.CREATED,
                JobStatus.BROADCASTED,
                JobStatus.ASSIGNED
        );

        // Count jobs within a 5km radius
        Point point = new Point(lon, lat);
        Distance distance = new Distance(5, Metrics.KILOMETERS);
        long activeJobCount = jobRepository.countByLocationNearAndStatusIn(point, distance, activeStatuses);

        log.debug("PricingService: Found {} active jobs within 5km of ({}, {})", activeJobCount, lat, lon);

        double surgeMultiplier = calculateSurgeMultiplier(activeJobCount);
        double finalEstimate = baseRate * surgeMultiplier;

        SurgeEstimate estimate = new SurgeEstimate();
        estimate.setBaseRate(baseRate);
        estimate.setSurgeMultiplier(surgeMultiplier);
        estimate.setLocalActiveJobs(activeJobCount);
        estimate.setFinalEstimate(finalEstimate);

        log.info("PricingService: [EXIT] estimatePrice - Multiplier {}x applied. Final estimate: {}", surgeMultiplier, finalEstimate);
        return estimate;
    }

    private double calculateSurgeMultiplier(long activeJobCount) {
        if (activeJobCount >= 50) {
            return 2.0; // Extreme demand
        } else if (activeJobCount >= 20) {
            return 1.5; // High demand
        } else if (activeJobCount >= 10) {
            return 1.25; // Moderate demand
        }
        return 1.0; // Normal demand
    }
}
