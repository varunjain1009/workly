package com.workly.modules.analytics;

import com.workly.modules.analytics.AnalyticsController.DashboardStats;
import com.workly.modules.analytics.AnalyticsController.DataPoint;
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
        log.debug("AnalyticsService: [ENTER] getDashboardStats - Aggregating platform-wide metrics");
        log.info("Fetching dashboard stats");

        long totalSeekers = seekerRepository.count();
        long totalWorkers = workerRepository.count();
        long totalUsers = totalSeekers + totalWorkers;
        log.debug("AnalyticsService: User counts - seekers: {}, workers: {}, total: {}", totalSeekers, totalWorkers, totalUsers);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long newSeekersToday = seekerRepository.countByCreatedAtAfter(startOfDay);
        long newWorkersToday = workerRepository.countByCreatedAtAfter(startOfDay);
        long newUsersToday = newSeekersToday + newWorkersToday;
        log.debug("AnalyticsService: Today's signups - seekers: {}, workers: {}, total: {}", newSeekersToday, newWorkersToday, newUsersToday);

        long activeJobs = jobRepository.countByStatusIn(List.of(
                JobStatus.CREATED,
                JobStatus.SCHEDULED,
                JobStatus.BROADCASTED,
                JobStatus.ASSIGNED,
                JobStatus.PENDING_ACCEPTANCE));

        long completedJobs = jobRepository.countByStatus(JobStatus.COMPLETED);
        log.debug("AnalyticsService: Job metrics - active: {}, completed: {}", activeJobs, completedJobs);

        Double revenueCalc = jobRepository.sumBudgetByStatus(JobStatus.COMPLETED);
        double revenue = revenueCalc != null ? revenueCalc : 0.0;
        log.debug("AnalyticsService: Revenue aggregation: {}", revenue);

        DashboardStats stats = DashboardStats.builder()
                .totalUsers(totalUsers)
                .totalSeekers(totalSeekers)
                .totalWorkers(totalWorkers)
                .newUsersToday(newUsersToday)
                .activeJobs(activeJobs)
                .completedJobs(completedJobs)
                .revenue(revenue)
                .userGrowth(getUserGrowth())
                .jobStatusDistribution(getJobStatusDistribution())
                .build();
        log.debug("AnalyticsService: [EXIT] getDashboardStats - Stats assembled");
        return stats;
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
