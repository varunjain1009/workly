package com.workly.modules.profile;

import com.workly.core.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/worker")
    public ApiResponse<WorkerProfile> getWorkerProfile() {
        log.debug("ProfileController: [ENTER] getWorkerProfile");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        return profileService.getWorkerProfile(mobileNumber)
                .map(p -> {
                    log.debug("ProfileController: [EXIT] getWorkerProfile - Mapped to existing data model.");
                    return ApiResponse.success(p, "Profile found");
                })
                .orElseGet(() -> {
                    log.debug("ProfileController: [EXIT] getWorkerProfile - No worker profile found.");
                    return ApiResponse.error("Worker profile not found");
                });
    }

    @PostMapping("/worker")
    public ApiResponse<WorkerProfile> createOrUpdateWorkerProfile(@RequestBody WorkerProfile profile) {
        log.debug("ProfileController: [ENTER] createOrUpdateWorkerProfile");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        profile.setMobileNumber(mobileNumber);
        WorkerProfile savedProfile = profileService.createOrUpdateWorkerProfile(profile);
        log.debug("ProfileController: [EXIT] createOrUpdateWorkerProfile - Resolving changes.");
        return ApiResponse.success(savedProfile, "Profile updated");
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
        log.debug("ProfileController: [ENTER] updateAvailability - Toggle status: {}", available);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        profileService.updateAvailability(mobileNumber, available);
        log.debug("ProfileController: [EXIT] updateAvailability - Toggle sequence complete.");
        return ApiResponse.success(null, "Availability updated");
    }

    @PostMapping("/device-token")
    public ApiResponse<Void> updateDeviceToken(@RequestBody java.util.Map<String, String> payload) {
        log.debug("ProfileController: [ENTER] updateDeviceToken - Handling JWT Map injection");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        String token = payload.get("token");
        if (token == null || token.isBlank()) {
            log.debug("ProfileController: [FAIL] updateDeviceToken - Token mapping is empty or null");
            throw com.workly.core.WorklyException.badRequest("Device token is required");
        }
        profileService.updateDeviceToken(mobileNumber, token);
        log.debug("ProfileController: [EXIT] updateDeviceToken - Processed success trace.");
        return ApiResponse.success(null, "Device token updated");
    }
}
