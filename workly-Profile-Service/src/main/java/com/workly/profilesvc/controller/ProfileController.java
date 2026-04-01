package com.workly.profilesvc.controller;

import com.workly.core.ApiResponse;
import com.workly.core.WorklyException;
import com.workly.profilesvc.domain.seeker.SkillSeekerProfile;
import com.workly.profilesvc.domain.worker.WorkerProfile;
import com.workly.profilesvc.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/worker")
    public ApiResponse<WorkerProfile> getWorkerProfile() {
        String mobileNumber = getAuthMobile();
        return profileService.getWorkerProfile(mobileNumber)
                .map(p -> ApiResponse.success(p, "Profile found"))
                .orElseGet(() -> ApiResponse.error("Worker profile not found"));
    }

    @PostMapping("/worker")
    public ApiResponse<WorkerProfile> createOrUpdateWorkerProfile(@RequestBody WorkerProfile profile) {
        profile.setMobileNumber(getAuthMobile());
        return ApiResponse.success(profileService.createOrUpdateWorkerProfile(profile), "Profile updated");
    }

    @GetMapping("/seeker")
    public ApiResponse<SkillSeekerProfile> getSeekerProfile() {
        String mobileNumber = getAuthMobile();
        return profileService.getSeekerProfile(mobileNumber)
                .map(p -> ApiResponse.success(p, "Profile found"))
                .orElse(ApiResponse.error("Skill seeker profile not found"));
    }

    @PostMapping("/seeker")
    public ApiResponse<SkillSeekerProfile> createOrUpdateSeekerProfile(@RequestBody SkillSeekerProfile profile) {
        profile.setMobileNumber(getAuthMobile());
        return ApiResponse.success(profileService.createOrUpdateSeekerProfile(profile), "Profile updated");
    }

    @PatchMapping("/worker/availability")
    public ApiResponse<Void> updateAvailability(@RequestParam boolean available) {
        profileService.updateAvailability(getAuthMobile(), available);
        return ApiResponse.success(null, "Availability updated");
    }

    @PostMapping("/device-token")
    public ApiResponse<Void> updateDeviceToken(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        if (token == null || token.isBlank()) throw WorklyException.badRequest("Device token is required");
        profileService.updateDeviceToken(getAuthMobile(), token);
        return ApiResponse.success(null, "Device token updated");
    }

    private String getAuthMobile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
