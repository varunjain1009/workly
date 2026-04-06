package com.workly.modules.matching;

import com.workly.modules.profile.WorkerProfile;
import com.workly.modules.profile.WorkerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private WorkerProfileRepository workerRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService(workerRepository, redisTemplate);
    }

    @Test
    void findMatches_ShouldCallRepositoryWithCorrectDistance() {
        List<String> skills = List.of("Plumbing");
        double lon = 77.5946;
        double lat = 12.9716;
        double radiusKm = 10.0;
        double expectedDistanceMeters = 10000.0;

        // Mock Redis → return empty (force fallback)
        when(redisTemplate.opsForGeo()).thenThrow(new RuntimeException("Redis down"));

        List<WorkerProfile> mockWorkers = List.of(new WorkerProfile());

        when(workerRepository.findMatchingWorkers(skills, lon, lat, expectedDistanceMeters))
                .thenReturn(mockWorkers);

        List<WorkerProfile> result =
                matchingService.findMatches(skills, lon, lat, radiusKm, null);

        assertEquals(mockWorkers, result);
    }

    @Test
    void findMatches_scheduledJob_callsAvailableAt() {
        List<String> skills = List.of("Plumbing");
        when(redisTemplate.opsForGeo()).thenThrow(new RuntimeException("Redis down"));

        WorkerProfile wp = new WorkerProfile();
        when(workerRepository.findMatchingWorkersAvailableAt(eq(skills), anyDouble(), anyDouble(),
                anyDouble(), anyLong())).thenReturn(List.of(wp));

        List<WorkerProfile> result = matchingService.findMatches(skills, 77.6, 12.9, 10.0, 9999999999L);

        assertEquals(1, result.size());
    }

    @Test
    void findMatches_redisReturnsNull_fallsBackToMongo() {
        List<String> skills = List.of("Plumbing");
        when(redisTemplate.opsForGeo()).thenReturn(null);

        // opsForGeo().radius() would NPE → caught → fallback to mongo
        when(workerRepository.findMatchingWorkers(eq(skills), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        List<WorkerProfile> result = matchingService.findMatches(skills, 77.6, 12.9, 5.0, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void findMatches_sortsByTierDescending() {
        List<String> skills = List.of("Plumbing");
        when(redisTemplate.opsForGeo()).thenThrow(new RuntimeException("Redis down"));

        WorkerProfile standard = new WorkerProfile();
        standard.setTier(com.workly.modules.profile.WorkerProfile.ProviderTier.STANDARD);
        WorkerProfile premium = new WorkerProfile();
        premium.setTier(com.workly.modules.profile.WorkerProfile.ProviderTier.PREMIUM);
        WorkerProfile superPro = new WorkerProfile();
        superPro.setTier(com.workly.modules.profile.WorkerProfile.ProviderTier.SUPER_PRO);

        when(workerRepository.findMatchingWorkers(any(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(standard, superPro, premium));

        List<WorkerProfile> result = matchingService.findMatches(skills, 77.6, 12.9, 10.0, null);

        assertEquals(com.workly.modules.profile.WorkerProfile.ProviderTier.SUPER_PRO, result.get(0).getTier());
    }
}