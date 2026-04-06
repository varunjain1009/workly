package com.workly.modules.worker;

import com.workly.modules.matching.MatchingService;
import com.workly.modules.profile.ProfileService;
import com.workly.modules.profile.WorkerProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerControllerTest {

    @Mock private MatchingService matchingService;
    @Mock private ProfileService profileService;

    private WorkerController workerController;

    @BeforeEach
    void setUp() {
        workerController = new WorkerController(matchingService, profileService);
    }

    @Test
    void searchWorkers_returnsMatches() {
        WorkerProfile wp = new WorkerProfile();
        wp.setMobileNumber("w1");
        when(matchingService.findMatches(anyList(), anyDouble(), anyDouble(), anyDouble(), isNull()))
                .thenReturn(List.of(wp));

        var result = workerController.searchWorkers("plumbing", 12.9, 77.6, 10.0);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        verify(matchingService).findMatches(List.of("plumbing"), 77.6, 12.9, 10.0, null);
    }

    @Test
    void searchWorkers_noResults_returnsEmpty() {
        when(matchingService.findMatches(anyList(), anyDouble(), anyDouble(), anyDouble(), isNull()))
                .thenReturn(List.of());

        var result = workerController.searchWorkers("plumbing", 12.9, 77.6, 5.0);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void uploadKycDocument_profileFound_setsKycVerified() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("w1", null, List.of()));

        WorkerProfile profile = new WorkerProfile();
        profile.setMobileNumber("w1");
        when(profileService.getWorkerProfile("w1")).thenReturn(Optional.of(profile));
        when(profileService.createOrUpdateWorkerProfile(any())).thenAnswer(i -> i.getArgument(0));

        var result = workerController.uploadKycDocument(null);

        assertTrue(result.isSuccess());
        assertTrue(profile.isKycVerified());
        assertNotNull(profile.getIdDocumentUrl());
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadKycDocument_profileNotFound_throws() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("w1", null, List.of()));

        when(profileService.getWorkerProfile("w1")).thenReturn(Optional.empty());

        assertThrows(com.workly.core.WorklyException.class,
                () -> workerController.uploadKycDocument(null));
        SecurityContextHolder.clearContext();
    }
}
