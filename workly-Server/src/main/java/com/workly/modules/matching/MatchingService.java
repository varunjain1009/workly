package com.workly.modules.matching;

import com.workly.modules.profile.WorkerProfile;
import com.workly.modules.profile.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final WorkerProfileRepository workerRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String GEO_KEY = "worker:locations";

    public List<WorkerProfile> findMatches(List<String> requiredSkills, double longitude, double latitude,
            double radiusKm, Long scheduledTimeMillis) {
        log.debug("MatchingService: [ENTER] findMatches - skills: {}, lon: {}, lat: {}, radius: {}km, scheduledTimeMillis: {}",
                requiredSkills, longitude, latitude, radiusKm, scheduledTimeMillis);

        // ── Hot path: Redis Geo (sub-millisecond) ────────────────────────────
        List<WorkerProfile> results = findMatchesFromRedis(requiredSkills, longitude, latitude, radiusKm,
                scheduledTimeMillis);

        if (!results.isEmpty()) {
            log.debug("MatchingService: Redis Geo hit — {} candidates before skill filter", results.size());
            return sortByTier(results);
        }

        // ── Cold path: MongoDB $near (fallback) ──────────────────────────────
        log.debug("MatchingService: Redis Geo miss — falling back to MongoDB $near");
        double maxDistanceMeters = radiusKm * 1000;
        List<WorkerProfile> mongoResults;
        if (scheduledTimeMillis != null && scheduledTimeMillis > 0) {
            mongoResults = workerRepository.findMatchingWorkersAvailableAt(requiredSkills, longitude, latitude,
                    maxDistanceMeters, scheduledTimeMillis);
        } else {
            mongoResults = workerRepository.findMatchingWorkers(requiredSkills, longitude, latitude, maxDistanceMeters);
        }
        log.debug("MatchingService: [EXIT] findMatches - MongoDB found {} workers", mongoResults.size());
        return sortByTier(mongoResults);
    }

    /**
     * Query Redis Geo for all worker IDs within radiusKm, then load their profiles
     * from MongoDB and filter by skills (and optionally by schedule).
     *
     * This is the hot path: Redis GEORADIUS is O(N+log M) and sub-millisecond
     * for typical city-scale worker counts.
     */
    private List<WorkerProfile> findMatchesFromRedis(List<String> requiredSkills, double longitude, double latitude,
            double radiusKm, Long scheduledTimeMillis) {
        try {
            GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = redisTemplate.opsForGeo()
                    .radius(GEO_KEY, new Point(longitude, latitude),
                            new Distance(radiusKm, Metrics.KILOMETERS));

            if (geoResults == null || geoResults.getContent().isEmpty()) {
                return List.of();
            }

            List<String> nearbyMobileNumbers = geoResults.getContent().stream()
                    .map(r -> r.getContent().getName())
                    .toList();

            // Load profiles for nearby workers and filter by availability + skills
            List<WorkerProfile> candidates = workerRepository.findByMobileNumberIn(nearbyMobileNumbers);

            return candidates.stream()
                    .filter(WorkerProfile::isAvailable)
                    .filter(p -> p.getSkills() != null
                            && requiredSkills.stream().anyMatch(s -> p.getSkills().contains(s)))
                    .filter(p -> scheduledTimeMillis == null || scheduledTimeMillis <= 0
                            || isAvailableAt(p, scheduledTimeMillis))
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.warn("MatchingService: Redis Geo query failed, will fallback to MongoDB. Error: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean isAvailableAt(WorkerProfile p, long targetTimeMillis) {
        if (p.getUnavailableSlots() == null) return true;
        return p.getUnavailableSlots().stream()
                .noneMatch(s -> s.getStartTime() <= targetTimeMillis && s.getEndTime() >= targetTimeMillis);
    }

    private List<WorkerProfile> sortByTier(List<WorkerProfile> workers) {
        workers.sort(java.util.Comparator.comparing((WorkerProfile p) -> {
            if (p.getTier() == null) return 0;
            return p.getTier().ordinal();
        }).reversed());
        return workers;
    }
}
