package com.workly.modules.location;

import com.workly.modules.profile.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final ProfileService profileService;

    public void updateWorkerLocation(String mobileNumber, double longitude, double latitude) {
        profileService.updateLocation(mobileNumber, longitude, latitude);
        // Additional logic like region-awareness or Kafka events could be added here
    }
}
