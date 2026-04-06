package com.workly.modules.admin;

import com.workly.modules.job.Job;
import com.workly.modules.profile.SkillSeekerProfile;
import com.workly.modules.profile.WorkerProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    @Mock private AdminService adminService;

    private AdminController adminController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adminController = new AdminController(adminService);
    }

    @Test
    void getSeekers_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SkillSeekerProfile> page = new PageImpl<>(List.of(new SkillSeekerProfile()));
        when(adminService.getSeekers(pageable)).thenReturn(page);

        var result = adminController.getSeekers(pageable);

        assertEquals(1, result.getData().getNumberOfElements());
        verify(adminService).getSeekers(pageable);
    }

    @Test
    void getProviders_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<WorkerProfile> page = new PageImpl<>(List.of(new WorkerProfile(), new WorkerProfile()));
        when(adminService.getProviders(pageable)).thenReturn(page);

        var result = adminController.getProviders(pageable);

        assertEquals(2, result.getData().getNumberOfElements());
    }

    @Test
    void getJobs_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Job> page = new PageImpl<>(List.of(new Job()));
        when(adminService.getJobs(pageable)).thenReturn(page);

        var result = adminController.getJobs(pageable);

        assertEquals(1, result.getData().getNumberOfElements());
    }
}
