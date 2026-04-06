package com.workly.modules.admin;

import com.workly.modules.job.Job;
import com.workly.modules.job.JobRepository;
import com.workly.modules.profile.SkillSeekerProfile;
import com.workly.modules.profile.SkillSeekerProfileRepository;
import com.workly.modules.profile.WorkerProfile;
import com.workly.modules.profile.WorkerProfileRepository;
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

class AdminServiceTest {

    @Mock private SkillSeekerProfileRepository seekerRepository;
    @Mock private WorkerProfileRepository workerRepository;
    @Mock private JobRepository jobRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adminService = new AdminService(seekerRepository, workerRepository, jobRepository);
    }

    @Test
    void getSeekers_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SkillSeekerProfile> page = new PageImpl<>(List.of(new SkillSeekerProfile(), new SkillSeekerProfile()));
        when(seekerRepository.findAll(pageable)).thenReturn(page);

        Page<SkillSeekerProfile> result = adminService.getSeekers(pageable);

        assertEquals(2, result.getNumberOfElements());
        verify(seekerRepository).findAll(pageable);
    }

    @Test
    void getProviders_returnsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<WorkerProfile> page = new PageImpl<>(List.of(new WorkerProfile()));
        when(workerRepository.findAll(pageable)).thenReturn(page);

        Page<WorkerProfile> result = adminService.getProviders(pageable);

        assertEquals(1, result.getNumberOfElements());
    }

    @Test
    void getJobs_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Job> page = new PageImpl<>(List.of(new Job(), new Job(), new Job()));
        when(jobRepository.findAll(pageable)).thenReturn(page);

        Page<Job> result = adminService.getJobs(pageable);

        assertEquals(3, result.getNumberOfElements());
    }
}
