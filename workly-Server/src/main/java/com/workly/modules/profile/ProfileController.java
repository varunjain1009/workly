package com.workly.modules.profile;

import com.workly.core.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/worker")
    public ApiResponse<WorkerProfile> getWorkerProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        return profileService.getWorkerProfile(mobileNumber)
                .map(p -> ApiResponse.success(p, "Profile found"))
                .orElse(ApiResponse.error("Worker profile not found"));
    }

    @PostMapping("/worker")
    public ApiResponse<WorkerProfile> createOrUpdateWorkerProfile(@RequestBody WorkerProfile profile) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        profile.setMobileNumber(mobileNumber);
        return ApiResponse.success(profileService.createOrUpdateWorkerProfile(profile), "Profile updated");
    }

    @GetMapping("/seeker")
    public ApiResponse<SkillSeekerProfile> getSeekerProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        return profileService.getSeekerProfile(mobileNumber)
                .map(p -> ApiResponse.success(p, "Profile found"))
                .orElse(ApiResponse.error("Skill seeker profile not found"));
    }

    @PostMapping("/seeker")
    public ApiResponse<SkillSeekerProfile> createOrUpdateSeekerProfile(@RequestBody SkillSeekerProfile profile) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        profile.setMobileNumber(mobileNumber);
        return ApiResponse.success(profileService.createOrUpdateSeekerProfile(profile), "Profile updated");
    }

    @PatchMapping("/worker/availability")
    public ApiResponse<Void> updateAvailability(@RequestParam boolean available) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        profileService.updateAvailability(mobileNumber, available);
        return ApiResponse.success(null, "Availability updated");
    }

    @PostMapping("/device-token")
    public ApiResponse<Void> updateDeviceToken(@RequestBody java.util.Map<String, String> payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        String token = payload.get("token");
        profileService.updateDeviceToken(mobileNumber, token);
        return ApiResponse.success(null, "Device token updated");
    }
}
