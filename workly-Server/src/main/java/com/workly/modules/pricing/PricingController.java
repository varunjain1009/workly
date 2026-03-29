package com.workly.modules.pricing;

import com.workly.core.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
@Slf4j
public class PricingController {

    private final PricingService pricingService;

    @GetMapping("/estimate")
    public ApiResponse<SurgeEstimate> getPriceEstimate(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam double baseRate) {
        
        log.debug("PricingController: [ENTER] getPriceEstimate - lat: {}, lon: {}, base: {}", lat, lon, baseRate);
        
        SurgeEstimate estimate = pricingService.estimatePrice(lat, lon, baseRate);
        
        log.debug("PricingController: [EXIT] getPriceEstimate - multiplier: {}", estimate.getSurgeMultiplier());
        return ApiResponse.success(estimate, "Price estimate computed");
    }
}
