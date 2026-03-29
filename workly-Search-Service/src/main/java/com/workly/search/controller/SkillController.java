package com.workly.search.controller;

import com.workly.search.service.AutocompleteService;
import com.workly.search.service.SkillSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
@Slf4j
public class SkillController {

    private final SkillSyncService skillSyncService;
    private final AutocompleteService autocompleteService;
    private final com.workly.search.repository.search.SkillSearchRepository skillSearchRepository;

    @PostMapping("/sync")
    public String syncSkills() {
        log.debug("SkillController: [ENTER] syncSkills");
        skillSyncService.syncAll();
        log.debug("SkillController: [EXIT] syncSkills - completed");
        return "Sync Completed";
    }

    @GetMapping("/autocomplete")
    public java.util.List<String> autocomplete(@RequestParam String query) {
        log.debug("SkillController: [ENTER] autocomplete - query: '{}'", query);
        java.util.List<String> results = autocompleteService.autocomplete(query);
        log.debug("SkillController: [EXIT] autocomplete - query: '{}', results: {}", query, results.size());
        return results;
    }

    @GetMapping("/{name}")
    public com.workly.search.model.SkillDocument getSkillByName(@PathVariable String name) {
        log.debug("SkillController: [ENTER] getSkillByName - name: {}", name);
        com.workly.search.model.SkillDocument doc = skillSearchRepository.findByCanonicalName(name)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + name));
        log.debug("SkillController: [EXIT] getSkillByName - found skill id: {}", doc.getId());
        return doc;
    }

    @GetMapping
    public java.util.List<com.workly.search.model.Skill> getAllSkills() {
        log.debug("SkillController: [ENTER] getAllSkills");
        java.util.List<com.workly.search.model.Skill> skills = skillSyncService.getAllSkills();
        log.debug("SkillController: [EXIT] getAllSkills - count: {}", skills.size());
        return skills;
    }

    @PostMapping("/{name}/aliases")
    public void addAliases(@PathVariable String name, @RequestBody java.util.List<String> aliases) {
        log.debug("SkillController: [ENTER] addAliases - skill: {}, aliases: {}", name, aliases);
        skillSyncService.addAliases(name, aliases);
        log.debug("SkillController: [EXIT] addAliases - skill: {} updated", name);
    }
}
