package com.workly.modules.job;

import com.workly.core.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ApiResponse<com.workly.modules.job.dto.JobDTO> createJob(
            @RequestBody com.workly.modules.job.dto.JobDTO jobDto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        log.info("Received request to create job for mobile: {}", mobileNumber);

        Job job = new Job();
        job.setSeekerMobileNumber(mobileNumber);
        job.setTitle(jobDto.getTitle());
        job.setDescription(jobDto.getDescription());
        job.setRequiredSkills(jobDto.getRequiredSkills());
        if (jobDto.getRequiredSkill() != null
                && (job.getRequiredSkills() == null || job.getRequiredSkills().isEmpty())) {
            job.setRequiredSkills(List.of(jobDto.getRequiredSkill()));
        }
        job.setBudget(jobDto.getBudget() != null ? jobDto.getBudget() : 0.0);
        job.setStatus(jobDto.getStatus());
        job.setImmediate(jobDto.isImmediate());
        job.setJobType(jobDto.getJobType());
        job.setAssignmentMode(jobDto.getAssignmentMode());
        if (jobDto.getPreferredDateTime() > 0) {
            job.setScheduledTime(java.time.Instant.ofEpochMilli(jobDto.getPreferredDateTime())
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        }

        if (jobDto.getLocation() != null) {
            job.setAddress(jobDto.getLocation().getAddress());
            job.setLocation(new double[] { jobDto.getLocation().getLongitude(), jobDto.getLocation().getLatitude() });
        }
        job.setSearchRadiusKm(jobDto.getSearchRadiusKm());

        Job createdJob = jobService.createJob(job);
        log.info("Job created successfully with ID: {} for mobile: {}", createdJob.getId(), mobileNumber);
        return ApiResponse.success(toDto(createdJob), "Job created successfully");
    }

    @GetMapping
    public ApiResponse<List<com.workly.modules.job.dto.JobDTO>> getJobs() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        log.info("Received request to fetch all jobs for mobile: {}", mobileNumber);
        List<Job> jobs = jobService.getSeekerJobs(mobileNumber);
        log.info("Fetched {} jobs for mobile: {}", jobs.size(), mobileNumber);
        return ApiResponse.success(jobs.stream().map(this::toDto).toList(), "Jobs retrieved");
    }

    @GetMapping("/seeker")
    public ApiResponse<List<com.workly.modules.job.dto.JobDTO>> getSeekerJobs() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        return ApiResponse.success(jobService.getSeekerJobs(mobileNumber).stream().map(this::toDto).toList(),
                "Jobs retrieved");
    }

    @GetMapping("/worker")
    public ApiResponse<List<com.workly.modules.job.dto.JobDTO>> getWorkerJobs() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        return ApiResponse.success(jobService.getWorkerJobs(mobileNumber).stream().map(this::toDto).toList(),
                "Jobs retrieved");
    }

    @GetMapping("/available")
    public ApiResponse<List<com.workly.modules.job.dto.JobDTO>> getAvailableJobs() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        return ApiResponse.success(jobService.getMatchingJobs(mobileNumber).stream().map(this::toDto).toList(),
                "Available jobs retrieved");
    }

    @PostMapping("/{jobId}/accept")
    public ApiResponse<Void> acceptJob(@PathVariable String jobId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        jobService.acceptJob(jobId, mobileNumber);
        return ApiResponse.success(null, "Job accepted successfully");
    }

    @PostMapping("/{jobId}/complete")
    public ApiResponse<Void> completeJob(@PathVariable String jobId,
            @RequestBody java.util.Map<String, String> payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        String otp = payload.get("otp");
        jobService.completeJob(jobId, otp, mobileNumber);
        return ApiResponse.success(null, "Job completed successfully");
    }

    @PatchMapping("/{jobId}/status")
    public ApiResponse<com.workly.modules.job.dto.JobDTO> updateStatus(@PathVariable String jobId,
            @RequestParam JobStatus status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        return ApiResponse.success(toDto(jobService.updateJobStatus(jobId, status, mobileNumber)), "Status updated");
    }

    @PutMapping("/{jobId}")
    public ApiResponse<com.workly.modules.job.dto.JobDTO> updateJob(@PathVariable String jobId,
            @RequestBody com.workly.modules.job.dto.JobDTO jobDto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();

        Job job = new Job();
        if (jobDto.getPreferredDateTime() > 0) {
            job.setScheduledTime(java.time.Instant.ofEpochMilli(jobDto.getPreferredDateTime())
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        }
        return ApiResponse.success(toDto(jobService.updateJob(jobId, job, mobileNumber)), "Job updated successfully");
    }

    private com.workly.modules.job.dto.JobDTO toDto(Job job) {
        com.workly.modules.job.dto.JobDTO dto = new com.workly.modules.job.dto.JobDTO();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setDescription(job.getDescription());
        dto.setRequiredSkills(job.getRequiredSkills());
        if (job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty()) {
            dto.setRequiredSkill(job.getRequiredSkills().get(0));
        }
        dto.setBudget(job.getBudget());
        dto.setStatus(job.getStatus());
        dto.setImmediate(job.isImmediate());
        dto.setJobType(job.getJobType());
        dto.setAssignmentMode(job.getAssignmentMode());
        dto.setSearchRadiusKm(job.getSearchRadiusKm());
        if (job.getScheduledTime() != null) {
            dto.setPreferredDateTime(
                    job.getScheduledTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }

        com.workly.modules.job.dto.LocationDTO locationDto = new com.workly.modules.job.dto.LocationDTO();
        locationDto.setAddress(job.getAddress());
        if (job.getLocation() != null && job.getLocation().length >= 2) {
            locationDto.setLongitude(job.getLocation()[0]);
            locationDto.setLatitude(job.getLocation()[1]);
        }
        dto.setLocation(locationDto);

        return dto;
    }
}
