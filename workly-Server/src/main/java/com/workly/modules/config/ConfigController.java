package com.workly.modules.config;

import com.workly.core.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    @Value("${custom.location.update-interval-minutes:60}")
    private int locationUpdateIntervalMinutes;

    @Value("${custom.auth.otp.resend-delay-seconds:300}")
    private int otpResendDelaySeconds;

    @Value("${custom.app.debug-enabled:true}")
    private boolean debugEnabled;

    @Value("${custom.job.max-radius-km:50}")
    private int jobMaxRadiusKm;

    @Value("${custom.job.min-advance-hours:2}")
    private int jobMinAdvanceHours;

    @Value("${custom.assignment.mode:FIRST_ACCEPT}")
    private String assignmentMode;

    @Value("${custom.monetisation.enabled:false}")
    private boolean monetisationEnabled;

    @Value("${custom.monetisation.model:PER_JOB}")
    private String monetisationModel;

    @Value("${custom.monetisation.allow-browse-without-payment:true}")
    private boolean allowBrowseWithoutPayment;

    @Value("${custom.chat.url:ws://10.0.2.2:8082/ws/chat}")
    private String chatUrl;

    @org.springframework.beans.factory.annotation.Autowired
    private ConfigSyncService configSyncService;

    @GetMapping("/public")
    public ApiResponse<Map<String, Object>> getPublicConfig() {
        return ApiResponse.success(Map.of(
                "locationUpdateIntervalMinutes", locationUpdateIntervalMinutes,
                "otpResendDelaySeconds", otpResendDelaySeconds,
                "debugEnabled", debugEnabled,
                "jobMaxRadiusKm", jobMaxRadiusKm,
                "jobMinAdvanceHours", jobMinAdvanceHours,
                "assignmentMode", assignmentMode,
                "monetisation", Map.of(
                        "enabled", monetisationEnabled,
                        "model", monetisationModel,
                        "allowBrowseWithoutPayment", allowBrowseWithoutPayment),
                "chatUrl", chatUrl), "Config fetched successfully");
    }

    @org.springframework.web.bind.annotation.PostMapping("/sync")
    public ApiResponse<Void> syncConfig(
            @org.springframework.web.bind.annotation.RequestBody Map<String, String> request) {
        String mobileNumber = request.get("mobileNumber");
        if (mobileNumber != null) {
            configSyncService.markAppAsSynced(mobileNumber);
        }
        return ApiResponse.success(null, "Sync acknowledged");
    }
}
