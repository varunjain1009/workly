package com.workly.matching.service;

import com.workly.matching.domain.worker.WorkerProfile;
import com.workly.matching.domain.worker.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final WorkerProfileRepository workerRepository;

    public List<WorkerProfile> findMatches(List<String> requiredSkills, double longitude, double latitude,
            double radiusKm, Long scheduledTimeMillis) {
        log.debug("MatchingService: findMatches - skills: {}, lon: {}, lat: {}, radius: {}km", requiredSkills, longitude, latitude, radiusKm);
        double maxDistanceMeters = radiusKm * 1000;
        List<WorkerProfile> results;
        if (scheduledTimeMillis != null && scheduledTimeMillis > 0) {
            results = workerRepository.findMatchingWorkersAvailableAt(requiredSkills, longitude, latitude,
                    maxDistanceMeters, scheduledTimeMillis);
        } else {
            results = workerRepository.findMatchingWorkers(requiredSkills, longitude, latitude, maxDistanceMeters);
        }

        // Sort by ProviderTier descending (SUPER_PRO > PREMIUM > STANDARD)
        results.sort(Comparator.comparing((WorkerProfile p) ->
                p.getTier() == null ? 0 : p.getTier().ordinal()).reversed());

        log.debug("MatchingService: Found {} matching workers", results.size());
        return results;
    }
}
