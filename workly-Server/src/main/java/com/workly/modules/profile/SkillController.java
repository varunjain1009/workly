package com.workly.modules.profile;

import com.workly.core.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

    private static final List<String> STANDARD_SKILLS = Arrays.asList(
            "Plumbing",
            "Electrical",
            "Carpentry",
            "Cleaning",
            "Painting",
            "Appliance Repair",
            "Moving",
            "Landscaping",
            "Roofing",
            "HVAC",
            "Pest Control",
            "Handyman",
            "IT Support",
            "Mounting",
            "Assembly"
    );

    @GetMapping("/autocomplete")
    public ApiResponse<List<String>> getSkillSuggestions(@RequestParam("query") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ApiResponse.success(STANDARD_SKILLS.subList(0, Math.min(5, STANDARD_SKILLS.size())), "Suggestions");
        }

        String lowerQuery = query.trim().toLowerCase();

        // 1. Exact or prefix match
        List<String> suggestions = STANDARD_SKILLS.stream()
                .filter(skill -> skill.toLowerCase().startsWith(lowerQuery) || skill.toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());

        // 2. Simple typo tolerance (Levenshtein-like simplified for single character differences)
        if (suggestions.isEmpty() && lowerQuery.length() >= 3) {
            suggestions = STANDARD_SKILLS.stream()
                    .filter(skill -> isCloseMatch(skill.toLowerCase(), lowerQuery))
                    .collect(Collectors.toList());
        }

        return ApiResponse.success(suggestions, "Suggestions fetched");
    }

    private boolean isCloseMatch(String target, String query) {
        if (Math.abs(target.length() - query.length()) > 2) return false;
        
        // Simple subset matching to catch transposed or missing characters roughly
        int matches = 0;
        for (char c : query.toCharArray()) {
            if (target.indexOf(c) >= 0) {
                matches++;
            }
        }
        return matches >= query.length() - 1; // Tollereate 1 letter error
    }
}
