package com.workly.modules.admin;

import com.workly.core.ApiResponse;
import com.workly.modules.job.Job;
import com.workly.modules.profile.SkillSeekerProfile;
import com.workly.modules.profile.WorkerProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/seekers")
    public ApiResponse<Page<SkillSeekerProfile>> getSeekers(Pageable pageable) {
        log.debug("AdminController: [ENTER] getSeekers - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<SkillSeekerProfile> result = adminService.getSeekers(pageable);
        log.debug("AdminController: [EXIT] getSeekers - returned {} of {} total", result.getNumberOfElements(), result.getTotalElements());
        return ApiResponse.success(result, "Seekers fetched successfully");
    }

    @GetMapping("/providers")
    public ApiResponse<Page<WorkerProfile>> getProviders(Pageable pageable) {
        log.debug("AdminController: [ENTER] getProviders - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<WorkerProfile> result = adminService.getProviders(pageable);
        log.debug("AdminController: [EXIT] getProviders - returned {} of {} total", result.getNumberOfElements(), result.getTotalElements());
        return ApiResponse.success(result, "Providers fetched successfully");
    }

    @GetMapping("/jobs")
    public ApiResponse<Page<Job>> getJobs(Pageable pageable) {
        log.debug("AdminController: [ENTER] getJobs - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Job> result = adminService.getJobs(pageable);
        log.debug("AdminController: [EXIT] getJobs - returned {} of {} total", result.getNumberOfElements(), result.getTotalElements());
        return ApiResponse.success(result, "Jobs fetched successfully");
    }
}
