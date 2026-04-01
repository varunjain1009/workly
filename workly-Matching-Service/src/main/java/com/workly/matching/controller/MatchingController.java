package com.workly.matching.controller;

import com.workly.core.ApiResponse;
import com.workly.matching.domain.worker.WorkerProfile;
import com.workly.matching.dto.MatchingRequest;
import com.workly.matching.service.MatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    /**
     * Find workers matching the given skills, location, and radius.
     * Used internally by the notification service and the main server.
     */
    @PostMapping("/workers")
    public ApiResponse<List<WorkerProfile>> findMatchingWorkers(@Valid @RequestBody MatchingRequest request) {
        log.debug("MatchingController: findMatchingWorkers - skills: {}, radius: {}km", request.getRequiredSkills(), request.getRadiusKm());
        List<WorkerProfile> matches = matchingService.findMatches(
                request.getRequiredSkills(),
                request.getLongitude(),
                request.getLatitude(),
                request.getRadiusKm(),
                request.getScheduledTimeMillis());
        return ApiResponse.success(matches, matches.size() + " workers found");
    }
}
