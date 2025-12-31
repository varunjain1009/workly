package com.workly.search.controller;

import com.workly.search.service.AutocompleteService;
import com.workly.search.service.SkillSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillSyncService skillSyncService;
    private final AutocompleteService autocompleteService;
    private final com.workly.search.repository.search.SkillSearchRepository skillSearchRepository;

    @PostMapping("/sync")
    public String syncSkills() {
        skillSyncService.syncAll();
        return "Sync Completed";
    }

    @GetMapping("/autocomplete")
    public java.util.List<String> autocomplete(@RequestParam String query) {
        return autocompleteService.autocomplete(query);
    }

    @GetMapping("/{name}")
    public com.workly.search.model.SkillDocument getSkillByName(@PathVariable String name) {
        return skillSearchRepository.findByCanonicalName(name)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + name));
    }
}
