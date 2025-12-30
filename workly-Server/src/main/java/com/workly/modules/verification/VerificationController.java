package com.workly.modules.verification;

import com.workly.core.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final JobVerificationService verificationService;

    @PostMapping("/complete")
    public ApiResponse<JobCompletion> completeJob(@RequestBody VerificationRequest request) {
        return ApiResponse.success(
                verificationService.verifyAndCompleteJob(request.getJobId(), request.getOtp()),
                "Job verified and completed");
    }

    @Data
    public static class VerificationRequest {
        private String jobId;
        private String otp;
    }
}
