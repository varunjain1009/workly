package com.workly.modules.analytics;

import com.workly.modules.analytics.AnalyticsController.DashboardStats;
import com.workly.modules.analytics.AnalyticsController.DataPoint;
import com.workly.modules.job.Job;
import com.workly.modules.job.JobRepository;
import com.workly.modules.job.JobStatus;
import com.workly.modules.profile.SkillSeekerProfileRepository;
import com.workly.modules.profile.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JobRepository jobRepository;
    private final SkillSeekerProfileRepository seekerRepository;
    private final WorkerProfileRepository workerRepository;

    public DashboardStats getDashboardStats() {
        log.info("Fetching dashboard stats");

        long totalSeekers = seekerRepository.count();
        long totalWorkers = workerRepository.count();
        long totalUsers = totalSeekers + totalWorkers;

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long newSeekersToday = seekerRepository.countByCreatedAtAfter(startOfDay);
        long newWorkersToday = workerRepository.countByCreatedAtAfter(startOfDay);
        long newUsersToday = newSeekersToday + newWorkersToday;

        long activeJobs = jobRepository.countByStatusIn(List.of(
                JobStatus.CREATED,
                JobStatus.SCHEDULED,
                JobStatus.BROADCASTED,
                JobStatus.ASSIGNED,
                JobStatus.PENDING_ACCEPTANCE));

        long completedJobs = jobRepository.countByStatus(JobStatus.COMPLETED);

        // Calculate revenue from completed jobs
        List<Job> completedJobsList = jobRepository.findByStatus(JobStatus.COMPLETED);
        double revenue = completedJobsList.stream()
                .mapToDouble(Job::getBudget)
                .sum();

        return DashboardStats.builder()
                .totalUsers(totalUsers)
                .newUsersToday(newUsersToday)
                .activeJobs(activeJobs)
                .completedJobs(completedJobs)
                .revenue(revenue)
                .userGrowth(getUserGrowth())
                .jobStatusDistribution(getJobStatusDistribution())
                .build();
    }

    private List<DataPoint> getUserGrowth() {
        List<DataPoint> growth = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Calculate for the last 7 days
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            long seekers = seekerRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            long workers = workerRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            long totalNewUsers = seekers + workers;

            String dayLabel = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            growth.add(new DataPoint(dayLabel, (int) totalNewUsers));
        }
        return growth;
    }

    private Map<String, Integer> getJobStatusDistribution() {
        Map<String, Integer> distribution = new HashMap<>();

        long created = jobRepository.countByStatus(JobStatus.CREATED);
        long assigned = jobRepository.countByStatus(JobStatus.ASSIGNED);
        long completed = jobRepository.countByStatus(JobStatus.COMPLETED);
        long cancelled = jobRepository.countByStatus(JobStatus.CANCELLED);

        distribution.put("OPEN", (int) created); // Grouping CREATED as OPEN
        distribution.put("IN_PROGRESS", (int) assigned);
        distribution.put("COMPLETED", (int) completed);
        distribution.put("CANCELLED", (int) cancelled);

        return distribution;
    }
}
