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
            double radiusKm) {
        log.debug("MatchingService: [ENTER] findMatches - skills: {}, lon: {}, lat: {}, radius: {}km", requiredSkills, longitude, latitude, radiusKm);
        double maxDistanceMeters = radiusKm * 1000;
        List<WorkerProfile> results = workerRepository.findMatchingWorkers(requiredSkills, longitude, latitude, maxDistanceMeters);
        log.debug("MatchingService: [EXIT] findMatches - Found {} matching workers within {}m radius", results.size(), maxDistanceMeters);
        return results;
    }
}
