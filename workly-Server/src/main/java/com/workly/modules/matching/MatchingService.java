package com.workly.modules.matching;

import com.workly.modules.profile.WorkerProfile;
import com.workly.modules.profile.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final WorkerProfileRepository workerRepository;

    public List<WorkerProfile> findMatches(List<String> requiredSkills, double longitude, double latitude,
            double radiusKm, Long scheduledTimeMillis) {
        log.debug("MatchingService: [ENTER] findMatches - skills: {}, lon: {}, lat: {}, radius: {}km, scheduledTimeMillis: {}", requiredSkills, longitude, latitude, radiusKm, scheduledTimeMillis);
        double maxDistanceMeters = radiusKm * 1000;
        List<WorkerProfile> results;
        if (scheduledTimeMillis != null && scheduledTimeMillis > 0) {
            results = workerRepository.findMatchingWorkersAvailableAt(requiredSkills, longitude, latitude, maxDistanceMeters, scheduledTimeMillis);
        } else {
            results = workerRepository.findMatchingWorkers(requiredSkills, longitude, latitude, maxDistanceMeters);
        }
        log.debug("MatchingService: [EXIT] findMatches - Found {} matching workers within {}m radius", results.size(), maxDistanceMeters);
        return results;
    }
}
