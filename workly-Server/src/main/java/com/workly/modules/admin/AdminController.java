package com.workly.modules.admin;

import com.workly.core.ApiResponse;
import com.workly.modules.job.Job;
import com.workly.modules.profile.SkillSeekerProfile;
import com.workly.modules.profile.WorkerProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/seekers")
    public ApiResponse<Page<SkillSeekerProfile>> getSeekers(Pageable pageable) {
        return ApiResponse.success(adminService.getSeekers(pageable), "Seekers fetched successfully");
    }

    @GetMapping("/providers")
    public ApiResponse<Page<WorkerProfile>> getProviders(Pageable pageable) {
        return ApiResponse.success(adminService.getProviders(pageable), "Providers fetched successfully");
    }

    @GetMapping("/jobs")
    public ApiResponse<Page<Job>> getJobs(Pageable pageable) {
        return ApiResponse.success(adminService.getJobs(pageable), "Jobs fetched successfully");
    }
}
