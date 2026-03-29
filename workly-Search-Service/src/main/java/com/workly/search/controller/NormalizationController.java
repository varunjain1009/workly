package com.workly.search.controller;

import com.workly.search.service.NormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/normalization")
@RequiredArgsConstructor
@Slf4j
public class NormalizationController {

    private final NormalizationService normalizationService;

    @PostMapping("/skills")
    public List<String> normalizeSkills(@RequestBody List<String> skills) {
        log.debug("NormalizationController: [ENTER] normalizeSkills - input count: {}", skills.size());
        List<String> normalized = normalizationService.normalizeSkills(skills);
        log.debug("NormalizationController: [EXIT] normalizeSkills - output count: {}", normalized.size());
        return normalized;
    }
}
