package com.workly.tracking.controller;

import com.workly.core.ApiResponse;
import com.workly.tracking.service.LocationService;
import com.workly.tracking.service.LocationUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/update")
    public ApiResponse<Void> updateLocation(Authentication auth, @Valid @RequestBody LocationUpdateRequest request) {
        String mobileNumber = (String) auth.getPrincipal();
        log.debug("LocationController: updateLocation - mobile: {}, lon: {}, lat: {}",
                mobileNumber, request.getLongitude(), request.getLatitude());
        locationService.updateWorkerLocation(mobileNumber, request.getLongitude(), request.getLatitude());
        return ApiResponse.success(null, "Location updated");
    }
}
