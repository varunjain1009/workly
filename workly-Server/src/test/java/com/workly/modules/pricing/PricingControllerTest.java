package com.workly.modules.pricing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingControllerTest {

    @Mock private PricingService pricingService;

    private PricingController pricingController;

    @BeforeEach
    void setUp() {
        pricingController = new PricingController(pricingService);
    }

    @Test
    void getPriceEstimate_delegatesToService() {
        SurgeEstimate estimate = new SurgeEstimate();
        estimate.setSurgeMultiplier(1.25);
        estimate.setFinalEstimate(125.0);
        estimate.setBaseRate(100.0);
        when(pricingService.estimatePrice(12.9, 77.6, 100.0)).thenReturn(estimate);

        var result = pricingController.getPriceEstimate(12.9, 77.6, 100.0);

        assertTrue(result.isSuccess());
        assertEquals(1.25, result.getData().getSurgeMultiplier());
        verify(pricingService).estimatePrice(12.9, 77.6, 100.0);
    }
}
