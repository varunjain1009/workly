package com.workly.modules.job;

import com.workly.modules.job.dto.JobDTO;
import com.workly.modules.profile.ProfileService;
import com.workly.modules.profile.WorkerProfile;
import com.workly.modules.promotion.PromotionService;
import com.workly.modules.promotion.PromotionValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobControllerUnitTest {

    @Mock private JobService jobService;
    @Mock private ProfileService profileService;
    @Mock private PromotionService promotionService;

    private JobController jobController;

    @BeforeEach
    void setUp() {
        jobController = new JobController(jobService, profileService, promotionService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("s1", null, List.of()));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Job job(String id) {
        Job j = new Job();
        j.setId(id);
        j.setStatus(JobStatus.BROADCASTED);
        return j;
    }

    @Test
    void getJobs_returnsSeeekerJobs() {
        when(jobService.getSeekerJobs("s1", null, 0, 50)).thenReturn(List.of(job("j1")));

        var result = jobController.getJobs(null, 0, 50);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
    }

    @Test
    void getAvailableJobs_returnsMatchingJobs() {
        when(jobService.getMatchingJobs("s1", 0, 20)).thenReturn(List.of(job("j1"), job("j2")));

        var result = jobController.getAvailableJobs(0, 20);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().size());
    }

    @Test
    void acceptJob_delegatesToService() {
        var result = jobController.acceptJob("j1");

        assertTrue(result.isSuccess());
        verify(jobService).acceptJob("j1", "s1");
    }

    @Test
    void completeJob_withValidOtp_succeeds() {
        var result = jobController.completeJob("j1", Map.of("otp", "1234"));

        assertTrue(result.isSuccess());
        verify(jobService).completeJob("j1", "1234", "s1");
    }

    @Test
    void completeJob_missingOtp_throws() {
        assertThrows(com.workly.core.WorklyException.class,
                () -> jobController.completeJob("j1", Map.of()));
    }

    @Test
    void completeJob_blankOtp_throws() {
        assertThrows(com.workly.core.WorklyException.class,
                () -> jobController.completeJob("j1", Map.of("otp", "  ")));
    }

    @Test
    void updateJob_delegatesToService() {
        Job updated = job("j1");
        when(jobService.updateJob(eq("j1"), any(), eq("s1"))).thenReturn(updated);

        var result = jobController.updateJob("j1", new JobDTO());

        assertTrue(result.isSuccess());
    }

    @Test
    void createJob_withPromoCode_appliesDiscount() {
        JobDTO dto = new JobDTO();
        dto.setTitle("Fix pipes");
        dto.setBudget(100.0);
        dto.setAppliedPromoCode("SAVE10");

        PromotionValidationResult promoResult = new PromotionValidationResult();
        promoResult.setCode("SAVE10");
        promoResult.setDiscountAmount(10.0);
        when(promotionService.validatePromotion("SAVE10", 100.0)).thenReturn(promoResult);

        Job created = job("j1");
        created.setTitle("Fix pipes");
        when(jobService.createJob(any())).thenReturn(created);

        var result = jobController.createJob(dto);

        assertTrue(result.isSuccess());
        verify(promotionService).validatePromotion("SAVE10", 100.0);
    }

    @Test
    void getTrackingLocation_assignedJob_returnsLocation() {
        Job j = job("j1");
        j.setSeekerMobileNumber("s1");
        j.setStatus(JobStatus.ASSIGNED);
        j.setWorkerMobileNumber("w1");
        when(jobService.getJobById("j1")).thenReturn(j);

        WorkerProfile wp = new WorkerProfile();
        wp.setLastLocation(new double[]{77.6, 12.9});
        when(profileService.getWorkerProfile("w1")).thenReturn(Optional.of(wp));

        var result = jobController.getTrackingLocation("j1");

        assertTrue(result.isSuccess());
        assertEquals(77.6, result.getData().getLongitude());
        assertEquals(12.9, result.getData().getLatitude());
    }

    @Test
    void getTrackingLocation_notOwner_throws() {
        Job j = job("j1");
        j.setSeekerMobileNumber("other");
        j.setStatus(JobStatus.ASSIGNED);
        when(jobService.getJobById("j1")).thenReturn(j);

        assertThrows(com.workly.core.WorklyException.class,
                () -> jobController.getTrackingLocation("j1"));
    }

    @Test
    void getTrackingLocation_notAssigned_throws() {
        Job j = job("j1");
        j.setSeekerMobileNumber("s1");
        j.setStatus(JobStatus.BROADCASTED);
        when(jobService.getJobById("j1")).thenReturn(j);

        assertThrows(com.workly.core.WorklyException.class,
                () -> jobController.getTrackingLocation("j1"));
    }

    @Test
    void toDto_jobWithWorkerAndLocation_populatesFields() {
        Job j = job("j1");
        j.setTitle("Plumbing");
        j.setRequiredSkills(List.of("plumbing"));
        j.setWorkerMobileNumber("w1");
        j.setLocation(new double[]{77.6, 12.9});
        j.setScheduledTime(java.time.LocalDateTime.now().plusDays(1));

        WorkerProfile wp = new WorkerProfile();
        wp.setName("John");
        when(profileService.getWorkerProfile("w1")).thenReturn(Optional.of(wp));
        when(jobService.getSeekerJobs("s1", null, 0, 50)).thenReturn(List.of(j));

        var result = jobController.getJobs(null, 0, 50);

        assertEquals("Plumbing", result.getData().get(0).getTitle());
        assertEquals("John", result.getData().get(0).getWorkerName());
        assertEquals("plumbing", result.getData().get(0).getRequiredSkill());
    }
}
