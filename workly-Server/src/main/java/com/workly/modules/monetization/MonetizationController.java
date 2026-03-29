package com.workly.modules.monetization;

import com.workly.core.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/monetization")
@RequiredArgsConstructor
@Slf4j
public class MonetizationController {

    private final MonetizationService monetizationService;

    @GetMapping("/status")
    public ApiResponse<Boolean> getSubscriptionStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        log.debug("MonetizationController: [ENTER] getSubscriptionStatus - mobile: {}", mobileNumber);
        boolean authorized = monetizationService.isUserAuthorized(mobileNumber);
        log.debug("MonetizationController: [EXIT] getSubscriptionStatus - mobile: {}, authorized: {}", mobileNumber, authorized);
        return ApiResponse.success(authorized, "Status retrieved");
    }

    @PostMapping("/subscribe")
    public ApiResponse<Subscription> subscribe(@RequestBody SubscriptionRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        log.debug("MonetizationController: [ENTER] subscribe - mobile: {}, plan: {}, days: {}",
                mobileNumber, request.getPlanType(), request.getDurationDays());
        Subscription sub = monetizationService.subscribe(mobileNumber, request.getPlanType(), request.getDurationDays());
        log.debug("MonetizationController: [EXIT] subscribe - mobile: {}, subscriptionId: {}", mobileNumber, sub.getId());
        return ApiResponse.success(sub, "Subscription successful");
    }

    @Data
    public static class SubscriptionRequest {
        private String planType;
        private int durationDays;
    }
}
