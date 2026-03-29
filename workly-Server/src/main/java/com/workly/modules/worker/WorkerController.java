package com.workly.modules.worker;

import com.workly.core.ApiResponse;
import com.workly.modules.matching.MatchingService;
import com.workly.modules.profile.WorkerProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
@Slf4j
public class WorkerController {

    private final MatchingService matchingService;

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
                radius);

        log.info("Found {} matching workers", workers.size());
        log.debug("WorkerController: [EXIT] searchWorkers - Returning API Response encompassing {} worker matches.", workers.size());

        return ApiResponse.success(workers, "Workers found");
    }
}
