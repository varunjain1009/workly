package com.workly.modules.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.workly.modules.search.SearchServiceClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileServiceTest {

    @Mock
    private WorkerProfileRepository workerRepository;

    @Mock
    private SkillSeekerProfileRepository seekerRepository;

    @Mock
    private SearchServiceClient searchServiceClient;

    @Mock
    private com.workly.modules.notification.NotificationService notificationService;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        profileService = new ProfileService(workerRepository, seekerRepository, searchServiceClient);
        org.springframework.test.util.ReflectionTestUtils.setField(profileService, "notificationService", notificationService);
    }

    @Test
    void createOrUpdateWorkerProfile_ShouldSaveProfile() {
        WorkerProfile profile = new WorkerProfile();
        when(workerRepository.save(profile)).thenReturn(profile);

        WorkerProfile result = profileService.createOrUpdateWorkerProfile(profile);

        verify(workerRepository).save(profile);
        assertEquals(profile, result);
    }

    @Test
    void createOrUpdateSeekerProfile_ShouldSaveProfile() {
        SkillSeekerProfile profile = new SkillSeekerProfile();
        when(seekerRepository.save(profile)).thenReturn(profile);

        SkillSeekerProfile result = profileService.createOrUpdateSeekerProfile(profile);

        verify(seekerRepository).save(profile);
        assertEquals(profile, result);
    }

    @Test
    void getWorkerProfile_ShouldReturnProfile() {
        String mobile = "123";
        WorkerProfile profile = new WorkerProfile();
        when(workerRepository.findByMobileNumber(mobile)).thenReturn(java.util.List.of(profile));

        Optional<WorkerProfile> result = profileService.getWorkerProfile(mobile);

        assertTrue(result.isPresent());
        assertEquals(profile, result.get());
    }

    @Test
    void getSeekerProfile_ShouldReturnProfile() {
        String mobile = "123";
        SkillSeekerProfile profile = new SkillSeekerProfile();
        when(seekerRepository.findByMobileNumber(mobile)).thenReturn(Optional.of(profile));

        Optional<SkillSeekerProfile> result = profileService.getSeekerProfile(mobile);

        assertTrue(result.isPresent());
        assertEquals(profile, result.get());
    }

    @Test
    void updateLocation_ShouldUpdateWhenProfileExists() {
        String mobile = "123";
        WorkerProfile profile = new WorkerProfile();
        when(workerRepository.findByMobileNumber(mobile)).thenReturn(java.util.List.of(profile));

        profileService.updateLocation(mobile, 77.0, 12.0);

        assertArrayEquals(new double[] { 77.0, 12.0 }, profile.getLastLocation());
        verify(workerRepository).save(profile);
    }

    @Test
    void updateAvailability_ShouldUpdateWhenProfileExists() {
        String mobile = "123";
        WorkerProfile profile = new WorkerProfile();
        when(workerRepository.findByMobileNumber(mobile)).thenReturn(java.util.List.of(profile));

        profileService.updateAvailability(mobile, true);

        assertTrue(profile.isAvailable());
        verify(workerRepository).save(profile);
    }
}
