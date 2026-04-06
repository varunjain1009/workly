package com.workly.modules.location;

import com.workly.modules.profile.ProfileService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LocationServiceTest {

    @Mock private ProfileService profileService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private GeoOperations<String, String> geoOps;
    @Mock private SetOperations<String, String> setOps;
    @Mock private ZSetOperations<String, String> zSetOps;

    private MeterRegistry meterRegistry;
    private LocationService locationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        meterRegistry = new SimpleMeterRegistry();
        locationService = new LocationService(profileService, redisTemplate, meterRegistry);

        when(redisTemplate.opsForGeo()).thenReturn(geoOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
    }

    @Test
    void updateWorkerLocation_valid_writesToRedis() {
        when(geoOps.add(anyString(), any(org.springframework.data.geo.Point.class), anyString())).thenReturn(1L);
        when(setOps.add(anyString(), any(String[].class))).thenReturn(1L);

        assertDoesNotThrow(() -> locationService.updateWorkerLocation("mobile1", 77.6, 12.9));

        verify(geoOps).add(eq("worker:locations"), any(org.springframework.data.geo.Point.class), eq("mobile1"));
        verify(setOps).add(eq("worker:locations:dirty"), eq("mobile1"));
    }

    @Test
    void updateWorkerLocation_invalidLatitude_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> locationService.updateWorkerLocation("m1", 77.6, 91.0));
        assertThrows(IllegalArgumentException.class,
                () -> locationService.updateWorkerLocation("m1", 77.6, -91.0));
    }

    @Test
    void updateWorkerLocation_invalidLongitude_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> locationService.updateWorkerLocation("m1", 181.0, 12.9));
        assertThrows(IllegalArgumentException.class,
                () -> locationService.updateWorkerLocation("m1", -181.0, 12.9));
    }

    @Test
    void flushLocationsToMongo_emptyDirtySet_doesNothing() {
        when(setOps.members("worker:locations:dirty")).thenReturn(Set.of());

        locationService.flushLocationsToMongo();

        verify(profileService, never()).updateLocation(anyString(), anyDouble(), anyDouble());
    }

    @Test
    void flushLocationsToMongo_withDirtyWorkers_flushesToMongo() {
        when(setOps.members("worker:locations:dirty")).thenReturn(Set.of("mobile1", "mobile2"));
        Point p = new Point(77.6, 12.9);
        when(geoOps.position("worker:locations", "mobile1")).thenReturn(List.of(p));
        when(geoOps.position("worker:locations", "mobile2")).thenReturn(List.of(p));
        when(zSetOps.size("worker:locations")).thenReturn(2L);
        when(redisTemplate.delete("worker:locations:dirty")).thenReturn(true);

        locationService.flushLocationsToMongo();

        verify(profileService, times(2)).updateLocation(anyString(), anyDouble(), anyDouble());
        verify(redisTemplate).delete("worker:locations:dirty");
    }

    @Test
    void flushLocationsToMongo_nullDirtySet_doesNothing() {
        when(setOps.members("worker:locations:dirty")).thenReturn(null);

        locationService.flushLocationsToMongo();

        verify(profileService, never()).updateLocation(anyString(), anyDouble(), anyDouble());
    }
}
