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

    @PostMapping("/sync")
    public String syncSkills() {
        skillSyncService.syncAll();
        return "Sync Completed";
    }

    @GetMapping("/autocomplete")
    public java.util.List<String> autocomplete(@RequestParam String query) {
        return autocompleteService.autocomplete(query);
    }
}
