package com.workly.search.controller;

import com.workly.search.service.NormalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/normalization")
@RequiredArgsConstructor
public class NormalizationController {

    private final NormalizationService normalizationService;

    @PostMapping("/skills")
    public List<String> normalizeSkills(@RequestBody List<String> skills) {
        return normalizationService.normalizeSkills(skills);
    }
}
