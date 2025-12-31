package com.workly.modules.analytics;

import com.workly.modules.analytics.AnalyticsController.DashboardStats;
import com.workly.modules.job.Job;
import com.workly.modules.job.JobRepository;
import com.workly.modules.job.JobStatus;
import com.workly.modules.profile.SkillSeekerProfileRepository;
import com.workly.modules.profile.WorkerProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SkillSeekerProfileRepository seekerRepository;

    @Mock
    private WorkerProfileRepository workerRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void getDashboardStats_ShouldReturnCorrectStats() {
        // Arrange
        when(seekerRepository.count()).thenReturn(100L);
        when(workerRepository.count()).thenReturn(50L);
        when(seekerRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(5L);
        when(workerRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(2L);
        when(seekerRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1L);
        when(workerRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);

        when(jobRepository.countByStatusIn(anyList())).thenReturn(20L);
        when(jobRepository.countByStatus(JobStatus.COMPLETED)).thenReturn(10L);
        when(jobRepository.countByStatus(JobStatus.CREATED)).thenReturn(5L); // For distribution logic

        Job completedJob1 = new Job();
        completedJob1.setBudget(100.0);
        Job completedJob2 = new Job();
        completedJob2.setBudget(200.50);
        when(jobRepository.findByStatus(JobStatus.COMPLETED)).thenReturn(List.of(completedJob1, completedJob2));

        // Act
        DashboardStats stats = analyticsService.getDashboardStats();

        // Assert
        assertNotNull(stats);
        assertEquals(150L, stats.getTotalUsers()); // 100 + 50
        assertEquals(7L, stats.getNewUsersToday()); // 5 + 2
        assertEquals(20L, stats.getActiveJobs());
        assertEquals(10L, stats.getCompletedJobs());
        assertEquals(300.50, stats.getRevenue(), 0.001);

        // Detailed distribution checks
        assertNotNull(stats.getJobStatusDistribution());
        assertEquals(5, stats.getJobStatusDistribution().get("OPEN"));
    }
}
