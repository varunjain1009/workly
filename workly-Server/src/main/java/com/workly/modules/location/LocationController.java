package com.workly.modules.location;

import com.workly.core.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
        log.debug("LocationController: [ENTER] updateLocation - mobile: {}, lon: {}, lat: {}",
                mobileNumber, request.getLongitude(), request.getLatitude());
        locationService.updateWorkerLocation(mobileNumber, request.getLongitude(), request.getLatitude());
        log.debug("LocationController: [EXIT] updateLocation - coordinates persisted for {}", mobileNumber);
        return ApiResponse.success(null, "Location updated");
    }
}
