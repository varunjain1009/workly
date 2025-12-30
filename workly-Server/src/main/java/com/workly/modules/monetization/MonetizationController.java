package com.workly.modules.monetization;

import com.workly.core.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/monetization")
@RequiredArgsConstructor
public class MonetizationController {

    private final MonetizationService monetizationService;

    @GetMapping("/status")
    public ApiResponse<Boolean> getSubscriptionStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        return ApiResponse.success(monetizationService.isUserAuthorized(mobileNumber), "Status retrieved");
    }

    @PostMapping("/subscribe")
    public ApiResponse<Subscription> subscribe(@RequestBody SubscriptionRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        return ApiResponse.success(
                monetizationService.subscribe(mobileNumber, request.getPlanType(), request.getDurationDays()),
                "Subscription successful");
    }

    @Data
    public static class SubscriptionRequest {
        private String planType;
        private int durationDays;
    }
}
