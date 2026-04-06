package com.workly.modules.location;

import com.workly.modules.profile.ProfileService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final ProfileService profileService;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    private static final String GEO_KEY = "worker:locations";
    private static final String DIRTY_SET_KEY = "worker:locations:dirty";

    /**
     * Hot path: write location to Redis Geo (O(log(N)), sub-ms latency).
     * The worker's mobile number is tracked in a dirty set for later
     * batch-flush to MongoDB.
     */
    public void updateWorkerLocation(String mobileNumber, double longitude, double latitude) {
        log.debug("LocationService: [ENTER] updateWorkerLocation - mobile: {}, lon: {}, lat: {}", mobileNumber, longitude, latitude);
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            log.warn("LocationService: [FAIL] Invalid coordinates - lon: {}, lat: {}", longitude, latitude);
            throw new IllegalArgumentException("Invalid coordinates: latitude must be [-90,90] and longitude must be [-180,180]");
        }

        // Write to Redis Geo — sub-millisecond, handles 100K+ writes/sec
        redisTemplate.opsForGeo().add(GEO_KEY,
                new org.springframework.data.geo.Point(longitude, latitude), mobileNumber);

        // Persist to MongoDB immediately so $near queries are consistent without
        // waiting for the 60-second batch flush.  The dirty-set flush still runs as
        // a safety net for any entry that was missed.
        profileService.updateLocation(mobileNumber, longitude, latitude);
        log.debug("LocationService: [EXIT] updateWorkerLocation - Written to Redis Geo + MongoDB (lon={}, lat={})", longitude, latitude);
    }

    /**
     * Batch-flush dirty locations from Redis Geo to MongoDB every 60 seconds.
     * This reduces MongoDB write pressure by ~60x compared to direct writes.
     */
    @Scheduled(fixedDelay = 60_000)
    public void flushLocationsToMongo() {
        Set<String> dirtyWorkers = redisTemplate.opsForSet().members(DIRTY_SET_KEY);
        if (dirtyWorkers == null || dirtyWorkers.isEmpty()) return;

        log.debug("LocationService: Flushing {} dirty locations to MongoDB", dirtyWorkers.size());
        int flushed = 0;

        for (String mobile : dirtyWorkers) {
            try {
                var positions = redisTemplate.opsForGeo().position(GEO_KEY, mobile);
                if (positions != null && !positions.isEmpty() && positions.get(0) != null) {
                    var point = positions.get(0);
                    profileService.updateLocation(mobile, point.getX(), point.getY());
                    flushed++;
                }
            } catch (Exception e) {
                log.error("LocationService: Failed to flush location for {}", mobile, e);
            }
        }

        // Clear the dirty set after successful flush
        redisTemplate.delete(DIRTY_SET_KEY);
        log.info("LocationService: Flushed {}/{} locations to MongoDB", flushed, dirtyWorkers.size());

        // Emit current Geo set size as a one-shot counter so Prometheus can see the
        // post-flush cardinality in addition to the gauge registered in CacheMetricsConfig.
        Long geoSize = redisTemplate.opsForZSet().size(GEO_KEY);
        meterRegistry.gauge("location.redis.geo.size.snapshot", geoSize != null ? geoSize : 0);
        log.debug("LocationService: Redis Geo set size after flush = {}", geoSize);
    }
}

