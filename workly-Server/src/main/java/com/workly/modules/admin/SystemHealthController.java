package com.workly.modules.admin;

import com.workly.core.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class SystemHealthController {

    private final SystemHealthService systemHealthService;

    @GetMapping("/system-health")
    public ApiResponse<SystemHealthService.SystemHealthReport> getSystemHealth() {
        log.debug("SystemHealthController: [ENTER] getSystemHealth");
        SystemHealthService.SystemHealthReport report = systemHealthService.getHealth();
        log.debug("SystemHealthController: [EXIT] getSystemHealth");
        return ApiResponse.success(report, "System health fetched successfully");
    }
}
