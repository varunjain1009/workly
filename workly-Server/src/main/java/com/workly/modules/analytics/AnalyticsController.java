package com.workly.modules.analytics;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        log.debug("AnalyticsController: [ENTER] getDashboardStats");
        DashboardStats stats = analyticsService.getDashboardStats();
        log.debug("AnalyticsController: [EXIT] getDashboardStats - totalUsers: {}, activeJobs: {}, completedJobs: {}",
                stats.getTotalUsers(), stats.getActiveJobs(), stats.getCompletedJobs());
        return ResponseEntity.ok(stats);
    }

    @Data
    @Builder
    public static class DashboardStats {
        private long totalUsers;
        private long totalSeekers;
        private long totalWorkers;
        private long newUsersToday;
        private long activeJobs;
        private long completedJobs;
        private double revenue;
        private List<DataPoint> userGrowth;
        private Map<String, Integer> jobStatusDistribution;
    }

    @Data
    @RequiredArgsConstructor
    public static class DataPoint {
        private final String label;
        private final int value;
    }
}
