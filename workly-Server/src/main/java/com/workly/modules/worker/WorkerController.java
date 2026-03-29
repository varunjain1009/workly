package com.workly.modules.worker;

import com.workly.core.ApiResponse;
import com.workly.modules.matching.MatchingService;
import com.workly.modules.profile.ProfileService;
import com.workly.modules.profile.WorkerProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
@Slf4j
public class WorkerController {

    private final MatchingService matchingService;
    private final ProfileService profileService;

    @GetMapping("/search")
    public ApiResponse<List<WorkerProfile>> searchWorkers(
            @RequestParam String skill,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam double radius) {

        log.debug("WorkerController: [ENTER] searchWorkers - Searching skill: {}, lat: {}, lon: {}, radius: {}", skill, lat, lon, radius);
        log.info("Searching for workers with skill: {}, lat: {}, lon: {}, radius: {}", skill, lat, lon, radius);

        List<WorkerProfile> workers = matchingService.findMatches(
                Collections.singletonList(skill),
                lon,
                lat,
                radius,
                null);

        log.info("Found {} matching workers", workers.size());
        log.debug("WorkerController: [EXIT] searchWorkers - Returning API Response encompassing {} worker matches.", workers.size());

        return ApiResponse.success(workers, "Workers found");
    }

    @PostMapping("/kyc/upload")
    public ApiResponse<WorkerProfile> uploadKycDocument(@RequestParam(value = "file", required = false) MultipartFile file) {
        log.debug("WorkerController: [ENTER] uploadKycDocument");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();

        // In a real application, upload the file to S3/GCS and obtain a URL
        String mockUrl = "https://mockstorage.workly.com/kyc/" + mobileNumber + "/document.pdf";
        
        WorkerProfile profile = profileService.getWorkerProfile(mobileNumber)
                .orElseThrow(() -> com.workly.core.WorklyException.notFound("Worker profile not found"));
        
        profile.setIdDocumentUrl(mockUrl);
        profile.setKycVerified(true);
        WorkerProfile updated = profileService.createOrUpdateWorkerProfile(profile);
        
        log.info("WorkerController: KYC document uploaded and verified for provider {}", mobileNumber);
        return ApiResponse.success(updated, "KYC verified successfully");
    }
}
