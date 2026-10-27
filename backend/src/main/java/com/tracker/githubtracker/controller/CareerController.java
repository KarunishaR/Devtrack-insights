package com.tracker.githubtracker.controller;

import com.tracker.githubtracker.dto.CareerSuggestionDTO;
import com.tracker.githubtracker.dto.SkillSummaryDTO;
import com.tracker.githubtracker.service.CareerAdvisorService;
import com.tracker.githubtracker.service.SkillAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
public class CareerController {

    private final SkillAnalysisService skillAnalysisService;
    private final CareerAdvisorService careerAdvisorService;

    public CareerController(SkillAnalysisService skillAnalysisService, CareerAdvisorService careerAdvisorService) {
        this.skillAnalysisService = skillAnalysisService;
        this.careerAdvisorService = careerAdvisorService;
    }

    /**
     * AI-generated job-role eligibility + improvement suggestions, powered by Gemini.
     * GET http://localhost:8080/api/github/{username}/career-suggestions
     */
    @GetMapping("/{username}/career-suggestions")
    public ResponseEntity<CareerSuggestionDTO> getCareerSuggestions(@PathVariable String username) {
        SkillSummaryDTO skills = skillAnalysisService.getSkillSummary(username);
        return ResponseEntity.ok(careerAdvisorService.getCareerSuggestions(skills));
    }
}
