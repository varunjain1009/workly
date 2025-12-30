package com.workly.modules.matching;

import com.workly.modules.profile.WorkerProfile;
import com.workly.modules.profile.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final WorkerProfileRepository workerRepository;

    public List<WorkerProfile> findMatches(List<String> requiredSkills, double longitude, double latitude,
            double radiusKm) {
        // MongoDB maxDistance is in meters
        double maxDistanceMeters = radiusKm * 1000;
        return workerRepository.findMatchingWorkers(requiredSkills, longitude, latitude, maxDistanceMeters);
    }
}
