package com.workly.modules.location;

import com.workly.modules.profile.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final ProfileService profileService;

    public void updateWorkerLocation(String mobileNumber, double longitude, double latitude) {
        log.debug("LocationService: [ENTER] updateWorkerLocation - mobile: {}, lon: {}, lat: {}", mobileNumber, longitude, latitude);
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            log.warn("LocationService: [FAIL] Invalid coordinates - lon: {}, lat: {}", longitude, latitude);
            throw new IllegalArgumentException("Invalid coordinates: latitude must be [-90,90] and longitude must be [-180,180]");
        }
        profileService.updateLocation(mobileNumber, longitude, latitude);
        log.debug("LocationService: [EXIT] updateWorkerLocation - Geo coordinates persisted");
    }
}
