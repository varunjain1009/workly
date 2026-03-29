package com.workly.modules.matching;

import com.workly.modules.profile.WorkerProfile;
import com.workly.modules.profile.WorkerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class MatchingServiceTest {

    @Mock
    private WorkerProfileRepository workerRepository;

    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        matchingService = new MatchingService(workerRepository);
    }

    @Test
    void findMatches_ShouldCallRepositoryWithCorrectDistance() {
        List<String> skills = List.of("Plumbing");
        double lon = 77.5946;
        double lat = 12.9716;
        double radiusKm = 10.0;
        double expectedDistanceMeters = 10000.0;

        List<WorkerProfile> mockWorkers = List.of(new WorkerProfile());
        when(workerRepository.findMatchingWorkers(skills, lon, lat, expectedDistanceMeters))
                .thenReturn(mockWorkers);

        List<WorkerProfile> result = matchingService.findMatches(skills, lon, lat, radiusKm, null);

        assertEquals(mockWorkers, result);
    }
}
