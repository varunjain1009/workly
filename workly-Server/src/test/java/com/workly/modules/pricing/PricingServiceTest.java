package com.workly.modules.pricing;

import com.workly.modules.job.JobRepository;
import com.workly.modules.job.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PricingServiceTest {

    @Mock private JobRepository jobRepository;

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pricingService = new PricingService(jobRepository);
    }

    @Test
    void estimatePrice_normalDemand_multiplier1x() {
        when(jobRepository.countByLocationNearAndStatusIn(any(Point.class), any(Distance.class), anyList()))
                .thenReturn(5L);

        SurgeEstimate estimate = pricingService.estimatePrice(12.9, 77.6, 200.0);

        assertEquals(1.0, estimate.getSurgeMultiplier());
        assertEquals(200.0, estimate.getFinalEstimate());
        assertEquals(5L, estimate.getLocalActiveJobs());
    }

    @Test
    void estimatePrice_moderateDemand_multiplier1_25x() {
        when(jobRepository.countByLocationNearAndStatusIn(any(), any(), anyList())).thenReturn(15L);

        SurgeEstimate estimate = pricingService.estimatePrice(12.9, 77.6, 100.0);

        assertEquals(1.25, estimate.getSurgeMultiplier());
        assertEquals(125.0, estimate.getFinalEstimate());
    }

    @Test
    void estimatePrice_highDemand_multiplier1_5x() {
        when(jobRepository.countByLocationNearAndStatusIn(any(), any(), anyList())).thenReturn(25L);

        SurgeEstimate estimate = pricingService.estimatePrice(12.9, 77.6, 100.0);

        assertEquals(1.5, estimate.getSurgeMultiplier());
        assertEquals(150.0, estimate.getFinalEstimate());
    }

    @Test
    void estimatePrice_extremeDemand_multiplier2x() {
        when(jobRepository.countByLocationNearAndStatusIn(any(), any(), anyList())).thenReturn(60L);

        SurgeEstimate estimate = pricingService.estimatePrice(12.9, 77.6, 100.0);

        assertEquals(2.0, estimate.getSurgeMultiplier());
        assertEquals(200.0, estimate.getFinalEstimate());
        assertEquals(100.0, estimate.getBaseRate());
    }
}
