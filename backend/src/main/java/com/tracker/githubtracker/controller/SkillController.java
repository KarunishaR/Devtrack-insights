package com.tracker.githubtracker.controller;

import com.tracker.githubtracker.dto.SkillSummaryDTO;
import com.tracker.githubtracker.service.SkillAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
public class SkillController {

    private final SkillAnalysisService skillAnalysisService;

    public SkillController(SkillAnalysisService skillAnalysisService) {
        this.skillAnalysisService = skillAnalysisService;
    }

    /**
     * Detected tech stack / skill set based on repo languages.
     * GET http://localhost:8080/api/github/{username}/skills
     */
    @GetMapping("/{username}/skills")
    public ResponseEntity<SkillSummaryDTO> getSkills(@PathVariable String username) {
        return ResponseEntity.ok(skillAnalysisService.getSkillSummary(username));
    }
}
