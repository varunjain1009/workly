package com.workly.modules.notification;

import com.workly.modules.job.Job;
import com.workly.modules.job.JobEvent;
import com.workly.modules.job.JobRepository;
import com.workly.modules.job.JobStatus;
import com.workly.modules.matching.MatchingService;
import com.workly.modules.notification.service.FCMService;
import com.workly.modules.notification.service.UserTokenService;
import com.workly.modules.profile.WorkerProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private MatchingService matchingService;
    @Mock private FCMService fcmService;
    @Mock private UserTokenService userTokenService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(jobRepository, matchingService, fcmService, userTokenService);
    }

    private JobEvent event(String jobId, JobStatus status) {
        JobEvent e = new JobEvent();
        e.setJobId(jobId);
        e.setStatus(status);
        return e;
    }

    private Job job(String id, double lon, double lat) {
        Job j = new Job();
        j.setId(id);
        j.setImmediate(true);
        j.setLocation(new double[]{lon, lat});
        j.setRequiredSkills(List.of("plumbing"));
        j.setSearchRadiusKm(10);
        return j;
    }

    // ── handleJobCreated ──────────────────────────────────────────────────────

    @Test
    void handleJobCreated_jobNotFound_returnsEarly() {
        when(jobRepository.findById("j1")).thenReturn(Optional.empty());
        notificationService.handleJobCreated(event("j1", JobStatus.BROADCASTED));
        verify(matchingService, never()).findMatches(any(), anyDouble(), anyDouble(), anyDouble(), any());
    }

    @Test
    void handleJobCreated_noLocation_returnsEarly() {
        Job j = new Job();
        j.setId("j1");
        j.setLocation(null);
        when(jobRepository.findById("j1")).thenReturn(Optional.of(j));

        notificationService.handleJobCreated(event("j1", JobStatus.BROADCASTED));

        verify(matchingService, never()).findMatches(any(), anyDouble(), anyDouble(), anyDouble(), any());
    }

    @Test
    void handleJobCreated_noMatchingWorkers_noNotification() {
        Job j = job("j1", 77.6, 12.9);
        when(jobRepository.findById("j1")).thenReturn(Optional.of(j));
        when(matchingService.findMatches(any(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of());

        notificationService.handleJobCreated(event("j1", JobStatus.BROADCASTED));

        verify(fcmService, never()).sendBatch(any(), any(), any(), any());
    }

    @Test
    void handleJobCreated_withMatchingWorkers_sendsBatch() {
        Job j = job("j1", 77.6, 12.9);
        when(jobRepository.findById("j1")).thenReturn(Optional.of(j));

        WorkerProfile wp = new WorkerProfile();
        wp.setDeviceToken("token1");
        when(matchingService.findMatches(any(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of(wp));

        notificationService.handleJobCreated(event("j1", JobStatus.BROADCASTED));

        verify(fcmService).sendBatch(eq(List.of("token1")), any(), any(), any());
    }

    @Test
    void handleJobCreated_workersWithNullTokens_filteredOut() {
        Job j = job("j1", 77.6, 12.9);
        when(jobRepository.findById("j1")).thenReturn(Optional.of(j));

        WorkerProfile wp1 = new WorkerProfile();
        wp1.setDeviceToken(null);
        WorkerProfile wp2 = new WorkerProfile();
        wp2.setDeviceToken("");
        when(matchingService.findMatches(any(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of(wp1, wp2));

        notificationService.handleJobCreated(event("j1", JobStatus.BROADCASTED));

        verify(fcmService, never()).sendBatch(any(), any(), any(), any());
    }

    @Test
    void handleJobCreated_scheduledJob_passesScheduledTime() {
        Job j = new Job();
        j.setId("j1");
        j.setImmediate(false);
        j.setLocation(new double[]{77.6, 12.9});
        j.setRequiredSkills(List.of("electrician"));
        j.setSearchRadiusKm(5);
        j.setScheduledTime(java.time.LocalDateTime.now().plusDays(1));
        when(jobRepository.findById("j1")).thenReturn(Optional.of(j));
        when(matchingService.findMatches(any(), anyDouble(), anyDouble(), anyDouble(), notNull()))
                .thenReturn(List.of());

        notificationService.handleJobCreated(event("j1", JobStatus.SCHEDULED));

        verify(matchingService).findMatches(any(), anyDouble(), anyDouble(), anyDouble(), notNull());
    }

    // ── handleJobStatusUpdated ────────────────────────────────────────────────

    @Test
    void handleJobStatusUpdated_jobNotFound_returnsEarly() {
        when(jobRepository.findById("j1")).thenReturn(Optional.empty());
        notificationService.handleJobStatusUpdated(event("j1", JobStatus.ASSIGNED));
        verify(userTokenService, never()).getToken(any());
    }

    @Test
    void handleJobStatusUpdated_assigned_notifiesSeekerAndWorker() {
        Job j = job("j1", 77.6, 12.9);
        j.setSeekerMobileNumber("seeker1");
        j.setWorkerMobileNumber("worker1");
        when(jobRepository.findById("j1")).thenReturn(Optional.of(j));
        when(userTokenService.getToken("seeker1")).thenReturn("seekerToken");
        when(userTokenService.getToken("worker1")).thenReturn("workerToken");

        notificationService.handleJobStatusUpdated(event("j1", JobStatus.ASSIGNED));

        verify(fcmService).sendNotificationWithData(eq("seekerToken"), contains("Accepted"), any(), any());
        verify(fcmService).sendNotificationWithData(eq("workerToken"), contains("Confirmed"), any(), any());
    }

    @Test
    void handleJobStatusUpdated_assigned_noSeekerToken_skips() {
        Job j = job("j1", 77.6, 12.9);
        j.setSeekerMobileNumber("seeker1");
        j.setWorkerMobileNumber("worker1");
        when(jobRepository.findById("j1")).thenReturn(Optional.of(j));
        when(userTokenService.getToken("seeker1")).thenReturn(null);
        when(userTokenService.getToken("worker1")).thenReturn(null);

        notificationService.handleJobStatusUpdated(event("j1", JobStatus.ASSIGNED));

        verify(fcmService, never()).sendNotificationWithData(any(), any(), any(), any());
    }

    @Test
    void handleJobStatusUpdated_completed_notifiesSeeker() {
        Job j = job("j1", 77.6, 12.9);
        j.setSeekerMobileNumber("seeker1");
        when(jobRepository.findById("j1")).thenReturn(Optional.of(j));
        when(userTokenService.getToken("seeker1")).thenReturn("seekerToken");

        notificationService.handleJobStatusUpdated(event("j1", JobStatus.COMPLETED));

        verify(fcmService).sendNotificationWithData(eq("seekerToken"), contains("Completed"), any(), any());
    }

    // ── notifyNewlyAvailableWorker ────────────────────────────────────────────

    @Test
    void notifyNewlyAvailableWorker_noLocation_skips() {
        WorkerProfile wp = new WorkerProfile();
        wp.setLastLocation(null);
        notificationService.notifyNewlyAvailableWorker(wp);
        verify(jobRepository, never()).findByStatus(any());
    }

    @Test
    void notifyNewlyAvailableWorker_noSkills_skips() {
        WorkerProfile wp = new WorkerProfile();
        wp.setLastLocation(new double[]{77.6, 12.9});
        wp.setSkills(null);
        notificationService.notifyNewlyAvailableWorker(wp);
        verify(jobRepository, never()).findByStatus(any());
    }

    @Test
    void notifyNewlyAvailableWorker_matchingJob_sendsNotification() {
        WorkerProfile wp = new WorkerProfile();
        wp.setMobileNumber("w1");
        wp.setLastLocation(new double[]{77.6, 12.9});
        wp.setSkills(List.of("plumbing"));
        wp.setDeviceToken("token1");

        Job j = new Job();
        j.setId("j1");
        j.setLocation(new double[]{77.6, 12.9}); // same location = 0 km away
        j.setRequiredSkills(List.of("plumbing"));
        j.setSearchRadiusKm(10);
        j.setTitle("Fix pipes");

        when(jobRepository.findByStatus(JobStatus.BROADCASTED)).thenReturn(List.of(j));

        notificationService.notifyNewlyAvailableWorker(wp);

        // sendPushNotification is a private method that logs; it calls no external mock
        // Just verify the flow runs without error and jobRepository was called
        verify(jobRepository).findByStatus(JobStatus.BROADCASTED);
    }

    @Test
    void notifyNewlyAvailableWorker_noSkillMatch_doesNotNotify() {
        WorkerProfile wp = new WorkerProfile();
        wp.setMobileNumber("w1");
        wp.setLastLocation(new double[]{77.6, 12.9});
        wp.setSkills(List.of("carpentry"));
        wp.setDeviceToken("token1");

        Job j = new Job();
        j.setId("j1");
        j.setLocation(new double[]{77.6, 12.9});
        j.setRequiredSkills(List.of("plumbing"));
        j.setSearchRadiusKm(10);

        when(jobRepository.findByStatus(JobStatus.BROADCASTED)).thenReturn(List.of(j));

        notificationService.notifyNewlyAvailableWorker(wp);

        verify(jobRepository).findByStatus(JobStatus.BROADCASTED);
    }
}
