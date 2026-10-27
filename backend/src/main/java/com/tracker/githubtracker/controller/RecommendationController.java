package com.tracker.githubtracker.controller;

import com.tracker.githubtracker.dto.RepoSuggestionSummaryDTO;
import com.tracker.githubtracker.dto.SkillSummaryDTO;
import com.tracker.githubtracker.service.RepoRecommendationService;
import com.tracker.githubtracker.service.SkillAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
public class RecommendationController {

    private final SkillAnalysisService skillAnalysisService;
    private final RepoRecommendationService repoRecommendationService;

    public RecommendationController(SkillAnalysisService skillAnalysisService, RepoRecommendationService repoRecommendationService) {
        this.skillAnalysisService = skillAnalysisService;
        this.repoRecommendationService = repoRecommendationService;
    }

    /**
     * Suggested open-source repos to contribute to, based on detected skills.
     * GET http://localhost:8080/api/github/{username}/repo-suggestions
     */
    @GetMapping("/{username}/repo-suggestions")
    public ResponseEntity<RepoSuggestionSummaryDTO> getRepoSuggestions(@PathVariable String username) {
        SkillSummaryDTO skills = skillAnalysisService.getSkillSummary(username);
        return ResponseEntity.ok(repoRecommendationService.suggestRepositories(skills));
    }
}
